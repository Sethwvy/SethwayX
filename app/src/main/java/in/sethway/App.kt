package `in`.sethway

import android.app.Application
import com.google.android.material.color.DynamicColors
import com.tencent.mmkv.MMKV

class App: Application() {
  override fun onCreate() {
    super.onCreate()
    DynamicColors.applyToActivitiesIfAvailable(this)
    MMKV.initialize(this)
  }
}