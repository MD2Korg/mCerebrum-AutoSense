package org.md2k.autosense.devices;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;

import org.md2k.autosense.Constants;
import org.md2k.autosense.antradio.connection.ServiceAutoSenses;
import org.md2k.autosense.data_quality.DataQuality;
import org.md2k.datakitapi.exception.DataKitException;
import org.md2k.datakitapi.source.METADATA;
import org.md2k.datakitapi.source.platform.Platform;
import org.md2k.datakitapi.source.platform.PlatformBuilder;
import org.md2k.mcerebrum.core.data_format.DATA_QUALITY;

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
    public static final int RESTART_NO_DATA = 60000;
    private static final String TAG = AutoSensePlatform.class.getSimpleName();
    public ArrayList<DataQuality> dataQualities;
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
            int samples[] = new int[dataQualities.size()];
            try {
                for (int i = 0; i < dataQualities.size(); i++) {
                    samples[i] = dataQualities.get(i).getStatus();
                    dataQualities.get(i).insertToDataKit(samples[i]);
                }
            } catch (DataKitException e) {
                LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Constants.INTENT_STOP));
                return;
            }
            if (samples[0] == DATA_QUALITY.BAND_OFF)
                noData += DELAY;
            else noData = 0;
            if (noData >= RESTART_NO_DATA) {
                Intent intent = new Intent(ServiceAutoSenses.INTENT_RESTART);
                intent.putExtra("device_id",deviceId);
                intent.putExtra("platform_id",platformId);
                intent.putExtra("platform_type",platformType);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                noData = 0;
            }
            handler.postDelayed(this, DELAY);
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
        Platform platform = new PlatformBuilder().setId(platformId).setType(platformType).setMetadata(METADATA.DEVICE_ID, deviceId).setMetadata(METADATA.NAME, name).build();
        for (int i = 0; i < autoSenseDataSources.size(); i++) {
            try {
                autoSenseDataSources.get(i).register(platform);
            } catch (DataKitException e) {
                LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Constants.INTENT_STOP));
                return;
            }
        }
        for (int i = 0; i < dataQualities.size(); i++)
            try {
                dataQualities.get(i).register(platform);
            } catch (DataKitException e) {
                LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Constants.INTENT_STOP));
                return;
            }
        handler.removeCallbacks(runnableDataQuality);
        handler.post(runnableDataQuality);
    }

    public void unregister() {
        handler.removeCallbacks(runnableDataQuality);
        for (int i = 0; i < autoSenseDataSources.size(); i++) {
            try {
                autoSenseDataSources.get(i).unregister();
            } catch (DataKitException ignored) {
            }
        }
        for (int i = 0; i < dataQualities.size(); i++)
            try {
                dataQualities.get(i).unregister();
            } catch (DataKitException ignored) {
            }
    }
}
