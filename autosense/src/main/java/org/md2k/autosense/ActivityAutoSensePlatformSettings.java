package org.md2k.autosense;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.md2k.autosense.antradio.ChannelInfo;
import org.md2k.autosense.antradio.backgroundscan.ServiceBackgroundScan;
import org.md2k.datakitapi.source.platform.PlatformType;

import java.util.ArrayList;
/**
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

public class ActivityAutoSensePlatformSettings extends PreferenceActivity {
    public static final String TAG=ActivityAutoSensePlatformSettings.class.getSimpleName();
    String platformType, platformId,location;
    private ServiceBackgroundScan.ChannelServiceComm mChannelService;

    private ArrayList<String> mChannelDisplayList = new ArrayList<>();
    private ArrayAdapter<String> mChannelListAdapter;
    private SparseArray<Integer> mIdChannelListIndexMap = new SparseArray<>();

    private boolean mChannelServiceBound = false;

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        setContentView(R.layout.activity_autosense_platform_settings);
        if(Constants.sharedPreferences==null) Constants.createSharedPreference(getBaseContext());

        platformType=Constants.getSharedPreferenceString("platformType");
        platformId=Constants.getSharedPreferenceString("platformId");
        location=Constants.getSharedPreferenceString("location");
        addPreferencesFromResource(R.xml.pref_autosense_platform);

        mChannelServiceBound = false;
        mChannelListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_single_choice,
                android.R.id.text1, mChannelDisplayList);
        ListView listView_channelList = (ListView) findViewById(R.id.listView_channelList);
        listView_channelList.setAdapter(mChannelListAdapter);
        listView_channelList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String item = ((TextView) view).getText().toString().trim();
                Preference preference = findPreference("platformId");
                platformId = item;
                Constants.setSharedPreferencesString("platformId", item);
                preference.setSummary(item);
            }
        });
        mChannelDisplayList.clear();
        mIdChannelListIndexMap.clear();
        mChannelListAdapter.notifyDataSetChanged();

        if (!mChannelServiceBound)
            doBindChannelService();



        setupPreferenceLocation();
        setupPreferenecePlatformId();
        setAddButton();
        setCancelButton();
        if (getActionBar() != null)
            getActionBar().setDisplayHomeAsUpEnabled(true);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        // if null then will be set in onServiceConnected()
        if (mChannelService != null) {
            mChannelService.setActivityIsRunning(true);
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (mChannelService != null) {
            mChannelService.setActivityIsRunning(false);
        }
        super.onPause();
    }

    private void setupPreferenecePlatformId(){
        Preference preference = findPreference("platformId");
        preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Constants.setSharedPreferencesString(preference.getKey(), newValue.toString().trim());
                preference.setSummary(newValue.toString().trim());
                return false;
            }
        });

    }
    private void setupPreferenceLocation(){
        ListPreference locationPreference= (ListPreference) findPreference("location");
        locationPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Log.d(TAG, preference.getKey() + ":" + newValue.toString());
                location = newValue.toString();
                Constants.setSharedPreferencesString(preference.getKey(), newValue.toString());
                preference.setSummary(newValue.toString());
                return false;
            }
        });

        if(platformType.equals(PlatformType.AUTOSENSE_CHEST)) {
            this.setTitle("Settings -> AutoSense-> Chest");
            locationPreference.setEntries(R.array.chest_entries);
            locationPreference.setEntryValues(R.array.chest_entries);
        }
        else{
            this.setTitle("Settings -> AutoSense -> Wrist");
            locationPreference.setEntries(R.array.wrist_entries);
            locationPreference.setEntryValues(R.array.wrist_entries);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == 1) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                Preference preference=findPreference("platformId");
                Log.d(TAG,"platformId="+Constants.getSharedPreferenceString("platformId"));
                preference.setSummary(Constants.getSharedPreferenceString("platformId"));
                // The user picked a contact.
                // The Intent's data Uri identifies which contact was selected.

                // Do something with the contact here (bigger example below)
            }
        }
    }
    @Override
    public void onDestroy() {
        android.util.Log.v(TAG, "onDestroy...");
        doUnbindChannelService();

        if (isFinishing()) {
            stopService(new Intent(this, ServiceBackgroundScan.class));
        }

        mChannelServiceConnection = null;

        android.util.Log.v(TAG, "...onDestroy");

        super.onDestroy();
    }

    private ServiceConnection mChannelServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder serviceBinder) {
            android.util.Log.v(TAG, "mChannelServiceConnection.onServiceConnected...");

            mChannelService = (ServiceBackgroundScan.ChannelServiceComm) serviceBinder;

            mChannelService.setOnChannelChangedListener(new ServiceBackgroundScan.ChannelChangedListener() {
                @Override
                public void onChannelChanged(final ChannelInfo newInfo) {
                    final Integer index = mIdChannelListIndexMap.get(newInfo.DEVICE_NUMBER);

                    // If found channel info is not in list, add it
                    if (index == null) {
                        addChannelToList(newInfo);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mChannelDisplayList.add(getDisplayText(newInfo));
                                mChannelListAdapter.notifyDataSetChanged();
                            }
                        });
                    }/* else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mChannelDisplayList.set(index.intValue(), getDisplayText(newInfo));
                                    mChannelListAdapter.notifyDataSetChanged();
                                }
                            });
                        }*/
                }

                @Override
                public void onAllowStartScan(final boolean allowStartScan) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                        }
                    });
                }
            });

            refreshList();
            mChannelService.setActivityIsRunning(true);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            android.util.Log.v(TAG, "mChannelServiceConnection.onServiceDisconnected...");

            mChannelService = null;


            android.util.Log.v(TAG, "...mChannelServiceConnection.onServiceDisconnected");
        }
    };


    private void refreshList() {
        android.util.Log.v(TAG, "refreshList...");

        if (null != mChannelService) {
            ArrayList<ChannelInfo> chInfoList = mChannelService.getCurrentChannelInfoForAllChannels();

            mChannelDisplayList.clear();
            for (ChannelInfo i : chInfoList) {
                addChannelToList(i);
            }
            mChannelListAdapter.notifyDataSetChanged();
        }

        android.util.Log.v(TAG, "...refreshList");
    }

    private void addChannelToList(ChannelInfo channelInfo) {
        android.util.Log.v(TAG, "addChannelToList...");
        mIdChannelListIndexMap.put(channelInfo.DEVICE_NUMBER, mChannelDisplayList.size());

        android.util.Log.v(TAG, "...addChannelToList");
    }

    private static String getDisplayText(ChannelInfo channelInfo){
        android.util.Log.v(TAG, "getDisplayText...");
        String displayText;

        if (channelInfo.error) {
            displayText = String.format("#%X !:%s", channelInfo.DEVICE_NUMBER,
                    channelInfo.getErrorString());
        } else {
            displayText = String.format("%X", channelInfo.DEVICE_NUMBER);
            Log.d(TAG,"deviceNumber: "+displayText+" number:"+channelInfo.DEVICE_NUMBER);
        }

        android.util.Log.v(TAG, "...getDisplayText");

        return displayText;
    }

    private void setAddButton() {
        final Button button = (Button) findViewById(R.id.button_settings_platform_add);

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (platformId.equals("")) {
                    Toast.makeText(getBaseContext(), "!!! Device ID is missing !!!", Toast.LENGTH_LONG).show();
                } else if (location.equals(""))
                    Toast.makeText(getBaseContext(), "!!! Location is missing !!!", Toast.LENGTH_LONG).show();
                else {
                    Intent returnIntent = new Intent();
                    setResult(RESULT_OK, returnIntent);

                    finish();
                }
            }
        });
    }

    private void setCancelButton() {
        final Button button = (Button) findViewById(R.id.button_settings_platform_cancel);

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent returnIntent = new Intent();
                setResult(RESULT_CANCELED, returnIntent);
                finish();
            }
        });
    }

    private void doBindChannelService() {
        android.util.Log.v(TAG, "doBindChannelService...");

        Intent bindIntent = new Intent(this, ServiceBackgroundScan.class);
        startService(bindIntent);
        mChannelServiceBound = bindService(bindIntent, mChannelServiceConnection,
                Context.BIND_AUTO_CREATE);

        if (!mChannelServiceBound) // If the bind returns false, run the unbind
            // method to update the GUI
            doUnbindChannelService();

        android.util.Log.i(TAG, "  Channel Service binding = " + mChannelServiceBound);

        android.util.Log.v(TAG, "...doBindChannelService");
    }

    private void doUnbindChannelService() {
        android.util.Log.v(TAG, "doUnbindChannelService...");

        if (mChannelServiceBound) {
            unbindService(mChannelServiceConnection);

            mChannelServiceBound = false;
        }

        android.util.Log.v(TAG, "...doUnbindChannelService");
    }

}
