package `in`.sethway.engine.commit

import io.paperdb.Book
import io.paperdb.Paper
import java.security.MessageDigest

object CommitHelper {
  private val digest = MessageDigest.getInstance("SHA-256")

  @OptIn(ExperimentalStdlibApi::class)
  private fun String.commitHash() = digest.digest(toByteArray()).toHexString()

  private val books = HashMap<Book, String>()

  fun initBooks(vararg bookNames: String) {
    for (bookName in bookNames) {
      books.put(Paper.book(bookName), bookName)
    }
  }

  fun Book.commit(key: String, content: String) {
    write(key, content)
    val contentHash = content.commitHash()
    val bookName = books[this] ?: throw RuntimeException("Could not find book (was writing key=$key)")
    CommitBook.commit(bookName, key, contentHash)
  }
}