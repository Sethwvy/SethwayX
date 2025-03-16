package `in`.sethway.engine.commit

import org.json.JSONObject

data class Commit(val bookName: String,
  val key: String,
  val contentHash: String,
  val commitNumber: Long
) {

  companion object {
    fun JSONObject.toCommit(): Commit {
      return Commit(
        bookName = getString("book_name"),
        key = getString("key"),
        contentHash = getString("content_hash"),
        commitNumber = getLong("commit_number")
      )
    }
  }

  fun toJSON(): JSONObject = JSONObject()
    .put("book_name", bookName)
    .put("key", key)
    .put("content_hash", contentHash)
    .put("commit_number", commitNumber)
}