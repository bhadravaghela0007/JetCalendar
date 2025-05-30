# JetCalendar

A fully customizable and modern calendar application built using Jetpack Compose. This app offers both week view and full-month calendar view, along with powerful features like event notifications, note-taking, and integration with the Android system calendar.

 📸 Screenshot:- 
https://github.com/bhadravaghela0007/JetCalendar/blob/main/JetCalendar.jpg

🚀 Features
🗓️ Calendar Views
Full-Month Calendar: A responsive monthly view showing all dates with smooth Compose animations.

Week Calendar: A compact week-based calendar for better task overview in a smaller layout.

Day Selection: Tap to select any date; displays associated events/notes.

📝 Notes & Events
Add Notes: Add custom notes or events to any selected date.

Edit/Delete Notes: Manage and update previously added events easily.

🔔 Notifications
Event Reminders: Set reminders/notifications for events and notes.

AlarmManager Integration: Schedule local notifications using AlarmManager and BroadcastReceiver.

🔗 System Calendar Integration
Sync with the Android System Calendar to:

Fetch existing events from the user's calendar.

Add new notes/events directly to the system calendar for cross-app visibility.

🎨 Modern UI
Built entirely with Jetpack Compose, ensuring fluid animations, responsive layouts, and clean architecture.

🧱 Tech Stack
Language: Kotlin
UI: Jetpack Compose
Date Handling: java.time 
System Calendar Integration: ContentResolver, CalendarContract
Notifications: AlarmManager, PendingIntent, NotificationManager
Architecture: MVVM (ViewModel, StateFlow, Repository)

