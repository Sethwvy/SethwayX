package `in`.sethway.services

import java.util.concurrent.TimeUnit

object SyncConfig {
  val MAX_PERMITTED_TIME_NOT_SEEN = TimeUnit.SECONDS.toMillis(15)
}