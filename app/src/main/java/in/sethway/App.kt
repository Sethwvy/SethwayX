package `in`.sethway

import android.app.Application
import android.provider.Settings
import com.github.f4b6a3.uuid.UuidCreator
import com.google.android.material.color.DynamicColors
import com.tencent.mmkv.MMKV
import `in`.sethway.engine.group.Group

class App : Application() {

  companion object {
    lateinit var ID: String
    lateinit var mmkv: MMKV

    lateinit var deviceName: String

    fun setNewDeviceName(name: String) {
      this.deviceName = name
      mmkv.encode("device_name", deviceName)
    }
  }

  override fun onCreate() {
    super.onCreate()
    DynamicColors.applyToActivitiesIfAvailable(this)
    MMKV.initialize(this)
    Group.init()

    mmkv = MMKV.defaultMMKV()
    if (!mmkv.containsKey("id")) {
      ID = UuidCreator.getTimeOrderedEpoch().toString()
      mmkv.encode("id", ID)
    } else {
      ID = mmkv.decodeString("id")!!
    }
    deviceName = mmkv.getString(
      "device_name",
      Settings.Global.getString(contentResolver, Settings.Global.DEVICE_NAME)
    )!!
  }
}