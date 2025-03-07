package `in`.sethway.services.sync

import com.tencent.mmkv.MMKV
import org.json.JSONArray
import org.json.JSONObject

object EntryLog {

  private lateinit var mmkv: MMKV

  fun init() {
    mmkv = MMKV.mmkvWithID("entries")
  }

  fun addEntry(entryId: Long, entry: JSONObject) {
    val key = entryId.toString()
    mmkv.encode(key, entry.toString())
    val ids = JSONArray(mmkv.decodeString("ids", "[]"))
    ids.put(key)
    mmkv.encode("ids", ids.toString())
  }

  fun existEntry(entryId: Long): Boolean = mmkv.containsKey(entryId.toString())
}