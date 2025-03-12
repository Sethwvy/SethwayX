package `in`.sethway.services

import `in`.sethway.App
import java.util.concurrent.TimeUnit

object SyncApp {
  val MAX_PERMITTED_SOURCE_TIME_NOT_SEEN = TimeUnit.SECONDS.toMillis(15)

  fun forAllServerDestination(consumer: (address: String) -> Unit) {
    App.BRIDGE_SECONDARY_ADDR.let { if (it.isNotEmpty()) consumer(App.BRIDGE_SECONDARY_ADDR) }
    consumer(App.BRIDGE_PRIMARY_ADDR)
  }
}