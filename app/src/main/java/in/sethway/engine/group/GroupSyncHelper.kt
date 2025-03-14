package `in`.sethway.engine.group

import org.json.JSONArray
import org.json.JSONObject

object GroupSyncHelper {

  fun performPeerListMerge(theirUUIDArray: JSONArray, theirPeerObjects: JSONObject): Boolean {
    val uuidDifferenceList = getUUIDListDifference(theirUUIDArray)
    val ourPeerUUIDArray = Group.getPeerUUIDs()

    // first we update our peer UUID list
    for (newUUID in uuidDifferenceList) {
      ourPeerUUIDArray.put(newUUID)
    }
    Group.setPeerUUIDs(ourPeerUUIDArray)

    // then we update our actual peer list
    val ourPeerObjects = Group.getPeers()
    for (newUUID in uuidDifferenceList) {
      val peerInfo = theirPeerObjects.getJSONObject(newUUID)
      ourPeerObjects.put(newUUID, peerInfo)
    }
    Group.setPeers(ourPeerObjects)

    return uuidDifferenceList.isNotEmpty()
  }

  private fun getUUIDListDifference(theirUUIDArray: JSONArray): ArrayList<String> {
    val ourUUIDList = Group.getPeerUUIDs().toList<String>()
    val theirUUIDList = theirUUIDArray.toList<String>()

    val differenceList = ArrayList<String>()
    for (element in theirUUIDList) {
      if (element !in ourUUIDList) {
        differenceList += element
      }
    }
    return differenceList
  }

  private fun <E> JSONArray.toList(): ArrayList<E> {
    val len = length()
    val list = ArrayList<E>(len)
    for (i in 0..len) {
      @Suppress("UNCHECKED_CAST")
      list.add(get(i) as E)
    }
    return list
  }

}