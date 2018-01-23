package com.laudien.p1xelfehler.batterywarner.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

import com.laudien.p1xelfehler.batterywarner.helper.NotificationHelper;
import com.laudien.p1xelfehler.batterywarner.helper.RootHelper;

/**
 * An IntentService called by the app that toggles power saving mode.
 * It automatically checks if the power saving mode is enabled and toggles it on/off.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class TogglePowerSavingService extends IntentService {
    public TogglePowerSavingService() {
        super(null);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        PowerManager powerManager = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        if (powerManager == null) {
            return;
        }
        try {
            boolean powerSavingEnabled = powerManager.isPowerSaveMode();
            RootHelper.togglePowerSavingMode(!powerSavingEnabled);
        } catch (RootHelper.NotRootedException e) {
            NotificationHelper.showNotification(getApplicationContext(), NotificationHelper.ID_NOT_ROOTED);
        }
    }
}
