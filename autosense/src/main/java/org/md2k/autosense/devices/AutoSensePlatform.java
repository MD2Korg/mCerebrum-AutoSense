package org.md2k.autosense.devices;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import org.md2k.autosense.antradio.connection.ServiceAutoSense;
import org.md2k.autosense.data_quality.DataQuality;
import org.md2k.datakitapi.exception.DataKitException;
import org.md2k.datakitapi.source.METADATA;
import org.md2k.datakitapi.source.platform.Platform;
import org.md2k.datakitapi.source.platform.PlatformBuilder;
import org.md2k.utilities.Report.Log;
import org.md2k.utilities.data_format.DATA_QUALITY;

import java.io.Serializable;
import java.util.ArrayList;

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
public class AutoSensePlatform implements Serializable {
    public static final int DELAY = 3000;
    public static final int RESTART_NO_DATA = 30000;
    private static final String TAG = AutoSensePlatform.class.getSimpleName();
    public ArrayList<DataQuality> dataQuality;
    protected String platformId;
    protected String platformType;
    protected String deviceId;
    protected Context context;
    protected String name;
    protected ArrayList<AutoSenseDataSource> autoSenseDataSources;
    int noData = 0;
    Handler handler;

    Runnable runnableDataQuality = new Runnable() {
        @Override
        public void run() {
            try {
                int samples[] = new int[dataQuality.size()];
                Log.d(TAG, "runnableDataQuality..1...platformId=" + platformId + " deviceId=" + deviceId + " size=" + dataQuality.size());
                try {
                    for (int i = 0; i < dataQuality.size(); i++) {
                        Log.d(TAG, "runnableDataQuality..2...platformId=" + platformId + " deviceId=" + deviceId + " size=" + dataQuality.size());
                        samples[i] = dataQuality.get(i).getStatus();
                        Log.d(TAG, "runnableDataQuality..3...platformId=" + platformId + " deviceId=" + deviceId + " size=" + dataQuality.size());
                        Log.d(TAG, platformType + " status[" + i + "]=" + samples[i]);

                        dataQuality.get(i).insertToDataKit(samples[i]);
                        Log.d(TAG, "runnableDataQuality..4...platformId=" + platformId + " deviceId=" + deviceId + " size=" + dataQuality.size());
                    }
                } catch (DataKitException e) {
                    Intent intent = new Intent(ServiceAutoSense.INTENT_RESTART);
                    intent.putExtra(AutoSensePlatform.class.getSimpleName(), AutoSensePlatform.this);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                    //TODO: Figure out why this intent does not restart the AutoSense data collection and connection to DataKit
                    //Toast.makeText(context, "Reconnection Error", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
                if (samples[0] == DATA_QUALITY.BAND_OFF)
                    noData += DELAY;
                else noData = 0;
                if (noData >= RESTART_NO_DATA) {
                    Log.d(TAG, "restart ..platformId=" + platformId + " platformType=" + platformType);
                    Intent intent = new Intent(ServiceAutoSense.INTENT_RESTART);
                    intent.putExtra(AutoSensePlatform.class.getSimpleName(), AutoSensePlatform.this);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                    noData = 0;
                }
            } catch (Exception e) {
                Log.e(TAG, "platformId=" + platformId + " platformType=" + platformType + " e=" + e.getMessage());

            }
            handler.postDelayed(runnableDataQuality, DELAY);
        }
    };

    public AutoSensePlatform(Context context, String platformType, String platformId, String deviceId, String name) {
        this.context = context;
        this.platformType = platformType;
        this.platformId = platformId;
        this.deviceId = deviceId;
        this.name = name;
        handler = new Handler();
    }

    public String getPlatformId() {
        return platformId;
    }

    public AutoSenseDataSource getAutoSenseDataSource(String dataSourceType) {
        for (int i = 0; i < autoSenseDataSources.size(); i++)
            if (autoSenseDataSources.get(i).equals(dataSourceType))
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

    public boolean equals(String platformType, String platformId, String deviceId) {
        if (platformType != null && !this.platformType.equals(platformType)) return false;
        if (platformId != null && !this.platformId.equals(platformId)) return false;
        if (deviceId != null && !this.deviceId.equals(deviceId)) return false;
        return true;
    }

    public void register() {
        Log.d(TAG, "register()...platformId=" + platformId + " platformType=" + platformType + " deviceId=" + deviceId);
        Platform platform = new PlatformBuilder().setId(platformId).setType(platformType).setMetadata(METADATA.DEVICE_ID, deviceId).setMetadata(METADATA.NAME, name).build();
        for (int i = 0; i < autoSenseDataSources.size(); i++) {
            try {
                autoSenseDataSources.get(i).register(platform);
            } catch (DataKitException e) {
                //TODO: Restart service?
                Toast.makeText(context, "Registration Error: AutoSense", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        }
        for (int i = 0; i < dataQuality.size(); i++)
            try {
                dataQuality.get(i).register(platform);
            } catch (DataKitException e) {
                //TODO: Restart service?
                Toast.makeText(context, "Registration Error: DataQuality", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        handler.post(runnableDataQuality);
    }

    public void unregister() {
        handler.removeCallbacks(runnableDataQuality);
        for (int i = 0; i < autoSenseDataSources.size(); i++) {
            try {
                autoSenseDataSources.get(i).unregister();
            } catch (DataKitException e) {
//                Toast.makeText(context, "Unable to unregister AutoSense", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        }
        for (int i = 0; i < dataQuality.size(); i++)
            try {
                dataQuality.get(i).unregister();
            } catch (DataKitException e) {
//                Toast.makeText(context, "Unable to unregister DataQuality", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
    }
}
