# server.py
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import joblib, json, os, copy
import numpy as np
from typing import Dict, Any, List

# -------------------------------------------------------------------
# Load model + schema
# -------------------------------------------------------------------
MODEL_PATH = os.getenv("MODEL_PATH", "pipeline.joblib")
SCHEMA_PATH = os.getenv("SCHEMA_PATH", "feature_schema.json")

try:
    pipeline = joblib.load(MODEL_PATH)
except Exception as e:
    raise RuntimeError(f"Failed to load model at {MODEL_PATH}: {e}")

try:
    with open(SCHEMA_PATH, "r") as f:
        schema = json.load(f)
    FEATURE_ORDER: List[str] = schema.get("features") or [
        "age", "height", "weight", "bmi", "goal_type",
        "workouts_per_week", "calories_avg", "equipment"
    ]
except Exception as e:
    raise RuntimeError(f"Failed to load schema at {SCHEMA_PATH}: {e}")

# -------------------------------------------------------------------
# Workout template (you can move this to Firestore later)
# -------------------------------------------------------------------
BASE_PLAN = {
    "id": "hypertrophy_upper_lower_v1",
    "microcycle_days": 4,
    "exercises": [
        {"day": 1, "block": "Upper A", "name": "Flat Dumbbell Press", "sets": 4, "reps": "8–10", "tempo": "2-0-2", "rest_sec": 90},
        {"day": 1, "block": "Upper A", "name": "Lat Pulldown",        "sets": 4, "reps": "10–12", "tempo": "2-1-2", "rest_sec": 90},
        {"day": 1, "block": "Core",     "name": "Plank",               "sets": 3, "reps": "45s hold", "tempo": "-", "rest_sec": 60},
        {"day": 2, "block": "Lower A",  "name": "Back Squat",          "sets": 4, "reps": "6–8",  "tempo": "3-1-1", "rest_sec": 120},
        {"day": 3, "block": "Upper B",  "name": "Incline Push-up",     "sets": 3, "reps": "12–15","tempo": "2-0-2", "rest_sec": 60},
        {"day": 4, "block": "Lower B",  "name": "Romanian Deadlift",   "sets": 3, "reps": "8–10", "tempo": "3-1-1", "rest_sec": 120},
    ],
    "scaling": {
        "intensity": {"low": -20, "medium": 0, "high": 10},  # % for time-based work
        "volume":    {"low": -1,  "medium": 0, "high": 1},   # sets delta
    },
    "subs_no_equipment": {
        "Flat Dumbbell Press": "Push-up",
        "Lat Pulldown": "Inverted Row",
        "Back Squat": "Goblet Squat",
        "Romanian Deadlift": "Hip Hinge with Backpack",
    },
}

# -------------------------------------------------------------------
# Feature adapter (maps your 8 inputs to whatever the model expects)
# -------------------------------------------------------------------
# App → model alternate names
ALT_KEYS: Dict[str, List[str]] = {
    "height_cm": ["height"],
    "weight_kg": ["weight"],
    "daily_calories": ["calories_avg"],
    "bmi": ["bmi"],
    "goal_type": ["goal_type"],
    "workouts_per_week": ["workouts_per_week"],
    "equipment": ["equipment"],
    "age": ["age"],
}
DEFAULTS: Dict[str, Any] = {}  # numbers → 0; strings → ""; *_missing handled below

def _coalesce(src: Dict[str, Any], key: str, alts: List[str]):
    if key in src:
        return src[key]
    for a in alts:
        if a in src:
            return src[a]
    return None

def normalize_features(in_feats: Dict[str, Any], feature_order: List[str]) -> List[Any]:
    """
    Accepts your simple 8-field dict and returns a row ordered by FEATURE_ORDER,
    filling everything else with safe defaults. Also computes BMI if needed,
    coerces goal_type/equipment as required, and sets *_missing flags.
    """
    norm: Dict[str, Any] = {}

    # 1) Keep any keys already in schema
    for k in in_feats:
        if k in feature_order:
            norm[k] = in_feats[k]

    # 2) Alternate-key mapping (e.g., height -> height_cm)
    for target, alts in ALT_KEYS.items():
        if target not in norm:
            val = _coalesce(in_feats, target, alts)
            if val is not None:
                norm[target] = val

    # 3) Derive BMI if needed
    if "bmi" in feature_order and "bmi" not in norm:
        h_cm = norm.get("height_cm") or norm.get("height")
        w_kg = norm.get("weight_kg") or norm.get("weight")
        try:
            h = float(h_cm) if h_cm is not None else None
            w = float(w_kg) if w_kg is not None else None
            if h and h > 0 and w and w > 0:
                norm["bmi"] = w / ((h / 100.0) ** 2)
        except Exception:
            pass

    # 4) Clean up categorical/int
    if "equipment" in norm and isinstance(norm["equipment"], str):
        norm["equipment"] = norm["equipment"].lower()
    if "goal_type" in norm and isinstance(norm["goal_type"], str):
        s = norm["goal_type"].lower()
        norm["goal_type"] = 0 if s in ["fatloss", "weight_loss", "weight loss", "cut"] \
            else 1 if s in ["hypertrophy", "muscle", "bulk"] \
            else 2

    # 5) Build final ordered row
    row: List[Any] = []
    for f in feature_order:
        if f in norm and norm[f] is not None:
            row.append(norm[f])
        elif f.endswith("_missing"):
            base = f.replace("_missing", "")
            row.append(0 if base in norm and norm.get(base) is not None else 1)
        else:
            dv = DEFAULTS.get(f, 0 if not isinstance(f, str) else "")
            row.append(dv)
    return row

# -------------------------------------------------------------------
# API models
# -------------------------------------------------------------------
class PredictIn(BaseModel):
    user_id: str
    features: Dict[str, Any]   # your app sends flat fields (or nested; both ok)

class PredictOut(BaseModel):
    prediction: float
    plan_id: str
    microcycle_days: int
    exercises: list
    notes: str

# -------------------------------------------------------------------
# Helpers
# -------------------------------------------------------------------
def intensity_band(score: float) -> str:
    # if your model outputs not in [0,1], this still bands reasonably
    if score >= 0.8: return "high"
    if score >= 0.5: return "medium"
    return "low"

def scale_exercise(ex: Dict[str, Any], vol_delta: int, int_pct: int) -> Dict[str, Any]:
    e = copy.deepcopy(ex)
    # Volume (sets)
    if isinstance(e.get("sets"), int):
        e["sets"] = max(1, e["sets"] + vol_delta)
    # Time-based intensity scaling (e.g., "45s hold")
    reps = str(e.get("reps", ""))
    if "s" in reps:
        try:
            secs = int(reps.replace("s hold", "").replace("s", "").strip())
            secs = max(20, int(secs * (1 + int_pct / 100)))
            e["reps"] = f"{secs}s hold"
        except Exception:
            pass
    return e

# -------------------------------------------------------------------
# FastAPI app
# -------------------------------------------------------------------
app = FastAPI()

@app.get("/")
def root():
    return {"ok": True}

@app.get("/health")
def health():
    return {"status": "ok"}

@app.post("/predict", response_model=PredictOut)
def predict(p: PredictIn):
    try:
        # Allow either flat features or nested under key "features" (we already accept flat via Pydantic)
        in_feats = p.features or {}
        # Normalize to the model's schema
        row = [normalize_features(in_feats, FEATURE_ORDER)]
        x = np.array(row, dtype=object)

        # Predict
        score_raw = pipeline.predict(x)[0]
        try:
            score = float(score_raw)
        except Exception:
            # fallback if predict returns array-like
            score = float(np.array(score_raw).item())

        # Build base plan
        plan = copy.deepcopy(BASE_PLAN)

        # Equipment substitutions
        equipment = str(in_feats.get("equipment", "gym")).lower()
        if equipment == "none":
            subs = plan["subs_no_equipment"]
            for e in plan["exercises"]:
                e["name"] = subs.get(e["name"], e["name"])

        # Scaling by band
        band = intensity_band(score)
        vol_delta = plan["scaling"]["volume"][band]
        int_pct = plan["scaling"]["intensity"][band]
        exercises = [scale_exercise(e, vol_delta, int_pct) for e in plan["exercises"]]

        return {
            "prediction": score,
            "plan_id": plan["id"],
            "microcycle_days": plan["microcycle_days"],
            "exercises": exercises,
            "notes": "Progress if you hit top reps/time at RPE ≤ 8; otherwise hold.",
        }

    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))



# # server.py
# from fastapi import FastAPI, HTTPException
# from pydantic import BaseModel
# import joblib, json, os, copy
# import numpy as np

# # --- Load model + schema at startup ---
# MODEL_PATH = os.getenv("MODEL_PATH", "pipeline.joblib")
# SCHEMA_PATH = os.getenv("SCHEMA_PATH", "feature_schema.json")

# try:
#     pipeline = joblib.load(MODEL_PATH)
# except Exception as e:
#     raise RuntimeError(f"Failed to load model at {MODEL_PATH}: {e}")

# try:
#     with open(SCHEMA_PATH, "r") as f:
#         schema = json.load(f)
#     FEATURE_ORDER = schema.get("features") or [
#         "age","height","weight","bmi","goal_type","workouts_per_week","calories_avg","equipment"
#     ]
# except Exception as e:
#     raise RuntimeError(f"Failed to load schema at {SCHEMA_PATH}: {e}")

# # --- Minimal plan template (edit later or move to Firestore) ---
# BASE_PLAN = {
#   "id": "hypertrophy_upper_lower_v1",
#   "microcycle_days": 4,
#   "exercises": [
#     {"day":1,"block":"Upper A","name":"Flat Dumbbell Press","sets":4,"reps":"8–10","tempo":"2-0-2","rest_sec":90},
#     {"day":1,"block":"Upper A","name":"Lat Pulldown","sets":4,"reps":"10–12","tempo":"2-1-2","rest_sec":90},
#     {"day":1,"block":"Core","name":"Plank","sets":3,"reps":"45s hold","tempo":"-","rest_sec":60},
#     {"day":2,"block":"Lower A","name":"Back Squat","sets":4,"reps":"6–8","tempo":"3-1-1","rest_sec":120},
#     {"day":3,"block":"Upper B","name":"Incline Push-up","sets":3,"reps":"12–15","tempo":"2-0-2","rest_sec":60},
#     {"day":4,"block":"Lower B","name":"Romanian Deadlift","sets":3,"reps":"8–10","tempo":"3-1-1","rest_sec":120}
#   ],
#   "scaling": {
#     "intensity": {"low": -20, "medium": 0, "high": 10},  # % change for time-based work
#     "volume":    {"low": -1,  "medium": 0, "high": 1}    # sets delta
#   },
#   "subs_no_equipment": {
#     "Flat Dumbbell Press":"Push-up",
#     "Lat Pulldown":"Inverted Row",
#     "Back Squat":"Goblet Squat",
#     "Romanian Deadlift":"Hip Hinge with Backpack"
#   }
# }

# # --- FastAPI app ---
# app = FastAPI()

# class PredictIn(BaseModel):
#     user_id: str
#     features: dict  # keys: age,height,weight,bmi,goal_type,workouts_per_week,calories_avg,equipment

# class PredictOut(BaseModel):
#     prediction: float
#     plan_id: str
#     microcycle_days: int
#     exercises: list
#     notes: str

# def intensity_band(score: float) -> str:
#     if score >= 0.8: return "high"
#     if score >= 0.5: return "medium"
#     return "low"

# def scale_exercise(ex, vol_delta, int_pct):
#     e = copy.deepcopy(ex)
#     # volume scaling (sets)
#     if isinstance(e.get("sets"), int):
#         e["sets"] = max(1, e["sets"] + vol_delta)
#     # intensity scaling for time-based reps like "45s hold"
#     reps = str(e.get("reps", ""))
#     if "s" in reps:
#         try:
#             secs = int(reps.replace("s hold","").replace("s","").strip())
#             secs = max(20, int(secs * (1 + int_pct/100)))
#             e["reps"] = f"{secs}s hold"
#         except:
#             pass
#     return e

# @app.get("/health")
# def health():
#     return {"status": "ok"}

# @app.post("/predict", response_model=PredictOut)
# def predict(p: PredictIn):
#     try:
#         # Ensure all required features exist
#         missing = [k for k in FEATURE_ORDER if k not in p.features]
#         if missing:
#             raise HTTPException(status_code=400, detail=f"Missing feature(s): {missing}")

#         # Build row in correct order (ignore any extra keys)
#         row = [[p.features[k] for k in FEATURE_ORDER]]
#         x = np.array(row, dtype=object)  # dtype=object is safe for mixed numeric/str

#         # Predict (assume RF returns a score; cast to float)
#         score = float(pipeline.predict(x)[0])

#         # Start from template
#         plan = copy.deepcopy(BASE_PLAN)

#         # Equipment substitution
#         equipment = str(p.features.get("equipment", "gym")).lower()
#         if equipment == "none":
#             subs = plan["subs_no_equipment"]
#             for e in plan["exercises"]:
#                 e["name"] = subs.get(e["name"], e["name"])

#         # Scale volume/intensity by score band
#         band = intensity_band(score)
#         vol_delta = plan["scaling"]["volume"][band]
#         int_pct   = plan["scaling"]["intensity"][band]
#         exercises = [scale_exercise(e, vol_delta, int_pct) for e in plan["exercises"]]

#         return {
#             "prediction": score,
#             "plan_id": plan["id"],
#             "microcycle_days": plan["microcycle_days"],
#             "exercises": exercises,
#             "notes": "Progress if you hit top reps/time at RPE ≤ 8; otherwise hold."
#         }

#     except HTTPException:
#         raise
#     except Exception as e:
#         raise HTTPException(status_code=400, detail=str(e))
