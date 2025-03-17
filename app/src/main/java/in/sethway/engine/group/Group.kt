package `in`.sethway.engine.group

import `in`.sethway.engine.commit.CommitBook.getCommitContent
import `in`.sethway.engine.commit.CommitBook.makeCommitKey
import `in`.sethway.engine.commit.CommitHelper.commit
import io.paperdb.Paper
import org.json.JSONArray
import org.json.JSONObject

class Group(private val myId: String) {

  private val lock = Any()

  private val groupBook = Paper.book("group")

  private val peerInfoBook = Paper.book("peer_info")
  private val peerCommonInfoBook = Paper.book("peer_common_info")

  fun createGroup(groupId: String, creator: String) {
    synchronized(lock) {
      groupBook.write("group_id", groupId)
      groupBook.write("creator", creator)
    }
  }

  fun getGroup(): JSONObject = synchronized(lock) {
    JSONObject()
      .put("group_id", groupBook.read("group_id"))
      .put("creator", groupBook.read("creator"))
  }

  fun addSelf(peerInfo: String, commonInfo: String) {
    synchronized(lock) {
      if (!peerInfoBook.contains(myId)) {
        peerInfoBook.commit(myId, peerInfo)
        peerCommonInfoBook.commit(myId, commonInfo)
      }
    }
  }

  fun shareSelf(): JSONObject {
    synchronized(lock) {
      val commitsToShare = JSONArray().apply {
        put(makeCommitKey("peer_info", myId))
        put(makeCommitKey("peer_common_info", myId))
      }
      return getCommitContent(commitsToShare)
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