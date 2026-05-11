package com.terraconnect.exchangerates

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Application class — kept deliberately minimal. Hilt does the heavy lifting,
 * and Timber gives us structured logs in debug builds.
 */
@HiltAndroidApp
class ExchangeRatesApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
    }
}
