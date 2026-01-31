package com.example.fluidcheck.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiCoach(apiKey: String) {
    private val model = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = apiKey
    )

    suspend fun calculateHydrationGoal(
        weight: String,
        height: String,
        age: String,
        gender: String,
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
            - Gender: $gender
            - Activity Level: $activity
            - Environment: $environment
            
            Return ONLY the numerical value in ml. Do not include any other text.
        """.trimIndent()

        try {
            val response = model.generateContent(prompt)
            response.text?.filter { it.isDigit() }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getRecommendation(
        preferences: String,
        habits: String
    ): String? = withContext(Dispatchers.IO) {
        val prompt = """
            You are a professional hydration coach. 
            Give a short, personalized recommendation (max 2 sentences) for what the user should drink next or a hydration tip.
            
            User Preferences: $preferences
            User Habits: $habits
            
            Make it encouraging and based on their preferences.
        """.trimIndent()

        try {
            val response = model.generateContent(prompt)
            response.text
        } catch (e: Exception) {
            null
        }
    }
}