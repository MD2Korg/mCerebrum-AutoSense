package org.md2k.autosense.devices;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;

import org.md2k.autosense.data_quality.DataQuality;
import org.md2k.datakitapi.DataKitAPI;
import org.md2k.datakitapi.datatype.DataTypeIntArray;
import org.md2k.datakitapi.source.METADATA;
import org.md2k.datakitapi.source.datasource.DataSourceBuilder;
import org.md2k.datakitapi.source.datasource.DataSourceClient;
import org.md2k.datakitapi.source.datasource.DataSourceType;
import org.md2k.datakitapi.source.platform.Platform;
import org.md2k.datakitapi.source.platform.PlatformBuilder;
import org.md2k.datakitapi.time.DateTime;
import org.md2k.utilities.Report.Log;
import org.md2k.utilities.data_format.DATA_QUALITY;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

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
public class AutoSensePlatform implements Serializable{
    public static final int DELAY = 5000;
    public static final int RESTART_NO_DATA=30000;
    int noData=0;
    private static final String TAG = AutoSensePlatform.class.getSimpleName();
    public ArrayList<DataQuality> dataQuality;
    protected String platformId;
    protected String platformType;
    protected String deviceId;
    protected Context context;
    protected String name;
    protected ArrayList<AutoSenseDataSource> autoSenseDataSources;
    Handler handler;
    DataSourceClient dataSourceClient;
    Runnable getDataQuality = new Runnable() {
        @Override
        public void run() {
            int samples[] = new int[dataQuality.size()];
            for (int i = 0; i < dataQuality.size(); i++) {
                samples[i] = dataQuality.get(i).getStatus();
                Log.d(TAG, platformType + " status[" + i + "]=" + samples[i]);
            }
            if(samples[0]== DATA_QUALITY.BAND_OFF)
                noData+=DELAY;
            else noData=0;
            if(noData>=RESTART_NO_DATA){
                Intent intent=new Intent("restart");
                intent.putExtra(AutoSensePlatform.class.getSimpleName(), AutoSensePlatform.this);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                noData=0;
            }

            DataTypeIntArray dataTypeIntArray = new DataTypeIntArray(DateTime.getDateTime(), samples);
            DataKitAPI.getInstance(context).insert(dataSourceClient, dataTypeIntArray);
            if(samples.length==1)
                Log.d(TAG, "dataQuality...size=" + samples.length+" values=["+samples[0]+"]");
            else
                Log.d(TAG, "dataQuality...size=" + samples.length+" values=["+samples[0]+" "+samples[1]+"]");

            handler.postDelayed(getDataQuality, DELAY);
        }
    };
    public AutoSensePlatform(Context context, String platformType, String platformId, String deviceId, String name) {
        this.context=context;
        this.platformType = platformType;
        this.platformId = platformId;
        this.deviceId=deviceId;
        this.name=name;
        handler = new Handler();
    }

    public String getPlatformId() {
        return platformId;
    }
    public AutoSenseDataSource getAutoSenseDataSource(String dataSourceType){
        for(int i=0;i<autoSenseDataSources.size();i++)
            if(autoSenseDataSources.get(i).equals(dataSourceType))
                return autoSenseDataSources.get(i);
        return null;
    }
    public String getPlatformType() {
        return platformType;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public boolean equals(String platformType, String platformId, String deviceId){
        if(platformType!=null && !this.platformType.equals(platformType)) return false;
        if(platformId!=null && !this.platformId.equals(platformId)) return false;
        if(deviceId!=null && !this.deviceId.equals(deviceId)) return false;
        return true;
    }

    public void register() {
        Platform platform = new PlatformBuilder().setId(platformId).setType(platformType).setMetadata(METADATA.DEVICE_ID, deviceId).setMetadata(METADATA.NAME, name).build();
        for(int i=0;i<autoSenseDataSources.size();i++) {
            autoSenseDataSources.get(i).register(platform);
        }
        registerStatus(platform);
        handler.post(getDataQuality);
    }

    public void unregister() {
        for (int i = 0; i < autoSenseDataSources.size(); i++) {
            autoSenseDataSources.get(i).unregister();
        }
        handler.removeCallbacks(getDataQuality);
        if (dataSourceClient != null)
            DataKitAPI.getInstance(context).unregister(dataSourceClient);
    }

    public DataSourceBuilder createDatSourceBuilder(Platform platform) {
        DataSourceBuilder dataSourceBuilder = new DataSourceBuilder();
        dataSourceBuilder = dataSourceBuilder.setId(null).setType(DataSourceType.STATUS).setPlatform(platform);
        dataSourceBuilder = dataSourceBuilder.setDataDescriptors(createDataDescriptors());
        dataSourceBuilder = dataSourceBuilder.setMetadata(METADATA.FREQUENCY, String.valueOf(String.valueOf(1.0 / (DELAY / 1000.0))) + " Hz");
        dataSourceBuilder = dataSourceBuilder.setMetadata(METADATA.NAME, name);
        dataSourceBuilder = dataSourceBuilder.setMetadata(METADATA.UNIT, "");
        dataSourceBuilder = dataSourceBuilder.setMetadata(METADATA.DESCRIPTION, "");
        dataSourceBuilder = dataSourceBuilder.setMetadata(METADATA.DATA_TYPE, DataTypeIntArray.class.getName());
        return dataSourceBuilder;
    }

    ArrayList<HashMap<String, String>> createDataDescriptors() {
        ArrayList<HashMap<String, String>> dataDescriptors = new ArrayList<>();
        HashMap<String, String> dataDescriptor = new HashMap<>();
        dataDescriptor.put(METADATA.NAME, "Status");
        dataDescriptor.put(METADATA.MIN_VALUE, String.valueOf(-4));
        dataDescriptor.put(METADATA.MAX_VALUE, String.valueOf(0));
        dataDescriptor.put(METADATA.FREQUENCY, String.valueOf(String.valueOf(1.0 / (DELAY / 1000))) + " Hz");
        dataDescriptor.put(METADATA.DESCRIPTION, "Connection Status");
        dataDescriptor.put(METADATA.DATA_TYPE, int.class.getName());
        dataDescriptors.add(dataDescriptor);
        return dataDescriptors;
    }

    public boolean registerStatus(Platform platform) {
        DataSourceBuilder dataSourceBuilder = createDatSourceBuilder(platform);
        dataSourceClient = DataKitAPI.getInstance(context).register(dataSourceBuilder);
        return dataSourceClient != null;
    }
}
