package dotin.shanavasm.foodschedule.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Fired when the device boots.
 * AlarmManager alarms are cleared on reboot, so we re-schedule the daily 10 AM alarm here.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            NotificationHelper.createChannel(context)
            ReminderScheduler.scheduleDailyAlarm(context)
        }
    }
}
