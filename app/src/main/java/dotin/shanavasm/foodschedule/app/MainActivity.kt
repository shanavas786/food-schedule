package dotin.shanavasm.foodschedule.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.core.content.ContextCompat
import dotin.shanavasm.foodschedule.app.ui.FoodScheduleApp
import dotin.shanavasm.foodschedule.app.ui.theme.FoodScheduleTheme

class MainActivity : ComponentActivity() {

    // Launcher for POST_NOTIFICATIONS permission (Android 13+)
    private val notifPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // Permission granted or denied — alarm is already scheduled regardless
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up notification channel (idempotent)
        NotificationHelper.createChannel(this)

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Schedule the daily 10 AM reminder (replaces any existing alarm)
        ReminderScheduler.scheduleDailyAlarm(this)

        setContent {
            FoodScheduleTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    FoodScheduleApp()
                }
            }
        }
    }
}
