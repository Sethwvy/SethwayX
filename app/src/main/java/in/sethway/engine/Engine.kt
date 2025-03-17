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
  private val context: Context,
  private val groupCallback: () -> IGroupCallback?
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

  // The invitee scans the QR Code and contacts us for confirmation
  fun receiveInvitee() {
    smartUDP.route("receive_invitee") { address, bytes ->
      smartUDP.removeRoute("receive_invitee")

      val inviteeInfo = JSONObject(String(bytes))
      CommitBook.updateCommits(inviteeInfo)

      val inviteeCommonInfo = inviteeInfo.getJSONObject("peer_common_info")
      val inviteeDisplayName = inviteeCommonInfo.getString("display_name")

      executor.submit {
        trySafe {
          smartUDP.message(address, ENGINE_PORT, "Yep!".toByteArray(), "receive_success")
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
  private fun sayHiInviter() {
    smartUDP.route("receive_success") { address, bytes ->
      smartUDP.removeRoute("receive_success")
      Log.d(TAG, "Joined group successfully!")
      Handler(Looper.getMainLooper()).post {
        groupCallback()?.onGroupJoinSuccess()
      }
      null
    }
    println("here!")
    executor.submit {
      println("here2")
      val selfCommitInfo = group.shareSelf().toString().toByteArray()
      println("here3")

      forEachPeerAddress { address ->
        println("Sending receive me ping $address")
        smartUDP.message(
          address,
          ENGINE_PORT,
          selfCommitInfo,
          "receive_invitee"
        )
      }
    }
  }

  fun createNewGroup(groupId: String) {
    group.createGroup(groupId, myId)
  }

  fun getGroupInvitation(): JSONObject = JSONObject()
    .put("group_info", group.getGroup())
    .put("commit_content", group.shareSelf())

  fun acceptGroupInvite(invitation: JSONObject) {
    println("processing invitation $invitation")

    val groupInfo = invitation.getJSONObject("group_info")
    group.createGroup(groupInfo.getString("group_id"), groupInfo.getString("creator"))

    val inviterCommits = invitation.getJSONObject("commit_content")
    CommitBook.updateCommits(inviterCommits)

    println("processing content $inviterCommits")
    println("commits updated!")
    sayHiInviter()
  }

  fun commitNewEntry(entry: JSONObject) {
    // That's it! This commit should auto propagate with sync packets!
    entries.commit("${System.currentTimeMillis()}", entry.toString())
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
    } catch (e: Throwable) {
      Log.d(TAG, "[Ignored Throwable] ${e::class.simpleName} ${e.message}")
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