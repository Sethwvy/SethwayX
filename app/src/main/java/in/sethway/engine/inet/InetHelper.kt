package `in`.sethway.engine.inet

import `in`.sethway.engine.group.Group
import org.json.JSONArray

class InetHelper(private val group: Group) {

  private var previousIpAddresses: JSONArray? = null

  fun checkForIpChanges(): JSONArray? {
    val newIpAddresses = InetQuery.addresses()
    if (previousIpAddresses == null) {
      previousIpAddresses = JSONArray(group.getSelfInfo())
    }
    if (newIpAddresses != previousIpAddresses) {
      // Ohoo! IP has been changed...
      group.updateSelfInfo(newIpAddresses.toString())
      previousIpAddresses = newIpAddresses
      return newIpAddresses
    }
    return null
  }
}