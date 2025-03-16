package `in`.sethway.engine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import `in`.sethway.R
import io.paperdb.Paper

class SyncEngineService : Service() {

  companion object {
    const val ENTRY_RECEIVER_ACTION = "broadcast_new_entry"
  }

  public inner class EntryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      // TODO: we have to commit the entry
    }
  }

  private val entryReceiver = EntryReceiver()

  private lateinit var notificationManager: NotificationManager
  private lateinit var engine: Engine

  override fun onCreate() {
    super.onCreate()
    Paper.init(this)
    notificationManager = getSystemService(NotificationManager::class.java)
    engine = Engine()
    registerReceiver()
  }

  private fun registerReceiver() {
    val filter = IntentFilter(ENTRY_RECEIVER_ACTION).apply {
      priority = IntentFilter.SYSTEM_HIGH_PRIORITY - 1
    }
    ContextCompat.registerReceiver(this, entryReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
  }

  override fun onDestroy() {
    super.onDestroy()
    try {
      unregisterReceiver(entryReceiver)
    } catch (_: Exception) {

    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    createForeground()
    return START_STICKY
  }

  private fun createForeground() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      notificationManager.createNotificationChannel(
        NotificationChannel(
          "sync_engine",
          "Sethway Sync Engine",
          NotificationManager.IMPORTANCE_HIGH
        )
      )
      startForeground(
        8, NotificationCompat.Builder(this, "sync_engine")
          .setContentTitle("Sethway Sync")
          .setContentText("I'm syncing in the background!")
          .setSmallIcon(R.drawable.sync)
          .build()
      )
    }
  }

  override fun onBind(intent: Intent?) = null
}