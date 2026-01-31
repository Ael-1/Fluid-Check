# FluidCheck 💧

FluidCheck is an Android application designed to help users manage their hydration and nutrition goals using Google's Gemini AI. It provides personalized health recommendations based on user input such as weight, activity level, and environmental conditions.

> **Note:** This project is currently a **Work in Progress**.

## Features (WIP)
- **AI-Powered Personalized Goals:** Get custom daily water intake targets calculated by Gemini AI.
- **Smart Health Recommendations:** AI-driven advice on diet, hydration, and lifestyle improvements.
- **Interactive UI:** Built with Jetpack Compose for a modern, fluid user experience.

## 🛠Tech Stack
- **Language:** [Kotlin](https://kotlinlang.org/)
- **UI Framework:** [Jetpack Compose](https://developer.android.com/compose)
- **AI Integration:** [Google Generative AI SDK (Gemini)](https://ai.google.dev/)
- **Architecture:** MVVM (Model-View-ViewModel)

## Setup & Installation
1. **Clone the repository:**
2. Obtain a Gemini API Key: Get your key from the Google AI Studio.
3. Configure the API Key:
    ◦ Create a local.properties file in your root directory (if it doesn't exist).
    ◦ Add your key: GEMINI_API_KEY=your_actual_key_here. (Note: The project is configured to ignore local.properties for security.)
4. Build and Run: Open the project in Android Studio and run it on an emulator or physical device.

## Current Status
• [x] Basic UI Layout
• [x] Gemini AI integration logic
• [ ] Local Database for history tracking (Room)
• [ ] Daily notification reminders
• [ ] User profile customization

## License
This project is licensed under the MIT License - see the LICENSE file for details.

1. **Obtain a Gemini API Key:**
   Get your key from the [Google AI Studio](https://aistudio.google.com/).
2. **Configure the API Key:**
    - Create a `local.properties` file in your root directory (if it doesn't exist).
    - Add your key: `GEMINI_API_KEY=your_actual_key_here`.
      *(Note: The project is configured to ignore `local.properties` for security.)*
3. **Build and Run:**
   Open the project in Android Studio and run it on an emulator or physical device.
   