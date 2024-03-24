package xyz.angeloanan.healthconnectexports

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import xyz.angeloanan.healthconnectexports.ui.components.HealthConnectProblemsBanner
import xyz.angeloanan.healthconnectexports.ui.theme.HealthConnectExportsTheme
import xyz.angeloanan.healthconnectexports.ui.viewmodels.ExporterBackgroundWorkViewModel
import xyz.angeloanan.healthconnectexports.ui.viewmodels.WORK_NAME_ONCE

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
val EXPORT_DESTINATION_URI = stringPreferencesKey("export_destination_uri")

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val colorScheme = dynamicDarkColorScheme(applicationContext)
        super.onCreate(savedInstanceState)

        setContent {
            HealthConnectExportsTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(), color = colorScheme.background
                ) {
                    Scaffold(
                        topBar = {
                            TopAppBar(colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = colorScheme.surface,
                                scrolledContainerColor = colorScheme.surface,
                                navigationIconContentColor = colorScheme.onSurface,
                                actionIconContentColor = colorScheme.onSurface,
                                titleContentColor = colorScheme.onSurface
                            ), title = {
                                Text(
                                    text = "Health Connect Exporter",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            })
                        },
                    ) { innerPadding ->
                        Column(
                            modifier = Modifier.padding(innerPadding),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HealthConnectProblemsBanner()
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                RequestPermissionButton()
                                RequestHealthConnectPermissionButton()
                            }
                            ExportDestinationInputField()
                            Row {
                                RunDataExportButton()
                            }
                            ScheduleSwitch()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduleSwitch(
    viewModel: ExporterBackgroundWorkViewModel = viewModel()
) {
    val (checked, setChecked) = remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(true) {
        setChecked(viewModel.isWorkScheduled())
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Text("Schedule Data Export")
        Switch(
            enabled = checked != null,
            checked = checked ?: false,
            modifier = Modifier.padding(start = 16.dp),
            onCheckedChange = {
                setChecked(it)
                if (it) {
                    viewModel.scheduleWork()
                } else {
                    viewModel.cancelWork()
                }
            },
        )
    }
}

@Composable
fun ExportDestinationInputField() {
    val context = LocalContext.current
    val (fieldValue, setFieldValue) = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(true) {
        setFieldValue(context.dataStore.data.map { it[EXPORT_DESTINATION_URI] }.first() ?: "")
    }

    LaunchedEffect(fieldValue) {
        if (fieldValue == null) return@LaunchedEffect
        context.dataStore.edit { it[EXPORT_DESTINATION_URI] = fieldValue }
    }

    TextField(enabled = fieldValue != null,
        placeholder = { Text("Export destination URI") },
        value = fieldValue ?: "Loading...",
        maxLines = 1,
        prefix = { Text("http://") },
        keyboardOptions = KeyboardOptions(
            KeyboardCapitalization.None,
            autoCorrect = false,
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Go
        ),
        onValueChange = { setFieldValue(it) })
}

@Composable
fun RunDataExportButton() {
    val context = LocalContext.current
    val workManager = WorkManager.getInstance(context)

    Button(
        onClick = {
            workManager.enqueueUniqueWork(
                WORK_NAME_ONCE,
                androidx.work.ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequest.from(DataExporterScheduleWorker::class.java)
            )
        }
    ) {
        Text(text = "Run Data Export")
    }
}

val requiredPermissions = arrayOf(
    android.Manifest.permission.POST_NOTIFICATIONS,
)

@Composable
fun RequestPermissionButton() {
    val context = LocalContext.current
    val isPermissionGranted = ActivityCompat.checkSelfPermission(
        context as MainActivity, android.Manifest.permission.POST_NOTIFICATIONS
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    Button(
        enabled = !isPermissionGranted,
        onClick = {
            Log.d("MainActivity", "RequestPermissionButton is Pressed")
            ActivityCompat.requestPermissions(
                context,
                requiredPermissions,
                1,
            )
        },
    ) {
        Text(text = "Notification Permission")
    }
}

@Composable
fun RequestHealthConnectPermissionButton() {
    var isButtonEnabled = true

    val context = LocalContext.current
    val healthConnect = HealthConnectClient.getOrCreate(context)
    val permissionLauncher =
        rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
            Log.d("MainActivity", "Health Connect granted permissions: $granted")
        }

    Button(
        enabled = isButtonEnabled,
        onClick = {
            permissionLauncher.launch(requiredHealthConnectPermissions)
        },
    ) {
        Text(text = "Health Connect Permission")
    }
}
