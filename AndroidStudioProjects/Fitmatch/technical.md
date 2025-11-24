# FitMatchAI – Personalized Fitness Planning with Random Forest

**Technical Documentation – Final Year Project**

- **Author:** Charles Maina  
- **Date:** November 2025  
- **Core Model:** Random Forest–based workout plan recommender  
- **Runtime Stack:** Android (Kotlin/Jetpack Compose) + Firebase (Auth & Firestore) + FastAPI on Cloud Run

---

## 1. Project Overview

FitMatchAI is a mobile application that generates **personalized, goal-driven workout plans** and concise **nutrition guidance** from a small set of self-entered metrics.

The system focuses on:

- Helping users set **clear goals** (e.g., fat loss, hypertrophy, endurance).
- Using a **machine learning model** to map user profiles to an appropriate **plan variant / microcycle**.
- Updating plans **week by week** based on adherence and progress, instead of remaining static.

The current production Random Forest model achieves **~90% accuracy** on held-out test data when predicting the appropriate plan category for a user profile.

---

## 2. Data & Features

### 2.1 Datasets Used

FitMatchAI uses a blended dataset strategy:

- **FitLife Dataset**  
  Contains exercise and workout plan patterns, including training frequencies and rough outcome trends.

- **Gym Members Exercise Dataset (Vala Khorasani)**  
  Contains demographic data and gym behaviours such as weekly frequency and target outcomes.

These datasets are **harmonized into a single training table**.

---

### 2.2 Target Classes (Label)

The final model works as a **multi-class classifier**. The target is an internal **plan class** that maps to a workout microcycle (plan variant). Examples of classes (you can align with your actual labels):

- `CLASS_0` – lower-intensity / starter plan  
- `CLASS_1` – moderate-intensity / on-track plan  
- `CLASS_2` – higher-intensity / advanced plan

> Each class corresponds to a **plan template** (exercise list, sets, reps, and weekly structure) that the API uses to build the final weekly workout.

---

### 2.3 Input Variables (Features)

The production model uses **8 core features** that are easy to collect from users while still being meaningful for planning:

- **Numeric**
  - `age`
  - `height_cm`
  - `weight_kg`
  - `bmi` (derived)
  - `workouts_per_week`
  - `calories_avg` (user estimate / app suggestion)

- **Categorical**
  - `goal_type` – {`fat_loss`, `hypertrophy`, `endurance`}
  - `equipment` – {`home_workout`, `gym_workout`}

> Design choice: features are intentionally **simple and explainable** so users can understand why they are being asked for each value.

---

### 2.4 Preprocessing & Feature Selection

Key preprocessing steps:

- **Data Cleaning**
  - Type coercion for numeric fields.
  - Removal or clipping of impossible values (e.g. height < 120 cm, weight < 30 kg).
  - Handling of missing values via median / constant imputation.

- **Feature Engineering**
  - `bmi` calculated from height and weight.
  - Normalization / scaling of calorie-related features where required.

- **Encoding**
  - Ordinal / one-hot encoding for `goal_type` and `equipment`.

- **Feature Selection Rationale**
  - Features chosen based on:
    - Availability across both datasets.
    - Clear physiological meaning (energy balance, training load, body composition).
    - Low friction for real users to enter in an Android app.

---

## 3. Training, Validation & Metrics

### 3.1 Train/Test Split

- **Split:** `80%` training, `20%` testing (stratified by target class).  
- Reason:
  - Enough data in the training set for model fitting and cross-validation.
  - Sufficient held-out test set for **honest performance estimation**.

> If your actual split is different (e.g. 75/25), update this section and keep the justification.

---

### 3.2 Cross-Validation

To avoid overfitting and to get a stable estimate of performance:

- **Cross-validation scheme:**  
  - **Stratified 5-fold cross-validation** on the training set.  
  - Optionally repeated (e.g., 2 repeats) for more robust estimates.

This ensures that each fold maintains the same proportion of each class as the full dataset.

---

### 3.3 Evaluation Metrics


The Random Forest pipeline was evaluated with both a **regression head** (for weight delta) and a **classification head** (for plan class).

**Data split (grouped by user):**

- Total rows: 96,990 (98 columns before feature selection)
- Users split → train = 2,100, val = 450, test = 450
- Final shapes:
  - Train: (67,887, 98)
  - Val: (14,564, 98)
  - Test: (14,539, 98)

**Regression – Weight Delta**

- MAE   ≈ **0.69 kg**
- RMSE  ≈ **1.21 kg**
- R²    ≈ **0.995**

This confirms the model can closely approximate weekly weight change given the features.

**Classification – Plan Class (production head)**

- Overall **accuracy ≈ 0.90** on the test set
- Macro F1-score ≈ **0.90**
- Classification report shows all three classes performing well (precision, recall ~0.83–0.97)

Confusion matrix on the test set (rows = true, columns = predicted):

|        | Pred 0 | Pred 1 | Pred 2 |
|--------|--------|--------|--------|
| True 0 | 4104   | 481    | 117    |
| True 1 | 171    | 4821   | 0      |
| True 2 | 660    | 0      | 4185   |



This indicates that:

- Most predictions lie on the diagonal (correct class).
- Misclassifications are mostly between neighbouring intensity bands (e.g., class 0 → 1 or 2 → 0), which is acceptable for a progressive training system.

The **classification head** with these metrics is what is deployed in production; the regression head is mainly used for analysis and future refinement of progression rules.

---

## 4. Modelling Approach

### 4.1 Techniques Explored

Several techniques were tried before settling on the final model:

1. **Random Forest**
     - Strong performance on tabular data.
     - Robust to noise & non-linear interactions.
     - Easy to train and deploy on CPU.

3. **Transformer-based Sequence Model** (Experimental)
   - Small Transformer was tested to treat weekly progression more like a sequence problem.
   - Limited dataset size + complexity led to underperformance (≈33% accuracy).

---

### 4.2 Final Model: Random Forest Classifier

- **Chosen Model:** Random Forest Classifier.
- **Reason for selection:**
  - Highest or near-highest accuracy among tested models.
  - Good generalization on cross-validation and test set.
  - Works well with mixed numeric + categorical features.
  - CPU-friendly for Cloud Run (no GPU required).
  - Easier to retrain and interpret than deep models.

You can document your key hyperparameters (example, update with your real ones):

- `n_estimators = 200`
- `max_depth = None` (expand until stopping criteria)
- `min_samples_split = 2`
- `class_weight = "balanced"`

---

### 4.3 Epochs & Training Details

- **Random Forest:**  
  - Non-iterative in the neural-network sense → **no “epochs”**; training is based on tree construction.

- **Transformer Experiment (if asked):**
  - Training was capped at, e.g., **30 epochs** with **early stopping** (patience 3–5 epochs).
  - Validation accuracy plateaued at around **33%**, below Random Forest performance.
  - Conclusion: Transformer not suitable at this stage, due to data size and complexity → kept as **future research**.

---

## 5. Application Logic & Algorithms

### 5.1 Guarded Weekly Regeneration

After each week, the user can **finalize** their plan:

1. The app computes an **adherence summary**:
   - % of planned workouts actually completed.
   - Relative volume/intensity (did they do more or less than planned?).

2. The adherence summary is merged with the latest user features (weight changes, calories, etc.).

3. The app calls the **API** again to request a new plan:
   - The server uses the Random Forest model to predict the next plan class.
   - A microcycle is selected based on:
     - Goal type.
     - Plan class.
     - Adherence data.

4. **Day offset logic**:
   - Days continue across weeks (e.g., Week 1: Days 1–7 → Week 2: Days 8–14) for a continuous feel.

---

### 5.2 Anti-Repeat Logic

To avoid showing almost identical plans week after week:

- The server computes the **overlap** between the previous week’s exercise list and the new candidate plan.
- If overlap ≥ **75%** (e.g., using Jaccard similarity on exercise names):
  - The server retries with a different **plan variant seed**.
- This keeps plans **fresh** while still respecting the model’s intensity recommendation.

---

## 6. System Architecture & Modules

### 6.1 High-Level Architecture

- **Android App (Kotlin, Jetpack Compose)**
  - Handles onboarding, goal creation, metric entry, plan display, and progress tracking.

- **API Layer (FastAPI on Cloud Run)**
  - Wraps the Random Forest model.
  - Performs final preprocessing, prediction, and microcycle construction.

- **Firebase**
  - **Authentication:** Email/Password + Google Sign-In.
  - **Firestore:** Stores users, goals, weekly plans, and progress summaries.

---

### 6.2 Core App Modules

- **Goals**
  - Create and manage goals (goal type, duration).
  - Start/end dates auto-computed from start date + number of weeks.
  - Snackbar prompt guiding the user to open their new plan.

- **Plan**
  - Uses `generateFromMetrics(...)` to call the API with the latest features.
  - “Finalize This Week” triggers adherence computation and new plan generation.

- **Progress**
  - Weekly summaries: adherence %, missed days, and qualitative feedback.
  - Intended to later power charts and progress visualizations.

- **Nutrition**
  - Goal-aligned calorie and macro guidance.
  - Hydration and basic supplement suggestions.
  - “Regenerate” button to update nutrition targets as the user progresses.

---

## 7. Deployment & Reproducibility

### 7.1 ML Notebook & Data

In the GitHub repository, the following should be available:

- `notebooks/fitmatch_rf_training.ipynb`  
  - Shows data loading, preprocessing, train/test splitting, cross-validation, metrics, and confusion matrix.

- `data/`  
  - Contains cleaned / anonymized versions of the training data (or scripts to fetch and preprocess original datasets).

> During defense, keep the **notebook open** to quickly show:
> - The features and classes.
> - The train/test split.
> - The evaluation results (accuracy and confusion matrix).

---

### 7.2 Android App Setup

- Requirements:
  - Android Studio (Ladybug or newer)
  - JDK 17
  - Firebase project

- Steps:
  1. Clone the repository.
  2. Add `google-services.json` under `app/`.
  3. Enable Email/Password (and Google) sign-in in Firebase.
  4. Configure Firestore security rules for user-based access.
  5. Build and run on an emulator or physical device.

---

## 8. Limitations & Future Work

- Current model does not directly model:
  - Injury risk.
  - Recovery (sleep, stress).
  - Detailed exercise history across many months.
- Transformer and more advanced sequence models were explored but underperformed on the current dataset size.
- Future directions:
  - Collect richer **longitudinal data**.
  - Explore **hybrid models** (tree-based + sequence) for finer progression.
  - Integrate wearable data (steps, heart rate) where available.

---
