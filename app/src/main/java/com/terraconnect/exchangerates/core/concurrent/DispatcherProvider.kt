package com.terraconnect.exchangerates.core.concurrent

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Indirection over [kotlinx.coroutines.Dispatchers] so tests can swap in
 * [kotlinx.coroutines.test.StandardTestDispatcher] without touching production code.
 */
interface DispatcherProvider {
    val main: CoroutineDispatcher
    val default: CoroutineDispatcher
    val io: CoroutineDispatcher
    val unconfined: CoroutineDispatcher
}
