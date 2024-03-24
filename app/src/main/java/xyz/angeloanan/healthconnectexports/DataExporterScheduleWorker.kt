package xyz.angeloanan.healthconnectexports

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.util.TimeZone

val httpClient = HttpClient(Android)

val requiredHealthConnectPermissions = setOf(
    HealthPermission.getReadPermission(StepsRecord::class),
    HealthPermission.getReadPermission(SleepSessionRecord::class),
    HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
    HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
    HealthPermission.getReadPermission(HeartRateRecord::class),
)

class DataExporterScheduleWorker(
    appContext: Context, workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    private val notificationManager = applicationContext.getSystemService<NotificationManager>()!!
    private val healthConnect = HealthConnectClient.getOrCreate(applicationContext)

    private fun createNotificationChannel(): NotificationChannel {
        val notificationChannel = NotificationChannel(
            "export",
            "Data export",
            NotificationManager.IMPORTANCE_LOW,
        )
        notificationChannel.description = "Shown when Health Connect data is being exported"
        notificationChannel.enableLights(false)
        notificationChannel.enableVibration(false)

        notificationManager.createNotificationChannel(notificationChannel)
        return notificationChannel
    }

    private fun createExceptionNotification(e: Exception): Notification {
        return NotificationCompat.Builder(applicationContext, "export")
            .setContentTitle("Export failed")
            .setContentText("Failed to export Health Connect data")
            .setStyle(NotificationCompat.BigTextStyle().bigText(e.message))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
    }

    private suspend fun isHealthConnectPermissionGranted(healthConnect: HealthConnectClient): Boolean {
        val grantedPermissions = healthConnect.permissionController.getGrantedPermissions()
        return requiredHealthConnectPermissions.all { it in grantedPermissions }
    }

    override suspend fun doWork(): Result {
        val notificationChannel = createNotificationChannel()

        Log.d("DataExporterWorker", "Checking exports prerequisites")
        val isGranted = isHealthConnectPermissionGranted(healthConnect)

        if (!isGranted) {
            Log.d("DataExporterWorker", "Health Connect permissions not granted")
            return Result.failure()
        }
        Log.d("DataExporterWorker", "✅ Health Connect permissions granted")

        val exportDestination: String? =
            applicationContext.dataStore.data.map { it[EXPORT_DESTINATION_URI] }.first()
        if (exportDestination == null) {
            Log.d("DataExporterWorker", "Export destination not set")
            return Result.failure()
        }
        Log.d("DataExporterWorker", "✅ Export destination set")

        val foregroundNotification =
            NotificationCompat.Builder(applicationContext, notificationChannel.id)
                .setContentTitle("Exporting data")
                .setContentText("Exporting Health Connect data to the cloud")
                .setSmallIcon(R.drawable.ic_launcher_foreground).setOngoing(true)
                .build()

        notificationManager.notify(1, foregroundNotification)

        // TODO: Lock this to a specific timezone
        val zoneId = TimeZone.getDefault().toZoneId()
        // Start of day yesterday
        val startOfDay = LocalDate.now(zoneId).atStartOfDay(zoneId).minusDays(1).toInstant()
        val endOfDay = LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant().minusMillis(1)

        Log.d("DataExporterWorker", "Fetching health data")
        val healthDataAggregate = runBlocking {
            healthConnect.aggregate(
                AggregateRequest(
                    metrics = setOf(
                        StepsRecord.COUNT_TOTAL,
                        ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL,
                        TotalCaloriesBurnedRecord.ENERGY_TOTAL,
                        SleepSessionRecord.SLEEP_DURATION_TOTAL,
                    ),
                    timeRangeFilter = TimeRangeFilter.Companion.between(startOfDay, endOfDay),
                )
            )
        }
        Log.d("DataExporterWorker", "Raw data: ${Gson().toJson(healthDataAggregate)}")

        val jsonValues = HashMap<String, Number>()
        jsonValues["steps"] = healthDataAggregate[StepsRecord.COUNT_TOTAL] ?: 0
        jsonValues["active_calories"] =
            healthDataAggregate[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories
                ?: 0
        jsonValues["total_calories"] =
            healthDataAggregate[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories ?: 0
        jsonValues["sleep_duration_seconds"] =
            healthDataAggregate[SleepSessionRecord.SLEEP_DURATION_TOTAL]?.seconds ?: 0
        val json = Gson().toJson(mapOf("time" to startOfDay.toEpochMilli(), "data" to jsonValues))
        Log.d("DataExporterWorker", "Data: $json")

        try {
            Log.d("DataExporterWorker", "Exporting data to $exportDestination")
            httpClient.post("https://$exportDestination") {
                contentType(ContentType.Application.Json)
                setBody(json)
            }
        } catch (e: Exception) {
            Log.e("DataExporterWorker", "Failed to export data", e)

            notificationManager.cancel(1)
            notificationManager.notify(1, createExceptionNotification(e))
            return Result.failure()
        }

        notificationManager.cancel(1)
        return Result.success()
    }
}