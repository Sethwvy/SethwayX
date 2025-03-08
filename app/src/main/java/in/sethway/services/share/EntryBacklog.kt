package `in`.sethway.services.share

import com.tencent.mmkv.MMKV
import `in`.sethway.protocol.Devices
import org.json.JSONObject

object EntryBacklog {

  private lateinit var mmkv: MMKV
  private lateinit var backlogTable: MMKV

  fun init() {
    mmkv = MMKV.mmkvWithID("backlog")
    backlogTable = MMKV.mmkvWithID("backlog_keys")
  }

  fun add(entry: JSONObject) {
    val key = entry.getLong("time").toString()
    mmkv.encode(key, entry.toString())

    val pendingKey = "pending$key"
    backlogTable.encode(pendingKey, getClientIds().toString())
  }

  fun acknowledgeDelivery(id: String, entryId: Long) {
    val pendingKey = "pending$entryId"
    val pendingTo = JSONObject(backlogTable.decodeString(pendingKey)!!)

    pendingTo.remove(id)
    if (pendingTo.length() == 0) {
      mmkv.remove(entryId.toString())
      backlogTable.remove(pendingKey)
    } else {
      backlogTable.encode(pendingKey, pendingTo.toString())
    }
  }

  fun forEachBacklog(id: String, consumer: (entry: JSONObject) -> Unit) {
    backlogTable.allKeys()?.forEach { key ->
      val pendingTo = JSONObject(backlogTable.decodeString(key)!!)
      if (pendingTo.has(id)) {
        val entryId = key.substring("pending".length)
        val entry = JSONObject(mmkv.decodeString(entryId)!!)
        consumer(entry)
      }
    }
  }

  private fun getClientIds(): JSONObject {
    val clients = Devices.getClients()

    // an Object is more suited than Array, since it can be used as a map
    val clientIds = JSONObject()

    for (key in clients.keys()) {
      val client = clients.getJSONObject(key)
      clientIds.put(client.getString("id"), "")
    }

    return clientIds
  }

}