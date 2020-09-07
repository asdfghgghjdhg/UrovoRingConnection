package com.google.asdfghgghjdhg.urovoringconnection;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SettingsActivity extends AppCompatActivity {

    public class BluetoothReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action != null) {
                if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    SettingsActivity.this.settingsFragment.updatePreferences();
                }
            }
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            SwitchPreference switchPref = findPreference(getString(R.string.broadcast_messages_key));
            if (switchPref != null) {
                switchPref.setOnPreferenceChangeListener(this);
            }
            EditTextPreference editPref = findPreference(getString(R.string.intent_action_key));
            if (editPref != null) {
                editPref.setOnPreferenceChangeListener(this);
            }
            editPref = findPreference(getString(R.string.intent_barcode_data_key));
            if (editPref != null) {
                editPref.setOnPreferenceChangeListener(this);
            }

            updatePreferences();
        }

        public void updatePreferences() {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            Preference pref;

            SwitchPreference switchPref = findPreference(getString(R.string.broadcast_messages_key));
            if (switchPref != null) {
                pref = findPreference(getString(R.string.intent_action_key));
                if (pref != null) { pref.setEnabled(switchPref.isChecked()); }
                pref = findPreference(getString(R.string.intent_barcode_data_key));
                if (pref != null) { pref.setEnabled(switchPref.isChecked()); }
            }

            EditTextPreference editPref = findPreference(getString(R.string.intent_action_key));
            if (editPref != null) {
                if (editPref.getText() == null || editPref.getText().isEmpty()) {
                    editPref.setSummary(R.string.intent_action_summary);
                } else {
                    editPref.setSummary(editPref.getText());
                }
            }
            editPref = findPreference(getString(R.string.intent_barcode_data_key));
            if (editPref != null) {
                if (editPref.getText() == null || editPref.getText().isEmpty()) {
                    editPref.setSummary(R.string.intent_data_summary);
                } else {
                    editPref.setSummary(editPref.getText());
                }
            }

            if (adapter == null) {
                pref = findPreference(getString(R.string.no_bluetooth_adapter_key));
                if (pref != null) { pref.setVisible(true); }
                pref = findPreference(getString(R.string.bluetooth_off_key));
                if (pref != null) { pref.setVisible(false); }
                pref = findPreference(getString(R.string.bluetooth_on_key));
                if (pref != null) { pref.setEnabled(false); }
                pref = findPreference(getString(R.string.connect_to_device_key));
                if (pref != null) { pref.setEnabled(false); }
                return;
            }

            pref = findPreference(getString(R.string.no_bluetooth_adapter_key));
            if (pref != null) { pref.setVisible(false); }

            pref = findPreference(getString(R.string.bluetooth_on_key));
            if (pref != null) { pref.setEnabled(true); }

            if (!adapter.isEnabled()) {
                pref = findPreference(getString(R.string.no_bluetooth_adapter_key));
                if (pref != null) { pref.setVisible(false); }
                pref = findPreference(getString(R.string.bluetooth_off_key));
                if (pref != null) { pref.setVisible(true); }
                pref = findPreference(getString(R.string.connect_to_device_key));
                if (pref != null) { pref.setEnabled(false); }
                return;
            }

            pref = findPreference(getString(R.string.bluetooth_off_key));
            if (pref != null) { pref.setVisible(false); }

            ListPreference devicesPref = findPreference(getString(R.string.connect_to_device_key));
            if (devicesPref != null) {
                devicesPref.setEnabled(true);

                Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
                List<String> names = new ArrayList<>();
                List<String> addresses = new ArrayList<>();
                for (BluetoothDevice device : pairedDevices) {
                    names.add(device.getName());
                    addresses.add(device.getAddress());
                }
                devicesPref.setEntries(names.toArray(new CharSequence[names.size()]));
                devicesPref.setEntryValues(addresses.toArray(new CharSequence[addresses.size()]));
            }
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (preference.getKey().equals(getString(R.string.broadcast_messages_key))) {
                SwitchPreference switchPref = findPreference(getString(R.string.broadcast_messages_key));
                if (switchPref != null) {
                    Preference pref = findPreference(getString(R.string.intent_action_key));
                    if (pref != null) { pref.setEnabled((boolean)newValue); }
                    pref = findPreference(getString(R.string.intent_barcode_data_key));
                    if (pref != null) { pref.setEnabled((boolean)newValue); }
                }
                return true;
            }
            if (preference.getKey().equals(getString(R.string.intent_action_key))) {
                EditTextPreference editPref = findPreference(getString(R.string.intent_action_key));
                if (editPref != null) {
                    if (newValue == null || ((String)newValue).isEmpty()) {
                        editPref.setSummary(R.string.intent_action_summary);
                    } else {
                        editPref.setSummary((String)newValue);
                    }
                }
                return true;
            }
            if (preference.getKey().equals(getString(R.string.intent_barcode_data_key))) {
                EditTextPreference editPref = findPreference(getString(R.string.intent_barcode_data_key));
                if (editPref != null) {
                    if (newValue == null || ((String)newValue).isEmpty()) {
                        editPref.setSummary(R.string.intent_data_summary);
                    } else {
                        editPref.setSummary((String)newValue);
                    }
                }
                return true;
            }

            return false;
        }
    }

    public BluetoothReceiver bluetoothReceiver = new BluetoothReceiver();
    public SettingsFragment settingsFragment = new SettingsFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, settingsFragment)
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (preferences.getBoolean(getString(R.string.service_app_start_key), false)) {
            startConnectionService();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        ServiceManagementPreference pref = settingsFragment.findPreference(getString(R.string.service_management_key));
        if (pref != null) {
            pref.setOnStartServiceClick(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startConnectionService();
                }
            });
            pref.setOnStopServiceClick(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    stopConnectionService();
                }
            });
        }

        registerBluetoothReceiver();
    }

    @Override
    protected void onPause() {
        unregisterBluetoothReceiver();

        super.onPause();
    }

    private void registerBluetoothReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothReceiver, filter);
    }

    private void unregisterBluetoothReceiver() {
        unregisterReceiver(bluetoothReceiver);
    }

    private void startConnectionService() {
        Intent serviceIntent = new Intent(this, ConnectionService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    private void stopConnectionService() {
        Intent serviceIntent = new Intent(this, ConnectionService.class);
        stopService(serviceIntent);
    }
}