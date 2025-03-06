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
    val entries = getObject(name)
    entries.put(key, entry)
    App.mmkv.encode(name, entries.toString())
  }

  fun getClient(id: String): JSONObject = getObject("clients").getJSONObject(id)
  fun getSource(id: String): JSONObject = getObject("sources").getJSONObject(id)


  fun getClients() = getEntries("clients")
  fun getSources() = getEntries("sources")

  private fun getEntries(name: String): JSONArray {
    val entries = getObject(name)
    return entries.toJSONArray(entries.names()) ?: JSONArray()
  }

  private fun getObject(name: String): JSONObject =
    JSONObject(App.mmkv.decodeString(name, "{}")!!)
}