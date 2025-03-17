package `in`.sethway.engine.inet

import org.json.JSONArray
import java.net.Inet6Address
import java.net.NetworkInterface

object InetQuery {
  fun addresses(): JSONArray {
    val addresses = ArrayList<Inet6Address>()
    NetworkInterface.getNetworkInterfaces().iterator().forEach { i ->
      i.inetAddresses.iterator().forEach { a ->
        if (!a.isLinkLocalAddress
          && !a.isLoopbackAddress
          && !a.isSiteLocalAddress
          && !a.isAnyLocalAddress && a is Inet6Address
        ) addresses.add(a)
      }
    }
    return JSONArray(addresses.map { it.hostAddress })
  }
}