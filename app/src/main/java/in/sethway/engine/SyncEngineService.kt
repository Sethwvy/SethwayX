package `in`.sethway.engine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.Process
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import `in`.sethway.R
import inx.sethway.IGroupCallback
import inx.sethway.IIPCEngine
import io.paperdb.Paper
import org.json.JSONArray
import org.json.JSONObject

class SyncEngineService : Service() {

  companion object {
    const val ENTRY_RECEIVER_ACTION = "broadcast_new_entry"
  }

  public inner class EntryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      val entry = JSONObject(intent.getStringExtra("entry")!!)
      engine.commitNewEntry(entry)
    }
  }

  private val entryReceiver = EntryReceiver()

  private var groupCallback: IGroupCallback? = null

  private val binder = object : IIPCEngine.Stub() {
    override fun getPid(): Int = Process.myPid()

    override fun createGroup(groupId: String) {
      engine.createNewGroup(groupId)
    }

    override fun receiveInvitee() {
      engine.receiveInvitee()
    }

    override fun getInvite(): String = engine.getGroupInvitation().toString()

    override fun acceptInvite(invitation: String) {
      engine.acceptGroupInvite(JSONArray(invitation))
    }

    override fun registerGroupCallback(callback: IGroupCallback) {
      groupCallback = callback
    }

    override fun unregisterGroupCallback() {
      groupCallback = null
    }
  }

  override fun onBind(intent: Intent?): IBinder = binder

  private lateinit var notificationManager: NotificationManager
  private lateinit var engine: Engine

  override fun onCreate() {
    super.onCreate()
    Paper.init(this)
    notificationManager = getSystemService(NotificationManager::class.java)
    engine = Engine(this) { groupCallback }
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
    engine.close()
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

}