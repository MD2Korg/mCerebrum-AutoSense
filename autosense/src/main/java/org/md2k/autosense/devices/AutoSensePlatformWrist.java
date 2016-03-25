package org.md2k.autosense.devices;

import android.content.Context;

import org.md2k.autosense.data_quality.DataQualityACL;
import org.md2k.datakitapi.source.datasource.DataSourceType;

import java.util.ArrayList;
import java.util.Arrays;

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
public class AutoSensePlatformWrist extends AutoSensePlatform{
    public ArrayList<DATASOURCE> DATASOURCES=new ArrayList<>(Arrays.asList(
            new DATASOURCE(DataSourceType.ACCELEROMETER_X, "Accelerometer X",16),
            new DATASOURCE(DataSourceType.ACCELEROMETER_Y, "Accelerometer Y", 16),
            new DATASOURCE(DataSourceType.ACCELEROMETER_Z, "Accelerometer Z", 16),
            new DATASOURCE(DataSourceType.GYROSCOPE_X, "Gyroscope X", 16),
            new DATASOURCE(DataSourceType.GYROSCOPE_Y, "Gyroscope Y", 16),
            new DATASOURCE(DataSourceType.GYROSCOPE_Z, "Gyroscope Z", 16)
    ));
    public AutoSensePlatformWrist(Context context, String platformType, String platformId, String deviceId) {
        super(context,platformType,platformId,deviceId, "AutoSense ()");
        this.name=platformId.toLowerCase();
        if("LEFT_WRIST".equals(platformId))
            this.name = "AutoSense (Left Wrist)";
        else if("RIGHT_WRIST".equals(platformId))
            this.name = "AutoSense (Right Wrist)";
        dataQuality = new ArrayList<>();
        dataQuality.add(new DataQualityACL());

        autoSenseDataSources=new ArrayList<>();
        for (DATASOURCE datasource : DATASOURCES)
            autoSenseDataSources.add(new AutoSenseDataSource(context, datasource.dataSourceType,datasource.name,datasource.frequency,-2048,2048));
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
    }

}
