package org.md2k.autosense;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.dsi.ant.StoreDownloader;

import org.md2k.autosense.antradio.connection.ServiceAutoSenses;
import org.md2k.autosense.devices.AutoSensePlatforms;
import org.md2k.datakitapi.source.platform.PlatformType;
import org.md2k.utilities.Apps;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
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

public class ActivityAutoSenseSettings extends PreferenceActivity {
    private static final String TAG = ActivityAutoSenseSettings.class.getSimpleName();
    static final int ADD_DEVICE = 1;  // The request code
    AutoSensePlatforms autoSensePlatforms = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createMySharedPreference();
        autoSensePlatforms = new AutoSensePlatforms(ActivityAutoSenseSettings.this);
        setContentView(R.layout.activity_autosense_settings);
        addPreferencesFromResource(R.xml.pref_autosense_general);
        updatePreferenceScreen();
        setCancelButton();
        setSaveButton();
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

    private void createMySharedPreference() {
        Constants.createSharedPreference(getBaseContext());
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
                final Intent intent = new Intent(getBaseContext(), ActivityAutoSensePlatformSettings.class);
                Constants.setSharedPreferencesString("platformId", "");
                Constants.setSharedPreferencesString("platformType", PlatformType.AUTOSENSE_CHEST);
                Constants.setSharedPreferencesString("location", "");
                startActivityForResult(intent, ADD_DEVICE);
                return false;
            }
        });

        preference = findPreference("add_autosense_wrist");
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final Intent intent = new Intent(getBaseContext(), ActivityAutoSensePlatformSettings.class);
                Constants.setSharedPreferencesString("platformId", "");
                Constants.setSharedPreferencesString("platformType", PlatformType.AUTOSENSE_WRIST);
                Constants.setSharedPreferencesString("location", "");
                startActivityForResult(intent, ADD_DEVICE);
                return false;
            }
        });
    }

    private void setupPreferenceScreenAutoSenseConfigured() {
        PreferenceCategory category = (PreferenceCategory) findPreference("autosense_configured");
        category.removeAll();
        // TODO: check ant radio exists
//        ArrayList<AutoSensePlatform> autoSensePlatforms = this.autoSensePlatforms.getAutoSensePlatform(platformType);
        for (int i = 0; i < autoSensePlatforms.size(); i++) {
            Preference preference = new Preference(this);
            String platformType;
            if (autoSensePlatforms.get(i).getPlatformType().equals(PlatformType.AUTOSENSE_CHEST)) {
                platformType = "Chest";
                preference.setIcon(R.drawable.ic_chest_24_white);
            } else {
                platformType = "Wrist";
                preference.setIcon(R.drawable.ic_watch_white_24dp);
            }
            preference.setTitle(platformType + ":" + autoSensePlatforms.get(i).getPlatformId());
            preference.setSummary("Location: " + autoSensePlatforms.get(i).getLocation());
            preference.setKey(autoSensePlatforms.get(i).getPlatformType() + ":" + autoSensePlatforms.get(i).getPlatformId());
            preference.setOnPreferenceClickListener(autoSenseListener());
            category.addPreference(preference);
        }
    }

    private String getPlatformType(String string) {
        List<String> items = Arrays.asList(string.split("\\s*:\\s*"));
        if (items.size() > 0) return items.get(0);
        return "";
    }

    private String getPlatformId(String string) {
        List<String> items = Arrays.asList(string.split("\\s*:\\s*"));
        if (items.size() > 1) return items.get(1);
        return "";
    }

    private Preference.OnPreferenceClickListener autoSenseListener() {
        return new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final String platformType = getPlatformType(preference.getKey());
                final String platformId = getPlatformId(preference.getKey());
                final Intent intent = new Intent(getBaseContext(), ActivityAutoSensePlatformSettings.class);
                Constants.setSharedPreferencesString("platformId", platformId);
                Constants.setSharedPreferencesString("platformType", platformType);
                Constants.setSharedPreferencesString("location", autoSensePlatforms.getAutoSensePlatform(platformType, platformId).getLocation());
                AlertDialog alertDialog = new AlertDialog.Builder(ActivityAutoSenseSettings.this).create();
                alertDialog.setTitle("Delete Selected Device");
                alertDialog.setMessage("Delete Device (" + preference.getTitle() + ")?");
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Delete",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                autoSensePlatforms.deleteAutoSensePlatform(platformType, platformId);
                                updatePreferenceScreen();
                            }
                        });
                alertDialog.show();
                return true;
            }
        };
    }
    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    Intent intent = new Intent(ActivityAutoSenseSettings.this, ServiceAutoSenses.class);
                    stopService(intent);
                    saveConfigurationFile();
                    intent = new Intent(ActivityAutoSenseSettings.this, ServiceAutoSenses.class);
                    startService(intent);
                    finish();
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    Toast.makeText(getBaseContext(), "Configuration file is not saved.", Toast.LENGTH_LONG).show();
                    finish();
                    break;
            }
        }
    };

    private void setSaveButton() {
        final Button button = (Button) findViewById(R.id.button_settings_save);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (Apps.isServiceRunning(ActivityAutoSenseSettings.this, Constants.SERVICE_NAME)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(ActivityAutoSenseSettings.this);
                    builder.setMessage("Save configuration file and restart the AutoSense Service?").setPositiveButton("Yes", dialogClickListener)
                            .setNegativeButton("No", dialogClickListener).show();
                }
                else{
                    saveConfigurationFile();
                    finish();
                }
            }
        });
    }
    void saveConfigurationFile() {
        try {
            autoSensePlatforms.writeDataSourceToFile();
            Toast.makeText(getBaseContext(), "Configuration file is saved.", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "!!!Error:" + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void setCancelButton() {
        final Button button = (Button) findViewById(R.id.button_settings_cancel);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });
    }

    public void setupPreferenceScreenAntRadio() {
        Preference preference = findPreference("version");
        preference.setSummary(autoSensePlatforms.getVersionAntDriver());

        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                StoreDownloader.getAntRadioService(getApplicationContext());
                return true;
            }
        });
        preference = findPreference("antradio_support");
        preference.setEnabled(false);
        if (autoSensePlatforms.hasAntSupport(getBaseContext())) {
            preference.setSummary("Yes");
        } else {
            preference.setSummary("No");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ADD_DEVICE) {
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "onActivityResult(): result ok");
                String platformId = Constants.getSharedPreferenceString("platformId");
                String platformType = Constants.getSharedPreferenceString("platformType");
                String location = Constants.getSharedPreferenceString("location");
                if(autoSensePlatforms.getAutoSensePlatform(platformType,platformId)==null)
                    autoSensePlatforms.add(platformType, platformId, location);
                else autoSensePlatforms.getAutoSensePlatform(platformType,platformId).setLocation(location);

                Log.d(TAG, platformId + " " + platformType + " " + location);

                updatePreferenceScreen();

            }
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    void startService(){
        Intent intent = new Intent(ActivityAutoSenseSettings.this, ServiceAutoSenses.class);
        startService(intent);
    }

}
