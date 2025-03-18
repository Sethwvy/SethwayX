package `in`.sethway.engine

import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.DelicateCoroutinesApi
import org.json.JSONObject

class NotificationListenerService : NotificationListenerService() {

  private companion object {
    const val TAG = "SethwayListener"
  }

  @OptIn(DelicateCoroutinesApi::class)
  override fun onNotificationPosted(sbn: StatusBarNotification) {
    val entry: JSONObject? = createNotificationEntry(sbn)
    if (entry == null) {
      Log.d(TAG, "Could not prepare entry, ignoring")
      return
    }

    sendBroadcast(
      Intent(SyncEngineService.ENTRY_RECEIVER_ACTION)
        .putExtra("entry", entry.toString())
        .setPackage(packageName)
    )
  }

  private fun createNotificationEntry(sbn: StatusBarNotification): JSONObject? {
    val extras = sbn.notification.extras

    // Don't include notifications from our app
    if (sbn.packageName == packageName) return null

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