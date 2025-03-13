package `in`.sethway.engine.group

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.baxolino.smartudp.SmartUDP
import `in`.sethway.engine.InetQuery
import org.json.JSONObject
import java.io.IOException
import java.net.InetAddress
import java.util.concurrent.Executors

class SimpleGroupSync(groupJoined: (helperPeer: JSONObject) -> Unit) {

  companion object {
    private const val TAG = "SimpleGroupSync"

    const val SIMPLE_SYNC_PORT = 8877

    const val SYNC_BACK_ROUTE = "sync_back"
    const val NOTIFY_SUCCESS_ROUTE = "notify_success"
  }

  private val smartUDP = SmartUDP().create(SIMPLE_SYNC_PORT)
  private val executor = Executors.newSingleThreadExecutor()

  init {
    // The peer has scanned the QR Code :)
    // Now we have to add them to our version of group
    smartUDP.route(SYNC_BACK_ROUTE) { address, bytes ->
      val json = JSONObject(String(bytes))
      Group.addPeer(json.getJSONObject("me"))

      val syncPort = json.getInt("sync_port")
      val notifySuccessRoute = json.getString("notify_success_route")

      val successReply = JSONObject()
        .put("success", true)
        .put("me", Group.getMe())
        .toString()
        .toByteArray()

      executor.submit {
        trySafe {
          smartUDP.message(
            address,
            syncPort,
            successReply,
            notifySuccessRoute
          )
        }
      }
      null
    }

    // We scanned the QR Code, synced the group, notified the peer, and we got a success
    // response! Now we have successfully joined the group
    smartUDP.route(NOTIFY_SUCCESS_ROUTE) { address, bytes ->
      val json = JSONObject(String(bytes))
      val success = json.getBoolean("me")
      if (!success) {
        Log.d(TAG, "Did not get a good sync back response")
        return@route null
      }
      val they = json.getJSONObject("me")
      Handler(Looper.getMainLooper()).post {
        groupJoined(they)
      }
      null
    }
  }

  fun getInviteInfo(): JSONObject = JSONObject()
    .put("group_info", Group.getGroup())
    .put("sync_addresses", InetQuery.addresses())
    .put("sync_port", SIMPLE_SYNC_PORT)
    .put("sync_route_name", SYNC_BACK_ROUTE)

  fun connect(inviteInfo: JSONObject) {
    Group.copyGroup(inviteInfo.getJSONObject("group_info"))

    val syncAddresses = inviteInfo.getJSONArray("sync_addresses")
    val syncPort = inviteInfo.getInt("sync_port")
    val syncRouteName = inviteInfo.getString("sync_route_name")

    val syncBackRequest = JSONObject()
      .put("me", Group.getMe())
      .put("sync_port", SIMPLE_SYNC_PORT)
      .put("notify_success_route", NOTIFY_SUCCESS_ROUTE)
      .toString()
      .toByteArray()

    executor.submit {
      val addrLen = syncAddresses.length()
      for (i in 0..<addrLen) {
        val address = InetAddress.getByName(syncAddresses.getString(i))
        trySafe {
          smartUDP.message(
            address,
            syncPort,
            syncBackRequest,
            syncRouteName
          )
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

  fun close() {
    smartUDP.close()
    executor.shutdownNow()
  }
}