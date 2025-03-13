package `in`.sethway.engine

import android.content.ContentResolver
import android.provider.Settings
import org.json.JSONArray
import java.net.Inet6Address
import java.net.NetworkInterface

object Query {

  fun deviceName(resolver: ContentResolver): String =
    Settings.Global.getString(resolver, Settings.Global.DEVICE_NAME)

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