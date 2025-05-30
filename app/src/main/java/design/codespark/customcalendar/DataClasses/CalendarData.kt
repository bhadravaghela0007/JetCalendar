package design.codespark.customcalendar.DataClasses

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.time.LocalTime

@Parcelize
data class CalendarData(
    val eventId: String,
    val title: String,
    val date: String,
    var time: LocalTime = LocalTime.now()
) : Parcelable