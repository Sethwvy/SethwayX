package `in`.sethway

import android.app.Application
import com.baxolino.smartudp.SmartUDP
import com.google.android.material.color.DynamicColors
import com.tencent.mmkv.MMKV
import java.util.UUID

class App: Application() {

  companion object {
    const val PAIR_PORT = 8877

    lateinit var ID: String
    lateinit var mmkv: MMKV
    lateinit var SMART_UDP: SmartUDP
  }

  override fun onCreate() {
    super.onCreate()
    DynamicColors.applyToActivitiesIfAvailable(this)
    MMKV.initialize(this)
    mmkv = MMKV.defaultMMKV()
    if (!mmkv.containsKey("id")) {
      ID = UUID.randomUUID().toString()
      mmkv.encode("id", ID)
    } else {
      ID = mmkv.decodeString("id")!!
    }
    SMART_UDP = SmartUDP().create(PAIR_PORT)
  }
}