package `in`.sethway.engine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.baxolino.smartudp.SmartUDP
import `in`.sethway.App
import `in`.sethway.R
import `in`.sethway.engine.group.Group
import `in`.sethway.engine.group.GroupSyncHelper.performPeerListMerge
import org.json.JSONObject
import java.io.IOException
import java.lang.Exception
import java.net.InetAddress
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.collections.iterator

class SyncEngineService : Service() {

  companion object {
    val BRIDGE_IPS = arrayOf("2a01:4f9:3081:399c::4", "37.27.51.34")

    const val TAG = "SyncEngine"
    const val SYNC_ENGINE_PORT = 9966
  }

  private lateinit var smartUDP: SmartUDP

  private lateinit var executor: ExecutorService
  private lateinit var scheduledExecutor: ScheduledExecutorService

  private lateinit var notificationManager: NotificationManager

  public inner class EntryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      val entry = JSONObject(intent.getStringExtra("entry")!!)
      Log.d(TAG, "Received entry from listener service!")
      executor.submit { broadcastEntry(entry) }
    }
  }

  private val entryReceiver = EntryReceiver()

  override fun onCreate() {
    super.onCreate()

    App.initMMKV()
    UniqueEntries.init()

    smartUDP = SmartUDP().create(SYNC_ENGINE_PORT)
    executor = Executors.newSingleThreadExecutor()
    scheduledExecutor = Executors.newSingleThreadScheduledExecutor()

    notificationManager = getSystemService(NotificationManager::class.java)

    // We need to periodically attempt to reach out peers
    // saved in our version of group, and ask them for any updates.
    smartUDP.route("query_update") { address, bytes ->
      val json = JSONObject(String(bytes))

      // We gotta make peer list syncing lighter by separating into two step process
      val theirUUIDArrayVersion = json.getJSONArray("peer_uuid_list")
      val theirPeerObjectVersion = json.getJSONObject("peer_list")
      val peerInfo = json.getJSONObject("peer_info")

      updatePeerInfo(peerInfo)
      if (performPeerListMerge(theirUUIDArrayVersion, theirPeerObjectVersion)) {
        // Merge was successful!
        val deviceName = peerInfo.getString("device_name")
        Log.d(TAG, "Successfully updated peer list from $deviceName")
      } else {
        // No need to worry :) Our peer list matches!
        Log.d(TAG, "Received ping query (peers matched)")
      }


      val theirUpdateCursor = json.getLong("update_cursor")
      val ourUpdateCursor = UniqueEntries.getUpdateCursor()

      if (ourUpdateCursor > theirUpdateCursor) {
        // We are ahead... We gotta send them new entries...
        Log.d(TAG, "Helping peer with backlog... (their=$theirUpdateCursor, our=$ourUpdateCursor)")
        executor.submit { helpPeerWithBacklog(address, theirUpdateCursor) }
      } else if (theirUpdateCursor > ourUpdateCursor) {
        // Ah! We are behind!!!
        // They should be sending us new entries anytime now...
        Log.d(TAG, "We are behind (their=$theirUpdateCursor, our=$ourUpdateCursor)")
      } else {
        // Both the peers are up to date with the entries :)
        Log.d(TAG, "Both peers up to date")
      }
      null
    }

    smartUDP.route("consume_entry") { address, bytes ->
      val entry = JSONObject(String(bytes))
      if (UniqueEntries.consume(entry)) {
        // a new entry!
        consumeNotification(entry)
        Log.d(TAG, "Yay! Consumed a new entry!")
      }
      null
    }

    smartUDP.route("found_devices") { address, bytes ->
      val updatedPeerIPs = JSONObject(String(bytes))
      val peers = Group.getPeers()
      for (peerId in updatedPeerIPs.keys()) {
        if (peers.has(peerId)) {
          val peerInfo = peers.getJSONObject(peerId)
          peerInfo.put("sync_addresses", updatedPeerIPs.getJSONArray(peerId))
          peers.put(peerId, peerInfo)
        }
      }
      Group.setPeers(peers)
      Log.d(TAG, "Updated peer addresses")
      null
    }

    scheduledExecutor.scheduleWithFixedDelay({
      // We will reach out to all the peers and sync any group updates.
      // In future, we may also need to reach out to server for IP Querying.
      syncGroupUpdates()
    }, 0, 8, TimeUnit.SECONDS)

    if (Group.isGroupCreator()) {
      val filter = IntentFilter("sethway_broadcast_entry")
      filter.priority = IntentFilter.SYSTEM_HIGH_PRIORITY - 1
      ContextCompat.registerReceiver(
        this,
        entryReceiver,
        filter,
        ContextCompat.RECEIVER_NOT_EXPORTED
      )
    }
  }

  private fun updatePeerInfo(peerInfo: JSONObject) {
    val peers = Group.getPeers()
    peers.put(peerInfo.getString("uuid"), peerInfo)
    Group.setPeers(peers)
  }

  private fun syncGroupUpdates() {
    println("Tryna ping")
    val queryUpdate = JSONObject()
      .put("peer_info", Group.getMe())
      .put("peer_list", Group.getPeers())
      .put("peer_uuid_list", Group.getPeerUUIDs())
      .put("update_cursor", UniqueEntries.getUpdateCursor())
      .toString()
      .toByteArray()

    println("and here")
    try {
      forEachPeerAddresses { address ->
        smartUDP.message(address, SYNC_ENGINE_PORT, queryUpdate, "query_update")
      }
    } catch (t: Throwable) {
      t.printStackTrace()
    }
    println("ono?")

    val pingPayload = JSONObject()
      .put("id", App.ID)
      .put("addresses", InetQuery.addresses())
      .toString()
      .toByteArray()

    val lookupPayload = JSONObject()
      .put("reply_port", SYNC_ENGINE_PORT)
      .put("peer_uuid_list", Group.getPeerUUIDs())
      .toString()
      .toByteArray()

    for (bridgeAddress in BRIDGE_IPS) {
      println("Try resolve for $bridgeAddress")
      val inetAddress = InetAddress.getByName(bridgeAddress)
      trySafe {
        println("Try ping #0")
        smartUDP.message(inetAddress, SYNC_ENGINE_PORT, pingPayload, "ping")
        println("Try ping #1")
        smartUDP.message(inetAddress, SYNC_ENGINE_PORT, lookupPayload, "lookup")
        println("Try ping #2")
      }
    }

  }

  private fun helpPeerWithBacklog(address: InetAddress, theirUpdateCursor: Long) {
    UniqueEntries.forEachEntrySince(theirUpdateCursor) { entry ->
      trySafe {
        val payload = entry.toString().toByteArray()
        smartUDP.message(address, SYNC_ENGINE_PORT, payload, "consume_entry")
      }
    }
  }

  private fun broadcastEntry(entry: JSONObject) {
    if (UniqueEntries.consume(entry)) {
      val payload = entry.toString().toByteArray()
      forEachPeerAddresses { address ->
        println("broadcasting entry to $address")
        smartUDP.message(address, SYNC_ENGINE_PORT, payload, "consume_entry")
      }
    }
  }

  private fun forEachPeerAddresses(consumer: (InetAddress) -> Unit) {
    val peers = Group.getPeers()
    for (key in peers.keys()) {
      val peer = peers.getJSONObject(key)
      val uuid = peer.getString("uuid")
      if (App.ID == uuid) continue
      val addresses = peer.getJSONArray("sync_addresses")
      val addrSize = addresses.length()

      for (i in 0..<addrSize) {
        trySafe {
          consumer(InetAddress.getByName(addresses.getString(i)))
        }
      }
    }
  }

  private fun trySafe(block: () -> Unit) {
    try {
      block()
    } catch (_: IOException) {
    }
  }

  private fun consumeNotification(entry: JSONObject) {
    val notificationInfo = entry.getJSONObject("notification_info")
    val notificationContent = entry.getJSONObject("notification_content")

    val packageName = notificationInfo.getString("package_name")
    val tag = notificationInfo.getString("tag")
    val id = notificationInfo.getInt("id")

    val title = notificationContent.getString("title")
    val text = notificationContent.getString("text")

    println("$title $text")

    val notification = NotificationCompat.Builder(this, "sync_engine")
      .setContentTitle(title)
      .setContentText(text)
      .setSmallIcon(R.drawable.mark_email_unred)
      .setContentInfo(packageName)
      .build()

    notificationManager.notify(tag, id, notification)
  }

  override fun onDestroy() {
    super.onDestroy()
    smartUDP.close()
    executor.shutdownNow()
    scheduledExecutor.shutdownNow()

    try {
      unregisterReceiver(entryReceiver)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    createForeground()
    return START_STICKY
  }

  private fun createForeground() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      notificationManager.createNotificationChannel(
        NotificationChannel(
          "sync_engine",
          "Notification Sync Service",
          NotificationManager.IMPORTANCE_HIGH,
        )
      )
    }
    startForeground(
      8,
      NotificationCompat.Builder(this, "sync_engine")
        .setContentTitle("Sethway Sync")
        .setContentText("Synchronising in the background")
        .setSmallIcon(R.drawable.sync)
        .build()
    )
  }

  override fun onBind(intent: Intent?) = null
}