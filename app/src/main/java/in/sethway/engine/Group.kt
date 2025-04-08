package `in`.sethway.engine

import `in`.sethway.engine.commit.CommitBook
import `in`.sethway.engine.commit.CommitHelper.commit
import io.paperdb.Paper
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
class Group(private val myId: String) {

  private val lock = Any()

  private val groupBook = Paper.book("group")
  private val groupSecret = Paper.book("group_secret")

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

  private lateinit var encryptCipher: Cipher
  private lateinit var decryptCipher: Cipher

  init {
    if (exists) {
      val secretKey = SecretKeySpec(
        Base64.decode(groupSecret.read<String>("key")!!), "AES")
      val iv = Base64.decode(groupSecret.read<String>("iv")!!)
      loadCipher(secretKey, iv)
    }
  }

  val messageEncrypter: (ByteArray) -> ByteArray = { message ->
    encryptCipher.doFinal(message)
  }

  val messageDecrypter: (ByteArray) -> ByteArray = { encrypted ->
    decryptCipher.doFinal(encrypted)
  }

  fun createGroup(groupId: String, creator: String) {
    synchronized(lock) {
      groupBook.write("group_id", groupId)
      groupBook.write("creator", creator)

      groupBook.write("am_creator", creator == myId)
    }
  }

  fun makeGroupSecret() {
    val secretKey = KeyGenerator.getInstance("AES")
      .also { it.init(256) }.generateKey()
    val iv = ByteArray(16).also { SecureRandom().nextBytes(it) }

    groupSecret.write("key", Base64.encode(secretKey.encoded))
    groupSecret.write("iv", Base64.encode(iv))
    loadCipher(secretKey, iv)
  }

  fun useGroupSecret(source: ByteArrayInputStream) {
    synchronized(lock) {
      val secretKeyBytes = ByteArray(source.read()).also { source.read(it) }
      val iv = ByteArray(source.read()).also { source.read(it) }

      groupSecret.write("key", Base64.encode(secretKeyBytes))
      groupSecret.write("iv", Base64.encode(iv))
      loadCipher(SecretKeySpec(secretKeyBytes, "AES"), iv)
    }
  }

  private fun loadCipher(secretKey: SecretKey, iv: ByteArray) {
    encryptCipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    encryptCipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))

    decryptCipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    decryptCipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
  }

  fun getGroupSecret(): ByteArray {
    synchronized(lock) {
      val secretKey = Base64.decode(groupSecret.read<String>("key")!!)
      val iv = Base64.decode(groupSecret.read<String>("iv")!!)

      return ByteBuffer.allocate(4 + secretKey.size + 4 + iv.size)
        .putInt(secretKey.size)
        .put(secretKey)
        .putInt(iv.size)
        .put(iv)
        .array()
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
      return CommitBook.getCommitContent(filteredCommitBook)
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