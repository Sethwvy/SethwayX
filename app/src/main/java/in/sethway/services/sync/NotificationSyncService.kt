package `in`.sethway.services.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.baxolino.smartudp.SmartUDP
import com.tencent.mmkv.MMKV
import `in`.sethway.App
import `in`.sethway.R
import `in`.sethway.protocol.Devices
import org.json.JSONObject
import java.io.IOException
import java.net.InetAddress
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class NotificationSyncService : Service() {

  companion object {
    private const val TAG = "SethwayNSS"
  }

  private lateinit var mmkv: MMKV
  private lateinit var smartUDP: SmartUDP

  private lateinit var notificationManager: NotificationManager

  private val executor = Executors.newSingleThreadExecutor()
  private val periodicExecutor = Executors.newScheduledThreadPool(1)

  override fun onCreate() {
    super.onCreate()
    App.initMMKV(this)
    mmkv = MMKV.mmkvWithID("sync")
    smartUDP = SmartUDP().create(App.SYNC_REC_PORT)
    notificationManager = getSystemService(NotificationManager::class.java)

    smartUDP.route("sync_direct") { address, bytes ->
      val json = JSONObject(String(bytes))
      val id = json.getString("id")

      val notification = json.getJSONObject("notification")
      val entryId = notification.getLong("time") // entryId is Time!
      consumeSyncEntry(id, notification)

      executor.submit { acknowledgeSyncEntry(address, entryId) }

      null
    }

    periodicExecutor.scheduleWithFixedDelay({
      clearBacklog()
    }, 0, 5, TimeUnit.SECONDS)
  }

  /**
   * Attempt to clear any possible backlog with the sources
   */
  private fun clearBacklog() {
    val payload = JSONObject()
      .put("me", App.ID)
      .toString()
      .toByteArray()
    forEachSourceAddresses { address: String ->
      try {
        smartUDP.message(
          InetAddress.getByName(address),
          App.SYNC_TRANS_PORT,
          payload,
          "clear_backlog"
        )
      } catch (e: IOException) {
        println("I/O clearBacklog() ${e.javaClass.simpleName} ${e.message}")
      }
    }
  }

  private fun acknowledgeSyncEntry(address: InetAddress, entryId: Long) {
    val acknowledgement = JSONObject()
      .put("id", App.ID)
      .put("type", "acknowledgement")
      .put("entryId", entryId)
      .toString()
      .toByteArray()
    try {
      smartUDP.message(
        address,
        App.SYNC_TRANS_PORT,
        acknowledgement,
        "sync_direct_ack"
      )
    } catch (e: IOException) {
      println("I/O acknowledgeSyncEntry() ${e.javaClass.simpleName} ${e.message}")
    }
  }

  private fun consumeSyncEntry(id: String, notification: JSONObject) {
    val deviceName = getDeviceName(id)
    val title = notification.getString("title")
    val subtitle = notification.getString("text")

    Log.d(TAG, "Received notification from $deviceName (title=$title, text=$subtitle)")

    notificationManager.notify(
      Random.nextInt(),
      NotificationCompat.Builder(this, "sync_service")
        .setContentTitle(title)
        .setContentText(subtitle)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .build()
    )
  }

  private fun getDeviceName(id: String): String = Devices.getSource(id).getString("device_name")

  private fun forEachSourceAddresses(consumer: (address: String) -> Unit) {
    val sources = Devices.getSources()
    val sourcesLen = sources.length()
    for (i in 0..<sourcesLen) {
      val source = sources.getJSONObject(i)
      val addresses = source.getJSONArray("addresses")
      val addrLen = addresses.length()
      for (j in 0..<addrLen) {
        val address = addresses.getString(j)
        consumer(address)
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    smartUDP.close()
    executor.shutdownNow()
    periodicExecutor.shutdownNow()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    createForeground()
    return START_STICKY
  }

  private fun createForeground() {
    val notificationManager = getSystemService(NotificationManager::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      notificationManager.createNotificationChannel(
        NotificationChannel(
          "sync_service",
          "Notification Sync Service",
          NotificationManager.IMPORTANCE_HIGH
        )
      )
    }
    startForeground(
      1,
      NotificationCompat.Builder(this, "sync_service")
        .setContentTitle("Sethway Sync")
        .setContentText("Lookup for new messages")
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .build()
    )
  }

  override fun onBind(intent: Intent?) = null
}