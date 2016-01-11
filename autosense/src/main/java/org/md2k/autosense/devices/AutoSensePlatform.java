package org.md2k.autosense.devices;

import android.content.Context;

import org.md2k.datakitapi.source.METADATA;
import org.md2k.datakitapi.source.datasource.DataSourceBuilder;
import org.md2k.datakitapi.source.platform.Platform;
import org.md2k.datakitapi.source.platform.PlatformBuilder;

import java.io.Serializable;
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
public class AutoSensePlatform implements Serializable{
    protected String platformId;
    protected String platformType;
    protected String deviceId;
    protected Context context;
    protected ArrayList<AutoSenseDataSource> autoSenseDataSources;
    public void setDeviceId(String deviceId){
        this.deviceId=deviceId;
    }
    public AutoSensePlatform(Context context, String platformType, String platformId, String deviceId) {
        this.context=context;
        this.platformType = platformType;
        this.platformId = platformId;
        this.deviceId=deviceId;
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

    public boolean equals(String platformType, String deviceId) {
        return this.platformType.equals(platformType) && this.deviceId.equals(deviceId);
    }
    public void register() {
        for(int i=0;i<autoSenseDataSources.size();i++) {
            Platform platform=new PlatformBuilder().setId(platformId).setType(platformType).setMetadata(METADATA.DEVICE_ID, deviceId).build();
            autoSenseDataSources.get(i).register(platform);
        }
    }
}
