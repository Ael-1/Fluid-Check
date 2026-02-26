# FluidCheck 💧

FluidCheck is an Android application designed to help users manage their hydration goals using Google's Gemini AI. It provides personalized health recommendations based on user input such as weight, height, age, activity level, and environmental conditions.

## 🚀 Features
- **AI-Powered Personalized Goals:** Get custom daily water intake targets calculated by Gemini AI.
- **Smart Hydration Coach:** AI-driven personalized recommendations based on your preferences and habits.
- **Fluid Logging:** Track your daily fluid intake with various drink types and cumulative data visualization.
- **Progress Tracking:** Monitor your hydration trends over time with dynamic line charts (Day, Week, Month, Year views).
- **Firebase Authentication:** Secure login and sign-up using Firebase Auth, including Google Sign-In and username-based lookup.
- **Cloud Persistence:** Full Cloud Firestore integration for real-time synchronization of user records, hydration goals, and fluid logs.
- **Admin Dashboard:** Comprehensive tools for managing users, roles, and system-wide analytics.
- **Modern UI:** Beautiful, responsive interface built entirely with Jetpack Compose following Material 3 guidelines with a "Glassmorphism" aesthetic.

## 🛠 Tech Stack
- **Language:** [Kotlin](https://kotlinlang.org/)
- **UI Framework:** [Jetpack Compose](https://developer.android.com/compose)
- **AI Integration:** [Google Generative AI SDK (Gemini)](https://ai.google.dev/)
- **Backend/Services:** [Firebase](https://firebase.google.com/) (Authentication, Firestore, Analytics)
- **Local Storage:** [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) (Preferences)
- **Dependency Management:** Gradle Version Catalog (libs.versions.toml)

## 🔑 Setup & Installation
1. **Clone the repository:**
   ```bash
   git clone https://github.com/Ael-1/FluidCheck.git
   ```

2. **Obtain a Gemini API Key:**
   Get your key from the [Google AI Studio](https://aistudio.google.com/).

3. **Configure Secrets:**
   For security, this project uses a `secrets.properties` file (ignored by version control) to store sensitive keys.
   - Create a file named `secrets.properties` in the **root** directory of the project.
   - Add your Gemini API key to the file:
     ```properties
     GEMINI_API_KEY=your_actual_api_key_here
     ```

4. **Firebase Setup:**
   - Create a new project in the [Firebase Console](https://console.firebase.google.com/).
   - Enable **Authentication** (Email/Password & Google) and **Cloud Firestore**.
   - Add an Android app with the package name `com.example.fluidcheck`.
   - Download the `google-services.json` file and place it in the `app/` directory.

5. **Build and Run:**
   Open the project in Android Studio and run it on an emulator or physical device.

## 📝 Current Status
- [x] Basic UI Layout & Navigation
- [x] Gemini AI Integration (Goal calculation & recommendations)
- [x] Fluid Intake Logging with Edit/Delete capabilities
- [x] Firebase Authentication (Email/Password & Google Sign-In)
- [x] Cloud Firestore Integration (Real-time data syncing)
- [x] Dynamic Progress Charts (Cumulative daily and historical views)
- [x] Admin Dashboard & User Management
- [ ] Daily notification reminders
- [ ] Multi-language support

## 👨‍💻 Developer
**Vincent Rafael Apog**
- 3rd year Computer Engineering student at Mapúa Malayan Colleges Mindanao.
- Developed as a partial fulfillment for CPE144L - Mobile Application Development.
