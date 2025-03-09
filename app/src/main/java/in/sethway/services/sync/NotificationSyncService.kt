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
import `in`.sethway.protocol.Query
import `in`.sethway.services.SyncApp
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.InetAddress
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class NotificationSyncService : Service() {

  companion object {
    private const val TAG = "SethwayNSS"
  }

  private lateinit var mmkv: MMKV
  private lateinit var smartUDP: SmartUDP

  private lateinit var notificationManager: NotificationManager

  private lateinit var executor: ExecutorService
  private lateinit var periodicExecutor: ScheduledExecutorService

  override fun onCreate() {
    super.onCreate()
    App.initMMKV(this)
    Devices.init()
    EntryLog.init()
    mmkv = MMKV.mmkvWithID("sync")
    smartUDP = SmartUDP().create(App.SYNC_REC_PORT)
    executor = Executors.newSingleThreadExecutor()
    periodicExecutor = Executors.newScheduledThreadPool(1)

    notificationManager = getSystemService(NotificationManager::class.java)

    smartUDP.route("sync_direct") { address, bytes ->
      val json = JSONObject(String(bytes))
      val id = json.getString("id")
      if (Devices.sourceExists(id)) {
        val addresses = json.getJSONArray("addresses")

        updateSourceAddresses(id, addresses)

        val notification = json.getJSONObject("notification")
        val entryId = notification.getLong("time") // entryId is Time!

        if (!EntryLog.existEntry(entryId)) {
          EntryLog.addEntry(entryId, notification)
          consumeSyncEntry(id, notification)
          executor.submit { acknowledgeSyncEntry(address, entryId) }
        }
      }
      null
    }

    smartUDP.route("ping") { address, bytes ->
      val json = JSONObject(String(bytes))
      val id = json.getString("id")
      if (Devices.sourceExists(id)) {
        val addresses = json.getJSONArray("addresses")
        updateSourceAddresses(id, addresses)
      }
      null
    }

    // The server found the IP addresses of the Source that got lost
    smartUDP.route("device_found") { _, bytes ->
      val json = JSONObject(String(bytes))
      val whom = json.getString("whom")
      val addresses = json.getJSONArray("addresses")
      updateSourceAddresses(whom, addresses)
      Log.d(TAG, "Updated source addresses with server help")
      null
    }

    periodicExecutor.scheduleWithFixedDelay({
      periodicPing()
      findSources()
    }, 0, 5, TimeUnit.SECONDS)
  }

  /**
   * Attempt to clear any possible backlog with the sources
   */
  private fun periodicPing() {
    println("Doing periodic ping")
    val payload = Query.pingPayload()
    forEachSourceAddresses { address: String ->
      try {
        println("Sending ping to $address")
        smartUDP.message(
          InetAddress.getByName(address),
          App.SYNC_TRANS_PORT,
          payload,
          "ping"
        )
      } catch (e: IOException) {
        println("I/O periodicPing() ${e.javaClass.simpleName} ${e.message}")
      }
    }
    SyncApp.forAllServerDestination { address: String ->
      println("Sending server ping to $address")
      try {
        smartUDP.message(
          InetAddress.getByName(address),
          App.BRIDGE_PORT,
          payload,
          "ping"
        )
      } catch (e: IOException) {
        println("I/O server! periodicPing() ${e.javaClass.simpleName} ${e.message}")
      }
    }
  }

  private fun findSources() {
    val sources = Devices.getSources()
    for (key in sources.keys()) {
      val client = sources.getJSONObject(key)
      val addressUpdatedTime = client.getLong("address_updated_time")
      if (System.currentTimeMillis() - addressUpdatedTime >= SyncApp.MAX_PERMITTED_TIME_NOT_SEEN) {
        // we need to ask the server to give us Client's new IP address set
        requestUpdatedIpOfSource(client.getString("id"))
      }
    }
  }

  private fun requestUpdatedIpOfSource(sourceId: String) {
    val payload = JSONObject()
      .put("whom", sourceId)
      .put("reply_port", App.SYNC_TRANS_PORT)
      .toString()
      .toByteArray()
    SyncApp.forAllServerDestination { address: String ->
      try {
        smartUDP.message(
          InetAddress.getByName(address),
          App.BRIDGE_PORT,
          payload,
          "find"
        )
      } catch (e: IOException) {
        println("I/O server! requestUpdatedIpOfSource() ${e.javaClass.simpleName} ${e.message}")
      }
    }
  }

  private fun updateSourceAddresses(id: String, addresses: JSONArray) {
    val source = Devices.getSource(id)
    source.put("addresses", addresses)
    source.put("address_updated_time", System.currentTimeMillis())
    Devices.addSource(source)
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
    for (key in sources.keys()) {
      val source = sources.getJSONObject(key)
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