package com.zenulabidin.xposter.scheduler;

import android.app.Application;
import android.util.Log;

public class XPosterApplication extends Application {
    private static final String TAG = "XPosterApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "XPoster Application initialized");
        
        // Initialize any global application state here
    }
    
    @Override
    public void onTerminate() {
        super.onTerminate();
        Log.d(TAG, "XPoster Application terminated");
    }
}