package com.google.asdfghgghjdhg.urovoringconnection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

public class MainBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            if (preferences.getBoolean(context.getString(R.string.service_boot_start_key), false)) {
                Intent serviceIntent = new Intent(context, ConnectionService.class);
                ContextCompat.startForegroundService(context, serviceIntent);
            }
        }
    }
}
