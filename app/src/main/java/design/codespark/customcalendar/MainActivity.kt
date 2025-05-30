package design.codespark.customcalendar

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import design.codespark.customcalendar.DataClasses.CalendarData
import design.codespark.customcalendar.ui.theme.CustomCalendarTheme
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.TimeZone
import android.Manifest
import androidx.compose.runtime.mutableStateMapOf
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CheckPermissions()
        enableEdgeToEdge()
        setContent {
            CustomCalendarTheme {

                var showDialog by remember { mutableStateOf(false) }
                var noteInput by remember { mutableStateOf("") }
                val notes = remember { mutableStateMapOf<LocalDate, MutableList<CalendarData>>() }
                var selectedDate = remember { mutableStateOf(LocalDate.now()) }

                var hour by remember { mutableStateOf(12) }
                var minute by remember { mutableStateOf(0) }
                var isPM by remember { mutableStateOf(hour >= 12) } // <--- New state
                var showTimePicker by remember { mutableStateOf(false) }
                val formattedTimeState = remember { mutableStateOf("00:00 PM") }
                val context = LocalContext.current

                Box(modifier = Modifier.padding(top = 50.dp)) {
                    MonthlyCalendarView(
                        onAddNote = {
                            showDialog = true // Show the dialog
                        },
                        selectedDate = selectedDate, // Access the value of selectedDate
                        notes = notes, // Keep track of notes
                        onTimePicker = { showTimePicker = true },
                        selectedTime = formattedTimeState.value
                    )
                }

                fun generateEventId(): String {
                    return UUID.randomUUID().toString() // Generates a random unique event ID
                }

                if (showTimePicker) {
                    TimePickerDialogContent(
                        selectedHour = hour,
                        selectedMinute = minute,
                        isPM = isPM, // <--- pass the value
                        onTimeChange = { h, m, pm ->
                            hour = h
                            minute = m
                            isPM = pm // <--- update the value
                            formattedTimeState.value = "$h:$m ${if (pm) "PM" else "AM"}"
                            Log.d("==codespark", "Selected time: $h:$m ${if (pm) "PM" else "AM"}")
                            Log.d("==codespark", "${formattedTimeState.value}")
                        },
                        onDismiss = { showTimePicker = false },
                        onSave = {

                            // Save logic here, you can use hour, minute, and isPM
                            showTimePicker = false
                        }
                    )
                }

                // Add the AddNotes dialog
                if (showDialog) {
                    AddNotes(
                        noteInput = noteInput,
                        onNoteInputChange = { noteInput = it },
                        onDismiss = {
                            showDialog = false
                            noteInput = ""
                        },
                        onSave = {
                            if (noteInput.isNotBlank()) {
                                // Create a new CalendarData object
                                val newEventId = addEventToSystemCalendar(
                                    context = context,
                                    title = noteInput.trim(),
                                    description = "",
                                    date = selectedDate.value
                                )
                                    ?: generateEventId() // Use the fallback eventId if insertion fails

                                // Create the CalendarData object
                                val calendarData = CalendarData(
                                    eventId = newEventId,
                                    title = noteInput.trim(),
                                    date = selectedDate.toString()
                                )

                                Log.d("==codespark", "onCreate: $selectedDate")
                                // Get or create the list for the selected date and add the new note
                                val list = notes.getOrPut(selectedDate.value) { mutableListOf() }
                                list.add(calendarData)
                            }

                            // Reset and close the dialog
                            noteInput = ""
                            showDialog = false
                        }
                    )
                }

            }
        }
    }

    private fun CheckPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        permissions.add(android.Manifest.permission.WRITE_CALENDAR)

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissions.toTypedArray(),
                1001
            )
        }
    }
}


// ---------- MAIN CALENDAR VIEW ----------
@Composable
fun MonthlyCalendarView(
    onAddNote: () -> Unit,
    onTimePicker: () -> Unit,
    selectedDate: MutableState<LocalDate>, // MutableState passed directly
    notes: MutableMap<LocalDate, MutableList<CalendarData>>, // Now store CalendarData objects
    selectedTime: String
) {
    var context = LocalContext.current
    val activity = context as? Activity

    Log.d("==codespark", "MonthlyCalendarView: ${selectedDate.value}")

    // --- 1. Create notification channel once ---
    LaunchedEffect(Unit) {
        createNotificationChannel(context)
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_CALENDAR
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            activity?.let {
                ActivityCompat.requestPermissions(
                    it,
                    arrayOf(Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR),
                    1001
                )
            }
        }
    }


    var currentMonth by remember { mutableStateOf(LocalDate.now().withDayOfMonth(1)) }
    var weekStart by remember { mutableStateOf(selectedDate.value.startOfWeek()) }
    var isExpanded by remember { mutableStateOf(false) }

    val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
    var refreshTrigger by remember { mutableStateOf(0) }
    val selectedEvent = remember { mutableStateOf<CalendarData?>(null) }

    @SuppressLint("Range")
    fun fetchEventFromSystemCalendar(context: Context, selectedDate: LocalDate) {
        val contentResolver: ContentResolver = context.contentResolver
        val startMillis =
            selectedDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis =
            selectedDate.plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()
                .toEpochMilli()

        val uri = CalendarContract.Events.CONTENT_URI
        val selection =
            "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTEND} <= ?"
        val selectionArgs = arrayOf(startMillis.toString(), endMillis.toString())

        val cursor = contentResolver.query(
            uri,
            arrayOf(
                CalendarContract.Events._ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DTSTART
            ),
            selection,
            selectionArgs,
            null
        )

        val events = mutableListOf<CalendarData>()

        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getString(it.getColumnIndex(CalendarContract.Events._ID))
                val title = it.getString(it.getColumnIndex(CalendarContract.Events.TITLE))
                val startTimeMillis = it.getLong(it.getColumnIndex(CalendarContract.Events.DTSTART))
                val time = Instant.ofEpochMilli(startTimeMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalTime()
                events.add(
                    CalendarData(
                        eventId = id,
                        title = title,
                        date = selectedDate.toString(),
                        time = time
                    )
                )

            }
        }

        if (events.isNotEmpty()) {
            notes[selectedDate] = events.toMutableList()
        }
    }

    fun deleteNoteFromSystemCalendar(eventId: String, context: Context) {
        val contentResolver: ContentResolver = context.contentResolver
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId.toLong())
        contentResolver.delete(uri, null, null)
    }

    fun parseTimeStringToLocalTime(timeStr: String): LocalTime {
        // 'h' = 1–2 digit hour-of-am-pm, 'm' = 1–2 digit minute
        val formatter = DateTimeFormatter.ofPattern("h:m a", Locale.ENGLISH)
        return LocalTime.parse(timeStr.uppercase(Locale.ENGLISH).trim(), formatter)
    }

    fun updateEventTimeInSystemCalendar(
        context: Context,
        eventId: String,
        newTime: LocalTime,
        date: LocalDate
    ) {
        val contentResolver = context.contentResolver
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId.toLong())

        val startMillis = ZonedDateTime.of(date, newTime, ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        val endMillis = startMillis + 60 * 60 * 1000 // Assume 1-hour duration

        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
        }

        contentResolver.update(uri, values, null, null)
    }

    // --- 4. Schedule notification ---
    @SuppressLint("ScheduleExactAlarm")
    fun scheduleNotification(context: Context, calendarData: CalendarData, date: LocalDate) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 🔐 Permission check for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                context.startActivity(intent)
                return
            }
        }

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("title", calendarData.title)
            putExtra("eventId", calendarData.eventId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            calendarData.eventId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = ZonedDateTime.of(date, calendarData.time, ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        if (triggerTime > System.currentTimeMillis()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    LaunchedEffect(selectedTime) {
        Log.d("==dora", "LaunchedEffect triggered: selectedTime='$selectedTime'")

        if (selectedTime.isNotBlank()) {
            try {
                val newTime = parseTimeStringToLocalTime(selectedTime)
                val event = selectedEvent.value

                if (event != null) {
                    updateEventTimeInSystemCalendar(
                        context,
                        event.eventId,
                        newTime,
                        selectedDate.value
                    )
                    event.time = newTime
                    refreshTrigger++
                    scheduleNotification(context, event, selectedDate.value)

                    Log.d("==dora", "Updated time for eventId=${event.eventId}")
                } else {
                    Log.w("==dora", "selectedEvent was null")
                }
            } catch (e: Exception) {
                Log.e("==dora", "Error updating time", e)
            }
        } else {
            Log.d("==dora", "selectedTime was blank, skipping update")
        }
    }

    LaunchedEffect(selectedDate, refreshTrigger) {
        // Fetch events for the selected date
        fetchEventFromSystemCalendar(context, selectedDate.value)
    }



    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 6.dp, top = 8.dp, end = 14.dp)
        ) {

            // --- Month Header ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpanded)
                        currentMonth.format(monthFormatter)
                    else {
                        val weekEnd = weekStart.plusDays(6)
                        val startMonth = weekStart.format(DateTimeFormatter.ofPattern("MMMM"))
                        val endMonth = weekEnd.format(DateTimeFormatter.ofPattern("MMMM"))
                        val startYear = weekStart.format(DateTimeFormatter.ofPattern("yyyy"))
                        val endYear = weekEnd.format(DateTimeFormatter.ofPattern("yyyy"))
                        if (startMonth == endMonth && startYear == endYear) {
                            "$startMonth $startYear"
                        } else if (startYear == endYear) {
                            "$startMonth - $endMonth $startYear"
                        } else {
                            "$startMonth $startYear - $endMonth $endYear"
                        }
                    },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // --- Calendar View ---
            if (isExpanded) {
                FullMonthCalendarView(
                    selectedDate = selectedDate.value,
                    onDateSelected = {
                        selectedDate.value = it
                        currentMonth = it.withDayOfMonth(1)

                    },
                    currentMonth = currentMonth,
                    onMonthChange = { currentMonth = it }
                )
            } else {
                OneWeekCalendarView(
                    weekStart = weekStart,
                    selectedDate = selectedDate.value,
                    onDateSelected = {
                        selectedDate.value = it
                        weekStart = it.startOfWeek()
                    },
                    onWeekChange = { newWeekStart ->
                        weekStart = newWeekStart
                    }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle View"
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))

            // --- Notes card ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(0.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            ) {
                Column(modifier = Modifier.padding(16.dp, top = 6.dp)) {
                    Text(
                        text = "Notes for ${selectedDate.value.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}",
                        fontWeight = FontWeight.Bold,

                        )
                    Spacer(modifier = Modifier.height(8.dp))

                    val dateNotes = notes[selectedDate.value]
                    if (dateNotes.isNullOrEmpty()) {
                        Text("No notes for this day.")
                    } else {

                        val lightColors = arrayOf(
                            Color(0xFFFFF5B3),
                            Color(0xFFB3FCB4),
                            Color(0xFFFFB4B3),
                            Color(0xFFB3D9FF),
                            Color(0xFFFFDAB3),
                            Color(0xFFD3B3FF)
                        )

                        Box(modifier = Modifier.heightIn(max = 200.dp)) {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                dateNotes.forEachIndexed { index, calendarData ->
                                    val fixedColor = lightColors.random()
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 60.dp),
                                        colors = CardDefaults.cardColors(containerColor = fixedColor),
                                        shape = RoundedCornerShape(8.dp),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = calendarData.title,
                                                    fontSize = 18.sp
                                                )
                                                Text(
                                                    calendarData.time.format(
                                                        DateTimeFormatter.ofPattern(
                                                            "hh:mm a",
                                                            Locale.ENGLISH
                                                        )
                                                    ).uppercase(),
                                                    fontSize = 14.sp,
                                                    color = Color.DarkGray
                                                )
                                            }
                                            IconButton(onClick = {
                                                selectedEvent.value = calendarData
                                                onTimePicker()
                                            }) {
                                                Icon(
                                                    imageVector = Icons.Default.Notifications,
                                                    contentDescription = "Delete Note",
                                                    tint = Color(0xFFEA1515),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            IconButton(onClick = {
                                                deleteNoteFromSystemCalendar(
                                                    calendarData.eventId,
                                                    context
                                                )
                                                notes[selectedDate.value]?.removeAt(index)
                                                refreshTrigger++
                                            }) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Note",
                                                    tint = Color(0xFFEA1515),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Floating Action Button (FAB) to add note ---
        FloatingActionButton(
            onClick = { onAddNote() },
            modifier = Modifier
                .size(70.dp)
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .border(
                    width = 5.dp,
                    color = Color.White,
                    shape = CircleShape
                ),
            containerColor = Color(0xFFEA1515)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Note",
                tint = Color.White,
                modifier = Modifier.size(65.dp)
            )
        }
    }
}

// --- Helper function to create channel ---
@SuppressLint("ServiceCast")
fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Log.d("==dora", "Notification channel created")
        val channel = NotificationChannel(
            "calendar_channel_id",
            "Calendar Notifications",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Channel for calendar event notifications"
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}

fun addEventToSystemCalendar(
    context: Context,
    title: String,
    description: String,
    date: LocalDate,
    time: LocalTime = LocalTime.now(),
): String? {
    val contentResolver: ContentResolver = context.contentResolver

    val startDateTime = LocalDateTime.of(date, time)
    val startMillis = startDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val endMillis = startMillis + 60 * 60 * 1000 // default duration 1 hour

    val values = ContentValues().apply {
        put(CalendarContract.Events.DTSTART, startMillis)
        put(CalendarContract.Events.DTEND, endMillis)
        put(CalendarContract.Events.TITLE, title)
        put(CalendarContract.Events.DESCRIPTION, description)
        put(CalendarContract.Events.CALENDAR_ID, 1)
        put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
    }

    val uri = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
    return uri?.lastPathSegment?.toString()
}


// ---------- WEEK VIEW ----------
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OneWeekCalendarView(
    weekStart: LocalDate,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onWeekChange: (LocalDate) -> Unit
) {
    var swipeDirection by remember { mutableStateOf(0) }

    val weekDates = remember(weekStart) {
        (0..6).map { weekStart.plusDays(it.toLong()) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(weekStart) {
                detectHorizontalDragGestures { change, dragAmount ->
                    change.consume()
                    if (dragAmount > 0) {
                        swipeDirection = 1
                        onWeekChange(weekStart.minusWeeks(1))
                    } else if (dragAmount < 0) {
                        swipeDirection = -1
                        onWeekChange(weekStart.plusWeeks(1))
                    }
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DayOfWeekHeader()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clipToBounds() // Prevent overflow during animation
        ) {
            AnimatedContent(
                targetState = weekDates,
                transitionSpec = {
                    if (swipeDirection >= 0) {
                        slideInHorizontally(initialOffsetX = { fullWidth: Int -> fullWidth }) + fadeIn() with
                                slideOutHorizontally(targetOffsetX = { fullWidth: Int -> -fullWidth }) + fadeOut()
                    } else {
                        slideInHorizontally(initialOffsetX = { fullWidth: Int -> -fullWidth }) + fadeIn() with
                                slideOutHorizontally(targetOffsetX = { fullWidth: Int -> fullWidth }) + fadeOut()
                    }.using(SizeTransform(clip = false))
                },
                label = "WeekSlideAnimation"
            ) { dates ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    dates.forEach { date ->
                        DayCell(
                            day = date,
                            selectedDate = selectedDate,
                            onDateSelected = onDateSelected
                        )
                    }
                }
            }
        }
    }
}


// ---------- MONTH VIEW ----------
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun FullMonthCalendarView(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    currentMonth: LocalDate,
    onMonthChange: (LocalDate) -> Unit
) {
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfMonth = currentMonth.withDayOfMonth(1)
    val startOffset = (firstDayOfMonth.dayOfWeek.value % 7)

    val totalCells = ((daysInMonth + startOffset + 6) / 7) * 7
    val days = List(totalCells) { index ->
        val dayNum = index - startOffset + 1
        if (dayNum in 1..daysInMonth) currentMonth.withDayOfMonth(dayNum) else null
    }

    // State for swipe direction
    var swipeDirection by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clipToBounds() // Prevent overflow during animation
    ) {
        // AnimatedContent for smooth transitions between months
        AnimatedContent(
            targetState = currentMonth,
            transitionSpec = {
                if (swipeDirection >= 0) {
                    slideInHorizontally(initialOffsetX = { fullWidth: Int -> fullWidth }) + fadeIn() with
                            slideOutHorizontally(targetOffsetX = { fullWidth: Int -> -fullWidth }) + fadeOut()
                } else {
                    slideInHorizontally(initialOffsetX = { fullWidth: Int -> -fullWidth }) + fadeIn() with
                            slideOutHorizontally(targetOffsetX = { fullWidth: Int -> fullWidth }) + fadeOut()
                }.using(SizeTransform(clip = false))
            }
        ) { months ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(months) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            change.consume()
                            if (dragAmount > 0) {
                                // Finger moved right → show previous month
                                swipeDirection = -1
                                onMonthChange(currentMonth.minusMonths(1))
                            } else if (dragAmount < 0) {
                                // Finger moved left → show next month
                                swipeDirection = 1
                                onMonthChange(currentMonth.plusMonths(1))
                            }
                        }
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DayOfWeekHeader()

                days.chunked(7).forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        week.forEach { day ->
                            if (day != null) {
                                DayCell(
                                    day = day,
                                    selectedDate = selectedDate,
                                    onDateSelected = onDateSelected
                                )
                            } else {
                                Box(modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}


// ---------- WEEKDAY HEADER ----------
@Composable
fun DayOfWeekHeader() {
    val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp), // Match height of DayCell
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        daysOfWeek.forEach { day ->
            Box(
                modifier = Modifier
                    .fillMaxHeight(), // Ensure full 28.dp height
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// ---------- DAY CELL ----------
@Composable
fun DayCell(day: LocalDate, selectedDate: LocalDate, onDateSelected: (LocalDate) -> Unit) {
    val currentDate = LocalDate.now()
    val isSelected = day == selectedDate
    val isToday = day == currentDate

    // Set the colors
    val bgColor = when {
        isSelected -> Color(0xFFEA1515) // Selected date color
        isToday -> Color.Black // Today's date color
        else -> Color.Transparent // Default color for other dates
    }
    val textColor = when {
        isSelected -> Color.White // Text color for selected date
        isToday -> Color.White // Text color for today's date
        else -> Color.Black // Text color for other dates
    }

    Box(
        modifier = Modifier
            .size(28.dp)
            .background(bgColor, shape = RoundedCornerShape(50.dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }) { onDateSelected(day) },
        contentAlignment = Alignment.Center
    ) {
        Text(text = day.dayOfMonth.toString(), color = textColor)
    }
}


// ---------- HELPER ----------
fun LocalDate.startOfWeek(): LocalDate {
    val dayOfWeek = this.dayOfWeek
    val shift = (dayOfWeek.value % 7).toLong() // Sunday = 0
    return this.minusDays(shift)
}

@Composable
fun AddNotes(
    noteInput: String,
    onNoteInputChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(1f)
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                onClick = onDismiss,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .background(Color.White, shape = RoundedCornerShape(12.dp))
                .width(370.dp)
                .height(190.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Add Notes",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(70.dp),
                        color = Color(0xFFEFEEEE),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        NoteTextField(
                            value = noteInput,
                            onValueChange = onNoteInputChange,
                            placeholder = "Enter note",
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Divider(color = Color.Gray, thickness = 1.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Cancel",
                            color = Color.Gray,
                            fontSize = 18.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(Color.Gray)
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSave() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Save",
                            color = Color(0xFFEA1515),
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimePickerDialogContent(
    selectedHour: Int,
    selectedMinute: Int,
    isPM: Boolean = false,
    onTimeChange: (Int, Int, Boolean) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    // Convert selectedHour to 12-hour format
    var localIsPM by remember { mutableStateOf(isPM) }
    var hourText by remember {
        val hour12 = if (selectedHour % 12 == 0) 12 else selectedHour % 12
        mutableStateOf(hour12.toString().padStart(2, '0'))
    }
    var minuteText by remember { mutableStateOf(selectedMinute.toString().padStart(2, '0')) }

    fun validateAndUpdate() {
        val hour = hourText.toIntOrNull()?.coerceIn(1, 12) ?: 12
        val minute = minuteText.toIntOrNull()?.coerceIn(0, 59) ?: 0

        // Update text fields to show padded result (optional)
        hourText = hour.toString().padStart(2, '0')
        minuteText = minute.toString().padStart(2, '0')

        onTimeChange(hour, minute, localIsPM)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(1f)
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                onClick = onDismiss,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .background(Color.White, shape = RoundedCornerShape(12.dp))
                .width(350.dp)
                .wrapContentHeight()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Select Time",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        OutlinedTextField(
                            value = hourText,
                            onValueChange = {
                                if (it.length <= 2 && it.all { c -> c.isDigit() }) {
                                    val intVal = it.toIntOrNull()
                                    if (intVal == null || (intVal in 1..12)) {
                                        hourText = it
                                    }
                                }
                            },
                            modifier = Modifier.width(80.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Hour",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }


                    Column(horizontalAlignment = Alignment.Start) {
                        OutlinedTextField(
                            value = minuteText,
                            onValueChange = {
                                if (it.length <= 2 && it.all { c -> c.isDigit() }) {
                                    val intVal = it.toIntOrNull()
                                    if (intVal == null || (intVal in 0..59)) {
                                        minuteText = it
                                    }
                                }
                            },
                            modifier = Modifier.width(80.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Minute",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    // AM/PM Toggle inside same Row
                    Row(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.LightGray.copy(alpha = 0.3f))
                            .width(100.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("AM", "PM").forEach { period ->
                            val selected = (period == "PM") == localIsPM
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() })
                                    {
                                        localIsPM = period == "PM"
                                    }
                                    .background(
                                        if (selected) Color(0xFFEA1515).copy(alpha = 0.2f) else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = period,
                                    color = if (selected) Color(0xFFEA1515) else Color.Gray,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = Color.Gray.copy(alpha = 0.3f), thickness = 1.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Cancel",
                            color = Color.Gray,
                            fontSize = 18.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(Color.Gray.copy(alpha = 0.4f))
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                validateAndUpdate()
                                onSave()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Save",
                            color = Color(0xFFEA1515),
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun NoteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.padding(top = 10.dp, start = 10.dp, end = 10.dp),

        ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            maxLines = 3,
            textStyle = TextStyle(
                color = Black,
                fontSize = 14.sp
            ),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                innerTextField()
            }
        )
    }
}
