package dotin.shanavasm.foodschedule.app

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.core.content.ContextCompat
import dotin.shanavasm.foodschedule.app.ui.FoodScheduleApp
import dotin.shanavasm.foodschedule.app.ui.theme.FoodScheduleTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val vm: ScheduleViewModel by viewModels()

    // ── Notification permission ───────────────────────────────────────────────
    private val notifPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    // ── Export: SAF "create document" launcher ────────────────────────────────
    private val exportLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
            if (uri == null) return@registerForActivityResult
            try {
                val json = vm.exportJson()
                contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                Toast.makeText(this, "Export successful", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

    // ── Import: SAF "open document" launcher ─────────────────────────────────
    private val importLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri == null) return@registerForActivityResult
            try {
                val json = contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.readText()
                    ?: throw Exception("Could not read file")
                val error = vm.importJson(json)
                if (error == null) {
                    Toast.makeText(this, "Import successful", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

    /** Called from Compose UI to trigger the SAF export picker */
    fun launchExport() {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        exportLauncher.launch("food_schedule_$date.json")
    }

    /** Called from Compose UI to trigger the SAF import picker */
    fun launchImport() {
        importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
    }

    // ─────────────────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NotificationHelper.createChannel(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        ReminderScheduler.scheduleDailyAlarm(this)

        setContent {
            FoodScheduleTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    FoodScheduleApp(
                        vm            = vm,
                        onExport      = { launchExport() },
                        onImport      = { launchImport() }
                    )
                }
            }
        }
    }
}
