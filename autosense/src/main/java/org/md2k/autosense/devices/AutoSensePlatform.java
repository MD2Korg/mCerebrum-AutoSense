package org.md2k.autosense.devices;

import org.md2k.datakitapi.DataKitApi;
import org.md2k.datakitapi.source.datasource.DataSourceType;
import org.md2k.datakitapi.source.platform.Platform;
import org.md2k.datakitapi.source.platform.PlatformBuilder;

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
public class AutoSensePlatform {
    private String platformId;
    private String platformType;
    private String location;
    private AutoSenseDataSource autoSenseDataSource;
    public void setLocation(String location){
        this.location=location;
    }
    public AutoSensePlatform(String platformType, String platformId, String location) {
        this.platformType = platformType;
        this.platformId = platformId;
        this.location = location;
        autoSenseDataSource=new AutoSenseDataSource(DataSourceType.AUTOSENSE);
    }

    public String getPlatformId() {
        return platformId;
    }
    public AutoSenseDataSource getAutoSenseDataSource(){
        return autoSenseDataSource;
    }


    public String getPlatformType() {
        return platformType;
    }

    public boolean equals(String platformType, String platformId) {
        if (this.platformType.equals(platformType) && this.platformId.equals(platformId))
            return true;
        return false;
    }
    public String getLocation() {
        return location;
    }
    public Platform getPlatform() {
        Platform platform = new PlatformBuilder().setId(platformId).setType(platformType).setMetadata("location", location).build();
        return platform;
    }
    private DataKitApi mDataKitApi;
    public void register(DataKitApi dataKitApi) {
        mDataKitApi = dataKitApi;
        autoSenseDataSource.register(mDataKitApi, getPlatform());
    }

}
