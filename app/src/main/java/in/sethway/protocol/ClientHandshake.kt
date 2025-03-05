package `in`.sethway.protocol

import android.content.ContentResolver
import android.os.Handler
import android.os.Looper
import `in`.sethway.App
import `in`.sethway.App.Companion.SMART_UDP
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.InetAddress
import java.util.concurrent.Executors

class ClientHandshake(
  private val contentResolver: ContentResolver,
  accepted: (deviceName: String) -> Unit
) {

  private val executor = Executors.newSingleThreadExecutor()

  init {
    SMART_UDP.route("handshake_response") { address, bytes ->
      val json = JSONObject(String(bytes))
      val deviceName = json.getString("device_name")
      Devices.addSource(json)
      Handler(Looper.getMainLooper()).post {
        accepted(deviceName)
      }
      close()
      null
    }
  }

  fun connect(addresses: JSONArray) {
    executor.submit {
      val addrSize = addresses.length()
      for (i in 0..<addrSize) {
        val address = InetAddress.getByName(addresses.getString(i))
        try {
          SMART_UDP.message(
            address,
            App.PAIR_PORT,
            Query.shareSelf(contentResolver).toByteArray(),
            "handshake"
          )
        } catch (e: IOException) {
          println("Ignored I/O ${e.javaClass.simpleName} ${e.message}")
        }
      }
    }
  }

  private fun close() {
    SMART_UDP.removeRoute("handshake_response")
  }
}