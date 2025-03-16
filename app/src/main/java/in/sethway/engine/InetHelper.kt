package `in`.sethway.engine

import `in`.sethway.engine.group.Group
import org.json.JSONArray

class InetHelper(private val group: Group) {

  private var previousIpAddresses: JSONArray? = null

  fun checkForIpChanges() {
    val newIpAddresses = InetQuery.addresses()
    if (previousIpAddresses == null) {
      previousIpAddresses = JSONArray(group.getSelfInfo())
    }
    if (newIpAddresses != previousIpAddresses) {
      // Ohoo! IP has been changed...
      group.updateSelfInfo(newIpAddresses.toString())
    }
    previousIpAddresses = newIpAddresses
  }
}