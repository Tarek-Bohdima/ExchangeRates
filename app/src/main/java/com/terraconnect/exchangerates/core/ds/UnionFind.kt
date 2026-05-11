package com.terraconnect.exchangerates.core.ds

/**
 * Union-Find (Disjoint Set Union) — connectivity in nearly-constant time.
 *
 * **What it does.** Answers two questions blindingly fast:
 *  * "Are X and Y in the same group?" → [connected]
 *  * "Merge X's group with Y's group." → [union]
 *
 * **How it works.** Every element points to a "parent". Following the parent
 * chain leads to the *root* of the tree — that root represents the whole
 * group. Two elements are in the same group iff they reach the same root.
 *
 * Two optimisations are what make this near-O(1) instead of O(n):
 *
 * 1. **Union by rank.** When merging two trees, hang the *shorter* one under
 *    the taller one. This keeps the trees flat, so [find] climbs a short path.
 *    "Rank" is an upper bound on tree height.
 *
 * 2. **Path compression.** While [find] climbs to a root, on the way back down
 *    we re-point every visited node *directly* at the root. So the *next*
 *    find for any of those nodes is a single hop. The tree gets flatter the
 *    more we use it.
 *
 * Together these give amortised O(α(n)) per operation. α is the inverse
 * Ackermann function — for any number n that fits in this universe, α(n) ≤ 4.
 * Effectively constant.
 *
 * **Where it pays off in this app.** Before we ever spin up a path-finding
 * algorithm, we can ask Union-Find "is the target even reachable from the
 * source?" If the answer is no, we save the whole BFS/Dijkstra/Bellman-Ford
 * cost.
 */
class UnionFind<T>(elements: Iterable<T>) {

    // `parent[x] == x` means x is the root of its tree.
    private val parent = HashMap<T, T>()
    // `rank[x]` ≈ upper bound on the height of the tree rooted at x.
    private val rank = HashMap<T, Int>()

    init {
        // Each element starts in its own group, pointing at itself.
        elements.forEach { e ->
            parent[e] = e
            rank[e] = 0
        }
    }

    val size: Int get() = parent.size

    fun contains(element: T): Boolean = element in parent

    /**
     * Returns the root of [element]'s tree, applying path compression along
     * the way so future lookups are faster.
     *
     * Two passes:
     *  1. Walk up to find the actual root.
     *  2. Walk up again and re-point everything we touched straight at the root.
     *
     * Two-pass iterative version (rather than the prettier recursive one) so
     * very deep trees can't blow the stack.
     */
    fun find(element: T): T {
        require(element in parent) { "Element $element not in UnionFind" }

        // Pass 1: find the root by walking parents.
        var root = element
        while (parent[root] != root) root = parent.getValue(root)

        // Pass 2: flatten the path by pointing each visited node at the root.
        var node = element
        while (parent[node] != root) {
            val next = parent.getValue(node)
            parent[node] = root
            node = next
        }
        return root
    }

    /**
     * Merge the groups containing [a] and [b]. Returns true if they were in
     * different groups (a real merge happened), false if they were already in
     * the same group.
     */
    fun union(a: T, b: T): Boolean {
        val rootA = find(a)
        val rootB = find(b)
        if (rootA == rootB) return false // already merged — nothing to do

        // Hang the *shorter* tree under the *taller* one (union by rank).
        // Only when they're the exact same rank do we need to bump the rank.
        val rankA = rank.getValue(rootA)
        val rankB = rank.getValue(rootB)
        when {
            rankA < rankB -> parent[rootA] = rootB
            rankA > rankB -> parent[rootB] = rootA
            else -> {
                parent[rootB] = rootA
                rank[rootA] = rankA + 1
            }
        }
        return true
    }

    fun connected(a: T, b: T): Boolean = find(a) == find(b)

    /** All elements that share a root with [element] — i.e. its whole group. */
    fun componentOf(element: T): Set<T> {
        val root = find(element)
        return parent.keys.filterTo(mutableSetOf()) { find(it) == root }
    }
}
