package `in`.sethway.engine

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import `in`.sethway.engine.commit.CommitBook
import `in`.sethway.engine.commit.CommitHelper
import `in`.sethway.engine.commit.CommitHelper.commit
import `in`.sethway.engine.group.Group
import `in`.sethway.smartdatagram.Destination
import `in`.sethway.smartdatagram.SmartDatagram
import inx.sethway.IGroupCallback
import io.paperdb.Paper
import org.json.JSONArray
import org.json.JSONObject
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class Engine(
  private val groupCallback: () -> IGroupCallback?,
  private val entryConsumer: (JSONObject) -> Unit
) {

  companion object {
    private const val TAG = "SethwayEngine"

    private const val ENGINE_PORT = 8899

    private const val RENDEZVOUS_PORT = 2299

    // Note: The order matters! First we must have Ipv4 address
    private val RENDEZVOUS_DESTINATIONS = listOf(
      Destination(InetAddress.getByName("37.27.51.34"), RENDEZVOUS_PORT),
      Destination(InetAddress.getByName("2a01:4f9:3081:399c::4"), RENDEZVOUS_PORT)
    )

    // Maximum time we can wait before asking rendezvous server to lookup peers
    private const val MAX_TIME_PEER_OUTAGE = 30 * 1000
  }

  private val book = Paper.book()

  private val peerLogs = Paper.book("peer_logs")
  private val peerLastKnownGoodAddress = Paper.book("peer_last_good_addresses")

  private val entryBook = Paper.book("entries")

  val myId: String = book.read("id")!!

  private val group: Group = Group(myId)

  private val displayName: String = book.read("display_name", "${Build.BRAND} ${Build.MODEL}")!!

  private val executor = Executors.newSingleThreadScheduledExecutor()
  private val scheduledExecutor = Executors.newSingleThreadScheduledExecutor()

  private val datagram = SmartDatagram(InetSocketAddress("::", ENGINE_PORT))

  init {
    CommitBook.create()
    CommitHelper.initBooks()

    executor.submit {
      group.addSelf(myReachableAddresses().toString(), getMyCommonInfo().toString())
    }

    // checking for changes in our and theirs IPv6 interfaces
    scheduledExecutor.scheduleWithFixedDelay({
      trySafe { performStun() }
      trySafe { ensurePeerIpValidity() }
    }, 0, 6, TimeUnit.SECONDS)

    // Pings to rendezvous server
    scheduledExecutor.scheduleWithFixedDelay({
      trySafe { announceToRendezvous(myReachableAddresses()) }
      // Ensuring we have good updated IP of all the peers
    }, 0, 7, TimeUnit.SECONDS)

    // Broadcast group info periodically
    scheduledExecutor.scheduleWithFixedDelay({
      trySafe { broadcastCommitBook(group.getGroupCommits()) }
    }, 0, 20, TimeUnit.SECONDS)

    // broadcasting entry commit book
    scheduledExecutor.scheduleWithFixedDelay({
      trySafe { broadcastCommitBook(CommitBook.getCommitBook("entries")) }
    }, 0, 10, TimeUnit.SECONDS)

    datagram.subscribe("sync_commit_book") { address, port, bytes ->
      val json = JSONObject(String(bytes))

      val commonInfo = json.getJSONObject("common_info")
      val peerId = commonInfo.getString("id")

      peerLogs.write(peerId, System.currentTimeMillis())
      peerLastKnownGoodAddress.write(peerId, address.hostAddress)

      val theirCommitBook = json.getJSONObject("commit_book")

      val ourOutdatedCommitKeys = CommitBook.compareCommits(theirCommitBook)
      if (ourOutdatedCommitKeys.length() > 0) {
        val keysPayload = ourOutdatedCommitKeys.toString().toByteArray()
        trySafe {
          datagram.send(Destination(address, port), "request_provide_commits", keysPayload)
        }
      }
      null
    }

    datagram.subscribe("request_provide_commits") { address, port, bytes ->
      val theirOutdatedCommitKeys = JSONObject(String(bytes))
      val updatedCommitsContent = CommitBook.getCommitContent(theirOutdatedCommitKeys)
      val replyPayload = JSONObject()
        .put("content", updatedCommitsContent)
        .toString()
        .toByteArray()

      if (updatedCommitsContent.length() > 0) {
        trySafe {
          datagram.send(Destination(address, port), "provide_commits", replyPayload)
        }
      }
      null
    }

    datagram.subscribe("provide_commits") { address, port, bytes ->
      val json = JSONObject(String(bytes))
      val commitContent = json.getJSONObject("content")
      CommitBook.updateCommits(commitContent).forEach { commit ->
        if (commit.bookName == "entries") {
          // Oh! It's a new notification entry!
          val notificationContent = JSONObject(commit.fetchContent())
          Handler(Looper.getMainLooper()).post {
            entryConsumer(notificationContent)
          }
        }
      }
      null
    }

    // A reply from the rendezvous server! He has found peer(s)
    datagram.subscribe("peer_located") { address, port, bytes ->
      val peerMap = JSONObject(String(bytes))
      executor.submit { trySafe { reconnectWithPeers(peerMap) } }
      null
    }
  }

  private var myLastCommitAddressContent = ""

  // We perform a stun request to RENDEZVOUS to discover our public IP
  // and port beyond CGNAT mapping. This Public IP and port keeps changing.
  private fun performStun() {
    val newAddressArrayContent = myReachableAddresses().toString()
    if (newAddressArrayContent != myLastCommitAddressContent) {
      // We gotta commit hier
      group.updateSelfInfo(newAddressArrayContent)
      myLastCommitAddressContent = newAddressArrayContent
    }
  }

  // Announcing our presence to rendezvous, to let 'them know
  // our set of IP addresses if we/they get lost
  private fun announceToRendezvous(newAddresses: JSONArray) {
    val payload = JSONObject()
      .put("id", myId)
      .put("addresses", newAddresses)
      .toString()
      .toByteArray()
    datagram.send(RENDEZVOUS_DESTINATIONS, "announce", payload)
  }

  private fun ensurePeerIpValidity() {
    val missingPeers = JSONArray()
    for (peerId in peerLogs.allKeys) {
      val peerLastSeen: Long = peerLogs.read(peerId)!!
      if (System.currentTimeMillis() - peerLastSeen > MAX_TIME_PEER_OUTAGE) {
        // Oh! The peer has went missing! We must find 'em.
        // How? Just ask the rendezvous server!
        missingPeers.put(peerId)
      }
    }
    if (missingPeers.length() == 0) return
    val payload = JSONObject()
      .put("reply_port", ENGINE_PORT)
      .put("peer_ids", missingPeers)
      .toString()
      .toByteArray()

    datagram.send(RENDEZVOUS_DESTINATIONS, "lookup", payload)
  }

  // We've got a response from rendezvous server for our lookup request!
  // Now we got to reconnect with the peers...
  private fun reconnectWithPeers(peersLocation: JSONObject) {
    val commitBookPayload = getCommitBookPayload(group.getGroupCommits())
    val destinations = mutableListOf<Destination>()
    for (peerId in peersLocation.keys()) {
      val peerAddresses = peersLocation.getJSONArray(peerId)
      val addrLen = peerAddresses.length()
      for (i in 0..<addrLen) {
        trySafe {
          destinations += peerAddresses.getString(i).toDest()
        }
      }
    }
    datagram.send(destinations, "sync_commit_book", commitBookPayload) {}
  }

  // The invitee scans the QR Code and contacts us for confirmation
  fun receiveInvitee() {
    datagram.subscribe("receive_invitee") { address, port, bytes ->
      datagram.unsubscribe("receive_invitee")

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
          datagram.send(Destination(address, port), "receive_success", replyPayload)
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
    println("accepting group invite!")
    datagram.subscribe("receive_success") { address, port, bytes ->
      datagram.unsubscribe("receive_success")

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
      println("preparing payload")
      val payload = JSONObject()
        .put("invitee_commits", group.selfCommits())
        .put("invitee_common_info", getMyCommonInfo())
        .toString()
        .toByteArray()
      println("prepared payload ${String(payload)}")
      println("got address list $addresses")
      val addrLen = addresses.length()
      val destinations = mutableListOf<Destination>()
      for (i in 0..<addrLen) {
        trySafe {
          val destination = addresses.getString(i).toDest()
          destinations += destination
          println("parsed ${destination.address} ${destination.port}")
        }
      }
      datagram.send(destinations, "receive_invitee", payload)
    }
  }

  fun createNewGroup(groupId: String) {
    group.createGroup(groupId, myId)
  }

  fun getGroupInvitation(): JSONArray = myReachableAddresses()

  fun commitNewEntry(entry: JSONObject) {
    // That's it! This commit should auto propagate with sync packets!
    val simpleKey = System.currentTimeMillis().toString()
    entryBook.commit(simpleKey, entry.toString(), static = true)

    // We can directly send the commit content! Skipping the usual order
    val newEntryContent =
      CommitBook.getCommitContent(JSONObject().put("entries", JSONArray().put(simpleKey)))
    executor.submit { directlyBroadcastCommitContent(newEntryContent) }
  }

  private fun directlyBroadcastCommitContent(content: JSONObject) {
    val payload = JSONObject()
      .put("content", content)
      .toString()
      .toByteArray()
    forEachPeerAddress { destination ->
      datagram.send(destination, "provide_commits", payload)
    }
  }

  private fun broadcastCommitBook(commitBook: JSONObject) {
    val syncPacket = getCommitBookPayload(commitBook)
    forEachPeerAddress { destination ->
      datagram.send(destination, "sync_commit_book", syncPacket)
    }
  }

  private fun getCommitBookPayload(commitBook: JSONObject) = JSONObject()
    .put("commit_book", commitBook)
    .put("common_info", getMyCommonInfo())
    .toString()
    .toByteArray()

  private fun forEachPeerAddress(consumer: (Destination) -> Unit) {
    val peerInfo = group.getEachPeerInfo()
    for (peerId in peerInfo.keys()) {
      if (peerId == myId) continue
      val addresses = JSONArray(peerInfo.getString(peerId))
      val addrLen = addresses.length()
      for (i in 0..<addrLen) {
        val address = addresses.getString(i)
        trySafe { consumer(address.toDest()) }
      }
    }
  }

  private fun getMyCommonInfo(): JSONObject = JSONObject()
    .put("id", myId)
    .put("display_name", displayName)

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

  private fun String.toDest(): Destination = split(" ").let {
    Destination(
      InetAddress.getByName(it[0].replace("[", "").replace("]", "")),
      it[1].toInt()
    )
  }

  private fun myReachableAddresses(): JSONArray {
    val addresses = JSONArray()
    NetworkInterface.getNetworkInterfaces().iterator().forEach { i ->
      i.inetAddresses.iterator().forEach { a ->
        if (!a.isLinkLocalAddress
          && !a.isLoopbackAddress
          && !a.isSiteLocalAddress
          && !a.isAnyLocalAddress && a is Inet6Address
        ) {
          a.hostAddress.let { addresses.put("[$it] $ENGINE_PORT") }
        }
      }
    }
    // We perform a stun request to RENDEZVOUS server to discover our
    // public IPv4 and port beyond CGNAT mapping
    datagram.send(RENDEZVOUS_DESTINATIONS, "stun", "Hi!".toByteArray()) {}
    datagram.expectPacket("stun_response", 2000)?.let { stunReply ->
      val stunInfo = JSONObject(String(stunReply.data))
      val myPublicAddress = stunInfo.getString("address")
      val myPublicPort = stunInfo.getInt("port")

      addresses.put("$myPublicAddress:$myPublicPort")
    }
    return addresses
  }

  fun close() {
    datagram.close()
    executor.shutdownNow()
    scheduledExecutor.shutdownNow()
  }
}