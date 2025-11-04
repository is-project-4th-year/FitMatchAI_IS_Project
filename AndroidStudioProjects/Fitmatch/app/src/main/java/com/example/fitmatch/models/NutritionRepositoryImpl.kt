package com.example.fitmatch.models

import com.example.fitmatch.data.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class NutritionRepositoryImpl(
    private val db: FirebaseFirestore
) : NutritionRepository {

    override fun observeLatest(uid: String): Flow<DietPlanDTO?> = callbackFlow {
        val ref = db.collection("users").document(uid)
            .collection("nutrition_plans")
            .orderBy("created_at", Query.Direction.DESCENDING)
            .limit(1)

        val reg = ref.addSnapshotListener { snap, err ->
            if (err != null) { trySend(null).isSuccess; return@addSnapshotListener }
            val doc = snap?.documents?.firstOrNull() ?: run { trySend(null).isSuccess; return@addSnapshotListener }

            try {
                val macros = (doc.get("macros") as? Map<*, *>) ?: emptyMap<String, Any?>()
                val sups = (doc.get("supplements") as? List<*>) ?: emptyList<Any?>()

                val dto = DietPlanDTO(
                    goal = doc.getString("goal") ?: "",
                    safety_flags = (doc.get("safety_flags") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                    macros = MacroSplitDTO(
                        calories_target = (macros["calories_target"] as? Number)?.toInt() ?: 0,
                        protein_g       = (macros["protein_g"]       as? Number)?.toInt() ?: 0,
                        fat_g           = (macros["fat_g"]           as? Number)?.toInt() ?: 0,
                        carbs_g         = (macros["carbs_g"]         as? Number)?.toInt() ?: 0,
                        fiber_g         = (macros["fiber_g"]         as? Number)?.toInt() ?: 0
                    ),
                    supplements = sups.mapNotNull { s ->
                        (s as? Map<*, *>)?.let {
                            SupplementRecDTO(
                                name = it["name"]?.toString() ?: "",
                                dose = it["dose"]?.toString() ?: "",
                                notes = it["notes"]?.toString() ?: ""
                            )
                        }
                    },
                    hydration_liters = (doc.get("hydration_liters") as? Number)?.toDouble() ?: 0.0,
                    created_at_ms = doc.getTimestamp("created_at")?.toDate()?.time ?: 0L
                )
                trySend(dto).isSuccess
            } catch (_: Exception) {
                trySend(null).isSuccess
            }
        }
        awaitClose { reg.remove() }
    }

    override suspend fun generateAndSaveFromFeatures(uid: String, features: Map<String, Any?>): DietPlanDTO {
        // Expected features you already collect for training plans
        val age       = (features["age"] as? Number)?.toInt() ?: 25
        val heightCm  = (features["height"] as? Number)?.toDouble() ?: 170.0
        val weightKg  = (features["weight"] as? Number)?.toDouble() ?: 70.0
        val goalType  = (features["goal_type"] as? Number)?.toInt() ?: 1   // 0 fatloss, 1 hypertrophy, 2 endurance
        val workouts  = (features["workouts_per_week"] as? Number)?.toInt() ?: 3
        val equipment = (features["equipment"] as? String)?.lowercase() ?: "gym"

        // --- 1) Calories (safe bounds) -----------------------------
        // Simple Mifflin-St Jeor using a neutral sex term (approx). You can refine later.
        val heightM = heightCm / 100.0
        val bmr = (10 * weightKg) + (6.25 * heightCm) - (5 * age) + 5      // male-ish
        val pal = when (workouts.coerceIn(0, 14)) {                         // activity factor
            0   -> 1.2
            in 1..2 -> 1.35
            in 3..5 -> 1.5
            in 6..8 -> 1.6
            else    -> 1.7
        }
        val tdee = bmr * pal

        // goal adjustments with safeguards
        val (goalLabel, calTarget, safety) = when (goalType) {
            0 -> { // fat loss
                val deficit = 0.15 * tdee           // ~15%
                val raw = (tdee - deficit)
                val safeMin = 1500.0                // DO NOT go below 1500 kcal/day
                val target = max(raw, safeMin)
                Triple("fatloss", target, listOf("min_1500_kcal", "safe_rate_loss"))
            }
            1 -> { // hypertrophy
                val surplus = 0.10 * tdee           // ~10%
                Triple("hypertrophy", tdee + surplus, emptyList())
            }
            else -> { // endurance / recomposition
                Triple("endurance", tdee, emptyList())
            }
        }

        val caloriesTarget = calTarget.roundToInt()

        // --- 2) Macros ------------------------------------------------
        // Protein (g/kg): cut 1.6–2.2, bulk 1.6–2.2, endurance 1.2–1.6
        val proteinPerKg = when (goalType) {
            0 -> 2.0
            1 -> 1.8
            else -> 1.4
        }
        val proteinG = (weightKg * proteinPerKg).roundToInt()

        // Fat (≥0.6 g/kg, ~25% kcal typical). Respect lower bound.
        val fatLower = (0.6 * weightKg).roundToInt()
        val fatFromPct = ((0.25 * caloriesTarget) / 9.0).roundToInt()
        val fatG = max(fatLower, fatFromPct)

        // Carbs are the remainder.
        val kcalFromProtein = proteinG * 4
        val kcalFromFat = fatG * 9
        val carbsG = max(0, ((caloriesTarget - kcalFromProtein - kcalFromFat) / 4.0).roundToInt())

        // Fiber heuristic: 14g per 1000 kcal (IOM)
        val fiberG = max(20, ((caloriesTarget / 1000.0) * 14.0).roundToInt())

        // --- 3) Hydration --------------------------------------------
        val hydrationLiters = (weightKg * 0.033) // ~33 ml/kg/day

        // --- 4) Supplements (sensible defaults) -----------------------
        val supplements = mutableListOf<SupplementRecDTO>()
        supplements += SupplementRecDTO("Magnesium", "300–400 mg/day", "Glycinate/citrate preferred")
        supplements += SupplementRecDTO("Calcium", "1000 mg/day", "Split doses if sensitive")
        supplements += SupplementRecDTO("Vitamin C", "200–500 mg/day", "Support immune recovery")
        if (goalType == 1) {
            supplements += SupplementRecDTO("Creatine Monohydrate", "3–5 g/day", "Hydrate well; daily, no cycling needed")
        }
        // Optional: Protein powder as food replacement convenience
        supplements += SupplementRecDTO("Protein (whey/casein/plant)", "1–2 scoops as needed", "To hit protein target")

        // --- 5) Persist -----------------------------------------------
        val doc = hashMapOf(
            "goal" to goalLabel,
            "safety_flags" to safety,
            "hydration_liters" to hydrationLiters,
            "created_at" to FieldValue.serverTimestamp(),
            "macros" to mapOf(
                "calories_target" to caloriesTarget,
                "protein_g" to proteinG,
                "fat_g" to fatG,
                "carbs_g" to carbsG,
                "fiber_g" to fiberG
            ),
            "supplements" to supplements.map { mapOf("name" to it.name, "dose" to it.dose, "notes" to it.notes) }
        )

        val coll = db.collection("users").document(uid).collection("nutrition_plans")
        val ref = coll.add(doc).await()

        val saved = ref.get().await()
        return DietPlanDTO(
            goal = goalLabel,
            safety_flags = safety,
            macros = MacroSplitDTO(caloriesTarget, proteinG, fatG, carbsG, fiberG),
            supplements = supplements,
            hydration_liters = hydrationLiters,
            created_at_ms = saved.getTimestamp("created_at")?.toDate()?.time ?: System.currentTimeMillis()
        )
    }
}