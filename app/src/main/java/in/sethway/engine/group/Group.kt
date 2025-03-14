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

  fun inGroup() = mmkv.containsKey("group_uuid")
  fun getGroupCreator() = mmkv.getString("creator", "")
  fun isGroupCreator() = mmkv.getString("creator", "") == App.Companion.ID

  fun isGroupEmpty(): Boolean {
    val peerUUIDs = JSONArray(mmkv.getString("peer_uuid_list", "[]"))
    return peerUUIDs.length() < 2
  }

  fun getGroupId() = mmkv.getString("group_uuid", "Group not found")!!

  fun createGroup(groupUUID: String) {
    mmkv.apply {
      putString("group_uuid", groupUUID)
      putString("creator", App.Companion.ID)
      putLong("time_created", System.currentTimeMillis())
      putLong("time_copied", 0)
    }
    addPeer(getMe())
  }

  fun addPeer(peerInfo: JSONObject) {
    val peers = JSONObject(mmkv.getString("peer_list", "{}")!!)
    peers.put(peerInfo.getString("uuid"), peerInfo)
    mmkv.putString("peer_list", peers.toString())

    val peerUUIDs = JSONArray(mmkv.getString("peer_uuid_list", "[]"))
    peerUUIDs.put(peerInfo.getString("uuid"))
    mmkv.putString("peer_uuid_list", peerUUIDs.toString())
  }

  fun getMe(): JSONObject = JSONObject()
    .put("uuid", App.Companion.ID)
    .put("device_name", App.Companion.deviceName)
    .put("sync_addresses", InetQuery.addresses())

  fun getGroup(): JSONObject {
    val groupUUID = mmkv.getString("group_uuid", null) ?: RuntimeException("Group not found")
    return JSONObject()
      .put("group_uuid", groupUUID)
      .put("creator", mmkv.decodeString("creator"))
      .put("time_created", mmkv.decodeLong("time_created"))
      .put("peer_list", getPeers())
      .put("peer_uuid_list", getPeerUUIDs())
  }

  fun copyGroup(groupInfo: JSONObject) {
    mmkv.apply {
      putString("group_uuid", groupInfo.getString("group_uuid"))
      putString("creator", groupInfo.getString("creator"))
      putLong("time_created", groupInfo.getLong("time_created"))
      putLong("time_copied", System.currentTimeMillis())
    }

    mmkv.putString("peer_list", groupInfo.getJSONObject("peer_list").toString())
    mmkv.putString("peer_uuid_list", groupInfo.getJSONArray("peer_uuid_list").toString())
    addPeer(getMe())
  }

  fun getPeers(): JSONObject = JSONObject(mmkv.getString("peer_list", "{}")!!)
  fun setPeers(peers: JSONObject) {
    mmkv.encode("peer_list", peers.toString())
  }

  fun getPeerUUIDs(): JSONArray = JSONArray(mmkv.getString("peer_uuid_list", "[]"))
  fun setPeerUUIDs(peerUUIDs: JSONArray) {
    mmkv.encode("peer_uuid_list", peerUUIDs.toString())
  }
}