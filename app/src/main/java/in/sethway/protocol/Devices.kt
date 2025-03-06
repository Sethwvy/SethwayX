package `in`.sethway.protocol

import `in`.sethway.App
import org.json.JSONArray
import org.json.JSONObject

object Devices {

  fun addClient(entry: JSONObject) {
    addEntry("clients", entry.getString("id"), entry)
  }

  fun addSource(entry: JSONObject) {
    addEntry("sources", entry.getString("id"), entry)
  }

  private fun addEntry(name: String, key: String, entry: JSONObject) {
    val entries = JSONObject(App.mmkv.decodeString(name, "{}")!!)
    entries.put(key, entry)
    App.mmkv.encode(name, entries.toString())
  }


  fun getClients() = getEntries("clients")
  fun getSources() = getEntries("sources")

  private fun getEntries(name: String): JSONArray {
    val entries = JSONObject(App.mmkv.decodeString(name, "{}")!!)
    return entries.toJSONArray(entries.names()) ?: JSONArray()
  }
}