package org.md2k.autosense;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.dsi.ant.StoreDownloader;

import org.md2k.autosense.antradio.connection.ServiceAutoSenses;
import org.md2k.autosense.devices.AutoSensePlatforms;
import org.md2k.datakitapi.source.platform.PlatformType;
import org.md2k.utilities.Apps;
import org.md2k.utilities.UI.AlertDialogs;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

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

public class PrefsFragmentAutoSenseSettings extends PreferenceFragment {
    static final int ADD_DEVICE = 1;  // The request code
    private static final String TAG = PrefsFragmentAutoSenseSettings.class.getSimpleName();
    AutoSensePlatforms autoSensePlatforms = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createMySharedPreference();
        autoSensePlatforms = new AutoSensePlatforms(getActivity());
        addPreferencesFromResource(R.xml.pref_autosense_general);
        updatePreferenceScreen();
        setCancelButton();
        setSaveButton();
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

    private void createMySharedPreference() {
        Constants.createSharedPreference(getActivity());
    }

    public void updatePreferenceScreen() {
        setupPreferenceScreenAntRadio();
        setupPreferenceScreenAutoSenseConfigured();
        setupPreferenceScreenAutoSenseAvailable();
    }

    private void setupPreferenceScreenAutoSenseAvailable() {
        Preference preference = findPreference("add_autosense_chest");
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final Intent intent = new Intent(getActivity(), ActivityAutoSensePlatformSettings.class);
                Constants.setSharedPreferencesString("platformId", "");
                Constants.setSharedPreferencesString("platformType", PlatformType.AUTOSENSE_CHEST);
                Constants.setSharedPreferencesString("deviceId", "");
                startActivityForResult(intent, ADD_DEVICE);
                return false;
            }
        });

        preference = findPreference("add_autosense_wrist");
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final Intent intent = new Intent(getActivity(), ActivityAutoSensePlatformSettings.class);
                Constants.setSharedPreferencesString("platformId", "");
                Constants.setSharedPreferencesString("platformType", PlatformType.AUTOSENSE_WRIST);
                Constants.setSharedPreferencesString("deviceId", "");
                startActivityForResult(intent, ADD_DEVICE);
                return false;
            }
        });
    }

    private void setupPreferenceScreenAutoSenseConfigured() {
        PreferenceCategory category = (PreferenceCategory) findPreference("autosense_configured");
        category.removeAll();
        // TODO: check ant radio exists
//        ArrayList<AutoSensePlatform> autoSensePlatforms = this.autoSensePlatforms.find(platformType);
        for (int i = 0; i < autoSensePlatforms.size(); i++) {
            Preference preference = new Preference(getActivity());
            String platformType;
            if (autoSensePlatforms.get(i).getPlatformType().equals(PlatformType.AUTOSENSE_CHEST)) {
                platformType = "Chest";
                preference.setIcon(R.drawable.ic_chest_teal_48dp);
            } else {
                platformType = "Wrist";
                preference.setIcon(R.drawable.ic_watch_teal_48dp);
            }
            preference.setTitle(platformType + ":" + autoSensePlatforms.get(i).getDeviceId());
            preference.setSummary(autoSensePlatforms.get(i).getPlatformId());
            preference.setKey(autoSensePlatforms.get(i).getPlatformType() + ":" + autoSensePlatforms.get(i).getDeviceId());
            preference.setOnPreferenceClickListener(autoSenseListener());
            category.addPreference(preference);
        }
    }

    private String getPlatformType(String string) {
        List<String> items = Arrays.asList(string.split("\\s*:\\s*"));
        if (items.size() > 0) return items.get(0);
        return "";
    }

    private String getDeviceId(String string) {
        List<String> items = Arrays.asList(string.split("\\s*:\\s*"));
        if (items.size() > 1) return items.get(1);
        return "";
    }

    private Preference.OnPreferenceClickListener autoSenseListener() {
        return new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final String platformType = getPlatformType(preference.getKey());
                final String deviceId = getDeviceId(preference.getKey());
                Constants.setSharedPreferencesString("deviceId", deviceId);
                Constants.setSharedPreferencesString("platformType", platformType);
                Constants.setSharedPreferencesString("platformId", autoSensePlatforms.find(platformType, null, deviceId).get(0).getPlatformId());
                AlertDialogs.AlertDialog(getActivity(), "Delete Selected Device", "Delete Device (" + preference.getTitle() + ")?", R.drawable.ic_delete_red_48dp, "Delete", "Cancel", null, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            autoSensePlatforms.deleteAutoSensePlatform(platformType, null, deviceId);
                            updatePreferenceScreen();
                        } else {
                            dialog.dismiss();
                        }
                    }
                });
                return true;
            }
        };
    }

    private void setSaveButton() {
        final Button button = (Button) getActivity().findViewById(R.id.button_1);
        button.setText("Save");
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (Apps.isServiceRunning(getActivity(), Constants.SERVICE_NAME)) {
                    AlertDialogs.AlertDialog(getActivity(), "Save and Restart?", "Save configuration file and restart AutoSense App?", R.drawable.ic_info_teal_48dp, "Yes", "Cancel", null, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case DialogInterface.BUTTON_POSITIVE:
                                    Intent intent = new Intent(getActivity(), ServiceAutoSenses.class);
                                    getActivity().stopService(intent);
                                    if (saveConfigurationFile()) {
                                        intent = new Intent(getActivity(), ServiceAutoSenses.class);
                                        getActivity().startService(intent);
                                        getActivity().finish();
                                    }
                                    break;

                                case DialogInterface.BUTTON_NEGATIVE:
                                    Toast.makeText(getActivity(), "Configuration file is not saved.", Toast.LENGTH_LONG).show();
                                    getActivity().finish();
                                    break;
                            }
                        }
                    });
                } else {
                    if (saveConfigurationFile())
                        getActivity().finish();
                }
            }
        });
    }

    boolean saveConfigurationFile() {
        try {
            autoSensePlatforms.writeDataSourceToFile();
            Toast.makeText(getActivity(), "Configuration file is saved.", Toast.LENGTH_LONG).show();
            return true;
        } catch (IOException e) {
            Toast.makeText(getActivity(), "!!!Error:" + e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
    }

    private void setCancelButton() {
        final Button button = (Button) getActivity().findViewById(R.id.button_2);
        button.setText("Close");
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getActivity().finish();
            }
        });
    }

    public void setupPreferenceScreenAntRadio() {
        Preference preference = findPreference("version");
        preference.setSummary(autoSensePlatforms.getVersionAntDriver());

        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                StoreDownloader.getAntRadioService(getActivity());
                return true;
            }
        });
        preference = findPreference("antradio_support");
        preference.setEnabled(false);
        if (autoSensePlatforms.hasAntSupport(getActivity())) {
            preference.setSummary("Yes");
        } else {
            preference.setSummary("No");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ADD_DEVICE) {
            if (resultCode == getActivity().RESULT_OK) {
                Log.d(TAG, "onActivityResult(): result ok");
                String platformId = Constants.getSharedPreferenceString("platformId");
                String platformType = Constants.getSharedPreferenceString("platformType");
                String deviceId = Constants.getSharedPreferenceString("deviceId");
                Log.d(TAG, "platformType=" + platformType + " platformId=" + platformId + " deviceId=" + deviceId);
                if (autoSensePlatforms.find(platformType, platformId, null).size() != 0)
                    Toast.makeText(getActivity(), "Error: A device is already configured with same placement...", Toast.LENGTH_SHORT).show();
                else if (autoSensePlatforms.find(platformType, null, deviceId).size() != 0)
                    Toast.makeText(getActivity(), "Error: Device is already configured...", Toast.LENGTH_SHORT).show();
                else
                    autoSensePlatforms.add(platformType, platformId, deviceId);
                updatePreferenceScreen();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    void startService() {
        Intent intent = new Intent(getActivity(), ServiceAutoSenses.class);
        getActivity().startService(intent);
    }

}
