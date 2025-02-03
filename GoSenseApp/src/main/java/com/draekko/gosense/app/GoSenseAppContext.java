package com.draekko.gosense.app;

import android.content.Context;

public class GoSenseAppContext {

    private static GoSenseAppContext mInstance;
    private Context context;

    public static GoSenseAppContext getInstance() {
        if (mInstance == null) {
            mInstance = getSync();
        }
        return mInstance;
    }

    private static synchronized GoSenseAppContext getSync() {
        GoSenseAppContext applicationContext;
        synchronized (GoSenseAppContext.class) {
            if (mInstance == null) {
                mInstance = new GoSenseAppContext();
            }
            applicationContext = mInstance;
        }
        return applicationContext;
    }

    void initialize(Context context) {
        this.context = context;
    }

    public Context getApplicationContext() {
        return this.context;
    }

}
