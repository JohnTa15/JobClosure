package gr.gtar.jobclosure.ui.dagr

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Login
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.getSystemService
import gr.gtar.jobclosure.dagr.DagrAutofill
import gr.gtar.jobclosure.dagr.DagrField
import gr.gtar.jobclosure.dagr.DagrFillReport
import gr.gtar.jobclosure.drone.DroneAware

/**
 * DAGR inside the app, with the flight request for one venue ready to be poured into its form.
 *
 * The WebView keeps its own cookie jar, so signing in survives leaving the screen - and when the
 * account is stored, the login form is filled the moment it appears. What the screen will never do
 * is press submit: filling a form is reversible and visible, whereas sending a flight request to
 * the civil aviation authority is neither, so that button stays DAGR's and the pilot's.
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DagrScreen(viewModel: DagrViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var isPageLoading by remember { mutableStateOf(true) }
    var canGoBack by remember { mutableStateOf(false) }

    BackHandler(enabled = canGoBack) { webView?.goBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Drone Aware - GR", style = MaterialTheme.typography.titleMedium)
                        state.request?.let {
                            Text(
                                "${it.venue.label}${if (it.venueName.isNotBlank()) " · ${it.venueName}" else ""}",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { if (canGoBack) webView?.goBack() else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Πίσω")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isPageLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.databaseEnabled = true
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                            webViewClient = object : WebViewClient() {
                                private fun autoLoginIfOnLoginPage(view: WebView?, url: String?) {
                                    val account = state.account
                                    if (!account.isConfigured || state.hasAttemptedLogin || !url.isLoginLike()) return
                                    view?.evaluateJavascript(
                                        DagrAutofill.loginScript(account.username, account.password),
                                    ) { raw -> viewModel.onLoginAttempted(DagrAutofill.parseReport(raw)) }
                                }

                                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                    isPageLoading = true
                                    // A report from the previous page would otherwise sit there
                                    // claiming a form was filled that is no longer on screen.
                                    viewModel.dismissReports()
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isPageLoading = false
                                    canGoBack = view?.canGoBack() == true
                                    // Cookies are only durable once flushed; without this a kill
                                    // from the launcher loses the session that was just created.
                                    CookieManager.getInstance().flush()
                                    autoLoginIfOnLoginPage(view, url)
                                }

                                // DAGR routes on the URL hash, and a hash change is not a page
                                // load - onPageFinished may never fire for it. This does, so it is
                                // what actually catches the move to the pilot login page.
                                override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                                    canGoBack = view?.canGoBack() == true
                                    autoLoginIfOnLoginPage(view, url)
                                }
                            }
                            loadUrl(DroneAware.MAP_URL)
                            webView = this
                        }
                    },
                )
            }

            Surface(tonalElevation = 3.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val request = state.request
                    val fields = request?.fields().orEmpty()

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                val account = state.account
                                if (!account.isConfigured) {
                                    Toast.makeText(
                                        context,
                                        "Πρόσθεσε πρώτα λογαριασμό DAGR στις Ρυθμίσεις.",
                                        Toast.LENGTH_LONG,
                                    ).show()
                                    return@OutlinedButton
                                }
                                webView?.evaluateJavascript(
                                    DagrAutofill.loginScript(account.username, account.password),
                                ) { raw -> viewModel.onLoginAttempted(DagrAutofill.parseReport(raw)) }
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Filled.Login, contentDescription = null, modifier = Modifier.size(17.dp))
                            Text("  Σύνδεση")
                        }
                        Button(
                            onClick = {
                                if (fields.isEmpty()) {
                                    Toast.makeText(context, "Δεν υπάρχουν στοιχεία για συμπλήρωση.", Toast.LENGTH_LONG).show()
                                    return@Button
                                }
                                webView?.evaluateJavascript(DagrAutofill.fillScript(fields)) { raw ->
                                    viewModel.onFilled(DagrAutofill.parseReport(raw))
                                }
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Filled.EditNote, contentDescription = null, modifier = Modifier.size(17.dp))
                            Text("  Συμπλήρωση")
                        }
                    }

                    state.loginReport?.let { ReportLine(title = "Σύνδεση", report = it) }
                    state.fillReport?.let { ReportLine(title = "Συμπλήρωση", report = it) }

                    if (request != null && !request.hasCoordinates) {
                        Text(
                            "Δεν βρέθηκαν συντεταγμένες για αυτή τη διεύθυνση - τα υπόλοιπα πεδία " +
                                "συμπληρώνονται κανονικά, τη θέση βάλ' την στον χάρτη με το χέρι.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    HorizontalDivider()
                    Text(
                        "Στοιχεία αίτησης - πάτα ένα για αντιγραφή",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    fields.forEach { field -> FieldRow(field) }

                    Text(
                        "Η υποβολή γίνεται από εσένα, στο κουμπί του DAGR - η εφαρμογή μόνο " +
                            "συμπληρώνει. Έλεγξε τη φόρμα πριν την στείλεις.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportLine(title: String, report: DagrFillReport) {
    val error = report.error
    when {
        error != null -> Text(
            "$title: $error",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error,
        )
        report.isEmpty -> Text(
            "$title: δεν βρέθηκε φόρμα σε αυτή τη σελίδα.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error,
        )
        else -> Column {
            Text(
                "$title: συμπληρώθηκαν ${report.filled.size} από ${report.total}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = if (report.missed.isEmpty()) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
            if (report.missed.isNotEmpty()) {
                // Named, not just counted: an unfilled field on a flight request is the one thing
                // that has to be typed by hand, and the form looks complete without it.
                Text(
                    "Μένουν με το χέρι: ${report.missed.joinToString(", ")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun FieldRow(field: DagrField) {
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            field.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            field.value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1.4f),
        )
        IconButton(
            onClick = {
                context.getSystemService<ClipboardManager>()
                    ?.setPrimaryClip(ClipData.newPlainText(field.label, field.value))
                Toast.makeText(context, "Αντιγράφηκε: ${field.label}", Toast.LENGTH_SHORT).show()
            },
        ) {
            Icon(Icons.Filled.ContentCopy, contentDescription = "Αντιγραφή ${field.label}", modifier = Modifier.size(17.dp))
        }
    }
}

/**
 * DAGR is a single-page app, so "we are on the login page" is a guess from the URL rather than
 * something the app is told. Guessing wrong only costs a fill attempt that finds no fields and
 * says so.
 */
private fun String?.isLoginLike(): Boolean {
    val url = this?.lowercase() ?: return false
    return listOf("login", "signin", "sign-in", "account", "auth").any { url.contains(it) }
}
