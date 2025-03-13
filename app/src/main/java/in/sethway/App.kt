package `in`.sethway

import android.app.Application
import com.github.f4b6a3.uuid.UuidCreator
import com.google.android.material.color.DynamicColors
import com.tencent.mmkv.MMKV

class App: Application() {

  companion object {
    lateinit var ID: String
    lateinit var mmkv: MMKV
  }

  override fun onCreate() {
    super.onCreate()
    DynamicColors.applyToActivitiesIfAvailable(this)
    MMKV.initialize(this)
    mmkv = MMKV.defaultMMKV()
    if (!mmkv.containsKey("id")) {
      ID = UuidCreator.getTimeOrderedEpoch().toString()
      mmkv.encode("id", ID)
    } else {
      ID = mmkv.decodeString("id")!!
    }
  }
}