package `in`.sethway.engine

import android.app.Service
import android.content.Intent
import android.util.Log
import com.baxolino.smartudp.SmartUDP
import `in`.sethway.App
import `in`.sethway.engine.group.Group
import `in`.sethway.engine.group.GroupSyncHelper.performPeerListMerge
import org.json.JSONObject
import java.io.IOException
import java.net.InetAddress
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class SyncEngineService : Service() {

  private companion object {
    const val TAG = "SyncEngine"

    const val SYNC_ENGINE_PORT = 9966
  }

  private lateinit var smartUDP: SmartUDP

  private lateinit var executor: ExecutorService
  private lateinit var scheduledExecutor: ScheduledExecutorService

  override fun onCreate() {
    super.onCreate()
    App.initMMKV()

    smartUDP = SmartUDP().create(SYNC_ENGINE_PORT)
    executor = Executors.newSingleThreadExecutor()
    scheduledExecutor = Executors.newSingleThreadScheduledExecutor()

    // We need to periodically attempt to reach out peers
    // saved in our version of group, and ask them for any updates.
    smartUDP.route("query_update") { address, bytes ->
      val json = JSONObject(String(bytes))

      val theirUUIDArrayVersion = json.getJSONArray("peer_uuid_list")
      val theirPeerObjectVersion = json.getJSONObject("peer_list")

      if (performPeerListMerge(theirUUIDArrayVersion, theirPeerObjectVersion)) {
        // Merge was successful!
        val peerInfo = json.getJSONObject("peer_info")
        val deviceName = peerInfo.getString("device_name")
        Log.d(TAG, "Successfully updated peer list from $deviceName")
      } else {
        // No need to worry :) Our peer list matches!
        Log.d(TAG, "Peer list matches already!")
      }
      null
    }

    scheduledExecutor.scheduleWithFixedDelay({
      // We will reach out to all the peers and sync any group updates.
      // In future, we may also need to reach out to server for IP Querying.
      syncGroupUpdates()
    }, 0, 8, TimeUnit.SECONDS)
  }

  private fun syncGroupUpdates() {
    val queryUpdate = JSONObject()
      .put("peer_info", Group.getMe())
      .put("peer_list", Group.getPeers())
      .put("peer_uuid_list", Group.getPeerUUIDs())
      .toString()
      .toByteArray()

    forEachPeerAddresses { address ->
      smartUDP.message(address, SYNC_ENGINE_PORT, queryUpdate)
    }
  }

  private fun forEachPeerAddresses(consumer: (InetAddress) -> Unit) {
    val peers = Group.getPeers()
    for (key in peers.keys()) {
      val peer = peers.getJSONObject(key)
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

  override fun onDestroy() {
    super.onDestroy()
    smartUDP.close()
    executor.shutdownNow()
    scheduledExecutor.shutdownNow()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    return START_STICKY
  }

  override fun onBind(intent: Intent?) = null
}