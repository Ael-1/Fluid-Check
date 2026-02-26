# FluidCheck Development Plan

## 1. Application wide feature
- Offline Support: Your plan relies heavily on Firestore. What happens if the user logs a drink while their phone is offline? The app should gracefully handle this by storing the log locally on the device. When the internet connection is restored, it can then automatically sync the offline data with Firestore.
- Time Handling: For 'THIS DAY' logic, follow the device's local time. Note: Consider fetching date/time remotely if device tampering becomes an issue or for more robust synchronization.

## 2. Home screen **DONE
1. Users can UPDATE daily goal **DONE
2. Users can CREATE new log drink, then log drink is stored in Firestore (fluid type, amount and log date) **DONE
3. In Recent Logs, READ from Firestore to display the user's logs drink from THIS DAY ('THIS DAY' means it starts from 12:00am to 11:59pm) **DONE
4. In View Full Log History, READ from Firestore to display user's ALL log drink **DONE
5. User streak (consecutive number of days the user closed their progress ring) is stored in firestore and displayed in its container in Home Page **DONE
6. Users can UPDATE their log drink within 24 hours of the CREATION of the log drink **DONE
7. Users can DELETE their log drink within 24 hours of the CREATION of the log drink **DONE
8. Quick-Add Buttons: **DONE
    - Three default examples (e.g., 250ml, 500ml, 750ml).
    - A 'plus' button allowing users to customize their preferred quick-add drinks.
    - Maximum of 3 quick-add slots; the 'plus' button disappears once 3 are configured.

## 3. Progress screen **DONE
1. READ Firestore to see the date and amount of their log drink which is then displayed in Your Progress **DONE

## 4. AI Coach screen **DONE
1. READ Firestore to see user records (weight, height, etc.) so the textfields of these attributes in Personalized Goals are automatically inputted **DONE

## 5. Settings screen **DONE
1. READ Firestore for username to be displayed in profile header **DONE
2. READ Firestore for user's streak amount to be displayed in its container in Settings **DONE
3. users can CREATE or UPDATE their display photo in Edit Profile **DONE (Framework in place)
4. users can UPDATE their username in Edit Profile **DONE
5. users can UPDATE their email address in Edit Profile **DONE
6. users can UPDATE their password in Edit Profile **DONE
7. users can UPDATE their weight, height, age, sex, activity level, and environment in Edit Profile **DONE
8. READ from Firestore to display user's username and email in Edit Profile **DONE
9. READ from Firestore to display personal records in Edit Profile **DONE

## 6. Admin Analytics screen
1. READ Auth or Firestore to display total users **DONE
2. READ Firestore for total downloads **DONE (Using Total Users as proxy)
3. READ Auth or Firestore to display active users **DONE
4. READ Firestore and code calculation for avg streak **DONE
5. Admin can READ from Auth or Firestore to search users **DONE
6. Admin can UPDATE user's username **DONE
7. Admin can UPDATE user's role (admin or user) **DONE
8. Admin can UPDATE user's personal records (weight, height, etc.) **DONE
9. READ from Firestore to display each user's 'data'. 'data' are the following:
 account created, email, username, total rings closed, daily goal, streak, highest streak, all logs, personal records, display photo and any more you can think of possible data that is relevant for an admin to see **DONE (Profile details available in Edit Dialog)
10. Admin can DELETE users **DONE (Soft delete implemented)

## 7. Firestore Collection/Fields **DONE
1. isDeleted (Boolean): Instead of permanently deleting a user's data (which can be risky), you could mark it as deleted. This is safer for data integrity and allows for potential recovery. **DONE
2. fcmToken (String): If you plan to add push notifications (e.g., reminders to drink water) in the future, you'll need to store the user's Firebase Cloud Messaging token. **DONE
3. quickAddConfig (Array of Objects): To store the user's custom Quick-Add settings. Each object could contain amount and fluidType. This directly supports your "plus" button feature. **DONE
4. notificationsEnabled (Boolean): To let the user opt-in or out of app notifications. **DONE
5. lastRingClosedDate (String or Timestamp): To track when they last hit their goal. This is crucial for calculating the streak correctly (checking if it was "yesterday"). **DONE
6. highestStreak (Int): To display the user's all-time record, which is a common motivational feature. (You mentioned this in the Admin section, but it should be stored in the user document). **DONE
7. totalFluidDrankAllTime (Int): A running total of all fluid logged. It's much faster to read this single field than to query and sum up all historical logs. **DONE
8. createdAt (Timestamp): Store the date and time when the user first registered. **DONE
9. totalRingsClosed (Int): Store the total amount of progress closed by the user (progress rings that are closed multiple times is counted) **DONE
