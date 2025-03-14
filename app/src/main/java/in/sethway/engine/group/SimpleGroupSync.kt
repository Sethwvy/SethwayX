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

class SimpleGroupSync(
  weJoined: (helperPeer: JSONObject) -> Unit,
  someoneJoined: (peer: JSONObject) -> Unit
) {

  companion object {
    const val SIMPLE_SYNC_PORT = 8877

    const val SYNC_BACK_ROUTE = "sync_back"
    const val NOTIFY_SUCCESS_ROUTE = "notify_success"
  }

  private val smartUDP = SmartUDP().create(SIMPLE_SYNC_PORT)
  private val executor = Executors.newSingleThreadExecutor()

  init {
    // The peer has scanned the QR Code :)
    // We add them to our list, and share the entire group
    smartUDP.route(SYNC_BACK_ROUTE) { address, bytes ->
      val json = JSONObject(String(bytes))
      val they = json.getJSONObject("me")
      Group.addPeer(they)

      val syncPort = json.getInt("sync_port")
      val notifySuccessRoute = json.getString("notify_success_route")

      val successReply = JSONObject()
        .put("group_info", Group.getGroup())
        .put("me", Group.getMe())
        .toString()
        .toByteArray()

      Handler(Looper.getMainLooper()).post {
        someoneJoined(they)
      }

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


    // We scanned the QR Code, sent a sync response, and got a good
    // response containing full group info
    smartUDP.route(NOTIFY_SUCCESS_ROUTE) { address, bytes ->
      val json = JSONObject(String(bytes))
      Group.copyGroup(json.getJSONObject("group_info"))

      val they = json.getJSONObject("me")

      Handler(Looper.getMainLooper()).post {
        weJoined(they)
      }
      null
    }
  }

  // Content of QR Code
  fun getInviteInfo(): JSONObject = JSONObject()
    .put("sync_addresses", InetQuery.addresses())
    .put("sync_port", SIMPLE_SYNC_PORT)
    .put("sync_route_name", SYNC_BACK_ROUTE)

  // The QR Code has been scanned
  fun connect(inviteInfo: JSONObject) {
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