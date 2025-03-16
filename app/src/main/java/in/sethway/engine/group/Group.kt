package `in`.sethway.engine.group

import `in`.sethway.engine.commit.CommitHelper
import `in`.sethway.engine.commit.CommitHelper.commit
import io.paperdb.Paper

class Group(private val myId: String) {

  private val lock = Any()

  private val groupBook = Paper.book("group")

  private val peerInfoBook = Paper.book("peer_info")
  private val peerCommonInfoBook = Paper.book("peer_common_info")

  init {
    CommitHelper.initBooks("group", "peer_info", "peer_common_info")
  }

  fun createGroup(groupId: String) {
    synchronized(lock) {
      groupBook.commit("group_id", groupId)
      groupBook.commit("creator", myId)
    }
  }

  fun addSelf(peerInfo: String, commonInfo: String) {
    synchronized(lock) {
      peerInfoBook.commit(myId, peerInfo)
      peerCommonInfoBook.commit(myId, commonInfo)
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