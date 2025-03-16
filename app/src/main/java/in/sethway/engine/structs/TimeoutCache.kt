package `in`.sethway.engine.structs

class TimeoutCache<K, V>(private val liveTime: Long) : LinkedHashMap<K, Pair<V, Long>>() {

  override fun removeEldestEntry(eldest: Map.Entry<K, Pair<V, Long>>): Boolean {
    return System.currentTimeMillis() - eldest.value.second > liveTime
  }

  fun addNewEntry(k: K, v: V): Boolean {
    if (containsKey(k)) return true
    this[k] = v to System.currentTimeMillis()
    return false
  }
}