# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this project is

A single-screen Android app whose purpose is to *showcase* idiomatic Kotlin, classic data structures and algorithms, and Gang of Four design patterns — built around currency exchange because the conversion graph is a natural fit for graph algorithms. It is intentionally **not** a production app: there's no auth, no analytics, no backend, and the embedded rate dataset is hand-picked (including an arbitrage triangle) for demos. The intended audience is a candidate studying for an interview and an interviewer asking about it.

If you're modifying this codebase, keep the educational ergonomics in mind:
- Inline comments should explain *why* and *how* a piece of logic works, not just *what*.
- Algorithms in `core/algorithm/` are the most-trafficked teaching surface. Don't strip their step-by-step comments.
- The dataset in `EmbeddedExchangeRatesDataSource` *contains a deliberate ~1.19% arbitrage triangle* (USD → EUR → GBP → USD). Other 2-edge round trips are tuned to be within the 0.1% detection epsilon. Touching those numbers can change what `DetectArbitrageUseCase` finds.

## Build & test commands

Single-module Android/Gradle project (`:app`). Use the wrapper.

- Debug APK: `./gradlew :app:assembleDebug`
- Install on a running device: `./gradlew :app:installDebug`
- JVM unit tests: `./gradlew :app:testDebugUnitTest`
- Instrumented tests: `./gradlew :app:connectedDebugAndroidTest`
- Lint: `./gradlew :app:lint`
- One unit test: `./gradlew :app:testDebugUnitTest --tests "com.terraconnect.exchangerates.core.algorithm.PathFindingStrategyTest.dijkstra*"`
- Clean: `./gradlew clean`

Toolchain (pinned in `gradle/libs.versions.toml` + `gradle/wrapper/gradle-wrapper.properties`): Gradle 8.10.2, AGP 8.7.3, Kotlin 2.1.0, KSP 2.1.0-1.0.29, Hilt 2.53.1, Compose BOM 2024.12.01 with Material 3, Coroutines 1.10.1, Retrofit 2.11, Moshi 1.15.1. `compileSdk`/`targetSdk` 35, `minSdk` 24, JVM target 17. Plugins are aliased via the version catalog, so dependency changes happen in one file.

KSP (not kapt) runs both Hilt and Moshi codegen. Compose Compiler is the first-party `org.jetbrains.kotlin.plugin.compose` plugin that ships with Kotlin 2.x.

## Architecture

Clean Architecture in three rings plus DI, all under `com.terraconnect.exchangerates`:

```
ui/ ───────────► domain/usecase + domain/model ◄─────── data/repository ◄─── data/source
└── conversion/                                                              └── Embedded | Remote
└── theme/
core/algorithm/  (Strategy/Factory/Chain — referenced by use cases)
core/ds/         (CurrencyGraph, UnionFind, LruCache — used by algorithms)
core/concurrent/ (DispatcherProvider)
di/              (Hilt modules)
```

### Key abstractions and where to find them

- **Domain model** (`domain/model/`):
  - `Currency` — `@JvmInline value class` over an ISO-style code with `Currency.of(...)` validation.
  - `Money` — `Currency` + `BigDecimal` (precision preserved across multiplications).
  - `ExchangeRate` — directed edge with positive finite rate, no self-loops.
  - `ConversionPath` — connected sequence of edges. Empty edges list models the identity conversion.
  - `ConversionResult` — sealed interface with `Success`, `Unreachable`, `UnknownCurrency`, `Failure`.
  - `ArbitrageOpportunity` — a cycle of `Currency` plus its profit factor.

- **Core data structures** (`core/ds/`):
  - `CurrencyGraph` — immutable directed weighted multigraph, built via `CurrencyGraph.builder()` (GoF Builder).
  - `UnionFind<T>` — DSU with union-by-rank + iterative path compression. O(α(n)) amortized.
  - `LruCache<K, V>` — `LinkedHashMap(accessOrder = true)` + `removeEldestEntry` override. Synchronized.

- **Algorithms** (`core/algorithm/`):
  - `PathFindingStrategy` (`fun interface`) + `AbstractPathFindingStrategy` (Template Method base) + `StrategyKind` enum + `StrategyFactory` (Hilt-multibound) + `StrategyChain` (Chain of Responsibility).
  - Five implementations, each documented at the top of its file with the intuition, the complexity, and any precondition: `DirectLookupStrategy`, `BfsPathFindingStrategy`, `DijkstraPathFindingStrategy` (max-product variant; requires arbitrage-free graph), `BellmanFordPathFindingStrategy` (handles arbitrage via `-ln(rate)` transform), `FloydWarshallPathFindingStrategy` (O(V³) precompute cached per graph instance).

- **Data layer** (`data/`):
  - `ExchangeRatesDataSource` interface with `EmbeddedExchangeRatesDataSource` (default) and `RemoteExchangeRatesDataSource` (Retrofit + Moshi codegen DTOs in `data/remote/dto/`).
  - `ExchangeRatesRepository` interface, `DefaultExchangeRatesRepository` (uses `MutableStateFlow<CurrencyGraph?>` + `onSubscription` for lazy first-fetch + `Mutex`-guarded refresh), and `CachingExchangeRatesRepository` (Decorator over the default).

- **Use cases + Facade** (`domain/usecase/`):
  - `ConvertCurrencyUseCase` — picks a strategy via `StrategyFactory.get(kind)` and shapes the result. Same-currency goes through the identity-path branch (empty `ConversionPath`).
  - `DetectArbitrageUseCase` — Bellman-Ford from each vertex; canonicalises cycle rotations to dedupe; filters cycles whose profit factor is within `EPSILON = 1e-3`.
  - `GetReachableCurrenciesUseCase` — builds a UnionFind from edges and returns `componentOf(source) - source`.
  - `ConversionFacade` — the single collaborator the ViewModel knows about. Hides the three use cases behind a Facade (GoF).

- **UI** (`ui/conversion/`, `ui/theme/`):
  - `MainActivity` (single-activity, edge-to-edge) hosts `ConversionScreen`.
  - `ConversionViewModel` exposes a `StateFlow<ConversionUiState>` (sealed: Loading / Error / Ready) and a single `onEvent(ConversionEvent)` sink (sealed events).
  - `ConversionScreen` is Compose Material 3: input card, algorithm picker (filter chips), result card with a path breadcrumb, arbitrage card (tertiary container), and a reachable-currencies card. Theme supports dynamic colour on API 31+ with hand-tuned light/dark fallback palettes.

### Patterns used (with file pointers)

- **Strategy** + **Factory** + **Template Method** + **Chain of Responsibility** — `core/algorithm/`.
- **Builder** — `CurrencyGraph.Builder`.
- **Decorator** — `CachingExchangeRatesRepository`.
- **Adapter** — `data/remote/dto/ExchangeRatesDto.kt` (`toDomain()`).
- **Facade** — `domain/usecase/ConversionFacade.kt`.
- **Singleton** — Hilt `@Singleton` scopes throughout `di/`.
- **Observer** — Coroutines `StateFlow` / `Flow` in repository and ViewModel.
- **State** — `ConversionUiState` sealed interface in `ui/conversion/`.

### DI wiring (Hilt)

- `AlgorithmModule` — multi-binds every `PathFindingStrategy` into `Map<StrategyKind, PathFindingStrategy>` via `@IntoMap` + custom `@StrategyKey` `@MapKey`. Adding a sixth algorithm is one line in this module.
- `DataModule` — binds `ExchangeRatesDataSource` to embedded by default; swap to `RemoteExchangeRatesDataSource` is a one-line change.
- `NetworkModule` — Retrofit + Moshi + OkHttp; only exercised when the remote data source is bound.
- `CoroutineModule` — production `DispatcherProvider`.

### What *not* to do

- Don't reintroduce DataBinding or XML layouts; UI is Compose-only.
- Don't switch JSON parsing to reflection — Moshi `@JsonClass(generateAdapter = true)` + KSP keeps reflection out of release builds and is friendlier to R8.
- Don't bury algorithm explanations behind interfaces named "Helper" or split a strategy's logic across multiple files just to "clean it up". The whole-strategy-per-file layout is intentional for studying.
- Don't loosen `Currency.of(...)` validation — invalid codes are caught at the data-layer boundary in `RateDto.toDomain()` and that's the *only* place we want them filtered.
