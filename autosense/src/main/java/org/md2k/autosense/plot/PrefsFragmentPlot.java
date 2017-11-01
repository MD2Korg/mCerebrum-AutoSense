package org.md2k.autosense.plot;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import org.md2k.autosense.Configuration;
import org.md2k.autosense.R;
import org.md2k.datakitapi.source.METADATA;
import org.md2k.datakitapi.source.datasource.DataSource;
import org.md2k.datakitapi.source.datasource.DataSourceBuilder;
import org.md2k.datakitapi.source.platform.Platform;
import org.md2k.datakitapi.source.platform.PlatformBuilder;

import java.io.FileNotFoundException;
import java.util.ArrayList;

/**
 * Copyright (c) 2015, The University of Memphis, MD2K Center
 * - Syed Monowar Hossain <monowar.hossain@gmail.com>
 * All rights reserved.
 * <p/>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <p/>
 * * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * <p/>
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <p/>
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
public class PrefsFragmentPlot extends PreferenceFragment {
/*
    Preference.OnPreferenceChangeListener onPreferenceChangeListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            phoneSensorDataSources.find(preference.getKey()).setEnabled((Boolean) newValue);
            updatePreferenceScreen();
            return false;
        }
    };
*/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_plot_choice);
        addPreferenceScreenSensors();
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


    private Preference createPreference(final String dataSourceType, final String dataSourceId, final String platformType, final String platformId, final String deviceId) {

        Preference preference = new Preference(getActivity());
        String key = dataSourceType;
        if (dataSourceId != null) key += "_" + dataSourceId;
        preference.setKey(key);
        String title = key;
        title = title.replace("_", " ");
        title = title.substring(0, 1).toUpperCase() + title.substring(1).toLowerCase();
        preference.setTitle(title);
        preference.setSummary(deviceId);
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Platform p=new PlatformBuilder().setType(platformType).setId(platformId).build();
                DataSource ds=new DataSourceBuilder().setType(dataSourceType).setId(dataSourceId).setPlatform(p).build();
                Intent intent = new Intent(getActivity(), ActivityPlot.class);
                intent.putExtra(DataSource.class.getSimpleName(), ds);
                startActivity(intent);
                return false;
            }
        });
        return preference;
    }

    protected void addPreferenceScreenSensors() {
        ArrayList<DataSource> dataSources = null;
        try {
            dataSources = Configuration.getDataSources();
        } catch (FileNotFoundException e) {
            return;
        }
        String dataSourceType, platformId, dataSourceId, platformType, deviceId;
        PreferenceCategory preferenceCategory = (PreferenceCategory) findPreference("dataSourceType");
        preferenceCategory.removeAll();
        for (int i = 0; i < dataSources.size(); i++) {
            platformId = dataSources.get(i).getPlatform().getId();
            dataSourceType = dataSources.get(i).getType();
            dataSourceId = dataSources.get(i).getId();
            platformType = dataSources.get(i).getPlatform().getType();
            deviceId = dataSources.get(i).getPlatform().getMetadata().get(METADATA.DEVICE_ID);
            Preference preference = createPreference(dataSourceType, dataSourceId, platformType, platformId, deviceId);
            preferenceCategory.addPreference(preference);
        }
    }
}
