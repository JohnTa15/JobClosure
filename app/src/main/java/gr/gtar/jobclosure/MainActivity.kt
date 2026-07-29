package gr.gtar.jobclosure

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import gr.gtar.jobclosure.ui.navigation.JobClosureNavHost
import gr.gtar.jobclosure.ui.theme.JobClosureTheme
import gr.gtar.jobclosure.update.UpdateCheckResult
import gr.gtar.jobclosure.update.UpdateNotifier
import gr.gtar.jobclosure.update.UpdateStatusHolder
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { /* no-op: screens re-check on demand */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permissions = mutableListOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions += Manifest.permission.POST_NOTIFICATIONS
        }
        requestPermissions.launch(permissions.toTypedArray())

        checkForUpdate()

        setContent {
            JobClosureTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    JobClosureNavHost(app = application as JobClosureApp)
                }
            }
        }
    }

    private fun checkForUpdate() {
        val app = application as JobClosureApp
        lifecycleScope.launch {
            val result = app.updateRepository.checkForUpdate()
            UpdateStatusHolder.set(result)
            if (result is UpdateCheckResult.UpdateAvailable) {
                UpdateNotifier.showUpdateAvailable(this@MainActivity, result.versionName)
            }
        }
    }
}
