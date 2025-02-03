package com.draekko.gosense.app;

import android.app.Application;

public class GoSenseApplication  extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        GoSenseAppContext.getInstance().initialize(this);
    }

}
