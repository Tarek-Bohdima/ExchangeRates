package com.terraconnect.exchangerates.core.algorithm

/**
 * Catalogue of available path-finding strategies, ordered roughly by
 * sophistication. Used as the map key for DI multi-binding and as the user-
 * facing label in the algorithm picker.
 */
enum class StrategyKind(
    val displayName: String,
    val complexity: String,
    val description: String,
) {
    DIRECT(
        displayName = "Direct lookup",
        complexity = "O(1)",
        description = "Hash-map lookup of a single edge. Fastest path when one exists, returns nothing otherwise.",
    ),
    BFS(
        displayName = "BFS — fewest hops",
        complexity = "O(V + E)",
        description = "Breadth-first search; finds the path with the minimum number of conversions. Ignores rate quality.",
    ),
    DIJKSTRA(
        displayName = "Dijkstra — best rate",
        complexity = "O((V + E) log V)",
        description = "Modified max-product Dijkstra. Returns the best composite rate when the graph is arbitrage-free.",
    ),
    BELLMAN_FORD(
        displayName = "Bellman-Ford",
        complexity = "O(V · E)",
        description = "Operates on -log(rate) weights; handles arbitrage and reports negative-weight cycles.",
    ),
    FLOYD_WARSHALL(
        displayName = "Floyd-Warshall",
        complexity = "O(V³) once, then O(1) per query",
        description = "All-pairs shortest path; one heavy precompute then constant-time conversions across the whole graph.",
    ),
}
