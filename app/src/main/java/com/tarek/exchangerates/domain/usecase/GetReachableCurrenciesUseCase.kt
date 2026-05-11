package com.tarek.exchangerates.domain.usecase

import com.tarek.exchangerates.core.ds.CurrencyGraph
import com.tarek.exchangerates.core.ds.UnionFind
import com.tarek.exchangerates.domain.model.Currency
import javax.inject.Inject

/**
 * "Which currencies can I reach from X?" — answered with Union-Find.
 *
 * **Why Union-Find for this.** We could BFS from the source and collect every
 * vertex we touch. That's O(V+E) per call. But if the user keeps asking
 * "what's reachable from A?", "from B?", "from C?" — and the graph hasn't
 * changed — we'd be redoing the same connectivity work repeatedly.
 *
 * Union-Find lets us pay the O(E · α(V)) cost *once* per graph (effectively
 * O(E)) to build a DSU where every vertex points at its component root. After
 * that, "is X reachable from Y?" is two `find` calls — basically O(1).
 *
 * **Subtle correctness note.** Union-Find natively models *undirected*
 * connectivity. For currency graphs that's fine for "could there *ever* be a
 * conversion route?" because every directed edge implies *some* path-existence
 * benefit between the two endpoints when traversed by other algorithms that
 * understand direction. If you cared about strictly-directed reachability
 * (strongly-connected components), you'd swap this for Tarjan's or Kosaraju's
 * algorithm — same interface, different DS&A under the hood.
 */
class GetReachableCurrenciesUseCase @Inject constructor() {

    /**
     * Returns the set of currencies in the same connected component as [source],
     * minus the source itself. Empty if [source] is not in the graph or has no
     * other members in its component.
     */
    operator fun invoke(graph: CurrencyGraph, source: Currency): Set<Currency> {
        if (!graph.contains(source)) return emptySet()

        val dsu = UnionFind(graph.vertices)
        graph.edges.forEach { edge -> dsu.union(edge.from, edge.to) }

        return dsu.componentOf(source) - source
    }
}
