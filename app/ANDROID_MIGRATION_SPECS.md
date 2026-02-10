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
- **Progress Lap 2**: `#FF40E6FD` (Vibrant Cyan/Blue)

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
    - `AppIcons.Gender` used for biological Sex fields.
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
- **Trigger**: New users or forced via debug credentials (`ael/1234`). Controlled by `isSetupComplete` boolean in DataStore.
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
- **Credentials**: Validated against hard-coded mock data or local persistence.
- **Action Buttons**: SIGN IN (Primary), SIGN UP (Outlined), Gmail (Outlined).

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

## 5. User Features (UserRecord Driven)
### 5.1 Home Page (Hero Progress Ring)
- **Multi-Lap Progress**: Ring layers for >100% goals.
- **Recent Logs**: Displays last 5 entries with Edit/Delete capabilities.
- **Goal Management**: "Update Daily Goal" dialog uses `64.dp` high fields with `18.sp` font size for high visibility.

### 5.2 AI Coach Page
- **Smart Goal Setter**: Interactive form using `UserRecord` fields.
- **Goal Application**: Features a "Set as Daily Goal" action that persists the AI-recommended value directly to device storage.
- **Dropdowns**: Capped at `280.dp` height with internal scrolling to prevent screen overflow.
- **Validation**: "Calculate" button is disabled or blocked until all mandatory states are resolved.

---

## 6. Admin Dashboard
### 6.1 User Management
- **Model**: Uses `UserCredentials` for directory listings (Name, Email, Role, Streak).
- **Actions**: Edit/Delete icons mapped to `AppIcons`.

---

## 7. Profile & Information Management
### 7.1 Edit Profile Screen
- **UserRecord Section**: Allows updating physical data.
- **Validation**: "Save Changes" blocked if fields are blank or set to selection placeholders.
- **Consistency**: Uses same `ResponsiveEditField` and `ResponsiveDropdownField` components as Initial Setup.

---

## 8. Architecture & Data Management
### 8.1 Package Organization
- `com.example.fluidcheck.ui`: Composables and screen logic.
- `com.example.fluidcheck.model`: Data classes (`UserCredentials`, `UserRecord`, `FluidLog`).
- `com.example.fluidcheck.repository`: Local persistence and data logic.

### 8.2 Local Persistence (DataStore)
- **Implementation**: `androidx.datastore:datastore-preferences`.
- **Scope**: Stores `UserRecord` (physical data), `dailyGoal`, and `isSetupComplete` status indexed by username.
- **Reactivity**: Uses Kotlin `Flow` to provide real-time updates to the UI when preferences change.

### 8.3 AI Integration
- **Service**: `GeminiCoach.kt` utilizing `GenerativeModel`.
- **Connectivity**: Requires internet connection; provides fallback error handling ("Check connection") if offline.
- **Context**: Prompts explicitly reference "Biological Sex" for clinical accuracy in hydration modeling.
