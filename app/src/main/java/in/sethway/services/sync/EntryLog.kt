package `in`.sethway.services.sync

import com.tencent.mmkv.MMKV
import org.json.JSONObject

object EntryLog {

  private val mmkvMapping = HashMap<String, MMKV>()

  private fun getMMKV(id: String): MMKV {
    mmkvMapping[id]?.let { return it }
    return MMKV.mmkvWithID(id).also { mmkvMapping[id] = it }
  }

  fun addEntry(sourceId: String, entryId: Long, entry: JSONObject) {
    getMMKV("entries$sourceId").encode(entryId.toString(), entry.toString())
  }

  fun existEntry(sourceId: String, entryId: Long): Boolean =
    getMMKV("entries$sourceId").containsKey(entryId.toString())
}