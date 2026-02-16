package yug.ramoliya.daymaker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import yug.ramoliya.daymaker.screen.ChatScreen
import yug.ramoliya.daymaker.ui.theme.DaymakerTheme
import yug.ramoliya.daymaker.data.FirebaseRepository

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            DaymakerTheme {
                ChatScreen()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

    }
}