package com.example.fluidcheck.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiCoach(apiKey: String) {
    private val model = GenerativeModel(
        modelName = "gemma-4-31b-it",
        apiKey = apiKey,
        generationConfig = generationConfig {
            temperature = 0.1f
        }
    )

    suspend fun calculateHydrationGoal(
        unitsInfo: String,
        weight: String,
        height: String,
        age: String,
        sex: String,
        activity: String,
        environment: String
    ): String? = withContext(Dispatchers.IO) {
        val prompt = """
            You are a professional clinical dietitian. 
            Calculate the user's ideal daily water intake in milliliters (ml) basing your math strictly on established physiological standards (EFSA and NASEM guidelines):
            1. Baseline: Compute a baseline of 35 ml of water per kilogram of body weight.
            2. Activity Level Adjustment: Add 500 ml for Lightly Active/Moderate and 1000 ml for Very Active/Extra Active (representing sweat loss recovery).
            3. Environmental Adjustment: Add 500 ml for Hot/Humid conditions.
            
            $unitsInfo
            User Data:
            - Weight: $weight kg
            - Height: $height cm
            - Age: $age
            - Biological Sex: $sex
            - Activity Level: $activity
            - Environment: $environment
            
            Calculate the value step-by-step internally, but output ONLY the final value in the format "XXXX ml" (e.g., "3150 ml"). Do not include any other text, reasoning, markdown, or explanation.
        """.trimIndent()

        try {
            val response = model.generateContent(prompt)
            val rawResponse = response.text?.trim() ?: return@withContext null
            
            // Extract the last occurrence of a 3 to 5 digit number followed by 'ml'
            val mlRegex = Regex("""(\d{3,5})\s*(?i)ml""")
            val mlMatches = mlRegex.findAll(rawResponse)
            val lastMlMatch = mlMatches.lastOrNull()?.groupValues?.get(1)
            
            if (lastMlMatch != null) {
                "$lastMlMatch ml"
            } else {
                // Fallback to any 3 to 5 digit number
                val numberRegex = Regex("""\b(\d{3,5})\b""")
                val numberMatches = numberRegex.findAll(rawResponse)
                val lastNumMatch = numberMatches.lastOrNull()?.groupValues?.get(1)
                if (lastNumMatch != null) {
                    "$lastNumMatch ml"
                } else {
                    "3000 ml" // Clinical standard fallback
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getRecommendation(
        unitsInfo: String,
        preferences: String,
        habits: String
    ): String? = withContext(Dispatchers.IO) {
        val prompt = """
            You are a professional clinical dietitian. Give a short, personalized hydration recommendation (max 5 sentences) based on the user's preferences: '$preferences' and habits: '$habits'. $unitsInfo
            Base your advice strictly on established physiological science (such as beverage hydration index, optimal water timing, diuretic balance for coffee/alcohol, or electrolyte preservation).
            Keep it encouraging, clear, and professional. Provide only the recommendation itself. Do not output any thoughts, reasoning steps, or markdown tags.
        """.trimIndent()

        try {
            val response = model.generateContent(prompt)
            val rawResponse = response.text?.trim() ?: return@withContext null
            
            // Filter out system guidelines, echoed prompts, or draft metadata
            val filteredLines = rawResponse.lines()
                .map { it.trim() }
                .filter { line ->
                    line.isNotEmpty() &&
                    !line.startsWith("*") &&
                    !line.startsWith("-") &&
                    !line.startsWith("#") &&
                    !line.contains("Role:", ignoreCase = true) &&
                    !line.contains("Task:", ignoreCase = true) &&
                    !line.contains("Constraint", ignoreCase = true) &&
                    !line.contains("Guideline", ignoreCase = true) &&
                    !line.contains("Draft", ignoreCase = true) &&
                    !line.contains("Preferences:", ignoreCase = true) &&
                    !line.contains("Habits:", ignoreCase = true) &&
                    !line.contains("Output:", ignoreCase = true) &&
                    !line.contains("? Yes", ignoreCase = true) &&
                    !line.contains("? No", ignoreCase = true)
                }
            
            val cleanParagraph = filteredLines.joinToString(" ")
            
            // Split into sentences, deduplicate, and keep all sentences for a comprehensive guide
            val sentenceRegex = Regex("""(?<=[.!?])\s+""")
            val sentences = cleanParagraph.split(sentenceRegex)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                
            if (sentences.isNotEmpty()) {
                sentences.joinToString(" ")
            } else {
                // Return fallback recommendation if filtering left it empty
                "Based on clinical standards, aim to maintain a steady hydration routine, drinking water consistently throughout the day and adjusting for caffeine intake."
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getWeeklyScorecard(logsLast7Days: String): String? = withContext(Dispatchers.IO) {
        val prompt = """
            You are a professional clinical dietitian and hydration coach.
            Analyze the following 7-day fluid intake logs:
            $logsLast7Days
            
            Based on this data:
            1. Calculate a rating or grade (e.g., A, B+, C, etc.) representing their hydration consistency and compliance.
            2. Provide a concise, helpful summary explaining this grade and offering actionable tips for improvement.
            
            Ensure the output contains both the rating/grade and the summary, kept professional and clear. Do not output any thoughts, reasoning steps, or markdown tags.
        """.trimIndent()

        try {
            val response = model.generateContent(prompt)
            response.text?.trim()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun analyzeHabitsAndRules(
        unitsInfo: String,
        logsLast14Days: String,
        profile: String
    ): Pair<String, String>? = withContext(Dispatchers.IO) {
        val prompt = """
            You are a professional clinical dietitian and predictive health analyzer.
            Analyze the following 14-day fluid intake logs and user profile:
            
            $unitsInfo
            User Profile:
            $profile
            
            Fluid Logs (Last 14 Days):
            $logsLast14Days
            
            Note: Days marked with "No logs recorded" mean the user did not track any fluid intake on those days. If there are major tracking gaps, consider this in your habits and consistency analysis.
            
            Tasks:
            1. Provide a detailed analysis text of their habits, rules, and patterns. Address the user directly by name. The analysis paragraph MUST start with the exact format "[Username], you are ..." (substituting their actual Username from the profile details). E.g. "john_doe, you are exhibiting...".
            2. Generate up to 3 predictive alerts/reminders as a raw JSON array of objects. Each object should have:
               - "id": a unique identifier or number
               - "message": the helpful reminder message, personalized based on patterns
               - "triggerCondition": description of what triggers this alert
               - "timeToTrigger": approximate time or context (e.g., "14:00" or "Afternoon")
            
            Format your output EXACTLY as follows:
            [ANALYSIS_TEXT_START]
            [Username], you are <Detailed analysis paragraph here>
            [ANALYSIS_TEXT_END]
            [JSON_START]
            <Raw JSON array here, with no markdown code blocks>
            [JSON_END]
        """.trimIndent()

        android.util.Log.d("GeminiCoach", "Sending habits analysis prompt:\n$prompt")
        try {
            val response = model.generateContent(prompt)
            val rawResponse = response.text ?: return@withContext null
            android.util.Log.d("GeminiCoach", "Received habits analysis rawResponse:\n$rawResponse")
            
            val analysisStartTag = "[ANALYSIS_TEXT_START]"
            val analysisEndTag = "[ANALYSIS_TEXT_END]"
            val jsonStartTag = "[JSON_START]"
            val jsonEndTag = "[JSON_END]"
            
            val analysisStartIndex = rawResponse.lastIndexOf(analysisStartTag)
            val analysisEndIndex = if (analysisStartIndex != -1) rawResponse.indexOf(analysisEndTag, analysisStartIndex) else -1
            val jsonStartIndex = rawResponse.lastIndexOf(jsonStartTag)
            val jsonEndIndex = if (jsonStartIndex != -1) rawResponse.indexOf(jsonEndTag, jsonStartIndex) else -1
            
            if (analysisStartIndex != -1 && analysisEndIndex != -1 && jsonStartIndex != -1 && jsonEndIndex != -1) {
                val analysisText = rawResponse.substring(analysisStartIndex + analysisStartTag.length, analysisEndIndex).trim()
                var jsonText = rawResponse.substring(jsonStartIndex + jsonStartTag.length, jsonEndIndex).trim()
                
                if (jsonText.startsWith("```json")) {
                    jsonText = jsonText.removePrefix("```json")
                } else if (jsonText.startsWith("```")) {
                    jsonText = jsonText.removePrefix("```")
                }
                if (jsonText.endsWith("```")) {
                    jsonText = jsonText.removeSuffix("```")
                }
                jsonText = jsonText.trim()
                
                Pair(analysisText, jsonText)
            } else {
                val jsonRegex = Regex("""\[\s*\{.*\}\s*\]""", RegexOption.DOT_MATCHES_ALL)
                val jsonMatch = jsonRegex.find(rawResponse)?.value
                if (jsonMatch != null) {
                    val analysis = rawResponse.replace(jsonMatch, "").trim()
                    Pair(analysis, jsonMatch)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}

