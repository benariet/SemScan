package org.example.semscan;

import android.app.Application;

import org.example.semscan.utils.PreferencesManager;
import org.example.semscan.utils.Logger;

public class SemScanApplication extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize preferences manager
        PreferencesManager.getInstance(this);

        // Initialize global logger forwarding to server
        Logger.init(this);
    }
}
