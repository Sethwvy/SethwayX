package `in`.sethway.engine

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import com.baxolino.smartudp.SmartUDP
import `in`.sethway.engine.commit.CommitBook
import `in`.sethway.engine.commit.CommitHelper
import `in`.sethway.engine.commit.CommitHelper.commit
import `in`.sethway.engine.group.Group
import `in`.sethway.engine.inet.InetHelper
import `in`.sethway.engine.inet.InetQuery
import `in`.sethway.engine.structs.TimeoutCache
import inx.sethway.IGroupCallback
import io.paperdb.Paper
import org.json.JSONArray
import org.json.JSONObject
import java.net.InetAddress
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class Engine(
  context: Context,
  private val groupCallback: () -> IGroupCallback?,
  private val entryConsumer: (JSONObject) -> Unit
) {

  companion object {
    private const val TAG = "SethwayEngine"

    private const val ENGINE_PORT = 8899
  }

  private val book = Paper.book()
  private val entries = Paper.book("entries")

  private val myId: String = book.read("id")!!

  private val group: Group = Group(myId)

  private val displayName: String = book.read(
    "display_name",
    Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
  )!!

  private val inetHelper = InetHelper(group)

  private val executor = Executors.newSingleThreadScheduledExecutor()
  private val scheduledExecutor = Executors.newSingleThreadScheduledExecutor()

  private val recentPacketIds = TimeoutCache<String, String?>(12 * 1000)
  private val smartUDP = SmartUDP().create(ENGINE_PORT)

  init {
    CommitBook.create()
    CommitHelper.initBooks()

    group.addSelf(InetQuery.addresses().toString(), getMyCommonInfo().toString())

    // Broadcast group info periodically
    scheduledExecutor.scheduleWithFixedDelay({
      trySafe { broadcastCommitBook(group.getGroupCommits()) }
    }, 0, 20, TimeUnit.SECONDS)

    // broadcasting entry commit book
    scheduledExecutor.scheduleWithFixedDelay({
      trySafe { inetHelper.checkForIpChanges() }
      trySafe { broadcastCommitBook(CommitBook.getCommitBook("entries")) }
    }, 0, 5, TimeUnit.SECONDS)

    smartUDP.route("sync_commit_book") { address, bytes ->
      val json = JSONObject(String(bytes))
      val packetId = json.getString("packet_id")

      // we have to avoid burst of multiple packets! (when we have multiple IPs)
      if (recentPacketIds.addNewEntry(packetId, address.hostAddress)) return@route null

      val theirCommonInfo = json.getJSONObject("common_info")
      val displayName = theirCommonInfo.getString("display_name")

      println("Got sync_commit_book from $displayName")

      val theirCommitBook = json.getJSONObject("commit_book")
      println("Book: $theirCommitBook")
      val ourOutdatedCommitKeys = CommitBook.compareCommits(theirCommitBook)
      if (ourOutdatedCommitKeys.length() > 0) {
        val keysPayload = ourOutdatedCommitKeys.toString().toByteArray()
        trySafe {
          smartUDP.message(address, ENGINE_PORT, keysPayload, "request_provide_commits")
        }
      }
      null
    }

    smartUDP.route("request_provide_commits") { address, bytes ->
      val theirOutdatedCommitKeys = JSONObject(String(bytes))
      val updatedCommitsContent = CommitBook.getCommitContent(theirOutdatedCommitKeys)

      if (updatedCommitsContent.length() > 0) {
        trySafe {
          smartUDP.message(
            address,
            ENGINE_PORT,
            updatedCommitsContent.toString().toByteArray(),
            "response_provide_commits"
          )
        }
      }
      null
    }

    smartUDP.route("response_provide_commits") { address, bytes ->
      val filteredCommitBook = JSONObject(String(bytes))
      CommitBook.updateCommits(filteredCommitBook).forEach { commit ->
        if (commit.bookName == "entries") {
          // Oh! It's a new notification entry!
          val notificationContent = JSONObject(commit.fetchContent())
          Handler(Looper.getMainLooper()).post {
            entryConsumer(notificationContent)
          }
        }
      }
      println("Commits were updated")
      null
    }
  }

  // The invitee scans the QR Code and contacts us for confirmation
  fun receiveInvitee() {
    smartUDP.route("receive_invitee") { address, bytes ->
      smartUDP.removeRoute("receive_invitee")

      val json = JSONObject(String(bytes))
      val inviteeCommits = json.getJSONObject("invitee_commits")

      CommitBook.updateCommits(inviteeCommits)

      val inviteeCommonInfo = json.getJSONObject("invitee_common_info")
      val inviteeDisplayName = inviteeCommonInfo.getString("display_name")

      executor.submit {
        trySafe {
          val replyPayload = JSONObject()
            .put("group_info", group.getGroupInfo())
            .put("inviter_commits", group.selfCommits())
            .toString()
            .toByteArray()
          smartUDP.message(
            address,
            ENGINE_PORT,
            replyPayload,
            "receive_success"
          )
        }
      }

      Log.d(TAG, "Successfully received peer $inviteeDisplayName")
      Handler(Looper.getMainLooper()).post {
        groupCallback()?.onNewPeerConnected(inviteeCommonInfo.toString())
      }
      null
    }
  }

  // We (the invitee) have scanned the QR Code, awaiting for confirmation
  fun acceptGroupInvite(addresses: JSONArray) {
    smartUDP.route("receive_success") { address, bytes ->
      smartUDP.removeRoute("receive_success")

      val json = JSONObject(String(bytes))

      val groupInfo = json.getJSONObject("group_info")
      group.createGroup(groupInfo.getString("group_id"), groupInfo.getString("creator"))

      val inviterCommits = json.getJSONObject("inviter_commits")
      CommitBook.updateCommits(inviterCommits)

      Log.d(TAG, "Joined group successfully!")
      Handler(Looper.getMainLooper()).post {
        groupCallback()?.onGroupJoinSuccess()
      }
      null
    }
    executor.submit {
      val payload = JSONObject()
        .put("invitee_commits", group.selfCommits())
        .put("invitee_common_info", getMyCommonInfo())
        .toString()
        .toByteArray()
      val addrLen = addresses.length()
      for (i in 0..<addrLen) {
        trySafe {
          val address = InetAddress.getByName(addresses.getString(i))
          smartUDP.message(address, ENGINE_PORT, payload, "receive_invitee")
        }
      }
    }
  }

  fun createNewGroup(groupId: String) {
    group.createGroup(groupId, myId)
  }

  fun getGroupInvitation(): JSONArray = InetQuery.addresses()

  fun commitNewEntry(entry: JSONObject) {
    // That's it! This commit should auto propagate with sync packets!
    entries.commit("${System.currentTimeMillis()}", entry.toString(), static = true)
    broadcastCommitBook(CommitBook.getCommitBook("entries"))
  }

  private fun broadcastCommitBook(commitBook: JSONObject) {
    val syncPacket = JSONObject()
      .put("packet_id", UUID.randomUUID())
      .put("common_info", getMyCommonInfo())
      .put("commit_book", commitBook)
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
      val addresses = JSONArray(peerInfo.getString(peerId))
      val addrLen = addresses.length()
      for (i in 0..<addrLen) {
        val address = addresses.getString(i)
        trySafe { consumer(InetAddress.getByName(address)) }
      }
    }
  }

  private fun trySafe(printStackTrace: Boolean = false, block: () -> Unit) {
    try {
      block()
    } catch (e: Throwable) {
      Log.d(TAG, "[Ignored Throwable] ${e::class.simpleName} ${e.message}")
      if (printStackTrace) {
        e.printStackTrace()
      }
    }
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