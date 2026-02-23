package dotin.shanavasm.foodschedule.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import dotin.shanavasm.foodschedule.app.ui.FoodScheduleApp
import dotin.shanavasm.foodschedule.app.ui.theme.FoodScheduleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FoodScheduleTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    FoodScheduleApp()
                }
            }
        }
    }
}
