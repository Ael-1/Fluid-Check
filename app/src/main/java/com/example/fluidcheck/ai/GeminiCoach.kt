package com.example.fluidcheck.ai

import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiCoach(apiKey: String) {
    private val model = GenerativeModel(
        modelName = "gemma-3-12b-it",
        apiKey = apiKey
    )

    suspend fun calculateHydrationGoal(
        weight: String,
        height: String,
        age: String,
        sex: String,
        activity: String,
        environment: String
    ): String? = withContext(Dispatchers.IO) {
        val prompt = """
            You are a professional health and hydration coach. 
            Based on the following user data, calculate their ideal daily water intake in milliliters (ml).
            
            User Data:
            - Weight: $weight kg
            - Height: $height cm
            - Age: $age
            - Biological Sex: $sex
            - Activity Level: $activity
            - Environment: $environment
            
            Return ONLY the numerical value in ml. Do not include any other text.
        """.trimIndent()

        try {
            val response = model.generateContent(prompt)
            response.text?.trim()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getRecommendation(
        preferences: String,
        habits: String
    ): String? = withContext(Dispatchers.IO) {
        val prompt = "As a hydration coach, give a short, personalized recommendation (max 2 sentences) based on these preferences: '$preferences' and habits: '$habits'. Make it encouraging."

        try {
            val response = model.generateContent(prompt)
            response.text?.trim()
        } catch (e: Exception) {
            null
        }
    }
}
