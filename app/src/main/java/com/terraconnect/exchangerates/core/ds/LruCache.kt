package com.terraconnect.exchangerates.core.ds

/**
 * LRU cache — Least-Recently-Used eviction policy.
 *
 * **The idea.** A cache that always holds at most [capacity] entries. When
 * we're full and a new one arrives, we evict the entry that was *touched
 * longest ago* (read or written). The bet is that recent activity predicts
 * future activity, so the oldest-untouched entry is the cheapest to lose.
 *
 * **The neat trick.** A regular [LinkedHashMap] remembers *insertion order*.
 * Pass `accessOrder = true` to its constructor and it remembers *access
 * order* instead — every `get` or `put` moves the touched entry to the back.
 * Override [removeEldestEntry] to say "evict when full" and you have a
 * complete LRU in about ten lines of Kotlin.
 *
 * **Thread safety.** Coarse `synchronized` on the map. Fine for a path-cache
 * keyed by `(from, to)` pairs — the work happens *outside* the cache (BFS,
 * Dijkstra, etc.) so contention is negligible. If profiling ever showed
 * otherwise, the next step would be a striped lock or a concurrent
 * linked-hash-map.
 */
class LruCache<K, V>(private val capacity: Int) {

    init {
        require(capacity > 0) { "LRU capacity must be positive, got $capacity" }
    }

    // The third boolean arg (`accessOrder = true`) is the whole show — it
    // turns insertion-order tracking into access-order tracking.
    private val map = object : LinkedHashMap<K, V>(capacity, LOAD_FACTOR, /* accessOrder = */ true) {
        // Called by the map after each put. If we say "yes, evict the eldest",
        // it removes it for us. So this single override implements the entire
        // eviction policy.
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>): Boolean =
            size > capacity
    }

    val size: Int get() = synchronized(map) { map.size }

    operator fun get(key: K): V? = synchronized(map) { map[key] }

    operator fun set(key: K, value: V) {
        synchronized(map) { map[key] = value }
    }

    /** Return cached value or compute, store, and return it. The hot read path. */
    fun getOrPut(key: K, compute: () -> V): V = synchronized(map) {
        map[key] ?: compute().also { map[key] = it }
    }

    fun clear() {
        synchronized(map) { map.clear() }
    }

    fun snapshot(): Map<K, V> = synchronized(map) { LinkedHashMap(map) }

    private companion object {
        private const val LOAD_FACTOR = 0.75f
    }
}
