package `in`.sethway.protocol

import android.content.ContentResolver
import android.os.Handler
import android.os.Looper
import `in`.sethway.App
import `in`.sethway.App.Companion.smartUdp
import org.json.JSONObject

class ServerHandshake(
  resolver: ContentResolver,
  accepted: (deviceName: String) -> Unit
) {

  init {
    smartUdp.route("handshake") { address, bytes ->
      val json = JSONObject(String(bytes))
      val deviceName = json.getString("device_name")

      Devices.addClient(json)
      Handler(Looper.getMainLooper()).post {
        accepted(deviceName)
      }
      close()
      smartUdp.message(
        address,
        App.PAIR_PORT,
        Query.shareSelf(resolver).toByteArray(),
        "handshake_response"
      )
      null
    }
  }

  fun close() {
    smartUdp.removeRoute("handshake")
  }
}