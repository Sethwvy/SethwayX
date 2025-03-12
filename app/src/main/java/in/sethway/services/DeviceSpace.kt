package `in`.sethway.services

import com.tencent.mmkv.MMKV

object DeviceSpace {

  private val mmkvMapping = HashMap<String, MMKV>()

  fun getMMKV(id: String): MMKV {
    mmkvMapping[id]?.let { return it }
    return MMKV.mmkvWithID(id).also { mmkvMapping[id] = it }
  }

}