package `in`.sethway.engine

import `in`.sethway.engine.group.Group
import io.paperdb.Paper
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class Engine {

  private val book = Paper.book()
  private val myId: String = book.read("id")!!

  private val group: Group = Group(myId)

  private val displayName: String = book.read("display_name")!!

  private val inetHelper = InetHelper(group)
  private val scheduledExecutor = Executors.newSingleThreadScheduledExecutor()

  init {
    group.addSelf(InetQuery.addresses().toString(), getMe().toString())
    scheduledExecutor.scheduleWithFixedDelay({
      inetHelper.checkForIpChanges()
    }, 0, 2, TimeUnit.SECONDS)
  }

  fun createNewGroup(groupId: String) {
    group.createGroup(groupId)
  }

  fun getMe(): JSONObject = JSONObject()
    .put("id", myId)
    .put("display_name", displayName)

  fun close() {
    scheduledExecutor.shutdownNow()
  }
}