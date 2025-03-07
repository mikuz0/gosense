package com.draekko.gosense.app;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.draekko.gosense.database.GoSenseDb;

public class GoSenseApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        GoSenseAppContext.getInstance().initialize(this);
    }

}
