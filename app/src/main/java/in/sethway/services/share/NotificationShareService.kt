package `in`.sethway.services.share

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
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class NotificationShareService : NotificationListenerService() {

  companion object {
    private const val TAG = "SethwayNSX"
  }

  private lateinit var mmkv: MMKV
  private lateinit var smartUDP: SmartUDP

  private lateinit var executor: ExecutorService

  override fun onCreate() {
    super.onCreate()
    App.initMMKV(this)
    EntryBacklog.init()
    mmkv = MMKV.mmkvWithID("sync")
    smartUDP = SmartUDP().create(App.SYNC_TRANS_PORT)
    executor = Executors.newSingleThreadExecutor()

    smartUDP.route("sync_direct_ack") { _, bytes ->
      val json = JSONObject(String(bytes))
      val id = json.getString("id")
      val entryId = json.getLong("entryId")
      saveSyncAck(id, entryId)
      null
    }

    smartUDP.route("clear_backlog") { _, bytes ->
      Log.d(TAG, "Asking for backlog")
      val json = JSONObject(String(bytes))
      val id = json.getString("id")
      executor.submit { clearBacklog(id) }
      null
    }
  }

  private fun clearBacklog(id: String) {
    EntryBacklog.forEachBacklog(id) { entry ->
      deliverTo(id, createDeliveryEntry(entry))
    }
  }

  private fun saveSyncAck(id: String, entryId: Long) {
    EntryBacklog.acknowledgeDelivery(id, entryId)

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
    EntryBacklog.add(entry)
    executor.submit {
      deliverToAll(createDeliveryEntry(entry))
    }
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
    .put("id", App.ID)
    .put("type", "notification")
    .put("notification", entry)

  private fun deliverToAll(entry: JSONObject) {
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

  private fun deliverTo(id: String, entry: JSONObject) {
    val payload = entry.toString().toByteArray()
    forEachAddressOfClient(id) { address: String ->
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
        consumer(addresses.getString(j))
      }
    }
  }

  private fun forEachAddressOfClient(id: String, consumer: (address: String) -> Unit) {
    val client = Devices.getClient(id)
    val addresses = client.getJSONArray("addresses")
    val addrLen = addresses.length()
    for (i in 0..<addrLen) {
      consumer(addresses.getString(i))
    }
  }
}