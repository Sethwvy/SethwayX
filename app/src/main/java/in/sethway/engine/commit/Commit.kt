package `in`.sethway.engine.commit

import io.paperdb.Paper
import org.json.JSONObject

data class Commit(
  val bookName: String,
  val key: String,
  val contentHash: String? = null,
  val commitNumber: Long = -1
) {

  companion object {
    fun JSONObject.toCommit(bookName: String): Commit {
      return Commit(
        bookName = bookName,
        key = getString("key"),
        contentHash = if (has("hash")) getString("hash") else null,
        commitNumber = if (has("commit_no")) getLong("commit_no") else -1
      )
    }
  }

  fun toJSON(): JSONObject = JSONObject()
    .put("key", key)
    .also {
      if (contentHash != null) {
        it.put("hash", contentHash)
        it.put("commit_no", commitNumber)
      }
    }

  fun fetchContent(): String = Paper.book(bookName).read<String>(key)!!
}