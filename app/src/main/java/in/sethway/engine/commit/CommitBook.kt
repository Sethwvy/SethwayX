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

  fun makeCommitKey(bookName: String, key: String) = "$bookName _ $key"

  @OptIn(ExperimentalStdlibApi::class)
  fun commit(bookName: String, key: String, contentHash: String, static: Boolean = false) {
    synchronized(lock) {
      val commitKey = makeCommitKey(bookName, key)
      if (static) {
        commitBook.write(commitKey, Commit(bookName, key, null, -1))
      } else {
        val commitNumber = 1 + (commitBook.read<Commit>(key)?.commitNumber ?: 0L)
        commitBook.write(commitKey, Commit(bookName, key, contentHash, commitNumber))
      }
    }
  }

  /**
   * Compares commit from the other book, we only look
   * for commits that are outdated on our end.
   */
  fun compareCommits(otherBook: JSONObject): JSONObject {
    synchronized(lock) {
      val outdatedBookKeys = JSONObject()
      for (bookName in otherBook.keys()) {
        val commits = otherBook.getJSONArray(bookName)
        val commitLen = commits.length()

        for (i in 0..<commitLen) {
          val theirCommit: Commit = commits.getJSONObject(i).toCommit(bookName)
          val key = theirCommit.key
          val commitKey = makeCommitKey(bookName, key)
          val ourCommit: Commit? = commitBook.read(commitKey)

          if (ourCommit == null || theirCommit.commitNumber > ourCommit.commitNumber) {
            val keyBook =
              if (outdatedBookKeys.has(bookName)) outdatedBookKeys.getJSONArray(bookName) else JSONArray()
            keyBook.put(key)

            outdatedBookKeys.put(bookName, keyBook)
          }
        }
      }
      return outdatedBookKeys
    }
  }

  fun getCommit(commitKey: String): JSONObject? {
    val commit: Commit = commitBook.read(commitKey) ?: return null
    val content: String = Paper.book(commit.bookName).read(commit.key) ?: return null
    return JSONObject()
      .put("commit_info", commit.toJSON())
      .put("content", content)
  }

  fun getCommitContent(bookKeys: JSONObject): JSONObject {
    synchronized(lock) {
      val filteredCommitBook = JSONObject()
      for (bookName in bookKeys.keys()) {
        val keys = bookKeys.getJSONArray(bookName)
        val keyLen = keys.length()
        for (i in 0..<keyLen) {
          val commitKey = makeCommitKey(bookName, keys.getString(i))
          val commit = getCommit(commitKey) ?: continue

          val commitEntries = if (filteredCommitBook.has(bookName))
            filteredCommitBook.getJSONArray(bookName) else JSONArray()
          commitEntries.put(commit)

          filteredCommitBook.put(bookName, commitEntries)
        }
      }
      return filteredCommitBook
    }
  }

  fun updateCommits(filteredCommitBook: JSONObject): List<Commit> {
    synchronized(lock) {
      val updatedCommits = ArrayList<Commit>()
      for (bookName in filteredCommitBook.keys()) {
        val commits = filteredCommitBook.getJSONArray(bookName)
        val commitLen = commits.length()
        for (i in 0..<commitLen) {
          val commitJson = commits.getJSONObject(i)
          updateCommit(bookName, commitJson)?.let { updatedCommits.add(it) }
        }
      }
      return updatedCommits
    }
  }

  private fun updateCommit(bookName: String, commitJson: JSONObject): Commit? {
    val newCommit: Commit = commitJson.getJSONObject("commit_info").toCommit(bookName)
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

  fun getCommitBook(vararg bookNames: String): JSONObject {
    synchronized(lock) {
      val jsonCommitBook = JSONObject()
      for (bookName in bookNames) {
        val book = Paper.book(bookName)
        for (simpleKey in book.allKeys) {
          val commitKey = makeCommitKey(bookName, simpleKey)
          val commit: Commit = commitBook.read(commitKey)!!

          val commitEntries =
            jsonCommitBook.let { if (it.has(bookName)) it.getJSONArray(bookName) else JSONArray() }
          commitEntries.put(commit.toJSON())

          jsonCommitBook.put(bookName, commitEntries)
        }
      }
      return jsonCommitBook
    }
  }
}