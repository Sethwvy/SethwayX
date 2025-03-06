package `in`.sethway.services

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.baxolino.smartudp.SmartUDP
import com.tencent.mmkv.MMKV
import `in`.sethway.App
import `in`.sethway.protocol.Devices
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.InetAddress

class NotificationShareService : NotificationListenerService() {

  companion object {
    private const val TAG = "SethwayNSX"
  }

  private lateinit var mmkv: MMKV
  private lateinit var smartUDP: SmartUDP

  override fun onCreate() {
    super.onCreate()
    App.initMMKV(this)
    mmkv = MMKV.mmkvWithID("sync")
    smartUDP = SmartUDP().create(App.SYNC_TRANS_PORT)

    smartUDP.route("sync_direct_ack") { _, bytes ->
      val json = JSONObject(String(bytes))
      val id = json.getString("id")
      val entryId = json.getLong("entryId")
      saveSyncAck(id, entryId)
      null
    }
  }

  private fun saveSyncAck(id: String, entryId: Long) {
    val deviceName = getDeviceName(id)
    Log.d(TAG, "Received sync acknowledgment from $deviceName for Id $entryId")
    val entryAcknowledgements = JSONArray(mmkv.decodeString(entryId.toString(), "[]"))
    entryAcknowledgements.put(id)
    mmkv.encode(entryId.toString(), entryAcknowledgements.toString())
  }

  private fun getDeviceName(id: String): String = Devices.getClient(id).getString("device_name")

  override fun onDestroy() {
    super.onDestroy()
    smartUDP.close()
  }

  override fun onNotificationPosted(sbn: StatusBarNotification) {
    val extras = sbn.notification.extras
    val title = (extras.get("android.title") ?: return).toString()
    val text = (extras.get("android.text") ?: return).toString()

    val entry = createNotificationEntry(title, text)
    deliverNew(createDeliveryEntry(entry))
  }

  /**
   * Create an simple JSON entry of the notification
   */
  private fun createNotificationEntry(title: String, text: String): JSONObject = JSONObject()
    .put("title", title)
    .put("text", text)
    .put("time", System.currentTimeMillis()) // Time is a Unique Id!

  /**
   * Prepare the notification entry for transmission along with some metadata
   */
  private fun createDeliveryEntry(entry: JSONObject): JSONObject = JSONObject()
    .put("me", App.ID)
    .put("type", "notification")
    .put("notification", entry)

  private fun deliverNew(entry: JSONObject) {
    val payload = entry.toString().toByteArray()
    forEachClientAddress { address: String ->
      try {
        smartUDP.message(
          InetAddress.getByName(address),
          App.SYNC_REC_PORT,
          payload,
          "sync_direct"
        )
      } catch (e: IOException) {
        println("I/O deliverNew() ${e.javaClass.simpleName} ${e.message}")
      }
    }
  }

  private fun forEachClientAddress(consumer: (address: String) -> Unit) {
    val clients = Devices.getClients()
    val clientsLen = clients.length()
    for (i in 0..<clientsLen) {
      val client = clients.getJSONObject(i)
      val addresses = client.getJSONArray("addresses")
      val addrLen = addresses.length()
      for (j in 0..<addrLen) {
        val address = addresses.getString(j)
        consumer(address)
      }
    }
  }
}