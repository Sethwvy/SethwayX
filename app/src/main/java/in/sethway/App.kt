package `in`.sethway

import android.app.Application
import android.content.Context
import com.baxolino.smartudp.SmartUDP
import com.google.android.material.color.DynamicColors
import com.tencent.mmkv.MMKV
import java.util.UUID

class App: Application() {

  companion object {
    const val PAIR_PORT = 8877

    const val SYNC_REC_PORT = 7788
    const val SYNC_TRANS_PORT = 7781

    lateinit var ID: String
    lateinit var mmkv: MMKV
    lateinit var SMART_UDP: SmartUDP

    fun initMMKV(context: Context) {
      MMKV.initialize(context)
      mmkv = MMKV.defaultMMKV()
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
    SMART_UDP = SmartUDP().create(PAIR_PORT)
  }
}