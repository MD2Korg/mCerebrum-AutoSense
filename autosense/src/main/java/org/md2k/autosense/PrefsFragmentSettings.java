package org.md2k.autosense;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import org.md2k.autosense.antradio.ChannelInfo;
import org.md2k.autosense.antradio.backgroundscan.ServiceBackgroundScan;
import org.md2k.autosense.devices.AutoSensePlatforms;
import org.md2k.datakitapi.source.platform.PlatformId;
import org.md2k.datakitapi.source.platform.PlatformType;
import org.md2k.mcerebrum.commons.dialog.Dialog;
import org.md2k.mcerebrum.commons.dialog.DialogCallback;

import java.io.IOException;
import java.util.ArrayList;

import es.dmoral.toasty.Toasty;

/*
 * Copyright (c) 2015, The University of Memphis, MD2K Center
 * - Syed Monowar Hossain <monowar.hossain@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

public class PrefsFragmentSettings extends PreferenceFragment {
    private static final String TAG = PrefsFragmentSettings.class.getSimpleName();
    AutoSensePlatforms autoSensePlatforms = null;

    String platformType=PlatformType.AUTOSENSE_CHEST, platformId= PlatformId.CHEST, deviceId;
    private ServiceBackgroundScan.ChannelServiceComm mChannelService;
    private ArrayList<String> availableDevices = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        autoSensePlatforms = new AutoSensePlatforms(getActivity());
        addPreferencesFromResource(R.xml.pref_autosense);
        setupPreferenceScreenConfigured();
        scan();

    }
    void scan(){
        mChannelServiceBound = false;
        doBindChannelService();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        assert v != null;
        ListView lv = (ListView) v.findViewById(android.R.id.list);
        lv.setPadding(0, 0, 0, 0);
        return v;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                getActivity().finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void setupPreferenceScreenAutoSenseAvailable() {
        PreferenceCategory preferenceCategory = (PreferenceCategory) findPreference("key_device_available");
        preferenceCategory.removeAll();
        for(int i=0;i<availableDevices.size();i++){
            Preference preference=new Preference(getActivity());
            preference.setKey(availableDevices.get(i));
            preference.setTitle(availableDevices.get(i));
            final int finalI = i;
            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    autoSensePlatforms.add(platformType,platformId, preference.getKey());
                    availableDevices.remove(finalI);
                    saveConfigurationFile();
                    setupPreferenceScreenConfigured();
                    setupPreferenceScreenAutoSenseAvailable();
                    return true;
                }
            });
            preferenceCategory.addPreference(preference);
        }
    }

    private void setupPreferenceScreenConfigured() {
        PreferenceCategory category = (PreferenceCategory) findPreference("autosense_configured");
        category.removeAll();
        for (int i = 0; i < autoSensePlatforms.size(); i++) {
            Preference preference = new Preference(getActivity());
            preference.setIcon(R.drawable.ic_chest_teal_48dp);

            preference.setTitle(autoSensePlatforms.get(i).getDeviceId());
            preference.setSummary(autoSensePlatforms.get(i).getPlatformId());
            preference.setKey(autoSensePlatforms.get(i).getDeviceId());
            preference.setOnPreferenceClickListener(configuredListener());
            category.addPreference(preference);
        }
    }

    private Preference.OnPreferenceClickListener configuredListener() {
        return new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final String deviceId = preference.getKey();
                Dialog.simple(getActivity(), "Delete Device", "Delete Device (" + preference.getTitle() + ")?", "Delete", "Cancel", new DialogCallback() {
                    @Override
                    public void onSelected(String value) {
                        if ("Delete".equals(value)) {
                            autoSensePlatforms.deleteAutoSensePlatform(platformType, null, deviceId);
                            saveConfigurationFile();
                            setupPreferenceScreenConfigured();
                            setupPreferenceScreenAutoSenseAvailable();
                        }
                    }
                }).show();
                return true;
            }
        };
    }


    boolean saveConfigurationFile() {
        try {
            autoSensePlatforms.writeDataSourceToFile();
            return true;
        } catch (IOException e) {
            Toasty.error(getActivity(), "!!!Error:" + e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
    }

    void addToAvailableList(String deviceId){
        for(int i=0;i<autoSensePlatforms.size();i++)
            if(autoSensePlatforms.get(i).getDeviceId().equals(deviceId)) return;
        for(int i=0;i<availableDevices.size();i++)
            if(availableDevices.get(i).equals(deviceId)) return;
        availableDevices.add(deviceId);
        setupPreferenceScreenAutoSenseAvailable();
    }

    private boolean mChannelServiceBound = false;
    private ServiceConnection mChannelServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder serviceBinder) {
            Log.v(TAG, "mChannelServiceConnection.onServiceConnected...");

            mChannelService = (ServiceBackgroundScan.ChannelServiceComm) serviceBinder;

            mChannelService.setOnChannelChangedListener(new ServiceBackgroundScan.ChannelChangedListener() {
                @Override
                public void onChannelChanged(final ChannelInfo newInfo) {
                    addToAvailableList(getDisplayText(newInfo));
                }

                @Override
                public void onAllowStartScan(final boolean allowStartScan) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                        }
                    });
                }
            });
            mChannelService.setActivityIsRunning(true);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.v(TAG, "mChannelServiceConnection.onServiceDisconnected...");

            mChannelService = null;


            Log.v(TAG, "...mChannelServiceConnection.onServiceDisconnected");
        }
    };
    @Override
    public void onResume() {
        // if null then will be set in onServiceConnected()
        if (mChannelService != null) {
            mChannelService.setActivityIsRunning(true);
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        if (mChannelService != null) {
            mChannelService.setActivityIsRunning(false);
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy...");
        doUnbindChannelService();

            getActivity().stopService(new Intent(getActivity(), ServiceBackgroundScan.class));

        mChannelServiceConnection = null;

        Log.v(TAG, "...onDestroy");

        super.onDestroy();
    }

    private static String getDisplayText(ChannelInfo channelInfo) {
        Log.v(TAG, "getDisplayText...");
        String displayText;

        if (channelInfo.error) {
            displayText = String.format("#%X !:%s", channelInfo.DEVICE_NUMBER,
                    channelInfo.getErrorString());
        } else {
            displayText = String.format("%X", channelInfo.DEVICE_NUMBER);
            Log.d(TAG, "deviceNumber: " + displayText + " number:" + channelInfo.DEVICE_NUMBER);
        }

        Log.v(TAG, "...getDisplayText");

        return displayText;
    }
    private void doBindChannelService() {
        Log.v(TAG, "doBindChannelService...");

        Intent bindIntent = new Intent(getActivity(), ServiceBackgroundScan.class);
        getActivity().startService(bindIntent);
        mChannelServiceBound = getActivity().bindService(bindIntent, mChannelServiceConnection,
                Context.BIND_AUTO_CREATE);

        if (!mChannelServiceBound) // If the bind returns false, run the unbind
            // method to update the GUI
            doUnbindChannelService();

        Log.i(TAG, "  Channel Service binding = " + mChannelServiceBound);

        Log.v(TAG, "...doBindChannelService");
    }

    private void doUnbindChannelService() {
        Log.v(TAG, "doUnbindChannelService...");

        if (mChannelServiceBound) {
            getActivity().unbindService(mChannelServiceConnection);

            mChannelServiceBound = false;
        }

        Log.v(TAG, "...doUnbindChannelService");
    }

}
