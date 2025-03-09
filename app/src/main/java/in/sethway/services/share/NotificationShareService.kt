package `in`.sethway.services.share

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.baxolino.smartudp.SmartUDP
import com.tencent.mmkv.MMKV
import `in`.sethway.App
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

class NotificationShareService : NotificationListenerService() {

  companion object {
    private const val TAG = "SethwayNSX"
  }

  private lateinit var mmkv: MMKV
  private lateinit var smartUDP: SmartUDP

  private lateinit var executor: ExecutorService
  private lateinit var periodicExecutor: ScheduledExecutorService

  override fun onCreate() {
    super.onCreate()
    App.initMMKV(this)
    Devices.init()
    EntryBacklog.init()
    mmkv = MMKV.mmkvWithID("sync")
    smartUDP = SmartUDP().create(App.SYNC_TRANS_PORT)
    executor = Executors.newSingleThreadExecutor()
    periodicExecutor = Executors.newScheduledThreadPool(1)

    smartUDP.route("sync_direct_ack") { _, bytes ->
      val json = JSONObject(String(bytes))
      val id = json.getString("id")
      if (Devices.clientExists(id)) {
        val entryId = json.getLong("entryId")
        saveSyncAck(id, entryId)
      }
      null
    }

    smartUDP.route("ping") { _, bytes ->
      val json = JSONObject(String(bytes))
      val id = json.getString("id")
      if (Devices.clientExists(id)) {
        val addresses = json.getJSONArray("addresses")
        executor.submit {
          updateClientAddresses(id, addresses)
          clearBacklog(id)
        }
      }
      null
    }

    // The server found the IP addresses of the Client that got lost
    smartUDP.route("device_found") { _, bytes ->
      val json = JSONObject(String(bytes))
      val whom = json.getString("whom")
      val addresses = json.getJSONArray("addresses")
      updateClientAddresses(whom, addresses)
      Log.d(TAG, "Updated client addresses with server help")
      null
    }

    periodicExecutor.scheduleWithFixedDelay({
      periodicPing()
      findClients()
    }, 0, 5, TimeUnit.SECONDS)
  }

  private fun periodicPing() {
    val payload = Query.pingPayload()
    forEachClientAddress { address: String ->
      try {
        smartUDP.message(
          InetAddress.getByName(address),
          App.SYNC_REC_PORT,
          payload,
          "ping"
        )
      } catch (e: IOException) {
        println("I/O periodicPing() ${e.javaClass.simpleName} ${e.message}")
      }
    }
    // pinging the server
    SyncApp.forAllServerDestination { address: String ->
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

  /**
   * Periodically called to query new IP addresses of the clients
   * if they are not recently seen
   */

  private fun findClients() {
    val clients = Devices.getClients()
    for (key in clients.keys()) {
      val client = clients.getJSONObject(key)
      val addressUpdatedTime = client.getLong("address_updated_time")
      if (System.currentTimeMillis() - addressUpdatedTime >= SyncApp.MAX_PERMITTED_TIME_NOT_SEEN) {
        // we need to ask the server to give us Client's new IP address set
        requestUpdatedIpOfClient(client.getString("id"))
      }
    }
  }

  private fun requestUpdatedIpOfClient(clientId: String) {
    val payload = JSONObject()
      .put("whom", clientId)
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
        println("I/O server! requestUpdatedIpOfClient() ${e.javaClass.simpleName} ${e.message}")
      }
    }
  }

  private fun clearBacklog(id: String) {
    EntryBacklog.forEachBacklog(id) { entry ->
      deliverTo(id, createDeliveryEntry(entry))
    }
  }

  private fun updateClientAddresses(id: String, addresses: JSONArray) {
    val client = Devices.getClient(id)
    client.put("addresses", addresses)
    client.put("address_updated_time", System.currentTimeMillis())
    Devices.addClient(client)
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
    executor.shutdownNow()
    periodicExecutor.shutdownNow()
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
    .put("addresses", Query.addresses())

  private fun deliverToAll(entry: JSONObject) {
    val payload = entry.toString().toByteArray()
    forEachClientAddress { address: String ->
      println("Trying sync direct to $address")
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
    for (key in clients.keys()) {
      val client = clients.getJSONObject(key)
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