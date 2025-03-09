package `in`.sethway.services

import `in`.sethway.App
import java.util.concurrent.TimeUnit

object SyncApp {
  val MAX_PERMITTED_TIME_NOT_SEEN = TimeUnit.SECONDS.toMillis(15)

  fun forAllServerDestination(consumer: (address: String) -> Unit) {
    consumer(App.BRIDGE_IPV4)
    consumer(App.BRIDGE_IPV6)
  }
}