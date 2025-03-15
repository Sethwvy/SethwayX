package `in`.sethway.engine

import android.util.Log
import com.tencent.mmkv.MMKV
import org.json.JSONObject

object UniqueEntries {

  private const val TAG = "UniqueEntries"

  private lateinit var entryInfo: MMKV
  private lateinit var entries: MMKV

  fun init() {
    entryInfo = MMKV.mmkvWithID("entry_info")
    entries = MMKV.mmkvWithID("unique_entries")
  }

  fun consume(entry: JSONObject): Boolean {
    val signature = entry.getString("signature")
    if (entries.containsKey(signature)) return false
    entries.encode(signature, entry.toString())

    val currentCursor = entryInfo.getLong("update_cursor", 0)
    entryInfo.encode("cursor_$currentCursor", signature)
    entryInfo.encode("update_cursor", currentCursor + 1)

    Log.d(TAG, "Consumed entry $entry")
    return true
  }

  fun forEachEntrySince(fromCursor: Long, consumer: (JSONObject) -> Unit) {
    val tillCursor = getUpdateCursor()
    for (cursor in fromCursor..<tillCursor) {
      val entryKey = entryInfo.decodeString("cursor_$cursor")
      val entry = JSONObject(entries.decodeString(entryKey)!!)
      consumer(entry)
    }
  }

  fun getUpdateCursor(): Long = entryInfo.getLong("update_cursor", 0)
}