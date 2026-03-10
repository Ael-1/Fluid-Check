# FluidCheck Development Plan

## 1. Application wide feature

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

## 2. Home screen

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

## 4. AI Coach screen \*\*DONE

1. READ Firestore to see user records (weight, height, etc.) so the textfields of these attributes in Personalized Goals are automatically inputted \*\*DONE

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

## 6. Admin Dashboard screen

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

## 9. Sign in/Sign up

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

## 11. User Fields and Keyboard Behaviors (This will apply to ALL user fields inside the app)

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

## 12. Security & Validation Polish

1. Admin Self-Guard: Prevent admins from demoting or deleting their own account within the Admin Dashboard to avoid accidental lockout. \*\*DONE
2. Numeric Range Validation: Enforce reasonable limits on all numeric inputs (e.g., Weight 1–500kg, Height 30–300cm, Age 1–150, Daily Goal 100–20000ml). \*\*DONE
3. Button Loading States: Show a loading indicator (spinner) and disable 'Save' or 'Submit' buttons during asynchronous operations to prevent double-tap submissions. \*\*DONE
4. Google Account Restrictions: For accounts created via Google Sign-In, disable the ability to edit username and password in 'Edit Profile'. These fields should be read-only for Google-authenticated users. \*\*DONE
5. Password Consistency: Enforce the same 6-character minimum length for password updates in Edit Profile as used in the Sign Up flow. \*\*DONE

## 13. Email Verification

1. Verify Current Email Address: Add a "Verify Email" button in `EditProfileScreen` inside `ProfileSettingsContainer`, below the email field (visible only when `!isGoogleUser && authRepository.currentUser?.isEmailVerified == false`). When tapped, call a new `AuthRepository.sendEmailVerification()` method (wrapping `auth.currentUser?.sendEmailVerification()?.await()`). On success, show feedback via `statusDialogData` (the existing AlertDialog pattern used throughout the screen): "Verification email sent. Please check your inbox." If the email is already verified, hide the button entirely and optionally show a small "✓ Verified" label. \*\*DONE
2. Verify-Before-Update Email Flow: Replace the current `AuthRepository.updateEmail()` (which calls the deprecated `auth.currentUser?.updateEmail()`) with a new `AuthRepository.verifyBeforeUpdateEmail(newEmail)` method (wrapping `auth.currentUser?.verifyBeforeUpdateEmail(newEmail)?.await()`). In `EditProfileScreen`, update the email save block (currently at Step 3 in the save flow, around line 615–625) to call `verifyBeforeUpdateEmail()` instead of `updateEmail()`. On success, show via `statusDialogData`: "A verification email has been sent to [newEmail]. Your email will update after you verify it." **Crucially**, do **not** write the new email to the Firestore user record in `newRecord.copy()` (currently line 648) — keep the old email until Firebase Auth confirms the change on next sign-in. The existing re-authentication flow (`showReauthDialog`) already gates email changes, so no additional re-auth logic is needed. Google Sign-In users are already excluded (their email field is read-only per Task 12.4). \*\*DONE
