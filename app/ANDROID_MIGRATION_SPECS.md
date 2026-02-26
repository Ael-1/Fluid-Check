# Fluid Check: Definitive Android Migration Technical Specification

This document provides exhaustive structural, visual, and behavioral specifications for translating the Fluid Check web prototype into a native Android application using Jetpack Compose and Material 3.

---

## 1. Global Design System
### 1.1 Brand Colors (Material 3 Hex Mappings)
- **Background**: `#FFF0F9FF` (Light Cyan)
- **Primary (Water Blue)**: `#FF3B82F6`
- **Primary Container**: `#FFDBEAFE` (Light Blue)
- **Secondary (Accent Blue)**: `#FF0EA5E9`
- **Surface Variant**: `White.copy(alpha = 0.15f)` (Glassmorphism)
- **Error**: `#FFEF4444` (Destructive Red)
- **Success**: `#FF22C55E`
- **White (High Alpha)**: `#FFFFFFFF`
- **Progress Lap 1**: `#FFACE6FD` (Light Blue 200)
- **Progress Lap 2 / Glow Cyan**: `#FF40E6FD` (Vibrant Cyan)
- **Google Neutral Border**: `#FF747775` (Official Branding)

### 1.2 Typography (Material 3 Type Tokens)
- **Display Large (Hero Percentage)**: Poppins Black, 68sp, Tracking -0.02em.
- **Headline Large (Auth Title)**: Poppins Black, 36sp, Tracking -0.05em.
- **Title Medium (Section Headers)**: Poppins Bold, 22sp.
- **Title Small (Card Titles)**: Poppins SemiBold, 18sp.
- **Body Large (Standard)**: PT Sans Regular, 16sp.
- **Label Large (Buttons)**: PT Sans Bold, 14sp, All-Caps tracking.

### 1.3 Asset & Text Management (Resource Architecture)
- **Unified Icons (`AppIcons.kt`)**: 
    - Centralized semantic naming (e.g., `AppIcons.Streak`).
    - `AppIcons.GoogleLogo` pointing to official `ic_google_logo.xml`.
- **Unified Strings (`strings.xml`)**:
    - Zero hard-coded UI strings.
    - Placeholder Pattern: Dropdowns use "Select...", Text fields use "Input...".
- **Text Robustness**:
    - All labels, inputs, and placeholders must use `maxLines = 1` and `TextOverflow.Ellipsis`.
    - Prevents UI slicing/stretching on small devices.
    - Fields must remain a fixed height (`58.dp`) regardless of content length.

---

## 2. Onboarding & Setup
### 2.1 Initial Setup Assistance
- **Trigger**: New users or forced via Firestore record check (`setupCompleted` flag).
- **Data Capture**: Weight, Height, Age, Sex, Activity Level, and Environment.
- **AI Integration**: Invokes Gemini AI to calculate an ideal hydration goal based on physical data.
- **Goal Confirmation**: Displays an `AlertDialog` prompting the user to accept the AI-calculated goal or use the system default (3000ml).
- **Skip Functionality**: Users can "Skip for now", which bypasses physical data entry and sets the daily goal to the 3000ml default.
- **Reversibility**: Informational text clarifies that settings can be changed later in the app.

---

## 3. Authentication Screens
### 3.1 Global Auth Background
- **Attributes**: Full-screen `Brush.linearGradient`.
- **Interactivity**: Tap to clear focus.

### 3.2 Login Screen (LoginScreen.kt)
- **Traditional Auth**: Sign-in via Username/Email and Password. Supports username-to-email resolution via Firestore.
- **Social Auth (GMS Only)**:
    - **Trigger**: Only visible if Google Play Services is detected (`GoogleApiAvailability`).
    - **Visuals**: Centered 40dp white circular button with official borderless 24dp Google "G" logo.
    - **Layout**: Positioned below primary actions, separated by "or sign in with" horizontal divider.
- **Action Buttons**: SIGN IN (Primary), SIGN UP (Outlined).

### 3.3 Sign Up Screen (SignUpScreen.kt)
- **Validation**:
    - Username: `R.string.error_empty_username`.
    - Email: `R.string.error_invalid_email`.
    - Password: min 6 chars, `R.string.error_short_password`.

---

## 4. Global Navigation & Layout
### 4.1 UI Components & Responsiveness
- **Fixed Height Inputs**: Text fields and custom dropdown containers set to `58.dp` or `64.dp` (Home dialog) to ensure box stability.
- **Adaptive Grid**: Hero section and cards adjust padding based on screen height.
- **Dropdown Docking**: `ExposedDropdownMenu` must be correctly anchored using `Modifier.menuAnchor()` and `Modifier.exposedDropdownSize()` to appear docked directly below the field without overlaying it.

### 4.2 Floating Action Button (FAB)
- **Attributes**: `64.dp`, Primary Blue, `AppIcons.Add` icon.
- **Behavior**: Opens "Log New Drink" sheet with validated Amount and Time fields.

---

## 5. User Features (Firestore Driven)
### 5.1 Home Page (Hero Progress Ring)
- **Infinite Progress Architecture**: 
    - Supports unlimited laps using alternating colors (Lap 1: `#ACE6FD`, Lap 2+: `#40E6FD`).
    - Base layers use `drawCircle` for solid fills, active layers use `drawArc` with `StrokeCap.Round`.
- **Achievement State (Ring Closed)**:
    - Trigger: `totalIntake >= dailyGoal`.
    - Metrics card transforms into a **Radiating Achievement Card**.
    - Visuals: Intensive Cyan (`#40E6FD`) outer glow, 30dp pulse spread, and radiant border stroke.
- **Recent Logs**: Displays last 5 entries with Edit/Delete capabilities.
- **Goal Management**: "Update Daily Goal" dialog uses `64.dp` high fields with `18.sp` font size.

### 5.2 AI Coach Page
- **Smart Goal Setter**: Interactive form using `UserRecord` fields.
- **Goal Application**: Features a "Set as Daily Goal" action that persists the AI-recommended value to Firestore.

---

## 6. Admin Dashboard
### 6.1 User Management
- **Model**: Fetches directory listings from Firestore `users` collection.
- **Actions**: Edit/Delete icons mapped to `AppIcons`.

---

## 7. Profile & Information Management
### 7.1 Edit Profile Screen
- **UserRecord Section**: Allows updating physical data in Firestore.
- **Consistency**: Uses same `ResponsiveEditField` and `ResponsiveDropdownField` components as Initial Setup.

---

## 8. Architecture & Data Management
### 8.1 Package Organization
- `com.example.fluidcheck.ui`: Composables and screen logic.
- `com.example.fluidcheck.model`: Data classes (`FluidLog`, `UserRecord`).
- `com.example.fluidcheck.repository`: `AuthRepository` (Firebase Auth) and `FirestoreRepository`.

### 8.2 Backend (Firebase)
- **Authentication**: Firebase Auth with Email/Password and Google Sign-In support.
- **Database**: Cloud Firestore.
    - `users`: Main user records and sub-collection `fluid_logs`.
    - `usernames`: Mapping collection for username-based login resolution.
- **Security Rules**: Restricted to authenticated owners with public read for username lookup.

### 8.3 AI Integration
- **Service**: `GeminiCoach.kt` utilizing `GenerativeModel`.
- **Context**: Prompts explicitly reference "Biological Sex" for clinical accuracy in hydration modeling.
