package com.tarek.exchangerates.domain.model

/**
 * Ordered, connected sequence of [ExchangeRate]s converting one currency
 * into another.
 *
 * The path remembers the *whole* route — not just the composite rate — so
 * the UI can explain "USD → EUR → GBP" rather than handing back an opaque
 * scalar. That single design choice is what turns this app into a teaching
 * tool: you can see the graph reasoning, not just its output.
 *
 * Special case: `edges` may be empty when [source] equals the target (the
 * identity conversion has no work to do). [compositeRate] still answers
 * meaningfully — 1.0, the multiplicative identity.
 */
data class ConversionPath(
    val source: Currency,
    val edges: List<ExchangeRate>,
) {
    init {
        edges.firstOrNull()?.let { first ->
            require(first.from == source) {
                "First edge must originate at the declared source ($source, got ${first.from})"
            }
        }
        edges.zipWithNext { a, b ->
            require(a.to == b.from) {
                "Path is not connected: ${a.from}->${a.to} cannot be followed by ${b.from}->${b.to}"
            }
        }
    }

    val target: Currency get() = edges.lastOrNull()?.to ?: source
    val hops: Int get() = edges.size
    val compositeRate: Double get() = edges.fold(1.0) { acc, edge -> acc * edge.rate }
    val visited: List<Currency> get() = listOf(source) + edges.map { it.to }
    val isIdentity: Boolean get() = edges.isEmpty()
}
