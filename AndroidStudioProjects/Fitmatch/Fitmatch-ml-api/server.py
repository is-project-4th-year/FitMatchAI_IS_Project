# server.py
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import joblib, json, os, copy
import numpy as np

# --- Load model + schema at startup ---
MODEL_PATH = os.getenv("MODEL_PATH", "pipeline.joblib")
SCHEMA_PATH = os.getenv("SCHEMA_PATH", "feature_schema.json")

try:
    pipeline = joblib.load(MODEL_PATH)
except Exception as e:
    raise RuntimeError(f"Failed to load model at {MODEL_PATH}: {e}")

try:
    with open(SCHEMA_PATH, "r") as f:
        schema = json.load(f)
    FEATURE_ORDER = schema.get("features") or [
        "age","height","weight","bmi","goal_type","workouts_per_week","calories_avg","equipment"
    ]
except Exception as e:
    raise RuntimeError(f"Failed to load schema at {SCHEMA_PATH}: {e}")

# --- Minimal plan template (edit later or move to Firestore) ---
BASE_PLAN = {
  "id": "hypertrophy_upper_lower_v1",
  "microcycle_days": 4,
  "exercises": [
    {"day":1,"block":"Upper A","name":"Flat Dumbbell Press","sets":4,"reps":"8–10","tempo":"2-0-2","rest_sec":90},
    {"day":1,"block":"Upper A","name":"Lat Pulldown","sets":4,"reps":"10–12","tempo":"2-1-2","rest_sec":90},
    {"day":1,"block":"Core","name":"Plank","sets":3,"reps":"45s hold","tempo":"-","rest_sec":60},
    {"day":2,"block":"Lower A","name":"Back Squat","sets":4,"reps":"6–8","tempo":"3-1-1","rest_sec":120},
    {"day":3,"block":"Upper B","name":"Incline Push-up","sets":3,"reps":"12–15","tempo":"2-0-2","rest_sec":60},
    {"day":4,"block":"Lower B","name":"Romanian Deadlift","sets":3,"reps":"8–10","tempo":"3-1-1","rest_sec":120}
  ],
  "scaling": {
    "intensity": {"low": -20, "medium": 0, "high": 10},  # % change for time-based work
    "volume":    {"low": -1,  "medium": 0, "high": 1}    # sets delta
  },
  "subs_no_equipment": {
    "Flat Dumbbell Press":"Push-up",
    "Lat Pulldown":"Inverted Row",
    "Back Squat":"Goblet Squat",
    "Romanian Deadlift":"Hip Hinge with Backpack"
  }
}

# --- FastAPI app ---
app = FastAPI()

class PredictIn(BaseModel):
    user_id: str
    features: dict  # keys: age,height,weight,bmi,goal_type,workouts_per_week,calories_avg,equipment

class PredictOut(BaseModel):
    prediction: float
    plan_id: str
    microcycle_days: int
    exercises: list
    notes: str

def intensity_band(score: float) -> str:
    if score >= 0.8: return "high"
    if score >= 0.5: return "medium"
    return "low"

def scale_exercise(ex, vol_delta, int_pct):
    e = copy.deepcopy(ex)
    # volume scaling (sets)
    if isinstance(e.get("sets"), int):
        e["sets"] = max(1, e["sets"] + vol_delta)
    # intensity scaling for time-based reps like "45s hold"
    reps = str(e.get("reps", ""))
    if "s" in reps:
        try:
            secs = int(reps.replace("s hold","").replace("s","").strip())
            secs = max(20, int(secs * (1 + int_pct/100)))
            e["reps"] = f"{secs}s hold"
        except:
            pass
    return e

@app.get("/health")
def health():
    return {"status": "ok"}

@app.post("/predict", response_model=PredictOut)
def predict(p: PredictIn):
    try:
        # Ensure all required features exist
        missing = [k for k in FEATURE_ORDER if k not in p.features]
        if missing:
            raise HTTPException(status_code=400, detail=f"Missing feature(s): {missing}")

        # Build row in correct order (ignore any extra keys)
        row = [[p.features[k] for k in FEATURE_ORDER]]
        x = np.array(row, dtype=object)  # dtype=object is safe for mixed numeric/str

        # Predict (assume RF returns a score; cast to float)
        score = float(pipeline.predict(x)[0])

        # Start from template
        plan = copy.deepcopy(BASE_PLAN)

        # Equipment substitution
        equipment = str(p.features.get("equipment", "gym")).lower()
        if equipment == "none":
            subs = plan["subs_no_equipment"]
            for e in plan["exercises"]:
                e["name"] = subs.get(e["name"], e["name"])

        # Scale volume/intensity by score band
        band = intensity_band(score)
        vol_delta = plan["scaling"]["volume"][band]
        int_pct   = plan["scaling"]["intensity"][band]
        exercises = [scale_exercise(e, vol_delta, int_pct) for e in plan["exercises"]]

        return {
            "prediction": score,
            "plan_id": plan["id"],
            "microcycle_days": plan["microcycle_days"],
            "exercises": exercises,
            "notes": "Progress if you hit top reps/time at RPE ≤ 8; otherwise hold."
        }

    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))
