package com.tarek.exchangerates.core.ds

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class LruCacheTest {

    @Test
    fun `rejects non-positive capacity`() {
        assertThrows(IllegalArgumentException::class.java) { LruCache<String, Int>(0) }
        assertThrows(IllegalArgumentException::class.java) { LruCache<String, Int>(-1) }
    }

    @Test
    fun `stores and retrieves values`() {
        val cache = LruCache<String, Int>(3)
        cache["a"] = 1
        cache["b"] = 2
        assertEquals(1, cache["a"])
        assertEquals(2, cache["b"])
    }

    @Test
    fun `returns null for unknown keys`() {
        val cache = LruCache<String, Int>(3)
        assertNull(cache["missing"])
    }

    @Test
    fun `evicts the least recently used entry when capacity is exceeded`() {
        val cache = LruCache<String, Int>(2)
        cache["a"] = 1
        cache["b"] = 2
        cache["c"] = 3 // should evict "a"
        assertNull(cache["a"])
        assertEquals(2, cache["b"])
        assertEquals(3, cache["c"])
    }

    @Test
    fun `reading bumps recency`() {
        val cache = LruCache<String, Int>(2)
        cache["a"] = 1
        cache["b"] = 2
        // Touch "a" so "b" becomes the least-recently-used.
        assertEquals(1, cache["a"])
        cache["c"] = 3 // should now evict "b" rather than "a"
        assertEquals(1, cache["a"])
        assertNull(cache["b"])
        assertEquals(3, cache["c"])
    }

    @Test
    fun `getOrPut only computes on miss`() {
        val cache = LruCache<String, Int>(2)
        var calls = 0
        val v1 = cache.getOrPut("a") { calls++; 42 }
        val v2 = cache.getOrPut("a") { calls++; 99 } // hit — block must not run
        assertEquals(42, v1)
        assertEquals(42, v2)
        assertEquals(1, calls)
    }

    @Test
    fun `clear empties the cache`() {
        val cache = LruCache<String, Int>(3)
        cache["a"] = 1
        cache["b"] = 2
        cache.clear()
        assertEquals(0, cache.size)
        assertNull(cache["a"])
    }

    @Test
    fun `snapshot returns an independent map`() {
        val cache = LruCache<String, Int>(3)
        cache["a"] = 1
        cache["b"] = 2
        val snap = cache.snapshot()
        cache["c"] = 3
        // Snapshot taken before "c" was added — must not see it.
        assertEquals(2, snap.size)
        assertEquals(setOf("a", "b"), snap.keys)
    }
}
