package `in`.sethway

import android.app.Application
import android.content.Context
import com.baxolino.smartudp.SmartUDP
import com.google.android.material.color.DynamicColors
import com.tencent.mmkv.MMKV
import java.util.UUID

class App: Application() {

  companion object {
    const val BRIDGE_PORT = 8844
    const val BRIDGE_IP = "192.168.0.102"

    const val PAIR_PORT = 6681

    const val SYNC_REC_PORT = 7782
    const val SYNC_TRANS_PORT = 7783

    lateinit var ID: String
    lateinit var mmkv: MMKV
    private var _SMART_UDP: SmartUDP? = null

    val smartUdp get() = _SMART_UDP!!

    fun initMMKV(context: Context) {
      MMKV.initialize(context)
      mmkv = MMKV.defaultMMKV()
    }

    fun setupSmartUDP() {
      _SMART_UDP = SmartUDP().create(PAIR_PORT)
    }

    fun closeSmartUdp() {
      _SMART_UDP?.close()
    }
  }

  override fun onCreate() {
    super.onCreate()
    DynamicColors.applyToActivitiesIfAvailable(this)
    initMMKV(this)
    if (!mmkv.containsKey("id")) {
      ID = UUID.randomUUID().toString()
      mmkv.encode("id", ID)
    } else {
      ID = mmkv.decodeString("id")!!
    }
  }
}