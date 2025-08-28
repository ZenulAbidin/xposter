package com.zenulabidin.xposter.scheduler

import android.app.Application
import android.util.Log

class XPosterApplication : Application() {

    companion object {
        private const val TAG = "XPosterApplication"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "XPoster Application initialized")

        // Initialize any global application state here
    }

    override fun onTerminate() {
        super.onTerminate()
        Log.d(TAG, "XPoster Application terminated")
    }
}