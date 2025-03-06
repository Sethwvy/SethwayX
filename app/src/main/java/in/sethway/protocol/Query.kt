package `in`.sethway.protocol

import android.content.ContentResolver
import android.provider.Settings
import `in`.sethway.App
import org.json.JSONArray
import org.json.JSONObject
import java.net.Inet6Address
import java.net.NetworkInterface

object Query {

  fun shareSelf(resolver: ContentResolver) = JSONObject()
    .put("id", App.ID)
    .put("device_name", Settings.Global.getString(resolver, Settings.Global.DEVICE_NAME))
    .put("addresses", addresses())
    .toString()

  private fun addresses(): JSONArray {
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