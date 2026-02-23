package dotin.shanavasm.foodschedule.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fired every day at 10 AM by AlarmManager.
 * Checks whether any of the next 4 calendar days are unassigned,
 * and shows a notification if all 4 are unassigned.
 *
 * Also re-schedules the alarm for the next day so it survives across days
 * (exact repeating alarms are not guaranteed on modern Android).
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Re-schedule alarm for tomorrow so it keeps firing daily
        ReminderScheduler.scheduleDailyAlarm(context)

        // Check notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val dm = DataManager(context)

        // Collect the next 4 date strings starting from today
        val sdf  = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal  = Calendar.getInstance()
        val next4 = (0 until 4).map { offset ->
            val c = Calendar.getInstance().apply {
                time = cal.time
                add(Calendar.DAY_OF_YEAR, offset)
            }
            sdf.format(c.time)
        }

        // A date is "assigned" if any current-iteration member has that assignedDate
        // and has not had their schedule removed (confirmed != false)
        val assignedDates = dm.members
            .filter { it.assignedDate != null || it.confirmed != true }
            .mapNotNull { it.assignedDate }
            .toSet()

        val unassignedCount = next4.count { it !in assignedDates }

        // Only notify if ANY 4 upcoming days are unassigned
        if (unassignedCount > 0) {
            NotificationHelper.showUnassignedReminder(context, unassignedCount)
        }
    }
}
