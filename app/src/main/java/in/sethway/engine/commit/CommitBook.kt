package `in`.sethway.engine.commit

import `in`.sethway.engine.commit.Commit.Companion.toCommit
import io.paperdb.Book
import io.paperdb.Paper
import org.json.JSONArray
import org.json.JSONObject
import kotlin.collections.iterator

@OptIn(ExperimentalStdlibApi::class)
object CommitBook {

  private val lock = Any()

  private lateinit var commitBook: Book

  fun create() {
    commitBook = Paper.book("commits")
  }

  @OptIn(ExperimentalStdlibApi::class)
  fun commit(bookName: String, key: String, contentHash: String) {
    synchronized(lock) {
      val commitNumber = 1 + (commitBook.read<Commit>(key)?.commitNumber ?: 0L)
      commitBook.write("$bookName okio $key", Commit(bookName, key, contentHash, commitNumber))
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

  fun getCommitContent(commitKeys: JSONArray): JSONObject {
    synchronized(lock) {
      val commitContent = JSONObject()
      val keyLen = commitKeys.length()
      for (i in 0..<keyLen) {
        val key = commitKeys.getString(i)
        val commit: Commit = commitBook.read(key) ?: continue
        val content: String = Paper.book(commit.bookName).read(key) ?: continue

        commitContent.put(
          key,
          JSONObject()
            .put("commit", commit.toJSON())
            .put("content", content)
        )
      }
      return commitContent
    }
  }

  fun updateCommits(commitContent: JSONObject) {
    synchronized(lock) {
      for (key in commitContent.keys()) {
        val commitJson = commitContent.getJSONObject(key)
        val newCommit: Commit = commitJson.getJSONObject("commit").toCommit()
        val oldCommit: Commit? = commitBook.read(key)

        if (oldCommit == null || newCommit.commitNumber > oldCommit.commitNumber) {
          Paper.book(newCommit.bookName).write(key, newCommit)
          commitBook.write(key, newCommit)
        }
      }
    }
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