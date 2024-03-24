package xyz.angeloanan.healthconnectexports.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient

@Composable
fun HealthConnectProblemsBanner() {
    val context = LocalContext.current
    val availabilityStatus = HealthConnectClient.getSdkStatus(context)

    // Check if Android version is >= 9
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.P) {
        HealthConnectUnavailable()
    }

    when (availabilityStatus) {
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectNeedsUpdate()
        HealthConnectClient.SDK_UNAVAILABLE -> HealthConnectNeedsInstall()
    }
}

@Composable
fun HealthConnectUnavailable() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Red)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = "Warning",
                tint = Color.White
            )
            Column {
                Text(
                    text = "Health Connect is not supported",
                    color = Color.White,
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Please use a device running Android 9 or later",
                    color = Color.White,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
fun HealthConnectNeedsUpdate() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Red)
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = "Warning",
                    tint = Color.White
                )
                Column {
                    Text(
                        text = "Health Connect needs an update!",
                        color = Color.White,
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Please update Health Connect from the Play Store",
                        color = Color.White,
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun HealthConnectNeedsInstall() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Red)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = "Warning",
                tint = Color.White
            )
            Column {
                Text(
                    text = "Health Connect App is not installed",
                    color = Color.White,
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Please install Health Connect from the Play Store",
                    color = Color.White,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

