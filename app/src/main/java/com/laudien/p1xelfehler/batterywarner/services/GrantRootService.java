package com.laudien.p1xelfehler.batterywarner.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.Nullable;

import com.laudien.p1xelfehler.batterywarner.helper.NotificationHelper;
import com.laudien.p1xelfehler.batterywarner.helper.RootHelper;
import com.laudien.p1xelfehler.batterywarner.helper.ServiceHelper;

import static com.laudien.p1xelfehler.batterywarner.helper.NotificationHelper.ID_GRANT_ROOT;
import static com.laudien.p1xelfehler.batterywarner.helper.NotificationHelper.ID_NOT_ROOTED;

/**
 * An IntentService called by the app.
 * It asks for root permissions again and triggers the correct notifications depending on the
 * state in the battery file (which enables/disables the charging) and the battery state.
 * If the user does not grant the root permission, it calls the DisableRootFeaturesService.
 * It stops itself after it finished (like every IntentService does!).
 */
public class GrantRootService extends IntentService {
    public GrantRootService() {
        super(null);
    }

    public GrantRootService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        boolean rooted = RootHelper.isRootAvailable();
        NotificationHelper.cancelNotification(this, ID_GRANT_ROOT, ID_NOT_ROOTED);
        if (rooted) { // rooting was allowed now
            Intent batteryStatus = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (batteryStatus != null) {
                boolean isCharging = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, -1) != 0;
                if (!isCharging) { // if not charging make sure that it is not disabled by the app
                    try {
                        RootHelper.isChargingEnabled(GrantRootService.this);
                    } catch (RootHelper.NotRootedException e) { // user disabled root again after allowing it
                        NotificationHelper.showNotification(getApplicationContext(), ID_NOT_ROOTED);
                    } catch (RootHelper.NoBatteryFileFoundException e) {
                        // Should not happen! Is checked before the user can enable the feature!
                    }
                }
            }
        } else { // user is stupid and keeps root disabled -> disable all root features
            startService(new Intent(GrantRootService.this, DisableRootFeaturesService.class));
        }
        ServiceHelper.startService(getApplicationContext());
    }
}
