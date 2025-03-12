package `in`.sethway.services.sync

import com.tencent.mmkv.MMKV
import org.json.JSONObject

object EntryLog {

  private val mmkvMapping = HashMap<String, MMKV>()

  private fun getMMKV(sourceId: String): MMKV {
    mmkvMapping[sourceId]?.let { return it }
    return MMKV.mmkvWithID(sourceId).also { mmkvMapping[sourceId] = it }
  }

  fun addEntry(sourceId: String, entryId: Long, entry: JSONObject) {
    getMMKV(sourceId).encode(entryId.toString(), entry.toString())
  }

  fun existEntry(sourceId: String, entryId: Long): Boolean = getMMKV(sourceId).containsKey(entryId.toString())
}