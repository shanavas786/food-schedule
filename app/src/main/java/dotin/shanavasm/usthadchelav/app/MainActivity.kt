package dotin.shanavasm.usthadchelav.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import dotin.shanavasm.usthadchelav.app.ui.CleaningScheduleApp
import dotin.shanavasm.usthadchelav.app.ui.theme.ChelavScheduleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChelavScheduleTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    CleaningScheduleApp()
                }
            }
        }
    }
}
