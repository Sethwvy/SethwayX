package `in`.sethway.engine.group

import `in`.sethway.engine.commit.CommitBook
import `in`.sethway.engine.commit.CommitBook.getCommitContent
import `in`.sethway.engine.commit.CommitHelper.commit
import io.paperdb.Paper
import org.json.JSONArray
import org.json.JSONObject

class Group(private val myId: String) {

  private val lock = Any()

  private val groupBook = Paper.book("group")

  private val peerInfoBook = Paper.book("peer_info")
  private val peerCommonInfoBook = Paper.book("peer_common_info")

  val exists: Boolean get() = groupBook.contains("group_id")

  val amCreator: Boolean
    get() = synchronized(lock) {
      groupBook.read("am_creator", false)!!
    }

  val groupId: String
    get() = synchronized(lock) {
      groupBook.read("group_id")!!
    }

  fun createGroup(groupId: String, creator: String) {
    synchronized(lock) {
      groupBook.write("group_id", groupId)
      groupBook.write("creator", creator)

      groupBook.write("am_creator", creator == myId)
    }
  }

  fun getGroupInfo(): JSONObject = synchronized(lock) {
    JSONObject()
      .put("group_id", groupBook.read("group_id"))
      .put("creator", groupBook.read("creator"))
      .put("am_creator", amCreator)
  }

  fun getGroupCommits(): JSONObject = synchronized(lock) {
    CommitBook.getCommitBook("peer_info", "peer_common_info")
  }

  fun addSelf(peerInfo: String, commonInfo: String) {
    synchronized(lock) {
      if (!peerCommonInfoBook.contains(myId)) {
        peerInfoBook.commit(myId, peerInfo)
        peerCommonInfoBook.commit(myId, commonInfo)
      }
    }
  }

  fun selfCommits(): JSONObject {
    synchronized(lock) {
      val filteredCommitBook = JSONObject()
        .put("peer_info", JSONArray().put(myId))
        .put("peer_common_info", JSONArray().put(myId))
      return getCommitContent(filteredCommitBook)
    }
  }

  fun getEachPeerInfo(): JSONObject {
    synchronized(lock) {
      val peerInfo = JSONObject()
      for (peerId in peerInfoBook.allKeys) {
        peerInfo.put(peerId, peerInfoBook.read<String>(peerId)!!)
      }
      return peerInfo
    }
  }

  fun getEachPeerCommonInfo(): JSONObject {
    synchronized(lock) {
      val peerInfo = JSONObject()
      for (peerId in peerCommonInfoBook.allKeys) {
        peerInfo.put(peerId, peerCommonInfoBook.read<String>(peerId)!!)
      }
      return peerInfo
    }
  }

  fun getSelfInfo(): String {
    synchronized(lock) {
      return peerInfoBook.read(myId)!!
    }
  }

  fun updateSelfInfo(peerInfo: String) {
    synchronized(lock) {
      peerInfoBook.commit(myId, peerInfo)
    }
  }

  fun updateSelfCommonInfo(commonInfo: String) {
    synchronized(lock) {
      peerCommonInfoBook.commit(myId, commonInfo)
    }
  }
}