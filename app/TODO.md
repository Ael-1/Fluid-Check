# FluidCheck Development Plan

## 1. Application wide feature \\DONE

1. Offline Support & Quota Optimization: Refactor all `runTransaction` operations in `FirestoreRepository` to use `WriteBatch` and `FieldValue.increment()`. This enables offline writes and eliminates unnecessary read operations currently used to calculate totals, directly addressing the "Read quota exceeded" issue. \*\*DONE
2. Real-time Connectivity Tracking: Implement a `NetworkMonitor` utility to provide a reactive `StateFlow<Boolean>` for internet availability across the UI. \*\*DONE
3. Smart System Status (Settings): Enhance "Auto Sync Data" logic. It should turn red if offline or if the Firestore persistence layer detects sync errors. Green only when a stable connection to the database is verified. \*\*DONE
4. Dynamic Cloud Backup Indicator (Settings):
   - For Guest Users: Permanently "Disabled".
   - For Cloud Users: Display "Syncing..." while Firestore has `hasPendingWrites = true`, and "Enabled" only once synchronization is complete. \*\*DONE
5. Global Offline Consistency: Ensure all app roles (Admin, Moderator, User) can interact with cached data (logs, profiles, streaks) when disconnected, with automatic background reconciliation when re-established. \*\*DONE
6. Role Promotion Restriction: In EditUserDetailedDialog, restrict the role dropdown so that an Admin can only assign USER or MODERATOR roles. The ADMIN option must not appear. Only the system admin account should ever hold the ADMIN role. \*\*DONE
7. Real-time Role Reconciliation: When a user's role is changed by an admin, the app must detect this in real-time via Firestore listener. The session should be **restarted in-place**: re-evaluate the user's role, automatically switch the UI to the highest available mode (e.g., if demoted to USER, switch to User Mode), and display a dialog: 'Your role has been updated to [NEW_ROLE].' The user should NOT need to sign out. \*\*DONE
8. Unsaved Changes Guard (Admin): In `EditUserDetailedDialog` (Admin Dashboard), implement a `hasChanges()` check. When the admin presses the back/close button while there are unsaved changes, display a confirmation dialog: 'You have unsaved changes. Discard changes?' with 'Discard' and 'Keep Editing' buttons. \*\*DONE
9. Adaptive Save Button: In `EditProfileScreen`, disable the 'Save Changes' button (grayed out) when `hasChanges()` returns false. The button should only become enabled when the user has modified at least one field. \*\*DONE
10. Enhanced Username Validation: Enforce validation at Sign Up, Edit Profile, and Admin Edit: 4–20 alphanumeric characters, underscores (`_`), or periods (`.`). No spaces/emojis. Case-insensitive uniqueness check in Firestore. Display clear inline error messages. \*\*DONE
11. Offline Intake Feedback: When logging a fluid intake while offline, display a Toast: 'Intake logged locally. It will sync when you're back online.' Applies to manual logs and Quick Add. \*\*DONE
12. Reliable Midnight Reset: Implement a reset mechanism at 12:00 AM (GMT+8) to: 1. Reset progress ring (totalIntake to 0). 2. Evaluate/update streak and `lastRingClosedDate`. 3. Auto-refresh UI if app is open. 4. Handle catch-up if app was closed overnight. Use `WorkManager` for background consistency. \*\*DONE
13. Graceful Error Handling: Audit all coroutine-launched database operations (`FirestoreRepository`, `GuestRepository`) and their call sites across all screens. Wrap any unprotected `scope.launch { ... }` blocks that perform Firestore reads/writes in `try-catch` blocks. On caught exceptions, display a `Toast` with a user-friendly error message (e.g., "Something went wrong. Please try again."). Focus on call sites in `MainScreen`, `HomeScreen`, `EditProfileScreen`, `AdminDashboard`, `SettingsScreen`, `AICoachScreen`, and `MainActivity`. \*\*DONE
14. Role Change Detection on Cold Start: Persist the user's last-known role to `DataStore` (e.g., `UserPreferencesRepository`). On app launch, compare the stored role against the Firestore-fetched role. If they differ, show the "Your role has been updated to [NEW_ROLE]" dialog and update the stored role. This covers role changes that occur while the app is completely closed. \*\*DONE
15. Fix Offline Intake Feedback: When logging a fluid intake (via manual log or Quick Add) while offline, the feedback message ("Saved locally. Will sync once online.") does not appear. Debug the `isConnected` state in `MainScreen` to ensure it reflects the correct network status at the time the log is saved. Verify the Snackbar is displayed for both `saveFluidLog` (new logs) and Quick Add operations. Also check that the Quick Add callback in `HomeScreen` triggers the same offline feedback path as manual logging. \*\*DONE

## 2. Home screen \*\*DONE

1. Users can UPDATE daily goal \*\*DONE
2. Users can CREATE new log drink, then log drink is stored in Firestore (fluid type, amount and log date) \*\*DONE
3. In Recent Logs, READ from Firestore to display the user's logs drink from THIS DAY ('THIS DAY' means it starts from 12:00am to 11:59pm) \*\*DONE
4. In View Full Log History, READ from Firestore to display user's ALL log drink \*\*DONE
5. User streak (consecutive number of days the user closed their progress ring) is stored in firestore and displayed in its container in Home Page \*\*DONE
6. Users can UPDATE their log drink within 24 hours of the CREATION of the log drink \*\*DONE
7. Users can DELETE their log drink within 24 hours of the CREATION of the log drink \*\*DONE
8. Quick-Add Buttons: \*\*DONE
   - Three default examples (e.g., 250ml, 500ml, 750ml).
   - A 'plus' button allowing users to customize their preferred quick-add drinks.
   - Maximum of 3 quick-add slots; the 'plus' button disappears once 3 are configured.
9. Unified Log Interaction: Remove the edit button icon from log items in 'Recent Logs' and 'View Full Log History'. Tapping the entire log row should trigger the Edit Log dialog. Verify current implementation and clean up UI icons. \*\*DONE
10. Date Section Headers in Log History: In the "View Full Log History" dialog, group fluid logs by date and display section headers with contextual labels: **"Today"** for the current date, **"Yesterday"** for the previous date, and **"Previous Logs"** as a single header for all older dates. Logs within each group should remain sorted by time (most recent first). \*\*DONE
11. Stacked Dialog for Log Editing: When a user taps a log entry in the "View Full Log History" dialog, the Edit Log dialog should open **on top of** the history dialog (which remains visible but dimmed in the background). When the Edit Log dialog is dismissed (saved or cancelled), the user should return to the Log History dialog — not back to the Home Screen. \*\*DONE
12. Date Format in Log History: Display log dates in the format **"January 2, 2026"** (`MMMM d, yyyy`). For narrower screens or when space is constrained, dynamically truncate to **"Jan 2, 2026"** (`MMM d, yyyy`). Use `LocalConfiguration.current.screenWidthDp` or text measurement to determine which format to use. \*\*DONE

## 3. Progress screen \*\*DONE

1. READ Firestore to see the date and amount of their log drink which is then displayed in Your Progress \*\*DONE
2. Weekly Habits Scorecard (AI Progress Rating):
   - Data & Trigger: Compile the user's logged intake data from the last 7 days. \*\*DONE
   - AI Processing: Send this weekly intake history to `GeminiCoach` (using Gemini 3 Flash Live model). Instruct the AI to compute a comprehensive Weekly Hydration Rating (e.g., "A", "B+", etc.) and provide a concise, personal summary of successes (e.g., "Hit goal 6/7 days") and gaps (e.g., "Weekend intake was low"). \*\*DONE
   - UI Display: In `ProgressScreen.kt`, add an "AI Weekly Scorecard" section. Display a large visual grade badge with dynamic colors matching the rating, alongside the AI's feedback text. \*\*DONE

## 4. AI Coach screen \*\*DONE

1. READ Firestore to see user records (weight, height, etc.) so the textfields of these attributes in Personalized Goals are automatically inputted \*\*DONE
2. AI Logs & Habits Analysis (Assessment Engine):
   - Model Choice: Use Gemini 3 Flash Live as the core model. \*\*DONE
   - Data Collection: Query the user's last 14 days of fluid logs (`FluidLog` documents containing amount, type, timestamp) and user profile metrics (`weight`, `height`, `age`, `sex`, `activity`, `environment`) from Firestore remote or offline local cache. \*\*DONE
   - Gemini Processing: Pass the collected log dataset to `GeminiCoach`. Prompt the model to analyze: (a) hydration volume adequacy based on profile stats, (b) time-of-day distribution patterns, and (c) fluid type composition (e.g., coffee/tea diuretic analysis vs pure water). \*\*DONE
   - UI Integration: In `AICoachScreen.kt`, add a new "AI Hydration Assessment" card. Include a prominent "Analyze My Habits" button. When clicked, display a loading spinner, invoke the model, and render the resulting analysis in a structured, clean text container. \*\*DONE
3. AI-Powered Predictive Alerts & Custom Notification Rules:
   - Rule Generation: During the "Analyze My Habits" flow, instruct Gemini to output up to 3 predictive hydration alerts formatted as a structured JSON array. Example: `[{"dayOfWeek": 6, "time": "15:00", "message": "Based on your usual Friday habits, you tend to forget to drink water after 3:00 PM. Here is your extra nudge!"}]` (where Sunday = 1, Saturday = 7). \*\*DONE
   - Local & Remote Storage: Parse the JSON rules and save them in DataStore (`UserPreferencesRepository`) and Firestore under the user's profile document (`/users/{userId}/predictive_reminders`). \*\*DONE
   - Customization UI: Add a UI section in `SettingsScreen` under Notifications allowing users to view the list of current AI-generated predictive alert rules, toggle individual rules on/off, or trigger a regeneration. \*\*DONE
   - Dynamic Scheduling: Modify `NotificationHelper` / `WorkManager` background jobs to read these custom rules. Schedule local notification alarms that trigger on the specified days and times, sending the personalized alert if the user's intake is currently under 50% of their goal for the day at that hour. \*\*DONE
4. Dynamic Weather Integration:
   - Weather API Integration: Integrate a free weather API (e.g., Open-Meteo) to fetch the user's current environment temperature and humidity. \*\*DONE
   - Recalculation & Alerting: If weather metrics change dramatically (e.g., high heat or humidity), invoke `GeminiCoach` to automatically recalculate the recommended goal. Update the user's daily goal dynamically and display a descriptive notification/alert: "Daily goal adjusted by +500ml today due to high local heat (34°C)." \*\*DONE
   - Goal Control Toggle: Add a toggle switch in the "Personalized Goals" card on the `AICoachScreen` allowing users to enable/disable "Dynamic Weather Goal Adjustment". If disabled, the daily goal remains manual and static. \*\*DONE

## 5. Settings screen \*\*DONE

1. READ Firestore for username to be displayed in profile header \*\*DONE
2. READ Firestore for user's streak amount to be displayed in its container in Settings \*\*DONE
3. users can CREATE or UPDATE their display photo in Edit Profile \*\*DONE (Framework in place)
4. users can UPDATE their username in Edit Profile \*\*DONE
5. users can UPDATE their email address in Edit Profile \*\*DONE
6. users can UPDATE their password in Edit Profile \*\*DONE
7. users can UPDATE their weight, height, age, sex, activity level, and environment in Edit Profile \*\*DONE
8. READ from Firestore to display user's username and email in Edit Profile \*\*DONE
9. READ from Firestore to display personal records in Edit Profile \*\*DONE

## 6. Admin Dashboard screen \*\*DONE

1. READ Auth or Firestore to display total users \*\*DONE
2. READ Firestore for total downloads \*\*DONE (Using Total Users as proxy)
3. READ Auth or Firestore to display active users \*\*DONE
4. READ Firestore and code calculation for avg streak \*\*DONE
5. Admin can READ from Auth or Firestore to search users \*\*DONE
6. Admin can UPDATE user's username \*\*DONE
7. Admin can UPDATE user's role (admin or user) \*\*DONE
8. Admin can UPDATE user's personal records (weight, height, etc.) \*\*DONE
9. READ from Firestore to display each user's 'data'. 'data' are the following:
   account created, email, username, total rings closed, daily goal, streak, highest streak, all logs, personal records, display photo and any more you can think of possible data that is relevant for an admin to see \*\*DONE
10. Admin can DELETE users \*\*DONE
11. Add a progress graph for each user (similar to 'Your Progress') \*\*DONE
12. Instead of a 'Actions' column with respective buttons, pressing the whole row of the user will open the Edit User Details dialog \*\*DONE
13. In the Edit User Details dialog, add a delete button where there are double confirmation for the admin. The first is it will ask the admin if they are sure to delete this user and the action cannot be undone. If they press confirm, second confirmation prompt will pop up and the admin will input the admin account password to proceed. \*\*DONE
14. If holding a user in the user directory, it will be in selection mode, in selection mode, the whole row of the user will have a light blue background (similar to some apps that indicates that it is selected), and in its left side there is a checkbox (it is checked. Again indicating that it is selected). A delete button will appear in selection mode. when delete button is pressed, it will have the similar double confirmation for the admin. \*\*DONE
15. Profile Photo in Admin Dialog: In the `EditUserDetailedDialog`, display the user's profile photo in the header. Use `AsyncImage` (Coil) to load the URL, with a placeholder if missing. Should be a circular photo matching the Edit Profile aesthetic. \*\*DONE
16. Admin Dashboard Stats Restructure: Remove the "Active Now" stat card and its container entirely. Move the "Avg. Streak" card into the position previously occupied by "Active Now". Leave the original "Avg. Streak" position empty (no card rendered there). The remaining stats grid should have 3 cards: Total Users, Total Downloads, and Avg. Streak (in the former Active Now slot). \*\*DONE

## 7. Firestore Collection/Fields \*\*DONE

1. isDeleted (Boolean): Instead of permanently deleting a user's data (which can be risky), you could mark it as deleted. This is safer for data integrity and allows for potential recovery. \*\*DONE
2. fcmToken (String): If you plan to add push notifications (e.g., reminders to drink water) in the future, you'll need to store the user's Firebase Cloud Messaging token. \*\*DONE
3. quickAddConfig (Array of Objects): To store the user's custom Quick-Add settings. Each object could contain amount and fluidType. This directly supports your "plus" button feature. \*\*DONE
4. notificationsEnabled (Boolean): To let the user opt-in or out of app notifications. \*\*DONE
5. lastRingClosedDate (String or Timestamp): To track when they last hit their goal. This is crucial for calculating the streak correctly (checking if it was "yesterday"). \*\*DONE
6. highestStreak (Int): To display the user's all-time record, which is a common motivational feature. (You mentioned this in the Admin section, but it should be stored in the user document). \*\*DONE
7. totalFluidDrankAllTime (Int): A running total of all fluid logged. It's much faster to read this single field than to query and sum up all historical logs. \*\*DONE
8. createdAt (Timestamp): Store the date and time when the user first registered. \*\*DONE
9. totalRingsClosed (Int): Store the total amount of progress closed by the user (progress rings that are closed multiple times is counted) \*\*DONE
10. Old Username Cleanup Bug: When an admin updates a user's username from the User Directory, the old username document persists in the `usernames` collection. **Root cause**: In `AdminDashboard.kt` `onSave`, `saveUserRecord()` is called first (which creates the new username mapping via `batch.set`), and then `updateUsername()` is called. But `updateUsername()` checks `if (newUsernameDoc.exists())` and **fails silently** with "Username already taken" because `saveUserRecord()` already created it — so the old username is **never deleted**. Fix: either (a) call `updateUsername()` **before** `saveUserRecord()`, or (b) remove the username write from `saveUserRecord()` when a username change is detected, or (c) skip the availability check in `updateUsername()` when it's an admin operation. Ensure the old username mapping is deleted on every successful username change from both Edit Profile and Admin Dashboard. \*\*DONE

## 8. Moderator Mode \*\*DONE

\*ALMOST ALL TASKS IN 6. Admin Analytics screen EXCEPT:

1. Moderator CANNOT UPDATE user's role (admin or user) or any user data \*\*DONE
2. and DELETE users and any user features of the admin \*\*DONE

## 9. Sign in/Sign up \*\*DONE

1. Create a button continue as guest. this user is a 'GUEST' this type of user is not stored in the database. Any data and information from these users are stored only on their device. For every device, there is 1 guest user created. Regardless of how many users (from database) that are being logged in on a device that has a guest user, the guest user can still be logged in as long as user from that device will press continue as guest \*\*DONE
2. Multi-Identifier Sign-In: In `LoginScreen`, change input label to 'Email or Username'. Update backend logic to support lookup: if input contains '@', treat as email; otherwise, query Firestore for username to resolve the email before authenticating. \*\*DONE
3. Proactive Conflict Prevention: During sign-up, check Firestore for existing usernames (case-insensitive) and Auth for existing emails before creation. Surface specific errors: 'Username taken' or 'Email exists'. Apply same checks to Edit Profile updates. \*\*DONE
4. When there is no internet connection and Pressing 'Sign in' or 'Sign up' button, dialog box should prompt the user if to check if there is an internet connection \*\*DONE'
5. Google Sign-In Visibility Guard: Verify that `isGoogleAvailable` is **explicitly passed** from `MainActivity` to `LoginScreen` and `SignUpScreen` (not relying on the default `true`). Change the default parameter value for `isGoogleAvailable` in both `LoginScreen` and `SignUpScreen` from `true` to `false` to be fail-safe. Test on a device/emulator without Google Play Services to confirm the "or sign up with" section is hidden. \*\*DONE
6. Login Identifier Label: In `LoginScreen`, change the placeholder text of the identifier field from `R.string.username_label` ("Username") to a new or existing string resource that reads **"Email or Username"**. This aligns the UI with Task 9.2's multi-identifier sign-in logic that already supports both email and username input. \*\*DONE

## 10. Notification Feature \*\*DONE

1. Add a notification feature for the app (works regardless if there is internet connection or not), though it should fetch the field value from the database if notifications are enabled or not. \*\*DONE
2. According to the dropdown list options from the reminder frequency, the user can choose from every 30 min, 1 hr, 2 hrs, and 4 hrs. Picking an option should also be stored in the database (if there is no internet connection, when choosing, store locally until there is connection in the database, store it.) \*\*DONE
3. Add random notifications for the app too. Like reminders that they haven't finished their progress ring, or their streak is about to break, etc. (Add more notification that is relevant for the user). \*\*DONE

## 11. User Fields and Keyboard Behaviors (This will apply to ALL user fields inside the app) \*\*DONE

1. Global Keyboard Dismissal: Tapping anywhere outside a text field should hide the keyboard. Use `Modifier.pointerInput` with `detectTapGestures` calling `clearFocus()` on the root composable. \*\*DONE
2. Smart Keyboard Actions: Configure `KeyboardActions` app-wide: 'Next' moves focus to the next field; 'Done' triggers the primary action. Set appropriate `ImeAction` (Next/Done) for all forms. \*\*DONE
3. Unified Input Trimming: Apply `.trim()` to all user text inputs at the point of submission (Login, Sign-up, Edit Profile, Log Intake, AI Coach, Search, etc.) to ensure clean data storage. \*\*DONE
4. Non-expanding Input Fields: Ensure all text fields app-wide use `singleLine = true` (or `maxLines = 1`). Text should scroll horizontally within the field, not wrap vertically. \*\*DONE
5. Keyboard Type Audit: Set correct `KeyboardType` via `KeyboardOptions` for all inputs: Email -> Email; Password -> Password; Numeric (weight, goal, etc.) -> Number; Username/Search -> Text. \*\*DONE
6. Apply Keyboard Behaviors to Remaining Fields: Implement Tasks 11.1 through 11.5 (global keyboard dismissal, smart ImeActions, input trimming, singleLine, and correct KeyboardType) for the following fields that were missed. Audit each field first to check which of 11.1–11.5 are already applied and only add what's missing:
   - Edit User Details in User Directory from Admin Dashboard
   - Edit Profile 'Username', 'Email Address', 'New Password', 'Confirm Password', 'Weight', 'Height', and 'Age' text fields
   - 'Daily Goal' text field in Update Daily Goal
   - Edit Log 'Amount' text field in Home screen
   - Log New Drink 'Amount' text field in Home screen
     \*\*DONE
7. Initial Setup Keyboard Polish: Apply Tasks 11.1 through 11.5 to `InitialSetupScreen.kt`: global keyboard dismissal on the root composable, `singleLine = true` for all text fields, correct `KeyboardType` (Number for Weight/Height/Age), `ImeAction.Next`/`Done` flow, and `.trim()` on submission. Dropdown fields (Sex, Activity, Environment) should be skipped in the focus chain (see Task 11.8). \*\*DONE
8. Custom Focus Order for Personal Records: In both `AICoachScreen` and `InitialSetupScreen`, use `FocusRequester` to define a custom focus chain: Weight → Height → Age → Done. Pressing "Next" on the Height field should skip the Sex dropdown and jump directly to the Age field. Pressing "Done" on the Age field should dismiss the keyboard. \*\*DONE

## 12. Security & Validation Polish \*\*DONE

1. Admin Self-Guard: Prevent admins from demoting or deleting their own account within the Admin Dashboard to avoid accidental lockout. \*\*DONE
2. Numeric Range Validation: Enforce reasonable limits on all numeric inputs (e.g., Weight 1–500kg, Height 30–300cm, Age 1–150, Daily Goal 100–20000ml). \*\*DONE
3. Button Loading States: Show a loading indicator (spinner) and disable 'Save' or 'Submit' buttons during asynchronous operations to prevent double-tap submissions. \*\*DONE
4. Google Account Restrictions: For accounts created via Google Sign-In, disable the ability to edit username and password in 'Edit Profile'. These fields should be read-only for Google-authenticated users. \*\*DONE
5. Password Consistency: Enforce the same 6-character minimum length for password updates in Edit Profile as used in the Sign Up flow. \*\*DONE

## 13. Email Verification \*\*DONE

1. Verify Current Email Address: Add a "Verify Email" button in `EditProfileScreen` inside `ProfileSettingsContainer`, below the email field (visible only when `!isGoogleUser && authRepository.currentUser?.isEmailVerified == false`). When tapped, call a new `AuthRepository.sendEmailVerification()` method (wrapping `auth.currentUser?.sendEmailVerification()?.await()`). On success, show feedback via `statusDialogData` (the existing AlertDialog pattern used throughout the screen): "Verification email sent. Please check your inbox." If the email is already verified, hide the button entirely and optionally show a small "✓ Verified" label. \*\*DONE
2. Verify-Before-Update Email Flow: Replace the current `AuthRepository.updateEmail()` (which calls the deprecated `auth.currentUser?.updateEmail()`) with a new `AuthRepository.verifyBeforeUpdateEmail(newEmail)` method (wrapping `auth.currentUser?.verifyBeforeUpdateEmail(newEmail)?.await()`). In `EditProfileScreen`, update the email save block (currently at Step 3 in the save flow, around line 615–625) to call `verifyBeforeUpdateEmail()` instead of `updateEmail()`. On success, show via `statusDialogData`: "A verification email has been sent to [newEmail]. Your email will update after you verify it." **Crucially**, do **not** write the new email to the Firestore user record in `newRecord.copy()` (currently line 648) — keep the old email until Firebase Auth confirms the change on next sign-in. The existing re-authentication flow (`showReauthDialog`) already gates email changes, so no additional re-auth logic is needed. Google Sign-In users are already excluded (their email field is read-only per Task 12.4). \*\*DONE

## 14. Codebase Cleanup

### 14.1 Restructure Folder, Subfolder, and File Architecture

Reorganize the project source tree under `app/src/main/java/com/example/fluidcheck/` to follow clean architecture layering (data → domain → presentation), improving discoverability and separation of concerns.

**Current structure** is a flat package-by-layer approach: `model/`, `repository/`, `ui/`, `util/`, `ai/` — all at the same level with `MainActivity.kt` (37KB) and `MainScreen.kt` (59KB) as monolithic files.

**Target structure (Clean Architecture):**

```
com/example/fluidcheck/
├── MainActivity.kt              (Slim: only setContent + top-level nav)
├── data/
│   ├── local/
│   │   └── UserPreferencesRepository.kt   (from repository/)
│   ├── remote/
│   │   ├── AuthRepository.kt              (from repository/)
│   │   ├── FirestoreRepository.kt         (from repository/)
│   │   └── GuestRepository.kt             (from repository/)
│   └── model/
│       ├── ChartData.kt                   (from model/)
│       ├── FluidLog.kt                    (from model/)
│       ├── FluidType.kt                   (from model/)
│       ├── NavigationItem.kt              (from model/)
│       └── UserData.kt                    (from model/)
├── domain/
│   └── ai/
│       └── GeminiCoach.kt                 (from ai/)
├── ui/
│   ├── MainScreen.kt
│   ├── admin/AdminDashboard.kt
│   ├── auth/ (LoginScreen.kt, SignUpScreen.kt, VerifyAccountScreen.kt, AuthComponents.kt)
│   ├── components/                        (NEW — extract from MainScreen.kt)
│   │   ├── LogNewDrinkSheet.kt            (currently at ~line 679 in MainScreen.kt)
│   │   ├── EditLogDialog.kt               (currently in MainScreen.kt)
│   │   └── BottomNavigation.kt            (FluidBottomNavigation at ~line 1147 in MainScreen.kt)
│   ├── navigation/NavRoutes.kt
│   ├── screens/ (AICoachScreen.kt, AboutDeveloperScreen.kt, EditProfileScreen.kt, HomeScreen.kt, InitialSetupScreen.kt, ProgressScreen.kt, SettingsScreen.kt)
│   └── theme/ (AppIcons.kt, Color.kt, Theme.kt, Type.kt)
└── util/ (unchanged)
```

**Steps:**

1. Create new directories: `data/local/`, `data/remote/`, `data/model/`, `domain/ai/`, `ui/components/`.
2. Move files to their new locations as shown above.
3. Update all `package` declarations in moved files (e.g., `package com.example.fluidcheck.repository` → `package com.example.fluidcheck.data.remote`).
4. Update **all import statements** across the entire codebase to reference the new package paths. Search for old package prefixes: `com.example.fluidcheck.repository`, `com.example.fluidcheck.model`, `com.example.fluidcheck.ai`.
5. Extract from `MainScreen.kt`: `LogNewDrinkSheet` composable → `ui/components/LogNewDrinkSheet.kt`; `EditLogDialog` composable → `ui/components/EditLogDialog.kt`; `FluidBottomNavigation` composable → `ui/components/BottomNavigation.kt`.
6. Delete `PlaceholderScreens.kt` (87 bytes — contains only a package declaration and a comment, confirmed dead file).
7. **Verify:** Run `./gradlew assembleDebug` — build must succeed with zero unresolved reference errors. Perform after each major move step as this is a high-risk refactor touching every file.

---

### 14.2 Remove Hard-Coded Admin Username and Email in MainActivity

Remove any hard-coded admin-specific identity values (username, email, or role assignments) from `MainActivity.kt`. All admin identity should be derived solely from the `UserRecord.role` field stored in Firestore.

**Current state:** No literal hardcoded admin email/username strings (like `"admin@example.com"`) exist. However, multiple raw string literals are used as identity constants throughout `MainActivity.kt`:

- `"GUEST"` used as a user ID fallback (lines 84, 280, 292, 296-302, 424, 510, 516, 552).
- `"Guest"` used as a display username (lines 298, 303, 424).
- `"USER"` used as a default role (lines 300, 458).
- `3000` used as a default daily goal (lines 461, 519).

**Steps:**

1. Search `MainActivity.kt` and the full codebase for any string literals matching admin usernames or emails. Check for patterns: `"admin"`, `"ADMIN"`, any `@` email addresses, any known developer names.
2. Extract the raw string/number literals into named constants:
   ```kotlin
   companion object {
       const val GUEST_USER_ID = "GUEST"
       const val GUEST_USERNAME = "Guest"
       const val DEFAULT_ROLE = "USER"
       const val DEFAULT_DAILY_GOAL = 3000
   }
   ```
3. Replace all occurrences of `"GUEST"`, `"Guest"`, `"USER"`, and `3000` (as default goal) with the named constants across `MainActivity.kt` and any other files referencing these values.
4. Confirm admin role checking remains purely database-driven (currently correct — `MainScreen.kt` line 105: `val isDatabaseAdmin = userRole == "ADMIN" || userRole == "MODERATOR"`). Extract role string literals (`"ADMIN"`, `"MODERATOR"`, `"USER"`) into an enum or constants object for type safety.
5. **Verify:** Search the entire codebase for any remaining hardcoded admin emails or usernames. Run `./gradlew assembleDebug`.

---

### 14.3 Consolidate All Icons into AppIcons Object

Ensure all icon references across the app go through the centralized `AppIcons` object in `ui/theme/AppIcons.kt`. No file should directly reference `Icons.Default.*`, `Icons.Outlined.*`, `Icons.Filled.*`, or `Icons.AutoMirrored.*`.

**Current state:** `AppIcons.kt` defines ~50 icons in a well-organized object. However, violations exist in multiple files:

1. **`AdminDashboard.kt` line 1454** — directly uses `Icons.Default.ArrowDropDown` and `Icons.Default.ArrowDropUp`.
2. **`FluidType.kt` lines 10-31** — All **18 fluid types** use `Icons.Outlined.*` directly (e.g., `Icons.Outlined.WaterDrop`, `Icons.Outlined.Coffee`, etc.). This is also an architecture violation: the model layer should not depend on Compose UI icons.
3. **`EditProfileScreen.kt` line 1087** — directly uses `Icons.Default.CheckCircle`.
4. **`NotificationHelper.kt` line 71** — uses `R.drawable.fluid_check_icon` directly instead of `AppIcons.AppLogo`.
5. **`AboutDeveloperScreen.kt` line 66** — uses `R.drawable.me` directly (developer photo — not a standard app icon, but should be tracked as a drawable reference).

**Steps:**

1. Add missing icons to `AppIcons.kt`:
   ```kotlin
   val ArrowDropDown = Icons.Default.ArrowDropDown
   val ArrowDropUp = Icons.Default.ArrowDropUp
   val CheckCircle = Icons.Default.CheckCircle
   // Fluid Types (all 18 icons from FluidType.kt)
   val WaterDrop = Icons.Outlined.WaterDrop
   val Coffee = Icons.Outlined.Coffee
   // ... (add all 18 fluid type icons referenced in FluidType.kt)
   ```
2. Update `AdminDashboard.kt` line 1454 to use `AppIcons.ArrowDropDown` and `AppIcons.ArrowDropUp`.
3. Update `EditProfileScreen.kt` line 1087 to use `AppIcons.CheckCircle`.
4. Update `FluidType.kt` lines 10-31 to use `AppIcons.*` for all 18 fluid type icons. Import `AppIcons` from `com.example.fluidcheck.ui.theme`. Consider whether the icon reference should remain in the model or be moved to a UI-layer mapping function to avoid coupling the model layer to Compose UI.
5. Update `NotificationHelper.kt` line 71 to use `AppIcons.AppLogo`.
6. Run a full codebase grep for `Icons.Default`, `Icons.Outlined`, `Icons.Filled`, `Icons.AutoMirrored` in all `.kt` files **excluding** `AppIcons.kt` itself. Fix any remaining direct references.
7. **Verify:** `grep -rn "Icons\.Default\|Icons\.Outlined\|Icons\.Filled\|Icons\.AutoMirrored" --include="*.kt"` excluding `AppIcons.kt` should return **zero results**. `./gradlew assembleDebug` must pass.

---

### 14.4 Consolidate colors.xml — Remove Unused Color Values

Audit `colors.xml` (`app/src/main/res/values/colors.xml`) for unused color resources and remove them. Ensure colors used across the app are consolidated between the XML resources and the Compose `Color.kt`.

**Current state:**

- `colors.xml` defines 11 colors: `black`, `white`, `primary` (#2196F3), `primary_variant`, `secondary`, `background`, `text_primary`, `text_secondary`, `accent`, `water_blue`, `water_blue_dark`.
- `Color.kt` defines 15 Compose colors with **different hex values** for similar concepts (e.g., `PrimaryBlue = 0xFF3B82F6` vs XML `primary = #2196F3`). These two systems are completely disconnected.
- Additionally, **80+ instances** of inline `Color(0xFF...)` are scattered across `.kt` files (especially `AdminDashboard.kt` ~25+, `HomeScreen.kt` ~10+, `EditProfileScreen.kt` ~8+), bypassing both `colors.xml` and `Color.kt`.

**Steps:**

1. Search the entire codebase for references to each color in `colors.xml` (`@color/black`, `R.color.black`, etc.) for all 11 entries. Check `themes.xml` references too.
2. Remove any color entries from `colors.xml` that have **zero references** outside of `colors.xml` itself.
3. For colors referenced only in `themes.xml`, verify if `themes.xml` is actively used (the app uses Compose theming via `Theme.kt`). If only used for splash screen or system bars, keep only those required colors.
4. Reconcile `Color.kt` and `colors.xml` — unify to a single source of truth where both define a "primary blue" with different hex values.
5. Add named constants to `Color.kt` for all inline `Color(0xFF...)` values found across the codebase (e.g., `Color(0xFFF1F5F9)` → `val BorderGray = Color(0xFFF1F5F9)`). Replace all inline usages with the named constant.
6. **Verify:** `./gradlew assembleDebug` must pass. No `unresolved reference` errors for removed colors.

---

### 14.5 Extract Hardcoded Strings to strings.xml

Move all user-facing hardcoded string literals from `.kt` source files into `strings.xml` (`app/src/main/res/values/strings.xml`). Deduplicate strings that have the same value by referencing a single `strings.xml` entry.

**Current state:** `strings.xml` has 165 entries with good coverage, but **~60+ hardcoded user-facing strings** remain across `.kt` files.

**Known violations by file:**

**`MainActivity.kt` (~15 strings):** `"Cloud sync failed."` (lines 95, 113), `"Permission Required"` (135), `"Smart Reminders require notification permission..."` (136), `"Open Settings"` (147), `"Cancel"` (154), `"Account Verified!"` (238, 610 — duplicate), `"Welcome, $username!..."` (239), `"Sign In Failed"` / `"Firebase Google Auth Failed"` (251), `"Unexpected Auth Error"` (257), `"Awesome"` / `"Try Again"` (334), `"Internet Connection Required"` + message (373-374, 433-434 — duplicate), `"Sign In Error"` (405), `"Profile Error"` (473), `"Connection Error"` (486), `"Error saving setup..."` (527), `"Verification Failed"` / `"Verification Error"` (622-623, 628-629).

**`AdminDashboard.kt` (~10 strings):** `"Delete Selected Users"` (242), `"Discard Changes?"` (474), `"DISCARD"` (481), `"Delete User"` (521, 889), `"Save User Details"` (861), `"🔒 Security Verification"` (926), `"Enter your admin password to delete..."` (933), `"$selectedCount selected"` (1335).

**Auth screens (~10 strings):** `LoginScreen.kt`: `"Smart Fluid Intake Tracker"` (125), `"Continue as Guest"` (313), `" or sign in with "` (334). `SignUpScreen.kt`: `" or sign up with "` (318). `VerifyAccountScreen.kt`: `"Verify Your Account"` (101), `"Secure your local data..."` (108), `"Confirm Verification"` (269, 288).

**Other screens (~15 strings):** `HomeScreen.kt`: `"QUICK ADD"` (423), `"Please input a valid goal."` (1368). `EditProfileScreen.kt`: `"Change Profile Photo"` (544), `"Take a Photo"` (563), `"Choose from Gallery"` (578), `"Remove Current Photo"` (596), `"Are you sure..."` (709). `SettingsScreen.kt`: `"Verify Account"` (328), `"STREAK"` / `"$streak Days"` (472, 479). `AboutDeveloperScreen.kt`: `"Vincent Rafael Apog"` (77). `ProgressScreen.kt`: `"Select Date"` (379). `AICoachScreen.kt`: `"Please fill in all fields."` (177), `"Your Ideal Daily Intake: $resultMl"` (278). `InitialSetupScreen.kt`: `"You can skip this setup..."` (197).

**`FluidType.kt` — Untranslatable model strings:** All 18 fluid type names (`"Water"`, `"Coffee"`, `"Juice"`, etc.) are hardcoded as Kotlin strings and are NOT in `strings.xml`, making them untranslatable.

**Steps:**

1. Scan **all `.kt` files** for hardcoded user-facing strings using: `grep -rn 'Text("\|text = "\|title = "\|label = "\|Toast.makeText.*"' --include="*.kt"`.
2. For each hardcoded string found (see inventory above): (a) Check if an equivalent string already exists in `strings.xml` — if so, use `stringResource(R.string.existing_key)`. (b) If not, add a new entry to `strings.xml` with a descriptive, snake_case key name following the existing naming conventions (e.g., `error_cloud_sync_failed`, `toast_log_deleted`).
3. **Deduplication:** Identify strings in `strings.xml` with identical or near-identical values and consolidate. Examples: `login_failed_title` / `error_invalid_credentials_title` (both "Login Failed"); `awesome` / `awesome_caps` (both "AWESOME!"); `weight_kg_label` / `weight_label` and `height_cm_label` / `height_label` (near-duplicates); `sex_label` / `gender_label` (both refer to sex/gender). Consolidate to a single entry and update all references.
4. Replace `Toast.makeText(context, "...", ...)` with `context.getString(R.string.key)`.
5. Replace `Text("...")` with `Text(stringResource(R.string.key))`.
6. Add fluid type names to `strings.xml` and update `FluidType.kt` to use `stringResource()` or a context-aware approach.
7. Use Android string formatting for parameterized strings (e.g., `"Logged $amount ml of $type"` → `<string name="toast_logged_drink">Logged %1$d ml of %2$s</string>` with `context.getString(R.string.toast_logged_drink, amount, type)`).
8. **Verify:** `grep -rn 'Text("\|Toast.makeText.*"' --include="*.kt" | grep -v 'import\|package\|//' | grep -v 'stringResource\|getString\|R.string'` should return minimal results (only programmatically generated strings are acceptable). `./gradlew assembleDebug` must pass.

---

### 14.6 Remove Redundant Hard-Coded Static UI Code (POST-DATABASE INTEGRATION)

**PREREQUISITE: Execute ONLY after confirming that Firestore database integration is fully functional and all screens fetch data from the database. Executing prematurely will break the UI.**

Remove any remaining static/mock data, placeholder content, and hardcoded UI values that were used during pre-database development. All data displayed in the UI should come from Firestore or local DataStore.

**Steps:**

1. Search for static data patterns: `grep -rn "listOf\|mapOf\|arrayOf\|mutableListOf" --include="*.kt"` — identify any inline dummy lists used as default UI data (hardcoded log entries, user lists, chart data).
2. Delete `PlaceholderScreens.kt` (87 bytes — contains only a package declaration and comment `"// SettingsScreen moved to its own file."` — confirmed dead code) and remove all references.
3. Search for hardcoded numeric values that should be database-driven: default goal `3000` (appears in multiple places), default amount `"250"` (line 687 in `MainScreen.kt`). Extract to named constants.
4. Remove any `TODO` or `FIXME` comments referencing static/placeholder data (e.g., `UserData.kt` line 48: `"// New fields from section 7 of TODO.md"`).
5. **Verify:** Run the app and verify all screens display live Firestore data. `./gradlew assembleDebug` must pass.

---

### 14.7 Utilize Typography Object from Type.kt — Unify Application Typography

Replace all inline `fontSize`, `fontWeight`, `fontFamily`, and `TextStyle` declarations across the app with references to the centralized `Typography` object via `MaterialTheme.typography`.

**Current state:**

- `Type.kt` defines only 4 styles: `headlineLarge` (36sp/Black), `titleLarge` (24sp/Bold), `bodyLarge` (16sp/Normal), `labelMedium` (14sp/Medium). It also contains a TODO comment on line 9: `"In a real project, we would import Poppins and PT Sans fonts here."` — still using `FontFamily.SansSerif` as fallback.
- **130+ instances** of inline `fontSize` and `fontWeight` across the codebase, with virtually none referencing the Typography object.
- Common hardcoded sizes: 10.sp, 11.sp, 12.sp, 13.sp, 14.sp, 15.sp, 16.sp, 17.sp, 18.sp, 20.sp, 22.sp, 24.sp, 28.sp, 36.sp, 40.sp, 48.sp.
- Worst offenders by inline count: `AdminDashboard.kt` (45+), `HomeScreen.kt` (40+), `EditProfileScreen.kt` (30+), `SettingsScreen.kt` (25+), `AICoachScreen.kt` (20+), `MainScreen.kt` (15+).

**Step 1 — Expand the Typography Definition.** Update `Type.kt` to cover all text styles actually used in the app:

```kotlin
val Typography = Typography(
    displayLarge  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black, fontSize = 36.sp),
    displayMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,  fontSize = 28.sp),
    displaySmall  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,  fontSize = 24.sp),
    headlineLarge  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black, fontSize = 36.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,  fontSize = 24.sp),
    headlineSmall  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,  fontSize = 20.sp),
    titleLarge  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,   fontSize = 24.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,   fontSize = 18.sp),
    titleSmall  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,   fontSize = 16.sp),
    bodyLarge  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, letterSpacing = 0.25.sp),
    bodySmall  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 12.sp, letterSpacing = 0.4.sp),
    labelLarge  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 16.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelSmall  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp),
)
```

**Step 2 — Create a mapping guide** before editing screens:

- `fontSize = 36.sp, fontWeight = Black` → `MaterialTheme.typography.displayLarge`
- `fontSize = 28.sp, fontWeight = Bold` → `MaterialTheme.typography.displayMedium`
- `fontSize = 24.sp, fontWeight = Bold` → `MaterialTheme.typography.headlineMedium`
- `fontSize = 20.sp, fontWeight = Bold` → `MaterialTheme.typography.headlineSmall`
- `fontSize = 18.sp, fontWeight = Bold` → `MaterialTheme.typography.titleMedium`
- `fontSize = 16.sp, fontWeight = Bold/Medium` → `MaterialTheme.typography.titleSmall` or `labelLarge`
- `fontSize = 16.sp, fontWeight = Normal` → `MaterialTheme.typography.bodyLarge`
- `fontSize = 14.sp, fontWeight = Normal` → `MaterialTheme.typography.bodyMedium`
- `fontSize = 14.sp, fontWeight = Medium` → `MaterialTheme.typography.labelMedium`
- `fontSize = 12.sp, fontWeight = Normal` → `MaterialTheme.typography.bodySmall`
- `fontSize = 12.sp, fontWeight = Medium/Bold` → `MaterialTheme.typography.labelSmall`

**Step 3 — Update all screen files.** Replace inline text styling. Before: `Text(text = "Title", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextDark)`. After: `Text(text = "Title", style = MaterialTheme.typography.headlineMedium, color = TextDark)`. Process files by size (largest first): `AdminDashboard.kt`, `HomeScreen.kt`, `EditProfileScreen.kt`, `MainScreen.kt`, `ProgressScreen.kt`, `SettingsScreen.kt`, `AICoachScreen.kt`, `InitialSetupScreen.kt`, `LoginScreen.kt`, `SignUpScreen.kt`, `VerifyAccountScreen.kt`, `AboutDeveloperScreen.kt`, `AuthComponents.kt`, `MainActivity.kt`.

**Step 4 — Handle edge cases:** Preserve `color` when specified alongside `fontSize`/`fontWeight`. For `OutlinedTextField`/`TextField`, use `textStyle = MaterialTheme.typography.bodyMedium`. Investigate whether `@Suppress("DEPRECATION")` annotations on `Text` composables (found in `MainScreen.kt`) can be removed after migration to `style =`.

**Step 5 — Verify:** `grep -rn "fontSize = \|fontWeight = " --include="*.kt"` in the `ui/` directory should return near-zero results (exceptions: custom `AnnotatedString` spans, one-off UI elements). `./gradlew assembleDebug` must pass. Manually inspect each screen for correct text sizing. Do NOT change text colors as part of this task — only `fontSize`, `fontWeight`, `fontFamily`, and `letterSpacing`.

---

### Task Dependency and Parallelization

| Task               | Can Run In Parallel With | Dependencies                               |
| ------------------ | ------------------------ | ------------------------------------------ |
| 14.1 (Restructure) | None — do first          | None                                       |
| 14.2 (Admin creds) | 14.3, 14.4, 14.5, 14.7   | 14.1                                       |
| 14.3 (Icons)       | 14.2, 14.4, 14.5, 14.7   | 14.1                                       |
| 14.4 (Colors)      | 14.2, 14.3, 14.5, 14.7   | 14.1                                       |
| 14.5 (Strings)     | 14.2, 14.3, 14.4, 14.7   | 14.1                                       |
| 14.6 (Static UI)   | None — do last           | 14.1-14.5 + database integration confirmed |
| 14.7 (Typography)  | 14.2, 14.3, 14.4, 14.5   | 14.1                                       |

**WHEN PROCEEDING TO EXECUTE ALL TASKS UNDER SECTION 14, ENSURE THAT AFTER EXECUTING, EVERYTHING IS STABLE AND WORKING AS IT WAS BEFORE, IF THERE WILL BE CHANGES EVEN IN THE SLIGHTEST WAY, REVERT AND FIND OTHER WAYS SO IT WILL WORK AS IT WAS BEFORE.**

## 15. Role Addition and Hierarchy \*\*DONE

1. **Role Renaming:** Rename the existing "USER" role to "FREE USER". \*\*DONE
2. **New Role:** Create a new role named "PREMIUM USER". \*\*DONE
3. **Role Hierarchy:** The new system hierarchy is: `ADMIN > MODERATOR > PREMIUM USER > FREE USER`. \*\*DONE
4. **FREE USER Privileges & Restrictions:** \*\*DONE
   - **Fluid Logging:** Can only log "Water", "Coffee", and "Tea". Other fluid types remain visible in the UI but trigger a paywall dialog when pressed.
   - **Progress Viewing:** Limited to a 7-day lookback window.
     - _Daily View:_ Users can only navigate back up to 7 days; navigating further triggers the paywall.
     - _Weekly View:_ Pressing 'Back' or 'Next' triggers the paywall.
     - _Monthly/Yearly View:_ Accessing these tabs triggers the paywall.
   - **AI Features:** Completely restricted. Pressing any buttons or textboxes in the AI Coach Screen triggers the paywall. The AI weekly scorecard is visually de-emphasized, and interacting with it triggers the paywall.
   - **Weather Integration:** Restricted. Attempting to enable the "Dynamic Weather Goal Adjustment" toggle triggers the paywall.
   - **Quick-Add:** Completely hidden. The Quick-Add UI and functionality are not visible.
   - **Gamification:** Limited to basic "Streak" tracking only.
   - **Notifications:** Limited to standard default notifications. Smart reminders are disabled.
   - **Data Backup:** Cloud data is fully backed up in Firebase, but the _client-side display_ is restricted to the last 7 days (rolling window: new day in, oldest day out). This ensures no data is lost if the user upgrades later.
   - **Offline Mode:** The app requires an active internet connection to function.
   - **Customization:** No access to UI themes, custom logos, or custom progress meters.
   - **Advertisements:** Ads are enabled (Feature will be elaborated more in section 18).

5. **PREMIUM USER Privileges:** \*\*DONE
   - **UI Cleanliness:** The "Upgrade to Premium" button is removed from the Settings Screen.
   - **Fluid Logging:** Unlocked access to log all fluid types, plus the ability to create custom fluid types.
   - **Progress Viewing:** Unlimited historical access. Can view Daily, Weekly, Monthly, and Yearly progress dating back to account creation.
   - **AI Features:** Full access to the AI Hydration Coach, including ideal intake calculation, AI chat prompts, and habit analysis (No further development needed. leave this feature as is and only enabled for premium users).
   - **Weather Integration:** Full access to dynamic daily goal adjustments based on local weather metrics (No further development needed. leave this feature as is and only enabled for premium users).
   - **Quick-Add:** Full access to customizable quick-add slots (No further development needed. leave this feature as is and only enabled for premium users).
   - **Enhanced Gamification:** Access to the mission board, badges, and streak shields (Feature will be elaborated more in section 16).
   - **Smart Notifications:** Access to personalized, AI-driven smart reminders (No further development needed. leave this feature as is and only enabled for premium users).
   - **Unlimited Cloud Access:** Full sync and client-side display of all historical data. _Note: If a subscription expires, their data remains safe in Firebase, but the client-side app will revert to only displaying the last 7 days as a FREE USER._
   - **Offline Architecture:** Full offline support. Offline logs will sync to the cloud automatically upon reconnection. Use existing project code for this logic (No further development needed. leave this feature as is and only enabled for premium users).
   - **App Customization:** Exclusive access to UI skins (e.g., dark mode), custom app logos, changeable backgrounds, and alternative Progress meter UI designs (Feature will be elaborated more in section 17).
   - **Advertisements:** Completely ad-free experience.

6. **MODERATOR Privileges:** Inherits all PREMIUM USER privileges in addition to their existing moderator tools. \*\*DONE
7. **ADMIN Privileges:** Inherits all PREMIUM USER privileges in addition to their existing admin tools. \*\*DONE

## 16. Enhanced Gamification

1. **Mission System (Progress Screen):**
   - **Mission Board UI:** Create a section in the Progress Screen displaying a list of available daily missions.
   - **Mission Availability:** Maintain exactly 10 available missions at any time, labeled with varying difficulty levels (e.g., Easy, Moderate, Hard, Epic). Unselected missions remain on the board, and new missions populate the board the following day to replace completed or aborted ones.
   - **Mission Selection:** Users can have a maximum of 5 active missions simultaneously.
   - **Mission Abandonment:** Users can abort an active mission. However, once a mission is aborted, the user cannot replace it with a new one until the next daily reset.
   - **Content Generation:** Define a pool of 50 unique missions spanning different difficulties and hydration goals.
   - **Mission Appearance Probabilities (Board Population):**
     - _Easy Missions:_ 50% chance to appear in a slot.
     - _Moderate Missions:_ 30% chance to appear in a slot.
     - _Hard Missions:_ 14% chance to appear in a slot.
     - _Epic Missions:_ 6% chance to appear in a slot.
   - **Reward Tiers & Probabilities:**
     - _Easy Missions:_ Reward: Common Badges, with a 6.25% probability of dropping a "Streak Shield Fragment."
     - _Moderate Missions:_ Reward: Uncommon Badges, with a 12.5% probability of dropping a "Streak Shield Fragment."
     - _Hard Missions:_ Reward: Rare Badges, with a 33% probability of dropping a "Streak Shield Fragment."
     - _Epic Missions:_ Reward: Epic Badges and a guaranteed "Full Streak Shield." (Only 5 out of the 50 total missions should be categorized as Epic, appearing rarely on the board).
   - _Note: 10 Streak Shield Fragments automatically combine to craft 1 Full Streak Shield._

2. **Inventory & Collectibles (Settings Screen):**
   - **Access Point:** In the Settings Screen, within the Profile card (above the "Edit Profile" button), add a new button labeled "Shields and Badges."
   - **Inventory UI:** Tapping the button opens an inventory screen divided into two tabs/sections:
     - _Consumables:_ Displays the user's current count of Full Streak Shields and Streak Shield Fragments.
     - _Badge Gallery:_ Displays a grid of all available badges. Unearned badges appear as greyed-out silhouettes. Badges the user has earned appear fully colored, displaying a counter indicating how many times that specific badge has been awarded.
   - **Milestone Consistency Badges:** In addition to daily missions, implement Milestone Badges that are automatically awarded upon reaching specific long-term consistency goals (A milestone can only be obtained once. eg. if 7-day milestone is achieved, only obtain once. Having another 7-day streak does not reward another 7-day streak milestone). Examples include:
     - _Streak Milestones:_ Reach a 7-day, 25-day, 50-day, 100-day, and 365-day active streak.
     - _Volume Milestones:_ Reach lifetime fluid intake milestones (e.g., 50 Liters, 100 Liters, 500 Liters).
     - _Early Bird:_ Log water consistently before 8:00 AM for 10 consecutive days.
     - _Perfect Week:_ Close the progress ring 7 days in a row without using a single Streak Shield.
     - _Hydration Master:_ Log at least 3 different fluid types (e.g., Water, Tea, Coffee) in a single day for 14 consecutive days.
     - _Weekend Warrior:_ Successfully meet the daily goal on both Saturday and Sunday for 4 consecutive weeks.
     - _Night Owl:_ Log hydration after 10:00 PM consistently for 7 days.
   - **Item Details Dialog:** Tapping any item (Badge, Full Shield, or Fragment) opens a dialog containing:
     - A large, high-resolution image of the item.
     - For Badges: A description of the mission/achievement required to earn it.
     - For Shields/Fragments: A description of its utility (protecting a daily streak from breaking).
   - **Auto-Shield Toggle:** Place a toggle switch labeled "Automatically use shield to prevent streak loss" directly in the "Consumables" tab of the Inventory UI and within the main Settings Screen under a "Preferences" section. When enabled, the system will automatically consume one Full Streak Shield at midnight if the user failed to meet their daily goal.

3. **Active Mission Tracker (Home Screen):**
   - **Quick View UI:** Add a compact widget to the Home Screen that displays the real-time progress of the user's currently active missions (up to 5).

## 17. App UI Customization (Premium Feature)

1. **Global App Themes:**
   - **Theme Variety:** Develop multiple cohesive UI themes to provide extensive personalization. Examples: Light Mode, Dark Mode, AMOLED Pitch Black, Ocean Blue, Sunset Orange, Forest Green, Midnight Purple, Pastel Spring, Classic Sepia, and Cyberpunk Neon.
   - **Quality Assurance:** Ensure high contrast, text readability, and perfect icon/background color pairing for each theme palette.

2. **Custom Icons and Backgrounds:**
   - **App Icon:** Provide multiple alternative color variations of the app icon that users can set as their device launcher icon. _CRITICAL:_ Do not create or source entirely new icon designs. Strictly use the existing, original app icon and apply modifications to it (e.g., recoloring, color inversion, adding a gradient background, or creating a sleek monochrome version).
   - **App Icon Backgrounds:** Provide a wide selection of custom backgrounds for the app icon's background. Varieties should include solid colors, soft blurred gradients, subtle geometric patterns, and minimal dynamic particle effects.

3. **Progress Meter Variants:**
   - **Alternative Designs:** In addition to the default "Progress Ring", develop several alternative visual representations for the daily goal intake meter. Examples:
     - A vertical "bottle fill" liquid bar.
     - A dynamic wave animation that rises as the user drinks.
     - A minimalist horizontal line gauge.
     - A segmented battery-style meter.
     - A geometric dot matrix grid that lights up.
     - A liquid drop counter.
   - **Logic Preservation:** These alternatives are strictly UI replacements. The underlying logic, state management, and daily goal calculations remain identical to the original Progress Ring.

4. **Settings Integration (Customization Hub):**
   - **UI Section:** Add a new "Customization" container card within the Settings Screen.
   - **Theme Selector:** Implement a dropdown menu allowing users to instantly and dynamically change the active app theme.
   - **App Logo Selector:** Add a button that opens a dialog box displaying a grid of alternative app logos. Tapping a logo applies it.
   - **Progress Meter Selector:** Implement a dropdown menu allowing users to toggle between the different progress meter UI styles across the app.

## 18. Simulated Advertisements (Free Users Only)

_Note: This feature is strictly for Free Users. Premium Users will have a completely ad-free experience._

1. **Mock Advertisement System:**
   Since there is no actual third-party ad network integrated, develop a system that displays locally generated, mock advertisements throughout the app.

2. **Ad Banner Placements:**
   - Integrate static or slightly animated mock ad banners at the top or bottom of non-intrusive screens (e.g., Home Screen, Settings Screen).

3. **Interaction-Triggered Pop-up Ads (Interstitials):**
   - **Trigger Mechanism:** Implement a counter that tracks how many times the user interacts with active UI elements (such as tapping buttons, focusing textboxes, or toggling switches).
   - **Randomized Threshold:** Upon the first UI interaction, generate a random threshold number between 6 and 10.
   - **Display Logic:** Once the user's interaction count reaches the randomized threshold, instantly display a full-screen pop-up mock advertisement.
   - **Reset Cycle:** After the user dismisses the pop-up ad, the interaction counter resets to zero, a new random threshold (between 6 and 10) is generated, and the cycle repeats.
