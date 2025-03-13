package `in`.sethway.engine

import android.os.Handler
import android.os.Looper
import com.baxolino.smartudp.SmartUDP
import com.tencent.mmkv.MMKV
import `in`.sethway.App
import org.json.JSONObject
import java.io.IOException
import java.net.InetAddress
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object GroupManagement {

  private const val MANAGEMENT_PORT = 7799
  private val BRIDGES = arrayOf("2a01:4f9:3081:399c::4", "37.27.51.34")

  private lateinit var mmkv: MMKV

  private lateinit var smartUDP: SmartUDP
  private lateinit var executor: ExecutorService

  fun open(onGroupCreated: (success: Boolean) -> Unit = { }) {
    mmkv = MMKV.mmkvWithID("group")

    smartUDP = SmartUDP().create(MANAGEMENT_PORT)
    executor = Executors.newSingleThreadExecutor()

    smartUDP.route("group_created") { address, bytes ->
      val json = JSONObject(String(bytes))
      val success = json.getBoolean("success")
      Handler(Looper.getMainLooper()).post {
        onGroupCreated(success)
      }
      null
    }
  }

  fun createGroup(groupUuid: String) {
    val request = getGroupInfo().toByteArray()

    mmkv.encode("group_uuid", groupUuid)

    executor.submit {
      for (bridge in BRIDGES) {
        trySafe {
          smartUDP.message(
            InetAddress.getByName(bridge),
            MANAGEMENT_PORT,
            request,
            "create_group"
          )
        }
      }
    }
  }

  fun getGroupInfo(): String = JSONObject()
    .put("id", App.ID)
    .put("group_uuid", mmkv.decodeString("group_uuid"))
    .put("addresses", Query.addresses())
    .put("reply_port", MANAGEMENT_PORT)
    .toString()

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