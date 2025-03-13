package `in`.sethway.engine

import android.app.Service
import android.content.Intent

class SyncEngineService: Service() {

  override fun onCreate() {
    super.onCreate()
  }

  override fun onDestroy() {
    super.onDestroy()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    return super.onStartCommand(intent, flags, startId)
  }

  override fun onBind(intent: Intent?) = null
}