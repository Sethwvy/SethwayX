package `in`.sethway.engine

import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.tencent.mmkv.MMKV
import `in`.sethway.App
import kotlinx.coroutines.DelicateCoroutinesApi
import org.json.JSONArray
import org.json.JSONObject

class NotificationListenerService : NotificationListenerService() {

  private companion object {
    const val TAG = "SethwayListener"
  }

  //private lateinit var mmkv: MMKV

  override fun onCreate() {
    super.onCreate()
    App.initMMKV()
    //mmkv = MMKV.mmkvWithID("broadcast_backlog")
  }

  @OptIn(DelicateCoroutinesApi::class)
  override fun onNotificationPosted(sbn: StatusBarNotification) {
    val entry = createNotificationEntry(sbn)
    if (entry == null) {
      Log.d(TAG, "Could not prepare entry, ignoring")
      return
    }

    //val backlog = JSONArray(mmkv.decodeString("entries", "[]"))
    //backlog.put(entry)
    //mmkv.encode("entries", backlog.toString())
    //mmkv.encode("last_updated", System.currentTimeMillis())

    sendBroadcast(
      Intent("sethway_broadcast_entry")
        .putExtra("entry", entry.toString())
        .setPackage(packageName)
    )
  }

  private fun createNotificationEntry(sbn: StatusBarNotification): JSONObject? {
    val extras = sbn.notification.extras

    val signature = "${System.currentTimeMillis()} ${sbn.packageName} ${sbn.tag} ${sbn.id}"
    Log.d(TAG, "Signature: $signature")

    val title = (extras.get("android.title") ?: return null).toString()
    val text = (extras.get("android.text") ?: return null).toString()

    val notificationInfo = JSONObject()
      .put("package_name", sbn.packageName)
      .put("tag", sbn.tag ?: "_null_")
      .put("id", sbn.id)

    val notificationContent = JSONObject()
      .put("title", title)
      .put("text", text)

    return JSONObject()
      .put("signature", signature)
      .put("notification_info", notificationInfo)
      .put("notification_content", notificationContent)
  }


}