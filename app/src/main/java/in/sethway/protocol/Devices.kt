package `in`.sethway.protocol

import `in`.sethway.App
import org.json.JSONArray
import org.json.JSONObject

object Devices {

  fun addClient(entry: JSONObject) {
    addToArray("clients", entry)
  }

  fun addSource(entry: JSONObject) {
    addToArray("sources", entry)
  }

  private fun addToArray(key: String, entry: JSONObject) {
    val entries = getArray(key)
    entry.put("time_added", System.currentTimeMillis())
    entries.put(entry)
    App.mmkv.encode(key, entries.toString())
  }

  fun getClients() = getArray("clients")
  fun getSources() = getArray("sources")

  private fun getArray(key: String): JSONArray = JSONArray(App.mmkv.getString(key, "[]"))
}