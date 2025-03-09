package `in`.sethway.protocol

import com.tencent.mmkv.MMKV
import org.json.JSONObject

object Devices {

  private lateinit var mmkv: MMKV

  fun init() {
    mmkv = MMKV.mmkvWithID("devices")
  }

  fun addClient(entry: JSONObject) {
    addEntry("clients", entry.getString("id"), entry)
  }

  fun addSource(entry: JSONObject) {
    addEntry("sources", entry.getString("id"), entry)
  }

  private fun addEntry(name: String, key: String, entry: Any) {
    val entries = getObject(name)
    entries.put(key, entry)
    mmkv.encode(name, entries.toString())
  }

  fun removeClient(key: String) {
    removeEntry("clients", key)
  }

  fun removeSource(key: String) {
    removeEntry("sources", key)
  }

  private fun removeEntry(name: String, key: String) {
    val entries = getObject(name)
    entries.remove(key)
    mmkv.encode(name, entries.toString())
  }

  fun getClient(id: String): JSONObject = getObject("clients").getJSONObject(id)
  fun getSource(id: String): JSONObject = getObject("sources").getJSONObject(id)

  fun getClients() = getObject("clients")
  fun getSources() = getObject("sources")

  private fun getObject(name: String): JSONObject =
    JSONObject(mmkv.decodeString(name, "{}")!!)
}