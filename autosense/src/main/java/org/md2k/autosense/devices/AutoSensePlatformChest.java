package org.md2k.autosense.devices;

import android.content.Context;

import org.md2k.autosense.data_quality.DataQualityECG;
import org.md2k.autosense.data_quality.DataQualityRIP;
import org.md2k.autosense.data_quality.DataQualityRIPVariance;
import org.md2k.datakitapi.source.datasource.DataSourceType;
import org.md2k.datakitapi.source.platform.PlatformId;

import java.util.ArrayList;
import java.util.Arrays;

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
public class AutoSensePlatformChest extends AutoSensePlatform{
    private static final String TAG = AutoSensePlatformChest.class.getSimpleName();
    public ArrayList<DATASOURCE> DATASOURCES=new ArrayList<>(Arrays.asList(
            new DATASOURCE(DataSourceType.RESPIRATION,"Respiration", 64.0/3),
            new DATASOURCE(DataSourceType.ECG,"ECG",64.0),
            new DATASOURCE(DataSourceType.ACCELEROMETER_X,"Accelerometer X",64.0/6),
            new DATASOURCE(DataSourceType.ACCELEROMETER_Y,"Accelerometer Y",64.0/6),
            new DATASOURCE(DataSourceType.ACCELEROMETER_Z,"Accelerometer Z",64.0/6),
            new DATASOURCE(DataSourceType.RESPIRATION_BASELINE,"Respiration baseline",64.0/6),
            new DATASOURCE(DataSourceType.RESPIRATION_RAW,"Respiration raw",64.0/3),
            new DATASOURCE(DataSourceType.BATTERY,"Battery",6.4/5)
//            new DATASOURCE(DataSourceType.SKIN_TEMPERATURE,"Skin Temperature",6.4/5),
//            new DATASOURCE(DataSourceType.AMBIENT_TEMPERATURE,"Ambient Temperature",6.4/5)
    ));
    public AutoSensePlatformChest(Context context, String platformType, String platformId, String deviceId) {
        super(context,platformType,platformId,deviceId, "AutoSense (Chest)");
        this.platformId= PlatformId.CHEST;
        dataQualities = new ArrayList<>();
        dataQualities.add(new DataQualityRIPVariance(context)); //WHY DOES THIS ORDER MATTER?
        dataQualities.add(new DataQualityRIP(context));
        dataQualities.add(new DataQualityECG(context));

        autoSenseDataSources=new ArrayList<>();
        for (int i=0;i<DATASOURCES.size();i++) {
            DATASOURCE datasource=DATASOURCES.get(i);
            if (datasource.dataSourceType.equals(DataSourceType.RESPIRATION))
                autoSenseDataSources.add(new AutoSenseDataSource(context, datasource.dataSourceType, datasource.name, datasource.frequency, -4096, 4096));
            else
                autoSenseDataSources.add(new AutoSenseDataSource(context, datasource.dataSourceType, datasource.name, datasource.frequency, 0, 4096));
        }
    }
    class DATASOURCE{
        String dataSourceType;
        double frequency;
        String name;
        DATASOURCE(String dataSourceType, String name, double frequency){
            this.dataSourceType=dataSourceType;
            this.frequency=frequency;
            this.name=name;
        }
    };
}
