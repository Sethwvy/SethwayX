package `in`.sethway.engine.group

import com.tencent.mmkv.MMKV
import `in`.sethway.App
import `in`.sethway.engine.InetQuery
import org.json.JSONArray
import org.json.JSONObject

object Group {

  private lateinit var mmkv: MMKV

  fun init() {
    mmkv = MMKV.mmkvWithID("group")
  }

  fun createGroup(groupUUID: String) {
    mmkv.apply {
      putString("group_uuid", groupUUID)
      putString("creator", App.Companion.ID)
      putLong("time_created", System.currentTimeMillis())
      putLong("time_copied", 0)
      putLong("revision", 0)
    }
    addPeer(getMe())
  }

  fun addPeer(peerInfo: JSONObject) {
    val peers = JSONArray(mmkv.getString("peer_list", "[]"))
    peers.put(peerInfo)
    mmkv.putString("peer_list", peers.toString())

    val peerUUIDs = JSONArray(mmkv.getString("peer_uuid_list", "[]"))
    peerUUIDs.put(peerInfo.getString("uuid"))
    mmkv.putString("peer_uuid_list", peerUUIDs.toString())
  }

  fun getGroup(): JSONObject {
    val groupUUID = mmkv.getString("group_uuid", null) ?: RuntimeException("Group not found")
    return JSONObject()
      .put("group_uuid", groupUUID)
      .put("creator", mmkv.decodeString("creator"))
      .put("time_created", mmkv.decodeLong("time_created"))
      .put("revision", mmkv.decodeLong("revision"))
      .put("peer_list", getPeers())
      .put("peer_uuid_list", getPeersUUIDs())
  }

  fun getMe(): JSONObject = JSONObject()
    .put("uuid", App.ID)
    .put("device_name", App.deviceName)
    .put("sync_addresses", InetQuery.addresses())

  fun copyGroup(groupInfo: JSONObject) {
    mmkv.apply {
      putString("group_uuid", groupInfo.getString("group_uuid"))
      putString("creator", groupInfo.getString("creator"))
      putLong("time_created", groupInfo.getLong("time_created"))
      putLong("time_copied", System.currentTimeMillis())
      putLong("revision", groupInfo.getLong("revision"))
    }

    mmkv.putString("peer_list", groupInfo.getJSONArray("peer_list").toString())
    mmkv.putString("peer_uuid_list", groupInfo.getJSONArray("peer_uuid_list").toString())

    addPeer(getMe())
  }

  fun getPeers(): JSONArray = JSONArray(mmkv.getString("peer_list", "[]"))
  fun getPeersUUIDs(): JSONArray = JSONArray(mmkv.getString("peer_uuid_list", "[]"))
}