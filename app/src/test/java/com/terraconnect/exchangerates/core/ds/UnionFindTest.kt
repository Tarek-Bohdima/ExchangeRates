package com.terraconnect.exchangerates.core.ds

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnionFindTest {

    @Test
    fun `each element starts in its own component`() {
        val dsu = UnionFind(listOf("A", "B", "C"))
        assertFalse(dsu.connected("A", "B"))
        assertFalse(dsu.connected("B", "C"))
        assertEquals(setOf("A"), dsu.componentOf("A"))
    }

    @Test
    fun `union merges two components`() {
        val dsu = UnionFind(listOf("A", "B", "C", "D"))
        assertTrue(dsu.union("A", "B"))
        assertTrue(dsu.union("C", "D"))
        assertTrue(dsu.connected("A", "B"))
        assertFalse(dsu.connected("A", "C"))
        assertTrue(dsu.union("B", "C"))
        // Now all four are linked transitively.
        assertTrue(dsu.connected("A", "D"))
        assertEquals(setOf("A", "B", "C", "D"), dsu.componentOf("A"))
    }

    @Test
    fun `double union of already-connected pair is a no-op`() {
        val dsu = UnionFind(listOf("A", "B"))
        assertTrue(dsu.union("A", "B"))
        assertFalse(dsu.union("A", "B"))
    }
}
