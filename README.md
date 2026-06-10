# FluidCheck 💧

FluidCheck is an Android application designed to help users manage their hydration goals using Google's Gemini AI. It provides personalized health recommendations based on user input such as weight, height, age, activity level, and environmental conditions.

**This Commit is 95% done. Can be considered done, but with more polishing, code cleaning and folder restructuring, the app is 100% done.**

## 🚀 Features

- **Global Measurement System (Metric & Imperial):** Switch between Metric (ml, kg, cm) and Imperial (oz, lbs, ft/in) units globally. All tracking, progress charts, profile fields, and AI assessments scale and convert seamlessly. *Purpose: Facilitates international localization and seamless unit conversions.*
- **Premium Subscription Tier & Interactive Glassmorphic Paywall:** Unlocks advanced capabilities tailored to power-user requirements. *Purpose: Monetizes the application and expands hydration tracking depth.* Unlocked features include:
  - *All Fluid Types & Custom Logs*: Access a wider catalog of drinks and customize quick-add entries.
  - *Full Progress Views*: Detailed historical tracking (Daily, Weekly, Monthly, and Yearly charts).
  - *AI-Powered Coach*: Advanced personalization, habit/composition analysis, and predictive notifications.
  - *Dynamic Weather Integration*: Real-time target adjustments using weather APIs.
  - *Streak Shields*: Streak protection and rewards.
  - *No Ads & Premium UI*: An ad-free experience with custom UI skins.
- **Structured User Roles & Directory Management:** Establishes clear access privileges and permissions across the system for security and administrative delegation:
  - *Admin*: Complete control, user/username/role editing (with self-demotion/self-delete guards), and secure double-confirmation delete.
  - *Moderator*: Read-only directory access and system analytics view for tracking application metrics.
  - *Premium User*: Access to all premium features and unlimited cloud sync capabilities.
  - *Free User (Standard)*: Basic logging features and 7-day cloud history recovery.
- **Gamification & Achievements System:** Integrates structured daily/weekly missions and collectible milestone badges (e.g., "Water Warrior", "Streak Master") to incentivize consistent hydration. Includes a real-time Active Mission Tracker on the HomeScreen dashboard and a dedicated Inventory & Badges screen. *Purpose: Boosts user retention, hydration discipline, and interactive engagement.*
- **App UI Customization Hub (WIP - Premium Feature):** Unlocks deep personalization controls for premium users to tailor their visual environment:
  - *Dynamic App Themes (WIP)*: Dynamic theme swapping across 10 palettes including Light/Dark modes, Ocean Blue, Sunset Orange, Midnight Purple, AMOLED Pitch Black, Forest Green, Cyberpunk Neon, Pastel Spring, Classic Sepia, and Cyberpunk Neon.
  - *Alternative Progress Meters (WIP)*: Replaces the standard Progress Ring with Bottle Fill, Wave Animation, Horizontal Line, Segmented Battery, Dot Matrix, or Liquid Drop gauges.
  - *Custom Backgrounds (WIP)*: Renders custom solid colors, blurred gradients, or subtle patterns globally.
  - *Launcher Icon Switching (WIP)*: Dynamic application launcher icon swaps to match chosen themes.
- **AI-Powered Personalized Goals:** Get custom daily water intake targets calculated by Gemini AI.
- **Smart Hydration Coach:** AI-driven personalized recommendations based on your preferences and habits.
- **Profile Photo Management:** Sophisticated image handling using **uCrop** for precision editing and **ImgBB** for cloud hosting, featuring full offline support.
- **Fluid Logging:** Track your daily fluid intake with various drink types and cumulative data visualization.
- **Offline First & Atomic Operations:** Enhanced reliability using Firestore `WriteBatch` and atomic increments, ensuring data consistency and optimized database usage.
- **Smart Notification Ecosystem:** Contextual and periodic reminders scheduled via `WorkManager`, including Morning/Evening progress alerts, Streak Protection, and hourly hydration reminders.
- **Progress Tracking:** Monitor your hydration trends over time with dynamic line charts (Day, Week, Month, Year views).
- **Firebase Authentication:** Secure login and sign-up with Firebase Authentication, Google Sign-In, and sophisticated username-to-email mapping.
- **Cloud & Local Persistence:** Real-time synchronization between Cloud Firestore and local DataStore for seamless settings and data access across sessions.
- **Visual Achievement Feedback:** "Radiating Achievement State" and "Infinite Progress Architecture" for an immersive goal-completion experience.
- **Centralized Quality of Life (QOL) & Validation Polish:** Centralizes app-wide behaviors to ensure stability and user feedback:
  - Centralized colors and icons inside resource templates (`Color.kt`, `AppIcons.kt`).
  - Validation systems enforcing sensible bounds (Weight 1–500kg, Height 30–300cm, Age 1–150, Goal 100–20000ml).
  - Keyboard focus management (auto-dismiss and field-focus chains).
  - Unsaved change warning guards to prevent losing progress in administration dialogues.
- **Modern UI:** "Glassmorphism" aesthetic built with Jetpack Compose, Material 3, and optimized keyboard navigation.

## 🛠 Tech Stack

- **Language:** [Kotlin](https://kotlinlang.org/)
- **UI Framework:** [Jetpack Compose](https://developer.android.com/compose) (Material 3, Navigation Compose, Material Icons Extended)
- **AI Integration:** [Google Generative AI SDK (Gemini)](https://ai.google.dev/)
- **Backend/Services:** [Firebase](https://firebase.google.com/) (Authentication, Firestore, Analytics) & [Google Play Services Auth](https://developers.google.com/identity/sign-in/android) (Google Sign-In)
- **Image Handling:** [uCrop](https://github.com/Yalantis/uCrop) (Cropping), [Coil](https://coil-kt.github.io/coil/) (Loading), [ImgBB API](https://api.imgbb.com/) (Cloud Hosting)
- **Local Storage:** [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) (Preferences) & SharedPreferences
- **Background Tasks:** [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
- **UX Enhancements:** [Core Splash Screen API](https://developer.android.com/develop/ui/views/launch/splash-screen), [Core Library Desugaring](https://developer.android.com/studio/write/java8-support-table) (Java 8+ APIs on older devices)
- **Dependency Management:** Gradle Version Catalog (`libs.versions.toml`), [JitPack](https://jitpack.io/) (for uCrop)

## 🔑 Setup & Installation

### Prerequisites

- [Android Studio](https://developer.android.com/studio) (Ladybug or later recommended)
- Android SDK with **API Level 24** (minimum) and **API Level 36** (target)
- A physical device or emulator for testing

### Steps

1. **Clone the repository:**

   ```bash
   git clone https://github.com/Ael-1/FluidCheck.git
   ```

2. **Obtain API Keys:**
   - **Gemini AI:** Get your key from [Google AI Studio](https://aistudio.google.com/).
   - **ImgBB:** Create a free account at [ImgBB](https://imgbb.com/signup) and generate an API key from the [API Settings](https://api.imgbb.com/).

3. **Configure Secrets:**
   For security, this project uses a `secrets.properties` file (ignored by version control) to store sensitive keys.
   - Create a file named `secrets.properties` in the **root** directory of the project.
   - Add your keys to the file:
     ```properties
     GEMINI_API_KEY=your_gemini_key_here
     IMGBB_API_KEY=your_imgbb_key_here
     ```

4. **Firebase Setup:**
   - Create a new project in the [Firebase Console](https://console.firebase.google.com/).
   - Enable **Authentication** (Email/Password & Google Sign-In) and **Cloud Firestore**.
   - Add an Android app with the package name `com.example.fluidcheck`.
   - Download the `google-services.json` file and place it in the `app/` directory.
   - **Google Sign-In:** To enable Google Sign-In, you must add your app's **SHA-1 fingerprint** to your Firebase project settings. You can obtain it by running:
     ```bash
     ./gradlew signingReport
     ```
     Then copy the `SHA1` value and add it under **Project Settings > Your Apps > SHA certificate fingerprints** in the Firebase Console.

5. **Build and Run:**
   Open the project in Android Studio, sync Gradle, and run it on an emulator or physical device.

## 📝 Current Status

- [x] Basic UI Layout & Navigation
- [x] Gemini AI Integration (Goal calculation & recommendations)
- [x] Fluid Intake Logging with Edit/Delete capabilities
- [x] Firebase Authentication (Email/Password & Google Sign-In)
- [x] Cloud Firestore Integration (Real-time data syncing)
- [x] Offline Support (Atomic increments & local caching)
- [x] Dynamic Progress Charts (Cumulative daily and historical views)
- [x] Smart Notification Reminders (Hourly & Contextual)
- [x] Admin Dashboard & User Management
- [x] Global Measurement System (Metric & Imperial Unit Switching)
- [x] Premium Paywall Dialog & Tier System (Enterprise Edition)

## 👨‍💻 Developer

**Vincent Rafael Apog**

- 3rd year Computer Engineering student at Mapúa Malayan Colleges Mindanao.
- Developed as a partial fulfillment for CPE144L - Mobile Application Development.
