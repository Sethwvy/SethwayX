package `in`.sethway.engine

import android.app.Service
import android.content.Intent
import android.util.Log
import com.baxolino.smartudp.SmartUDP
import `in`.sethway.App
import `in`.sethway.engine.group.GroupSyncHelper.performPeerListMerge
import org.json.JSONObject
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
      val theirPeerArrayVersion = json.getJSONObject("peer_list")

      if (performPeerListMerge(theirUUIDArrayVersion, theirPeerArrayVersion)) {
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