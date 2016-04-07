package org.md2k.autosense;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.md2k.autosense.antradio.ChannelInfo;
import org.md2k.autosense.antradio.backgroundscan.ServiceBackgroundScan;
import org.md2k.datakitapi.source.platform.PlatformId;
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

public class PrefsFragmentAutoSensePlatformSettings extends PreferenceFragment {
    public static final String TAG=PrefsFragmentAutoSensePlatformSettings.class.getSimpleName();
    String platformType, platformId= PlatformId.CHEST, deviceId;
    private ServiceBackgroundScan.ChannelServiceComm mChannelService;

    private ArrayList<String> mChannelDisplayList = new ArrayList<>();
    private ArrayAdapter<String> mChannelListAdapter;
    private SparseArray<Integer> mIdChannelListIndexMap = new SparseArray<>();

    private boolean mChannelServiceBound = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(Constants.sharedPreferences==null) Constants.createSharedPreference(getActivity());

        platformType=Constants.getSharedPreferenceString("platformType");
        platformId=Constants.getSharedPreferenceString("platformId");
        deviceId=Constants.getSharedPreferenceString("deviceId");
        addPreferencesFromResource(R.xml.pref_autosense_platform);

        mChannelServiceBound = false;
        mChannelListAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_single_choice,
                android.R.id.text1, mChannelDisplayList);
        ListView listView_channelList = (ListView) getActivity().findViewById(R.id.listView_channelList);
        listView_channelList.setAdapter(mChannelListAdapter);
        listView_channelList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String item = ((TextView) view).getText().toString().trim();
                Preference preference = findPreference("deviceId");
                deviceId = item;
                Constants.setSharedPreferencesString("deviceId", item);
                preference.setSummary(item);
            }
        });
        mChannelDisplayList.clear();
        mIdChannelListIndexMap.clear();
        mChannelListAdapter.notifyDataSetChanged();

        if (!mChannelServiceBound)
            doBindChannelService();

        setupPreferencePlatformId();
        setupPreferenceDeviceId();
        setAddButton();
        setCancelButton();
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v=super.onCreateView(inflater, container,savedInstanceState);
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

    private void setupPreferenceDeviceId(){
        Preference preference = findPreference("deviceId");
        preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Log.d(TAG,preference.getKey()+" "+newValue.toString());
                deviceId=newValue.toString().trim();
                Constants.setSharedPreferencesString(preference.getKey(), newValue.toString().trim());
                preference.setSummary(newValue.toString().trim());
                return false;
            }
        });

    }
    private void setupPreferencePlatformId(){
        ListPreference platformIdPreference= (ListPreference) findPreference("platformId");
        platformIdPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Log.d(TAG, preference.getKey() + ":" + newValue.toString());
                platformId = newValue.toString();
                Constants.setSharedPreferencesString(preference.getKey(), newValue.toString());
                preference.setSummary(newValue.toString());
                return false;
            }
        });

        if(platformType.equals(PlatformType.AUTOSENSE_CHEST)) {
            getActivity().setTitle("AutoSenseChest Settings");
            platformIdPreference.setEntries(R.array.chest_entries);
            platformIdPreference.setEntryValues(R.array.chest_entries);
            platformIdPreference.setEnabled(false);
            Constants.setSharedPreferencesString("platformId", "CHEST");
            platformIdPreference.setSummary(PlatformId.CHEST);
            platformId = PlatformId.CHEST;
        }
        else{
            getActivity().setTitle("AutoSenseWrist Settings");
            platformIdPreference.setEntries(R.array.wrist_entries);
            platformIdPreference.setEntryValues(R.array.wrist_entries);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == 1) {
            // Make sure the request was successful
            if (resultCode == getActivity().RESULT_OK) {
                Preference preference=findPreference("deviceId");
//                Log.d(TAG,"platformId="+Constants.getSharedPreferenceString("platformId"));
                preference.setSummary(Constants.getSharedPreferenceString("deviceId"));
                // The user picked a contact.
                // The Intent's data Uri identifies which contact was selected.

                // Do something with the contact here (bigger example below)
            }
        }
    }
    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy...");
        doUnbindChannelService();

        if (getActivity().isFinishing()) {
            getActivity().stopService(new Intent(getActivity(), ServiceBackgroundScan.class));
        }

        mChannelServiceConnection = null;

        Log.v(TAG, "...onDestroy");

        super.onDestroy();
    }

    private ServiceConnection mChannelServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder serviceBinder) {
            Log.v(TAG, "mChannelServiceConnection.onServiceConnected...");

            mChannelService = (ServiceBackgroundScan.ChannelServiceComm) serviceBinder;

            mChannelService.setOnChannelChangedListener(new ServiceBackgroundScan.ChannelChangedListener() {
                @Override
                public void onChannelChanged(final ChannelInfo newInfo) {
                    final Integer index = mIdChannelListIndexMap.get(newInfo.DEVICE_NUMBER);

                    // If found channel info is not in list, add it
                    if (index == null) {
                        addChannelToList(newInfo);
                        getActivity().runOnUiThread(new Runnable() {
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
                    getActivity().runOnUiThread(new Runnable() {
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
            Log.v(TAG, "mChannelServiceConnection.onServiceDisconnected...");

            mChannelService = null;


            Log.v(TAG, "...mChannelServiceConnection.onServiceDisconnected");
        }
    };


    private void refreshList() {
        Log.v(TAG, "refreshList...");

        if (null != mChannelService) {
            ArrayList<ChannelInfo> chInfoList = mChannelService.getCurrentChannelInfoForAllChannels();

            mChannelDisplayList.clear();
            for (ChannelInfo i : chInfoList) {
                addChannelToList(i);
            }
            mChannelListAdapter.notifyDataSetChanged();
        }

        Log.v(TAG, "...refreshList");
    }

    private void addChannelToList(ChannelInfo channelInfo) {
        Log.v(TAG, "addChannelToList...");
        mIdChannelListIndexMap.put(channelInfo.DEVICE_NUMBER, mChannelDisplayList.size());

        Log.v(TAG, "...addChannelToList");
    }

    private static String getDisplayText(ChannelInfo channelInfo){
        Log.v(TAG, "getDisplayText...");
        String displayText;

        if (channelInfo.error) {
            displayText = String.format("#%X !:%s", channelInfo.DEVICE_NUMBER,
                    channelInfo.getErrorString());
        } else {
            displayText = String.format("%X", channelInfo.DEVICE_NUMBER);
            Log.d(TAG,"deviceNumber: "+displayText+" number:"+channelInfo.DEVICE_NUMBER);
        }

        Log.v(TAG, "...getDisplayText");

        return displayText;
    }

    private void setAddButton() {
        final Button button = (Button) getActivity().findViewById(R.id.button_1);
        button.setText("Save");

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (platformId==null || platformId.equals("")) {
                    Toast.makeText(getActivity(), "!!! Placement is missing !!!", Toast.LENGTH_LONG).show();
                } else if (deviceId==null || deviceId.equals(""))
                    Toast.makeText(getActivity(), "!!! Device ID is missing !!!", Toast.LENGTH_LONG).show();
                else {
                    Intent returnIntent = new Intent();
                    getActivity().setResult(getActivity().RESULT_OK, returnIntent);

                    getActivity().finish();
                }
            }
        });
    }

    private void setCancelButton() {
        final Button button = (Button) getActivity().findViewById(R.id.button_2);
        button.setText("Close");

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent returnIntent = new Intent();
                getActivity().setResult(getActivity().RESULT_CANCELED, returnIntent);
                getActivity().finish();
            }
        });
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
