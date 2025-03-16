package `in`.sethway.engine.commit

import io.paperdb.Book
import io.paperdb.Paper
import java.security.MessageDigest

object CommitHelper {
  private val digest = MessageDigest.getInstance("SHA-256")

  @OptIn(ExperimentalStdlibApi::class)
  private fun String.commitHash() = digest.digest(toByteArray()).toHexString()

  private val books = HashMap<Book, String>()

  fun initBooks() {
    books.apply {
      // group
      put(Paper.book("peer_info"), "peer_info")
      put(Paper.book("peer_common_info"), "peer_common_info")

      // notification entries
      put(Paper.book("entries"), "entries")
    }
  }

  fun Book.commit(key: String, content: String) {
    write(key, content)
    val contentHash = content.commitHash()
    val bookName =
      books[this] ?: throw RuntimeException("Could not find book (was writing key=$key)")
    CommitBook.commit(bookName, key, contentHash)
  }
}