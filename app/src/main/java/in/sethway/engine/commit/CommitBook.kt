package `in`.sethway.engine.commit

import `in`.sethway.engine.commit.Commit.Companion.toCommit
import io.paperdb.Book
import io.paperdb.Paper
import org.json.JSONArray
import org.json.JSONObject
import kotlin.collections.iterator

@OptIn(ExperimentalStdlibApi::class)
object CommitBook {

  private const val TAG = "CommitBook"

  private val lock = Any()

  private lateinit var commitBook: Book

  fun create() {
    commitBook = Paper.book("commits")
  }

  fun makeCommitKey(bookName: String, key: String) = "$bookName okio $key"

  @OptIn(ExperimentalStdlibApi::class)
  fun commit(bookName: String, key: String, contentHash: String) {
    synchronized(lock) {
      val commitNumber = 1 + (commitBook.read<Commit>(key)?.commitNumber ?: 0L)
      val commitKey = makeCommitKey(bookName, key)
      commitBook.write(commitKey, Commit(bookName, key, contentHash, commitNumber))
    }
  }

  /**
   * Compares commit from the other book, we only look
   * for commits that are outdated on our end.
   */
  fun compareCommits(otherBook: JSONObject): JSONArray {
    synchronized(lock) {
      val outdatedCommitKeys = JSONArray()
      for (key in otherBook.keys()) {
        val ourCommit: Commit? = commitBook.read(key)
        if (ourCommit != null) {
          val theirCommit: Commit = otherBook.getJSONObject(key).toCommit()
          if (theirCommit.commitNumber > ourCommit.commitNumber) {
            outdatedCommitKeys.put(key)
          }
        } else {
          outdatedCommitKeys.put(key)
        }
      }
      return outdatedCommitKeys
    }
  }

  fun getCommit(commitKey: String): JSONObject? {
    val commit: Commit = commitBook.read(commitKey) ?: return null
    val content: String = Paper.book(commit.bookName).read(commit.key) ?: return null
    return JSONObject()
      .put("commit_info", commit.toJSON())
      .put("content", content)
  }

  fun getCommitContent(commitKeys: JSONArray): JSONArray {
    synchronized(lock) {
      val commits = JSONArray()
      val keyLen = commitKeys.length()
      for (i in 0..<keyLen) {
        val commitKey = commitKeys.getString(i)
        getCommit(commitKey)?.let { commits.put(it) }
      }
      return commits
    }
  }

  fun updateCommits(newCommits: JSONArray): List<Commit> {
    synchronized(lock) {
      val commits = ArrayList<Commit>()
      val commitLen = newCommits.length()
      for (i in 0..<commitLen) {
        updateCommit(newCommits.getJSONObject(i))?.let { commits.add(it) }
      }
      return commits
    }
  }

  fun updateCommit(commitJson: JSONObject): Commit? {
    val newCommit: Commit = commitJson.getJSONObject("commit_info").toCommit()
    val commitKey = makeCommitKey(newCommit.bookName, newCommit.key)
    val oldCommit: Commit? = commitBook.read(commitKey)

    if (oldCommit == null || newCommit.commitNumber > oldCommit.commitNumber) {
      val content = commitJson.getString("content")
      Paper.book(newCommit.bookName).write(newCommit.key, content)
      commitBook.write(commitKey, newCommit)
      return newCommit
    }
    return null
  }

  fun getCommitBook(): JSONObject {
    synchronized(lock) {
      val json = JSONObject()
      for (key in commitBook.allKeys) {
        val commit: Commit = commitBook.read(key)!!
        json.put(key, commit.toJSON())
      }
      return json
    }
  }
}