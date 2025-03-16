package `in`.sethway.engine

import com.baxolino.smartudp.SmartUDP
import `in`.sethway.engine.commit.CommitBook
import `in`.sethway.engine.commit.CommitHelper
import `in`.sethway.engine.commit.CommitHelper.commit
import `in`.sethway.engine.group.Group
import `in`.sethway.engine.structs.TimeoutCache
import io.paperdb.Paper
import org.json.JSONArray
import org.json.JSONObject
import java.net.InetAddress
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class Engine {

  companion object {
    private const val ENGINE_PORT = 8899
  }

  private val book = Paper.book()
  private val entries = Paper.book("entries")

  private val myId: String = book.read("id")!!

  private val group: Group = Group(myId)

  private val displayName: String = book.read("display_name")!!

  private val inetHelper = InetHelper(group)

  private val executor = Executors.newSingleThreadScheduledExecutor()
  private val scheduledExecutor = Executors.newSingleThreadScheduledExecutor()

  private val recentPacketIds = TimeoutCache<String, String?>(12 * 1000)
  private val smartUDP = SmartUDP().create(ENGINE_PORT)

  init {
    CommitBook.create()
    CommitHelper.initBooks()

    group.addSelf(InetQuery.addresses().toString(), getMyCommonInfo().toString())
    scheduledExecutor.scheduleWithFixedDelay({
      inetHelper.checkForIpChanges()
      syncWithGroup()
    }, 0, 2, TimeUnit.SECONDS)

    smartUDP.route("sync_commit_book") { address, bytes ->
      val json = JSONObject(String(bytes))
      val packetId = json.getString("packet_id")

      // we have to avoid burst of multiple packets! (when we have multiple IPs)
      if (recentPacketIds.addNewEntry(packetId, address.hostAddress)) return@route null

      val theirCommonInfo = json.getJSONObject("common_info")
      val displayName = theirCommonInfo.getString("display_name")

      println("Got sync_commit_book from $displayName")

      val theirCommitBook = json.getJSONObject("commit_book")
      val ourOutdatedCommitKeys =
        CommitBook.compareCommits(theirCommitBook).toString().toByteArray()

      trySafe {
        smartUDP.message(address, ENGINE_PORT, ourOutdatedCommitKeys, "request_provide_commits")
      }
      null
    }

    smartUDP.route("request_provide_commits") { address, bytes ->
      val theirOutdatedCommitKeys = JSONArray(String(bytes))
      val updatedCommitsContent =
        CommitBook.getCommitContent(theirOutdatedCommitKeys).toString().toByteArray()

      trySafe {
        smartUDP.message(address, ENGINE_PORT, updatedCommitsContent, "response_provide_commits")
      }
      null
    }

    smartUDP.route("response_provide_commits") { address, bytes ->
      val updatedCommitsContent = JSONObject(String(bytes))
      CommitBook.updateCommits(updatedCommitsContent)

      println("Successfully updated commits content!")
      null
    }
  }

  private fun syncWithGroup() {
    val syncPacket = JSONObject()
      .put("packet_id", UUID.randomUUID())
      .put("common_info", getMyCommonInfo())
      .put("commit_book", CommitBook.getCommitBook())
      .toString()
      .toByteArray()
    forEachPeerAddress { address ->
      smartUDP.message(address, ENGINE_PORT, syncPacket, "sync_commit_book")
    }
  }

  private fun forEachPeerAddress(consumer: (InetAddress) -> Unit) {
    val peerInfo = group.getEachPeerInfo()
    for (peerId in peerInfo.keys()) {
      if (peerId == myId) continue
      val addresses = peerInfo.getJSONArray(peerId)
      val addrLen = addresses.length()
      for (i in 0..<addrLen) {
        val address = addresses.getString(i)
        trySafe { consumer(InetAddress.getByName(address)) }
      }
    }
  }

  private fun trySafe(block: () -> Unit) {
    try {
      block()
    } catch (_: Throwable) {

    }
  }

  fun createNewGroup(groupId: String) {
    group.createGroup(groupId, myId)
  }

  fun joinGroup(groupId: String, creator: String) {
    group.createGroup(groupId, creator)
  }

  fun commitNewEntry(entry: JSONObject) {
    // That's it! This commit should auto propagate with sync packets!
    entries.commit("${System.currentTimeMillis()}", entry.toString())
  }

  private fun getMyCommonInfo(): JSONObject = JSONObject()
    .put("id", myId)
    .put("display_name", displayName)

  fun close() {
    smartUDP.close()
    executor.shutdownNow()
    scheduledExecutor.shutdownNow()
  }
}