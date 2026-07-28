package gr.gtar.jobclosure

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import gr.gtar.jobclosure.ui.navigation.JobClosureNavHost
import gr.gtar.jobclosure.ui.theme.JobClosureTheme

class MainActivity : ComponentActivity() {

    private val requestCalendarPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { /* no-op: screens re-check on demand */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestCalendarPermissions.launch(
            arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
        )

        setContent {
            JobClosureTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    JobClosureNavHost(app = application as JobClosureApp)
                }
            }
        }
    }
}
