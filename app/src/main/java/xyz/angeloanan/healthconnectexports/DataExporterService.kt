package xyz.angeloanan.healthconnectexports

import android.app.Service
import android.content.Intent
import android.os.IBinder


class DataExporterService : Service() {
    // Notification Channel

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        val service = this
//
//        Log.d("DataExporterService", "Checking exports prerequisites")
//        val healthConnect = HealthConnectClient.getOrCreate(applicationContext)
//        val isGranted = runBlocking { isHealthConnectPermissionGranted(healthConnect) }
//
//        if (!isGranted) {
//            Log.d("DataExporterService", "Health Connect permissions not granted. Finishing")
//            stopSelf()
//            return START_REDELIVER_INTENT
//        }
//        Log.d("DataExporterService", "✅ Health Connect permissions granted")
//
//        val exportDestination: String? = runBlocking {
//            service.dataStore.data.map { it[EXPORT_DESTINATION_URI] }.first()
//        }
//        if (exportDestination == null) {
//            Log.d("DataExporterService", "Export destination not set. Finishing")
//            stopSelf()
//            return START_REDELIVER_INTENT
//        }
//        Log.d("DataExporterService", "✅ Export destination set")
//
//        val notificationChannel = createNotificationChannel()
//        val foregroundNotification =
//            NotificationCompat.Builder(applicationContext, notificationChannel.id)
//                .setContentTitle("Exporting data")
//                .setContentText("Exporting Health Connect data to the cloud")
//                .setSmallIcon(R.drawable.ic_launcher_foreground).setOngoing(true).build()
//
//        Log.d("DataExporterService", "Starting foreground service")
//        ServiceCompat.startForeground(
//            service, 1, foregroundNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
//        )
//
//        Log.d("DataExporterService", "Fetching health data")
//        val zoneId = TimeZone.getDefault().toZoneId()
//
//        // Start of day yesterday
//        val startOfDay = LocalDate.now(zoneId).atStartOfDay(zoneId).minusDays(1).toInstant()
//        val endOfDay =
//            LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant().minusMillis(1)
//        val healthDataAggregate = runBlocking {
//            healthConnect.aggregate(
//                AggregateRequest(
//                    metrics = setOf(
//                        StepsRecord.COUNT_TOTAL,
//                        ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL,
//                        TotalCaloriesBurnedRecord.ENERGY_TOTAL,
//                        SleepSessionRecord.SLEEP_DURATION_TOTAL,
//                    ),
//                    timeRangeFilter = TimeRangeFilter.Companion.between(startOfDay, endOfDay),
//                )
//            )
//        }
//        Log.d("DataExporterService", "Raw data: ${Gson().toJson(healthDataAggregate)}")
//
//        val jsonValues = HashMap<String, Number>()
//        jsonValues["steps"] = healthDataAggregate[StepsRecord.COUNT_TOTAL] ?: 0
//        jsonValues["active_calories"] =
//            healthDataAggregate[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories
//                ?: 0
//        jsonValues["total_calories"] =
//            healthDataAggregate[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories ?: 0
//        jsonValues["sleep_duration_seconds"] =
//            healthDataAggregate[SleepSessionRecord.SLEEP_DURATION_TOTAL]?.seconds ?: 0
//        val json = Gson().toJson(mapOf("time" to startOfDay.toEpochMilli(), "data" to jsonValues))
//        Log.d("DataExporterService", "Data: $json")
//
//        Log.d("DataExporterService", "Exporting data to $exportDestination")
//        runBlocking {
//            httpClient.post("https://$exportDestination") {
//                contentType(ContentType.Application.Json)
//                setBody(json)
//            }
//        }
//
//        Log.d("DataExporterService", "Finished all tasks. Stopping self")
//        stopSelf()

        return START_REDELIVER_INTENT
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}