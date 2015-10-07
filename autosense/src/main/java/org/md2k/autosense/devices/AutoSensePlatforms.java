package org.md2k.autosense.devices;

import android.content.Context;

import com.dsi.ant.AntLibVersionInfo;
import com.dsi.ant.AntSupportChecker;

import org.md2k.autosense.Constants;
import org.md2k.datakitapi.source.datasource.DataSource;
import org.md2k.datakitapi.source.datasource.DataSourceBuilder;
import org.md2k.datakitapi.source.platform.Platform;
import org.md2k.datakitapi.source.platform.PlatformBuilder;
import org.md2k.datakitapi.source.platform.PlatformType;
import org.md2k.utilities.Files;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.EmptyStackException;

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
public class AutoSensePlatforms implements Serializable{
    private static final String TAG = AutoSensePlatforms.class.getSimpleName();
    private ArrayList<AutoSensePlatform> autoSensePlatforms;
    private Context context;
    public int size() {
        return autoSensePlatforms.size();
    }
    public AutoSensePlatform get(int index) {
        return autoSensePlatforms.get(index);
    }

    public AutoSensePlatforms(Context context) {
        this.context = context;
        autoSensePlatforms = new ArrayList<>();
        try {
            readDataSourceFromFile();
        } catch (FileNotFoundException ignored) {
        }
    }

    public String getVersionAntDriver() {
        return AntLibVersionInfo.ANTLIB_VERSION_STRING;
    }

    public boolean hasAntSupport(Context context) {
        return AntSupportChecker.hasAntFeature(context);
    }
    public boolean isExists(String platformType, String platformId) {
        for (int i = 0; i < autoSensePlatforms.size(); i++)
            if (autoSensePlatforms.get(i).equals(platformType, platformId))
                return true;
        return false;
    }

    public void add(String platformType, String platformId, String location) {
        if (!isExists(platformType, platformId)) {
            if(platformType.equals(PlatformType.AUTOSENSE_CHEST))
                autoSensePlatforms.add(new AutoSensePlatformChest(context, platformType, platformId, location));
            else if(platformType.equals(PlatformType.AUTOSENSE_WRIST))
                autoSensePlatforms.add(new AutoSensePlatformWrist(context, platformType, platformId, location));
        }
    }

    public void readDataSourceFromFile() throws FileNotFoundException {
        ArrayList<DataSource> dataSources = Files.readDataSourceFromFile(Constants.DIR_FILENAME);
        for (int i = 0; i < dataSources.size(); i++) {
            String platformId = dataSources.get(i).getPlatform().getId();
            String platformType = dataSources.get(i).getPlatform().getType();
            String location = dataSources.get(i).getPlatform().getMetadata().get("location");
            add(platformType, platformId, location);
        }
    }

    public void deleteAutoSensePlatform(String platformType, String platformId) {
        for (int i = 0; i < autoSensePlatforms.size(); i++)
            if (autoSensePlatforms.get(i).equals(platformType, platformId)) {
                autoSensePlatforms.remove(i);
                return;
            }
    }

    public ArrayList<AutoSensePlatform> getAutoSensePlatform(String platformType) {
        ArrayList<AutoSensePlatform> t_autoSensePlatforms = new ArrayList<>();
        for (int i = 0; i < autoSensePlatforms.size(); i++)
            if (autoSensePlatforms.get(i).getPlatformType().equals(platformType))
                t_autoSensePlatforms.add(autoSensePlatforms.get(i));
        return t_autoSensePlatforms;
    }

    public AutoSensePlatform getAutoSensePlatform(String platformType, String platformId) {
        for (int i = 0; i < autoSensePlatforms.size(); i++)
            if (autoSensePlatforms.get(i).equals(platformType, platformId))
                return autoSensePlatforms.get(i);

        return null;
    }

    public void writeDataSourceToFile() throws IOException {
        ArrayList<DataSource> dataSources = new ArrayList<>();
        if (autoSensePlatforms == null) throw new NullPointerException();
        if (autoSensePlatforms.size() == 0) throw new EmptyStackException();

        for (int i = 0; i < autoSensePlatforms.size(); i++) {
            String platformId = autoSensePlatforms.get(i).getPlatformId();
            String platformType = autoSensePlatforms.get(i).getPlatformType();
            String location = autoSensePlatforms.get(i).getLocation();
            Platform platform = new PlatformBuilder().setId(platformId).setType(platformType).setMetadata("location", location).build();
            ArrayList<AutoSenseDataSource> autoSenseDataSources=autoSensePlatforms.get(i).autoSenseDataSources;
            for (int j = 0; j < autoSenseDataSources.size(); j++) {
                String dataSourceType = autoSenseDataSources.get(i).getDataSourceType();
                DataSource dataSource = new DataSourceBuilder().
                        setPlatform(platform).
                        setType(dataSourceType).build();
                dataSources.add(dataSource);
            }
        }
        Files.writeDataSourceToFile(Constants.DIRECTORY, Constants.FILENAME, dataSources);
    }

    public void register() {
        for (int i = 0; i < autoSensePlatforms.size(); i++) {
            autoSensePlatforms.get(i).register();

        }
    }
}
