# AquaTrack: Definitive Android Migration Technical Specification

This document provides exhaustive structural, visual, and behavioral specifications for translating the Fluid Check (AquaTrack) web prototype into a native Android application using Jetpack Compose and Material 3.

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

### 1.2 Typography (Material 3 Type Tokens)
- **Display Large (Hero Percentage)**: Poppins Black, 84sp, Tracking -0.02em.
- **Headline Large (Auth Title)**: Poppins Black, 36sp, Tracking -0.05em.
- **Title Medium (Section Headers)**: Poppins Bold, 22sp.
- **Title Small (Card Titles)**: Poppins SemiBold, 18sp.
- **Body Large (Standard)**: PT Sans Regular, 16sp.
- **Label Large (Buttons)**: PT Sans Bold, 14sp, All-Caps tracking.

---

## 2. Authentication Screen (Login/Signup)
### 2.1 Background Layout
- **Attributes**: Full-screen `Brush.linearGradient(colors = listOf(#3B82F6, #0EA5E9))`.
- **Overlay**: Decorative white circles (`alpha = 0.1f`) with `blur(32.dp)` for depth.

### 2.2 Auth Card (Glassmorphic)
- **Attributes**: `padding(24.dp)`, `RoundedCornerShape(40.dp)`, `border(1.dp, White.copy(0.2f))`, `blur(20.dp)`.
- **CardHeader**:
  - `Title`: "Welcome Back", White, Poppins Black.
  - `Description`: "Login with your credentials", White (60% alpha), PT Sans.
- **Form Elements**:
  - **Username/Password Textboxes**:
    - `Height`: 56.dp.
    - `Shape`: `RoundedCornerShape(16.dp)`.
    - `Background`: `White.copy(alpha = 0.1f)`.
    - `Leading Icon`: `User` / `Lock` vector, White (40% alpha).
    - `Behavior`: Updates state on each character input.
  - **Action Button**:
    - `Background`: `#FFFFFFFF`.
    - `ContentColor`: `#3B82F6`.
    - `Label`: "SIGN IN", PT Sans Bold.
    - `Behavior`: `onClick` validates credentials ('ael'/'1234') -> `setLoggedIn(true)`.
- **Auth Toggle Button**:
  - `Attributes`: Transparent button, White text (60% alpha).
  - `Behavior`: `onClick` toggles `authMode` state between "Login" and "Signup".

---

## 3. Global Navigation & Layout
### 3.1 Transparent Header
- **Attributes**: Height `64.dp`, Background `Color.Transparent`.
- **Logo Pill**: `RoundedCornerShape(12.dp)`, `PrimaryBlue.copy(0.1f)` background.
- **Behavior**: Remains fixed at top of all tabs.

### 3.2 Bottom Navigation Bar
- **Attributes**: Height `110.dp`, `Color.White` background, `0.dp` tonal elevation.
- **Items**: Home (Home Icon), Progress (BarChart Icon), AI Coach (AutoAwesome Icon), Settings (Settings Icon).
- **Visual Specifications**:
  - `Icon Size`: `36.dp`.
  - `Indicator`: Pill-shaped `PrimaryBlue.copy(alpha = 0.15f)` background behind the active icon.
  - `Icon Tint`: `PrimaryBlue` when active, `Color.Gray.copy(alpha = 0.6f)` when inactive.
  - `Labels`: Disabled (`label = null`).
- **Behavioral Logic**:
  - `onClick`: Triggers `onNavigate` to the respective route using `navController`.
  - `Navigation Settings`: `launchSingleTop = true`, `restoreState = true`, and `popUpTo` the start destination.

### 3.3 Floating Action Button (FAB)
- **Attributes**: `56.dp x 56.dp`, `RoundedCornerShape(16.dp)`, `PrimaryBlue`.
- **Behavior**: 
  - **Visibility**: Only `visible` when `activeTab == "Home"`.
  - `onClick`: Opens "Log New Drink" Bottom Sheet.

---

## 4. Home Page
### 4.1 Hero Progress Ring
- **Attributes**: Diameter `420px`, Stroke `18px`.
- **Arc Properties**: Background circle `#FFFFFFFF` (15% alpha), Progress arc `Solid White`.
- **Behavior**: Animates sweep angle based on `(totalIntake / dailyGoal) * 360f`.

### 4.2 Interactive Intake Pill (Edit Goal Trigger)
- **Attributes**: `RoundedCornerShape(50.dp)`, `White.copy(0.2f)` background, `border(1.dp, White.copy(0.1f))`.
- **Content**: Intake text ("1,200 / 3,000 ml") + `Edit2` Lucide icon.
- **Behavior**: `onClick` launches **Update Daily Goal Dialog**.

### 4.3 Update Daily Goal Dialog (Bottom Sheet Style)
- **Attributes**: Aligned to bottom, `RoundedCornerShape(top = 24.dp)`.
- **Input Field**: Large numeric field, `RoundedCornerShape(16.dp)`, autofocus.
- **Button**: "SAVE CHANGES", `PrimaryBlue`, high-elevation shadow.
- **Behavior**: `onClick` updates global `dailyGoal` and closes.

### 4.4 Streak Pill
- **Attributes**: Positioned `16.dp` below progress ring. Glassmorphic.
- **Content**: Flame icon (Orange) + "12 Day Streak" text.

---

## 5. Progress Page
### 5.1 Time Range Tabs
- **Options**: Day, Week, Month, Year.
- **Properties**: Pill container, white active indicator with `shadow(2.dp)`.
- **Behavior**: `onClick` resets `navOffset` to 0.

### 5.2 Date Navigation Bar
- **Properties**: Pill shape, `SecondaryBlue.copy(0.3f)` background.
- **Behavior**:
  - `Arrows`: Increment/Decrement `navOffset`.
  - `Labels`: "Today" -> "Yesterday" -> "March 15, 2024" (Dynamic formatting).

### 5.3 Chart Components (Unified Line Visuals)
- **All Views (Daily, Weekly, Monthly, Yearly)**:
  - **Type**: Line Chart (`monotone` curve).
  - **Stroke**: `PrimaryBlue`, `3.dp` width.
  - **Points**: `4.dp` radius white dots with blue borders.
  - **Behavior**: Displays Tooltip with ml value on long-press/touch.
  - **Data Mapping**:
    - Daily: 12AM -> 11PM hourly intervals.
    - Weekly: Mon -> Sun.
    - Monthly: Week 1 -> Week 4.
    - Yearly: Jan -> Dec.

---

## 6. AI Coach Page
### 6.1 Smart Goal Setter Card
- **Layout**: Grid layout for inputs (Weight, Height, Age, Gender, Activity, Environment).
- **Behavior**: `onClick Calculate` triggers Genkit AI Flow. Displays loading spinner.
- **Result Box**: Large ml text in a `PrimaryBlue.copy(0.1f)` alert container.

### 6.2 AI Recommendations Card
- **Layout**: TextAreas for "Preferences" and "Habits".
- **Behavior**: `onClick Get Recommendation` calls Genkit. Displays tip with `Lightbulb` icon.

---

## 7. Settings Page
### 7.1 Profile Header
- **Layout**: Top gradient section.
- **Elements**: 96dp User Icon, Name Text (Poppins Black), Streak Badge.

### 7.2 Reminders Section
- **Elements**: `Switch` (Material 3 Toggle) + `DropdownSelect` for frequency.
- **Behavior**: Toggle state is persistent; Dropdown selection updates notification worker interval.

### 7.3 Logout Section
- **Attributes**: Full-width Red Button, 14dp height, 24dp corner radius.
- **Behavior**: `onClick` resets `isLoggedIn` and clears cache.
