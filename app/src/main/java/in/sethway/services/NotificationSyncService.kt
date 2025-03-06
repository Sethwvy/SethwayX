package `in`.sethway.services

import android.app.Service
import android.content.Intent
import android.util.Log
import com.baxolino.smartudp.SmartUDP
import com.tencent.mmkv.MMKV
import `in`.sethway.App
import `in`.sethway.protocol.Devices
import org.json.JSONObject
import java.io.IOException
import java.net.InetAddress
import java.util.concurrent.Executors

class NotificationSyncService : Service() {

  companion object {
    private const val TAG = "SethwayNSS"
  }

  private lateinit var mmkv: MMKV
  private lateinit var smartUDP: SmartUDP

  private val executor = Executors.newSingleThreadExecutor()

  override fun onCreate() {
    super.onCreate()
    App.initMMKV(this)
    mmkv = MMKV.mmkvWithID("sync")
    smartUDP = SmartUDP().create(App.SYNC_REC_PORT)

    smartUDP.route("sync_direct") { address, bytes ->
      val json = JSONObject(String(bytes))
      val id = json.getString("id")
      val entryId = json.getLong("time") // entryId is Time!

      val notification = json.getJSONObject("notification")
      consumeSyncEntry(id, notification)

      executor.submit { acknowledgeSyncEntry(address, entryId) }

      null
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
    val subtitle = notification.getString("subtitle")

    Log.d(TAG, "Received notification from $deviceName (title=$title, subtitle=$subtitle)")
  }

  private fun getDeviceName(id: String): String = Devices.getSource(id).getString("device_name")

  override fun onDestroy() {
    super.onDestroy()
    smartUDP.close()
    executor.shutdownNow()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    return START_STICKY
  }

  override fun onBind(intent: Intent?) = null
}