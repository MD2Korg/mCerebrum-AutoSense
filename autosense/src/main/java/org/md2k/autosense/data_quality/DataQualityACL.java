package org.md2k.autosense.data_quality;

import android.content.Context;
import android.util.Log;

import org.md2k.autosense.devices.AutoSensePlatform;
import org.md2k.datakitapi.datatype.DataTypeInt;
import org.md2k.datakitapi.source.METADATA;
import org.md2k.datakitapi.source.datasource.DataSourceBuilder;
import org.md2k.datakitapi.source.datasource.DataSourceType;
import org.md2k.datakitapi.source.platform.Platform;
import org.md2k.mcerebrum.core.data_format.DATA_QUALITY;

import java.util.ArrayList;
import java.util.HashMap;

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
public class DataQualityACL extends DataQuality {
    private static final String TAG = DataQualityACL.class.getSimpleName();
    ACLQualityCalculation aclQualityCalculation;

    public DataQualityACL(Context context) {
        super(context);
        aclQualityCalculation = new ACLQualityCalculation();
    }

    public synchronized int getStatus() {
        try {
            int status;
            int size = samples.size();
            int samps[] = new int[size];
            for (int i = 0; i < size; i++)
                samps[i] = samples.get(i);
            samples.clear();
            status = aclQualityCalculation.currentQuality(samps);
            Log.d("DATA_QUALITY", "ACL_WRIST: " + status);
            return status;
        }catch (Exception e){
            return DATA_QUALITY.GOOD;
        }
    }
    public DataSourceBuilder createDatSourceBuilder(Platform platform) {
        DataSourceBuilder dataSourceBuilder = new DataSourceBuilder();
        dataSourceBuilder = dataSourceBuilder.setId(DataSourceType.ACCELEROMETER).setType(DataSourceType.DATA_QUALITY).setPlatform(platform);
        dataSourceBuilder = dataSourceBuilder.setDataDescriptors(createDataDescriptors());
        dataSourceBuilder = dataSourceBuilder.setMetadata(METADATA.FREQUENCY, String.valueOf(String.valueOf(1.0 / (AutoSensePlatform.DELAY / 1000.0))) + " Hz");
        dataSourceBuilder = dataSourceBuilder.setMetadata(METADATA.NAME, "DataQuality-RIP");
        dataSourceBuilder = dataSourceBuilder.setMetadata(METADATA.UNIT, "");
        dataSourceBuilder = dataSourceBuilder.setMetadata(METADATA.DESCRIPTION, "measures the Data Quality of Accelerometer. Values= "+ DATA_QUALITY.METADATA_STR);      dataSourceBuilder = dataSourceBuilder.setMetadata(METADATA.DATA_TYPE, DataTypeInt.class.getName());
        return dataSourceBuilder;
    }

    public ArrayList<HashMap<String, String>> createDataDescriptors() {
        ArrayList<HashMap<String, String>> dataDescriptors = new ArrayList<>();
        HashMap<String, String> dataDescriptor = new HashMap<>();
        dataDescriptor.put(METADATA.NAME, "DataQuality");
        dataDescriptor.put(METADATA.MIN_VALUE, String.valueOf(0));
        dataDescriptor.put(METADATA.MAX_VALUE, String.valueOf(8));
        dataDescriptor.put(METADATA.FREQUENCY, String.valueOf(String.valueOf(1.0 / (AutoSensePlatform.DELAY / 1000))) + " Hz");
        dataDescriptor.put(METADATA.DESCRIPTION, "measures the Data Quality of Accelerometer. Values= GOOD(0), BAND_OFF(1), NOT_WORN(2), BAND_LOOSE(3), NOISE(4)");
        dataDescriptor.put(METADATA.DATA_TYPE, int.class.getName());
        dataDescriptors.add(dataDescriptor);
        return dataDescriptors;
    }

}
