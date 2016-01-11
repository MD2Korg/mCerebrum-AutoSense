package org.md2k.autosense.devices;

import android.content.Context;

import org.md2k.datakitapi.datatype.DataTypeFloatArray;
import org.md2k.datakitapi.datatype.DataTypeInt;
import org.md2k.datakitapi.source.METADATA;
import org.md2k.datakitapi.source.datasource.DataSourceBuilder;
import org.md2k.datakitapi.source.datasource.DataSourceClient;
import org.md2k.datakitapi.source.platform.Platform;
import org.md2k.utilities.datakit.DataKitHandler;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

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
public class AutoSenseDataSource implements Serializable{
    private String dataSourceType;
    private double frequency;
    private DataSourceClient dataSourceClient;
    private Context context;
    int minValue;
    int maxValue;

    public AutoSenseDataSource(Context context, String dataSourceType, double frequency,int minValue,int maxValue) {
        this.context = context;
        this.dataSourceType = dataSourceType;
        this.frequency=frequency;
        this.maxValue=maxValue;
        this.minValue=minValue;
    }

    public double getFrequency() {
        return frequency;
    }

    public boolean equals(String dataSourceType){
        return this.dataSourceType.equals(dataSourceType);
    }

    public String getDataSourceType() {
        return dataSourceType;
    }

    public DataSourceClient getDataSourceClient() {
        return dataSourceClient;
    }

    public DataSourceBuilder createDatSourceBuilder(Platform platform){
        DataSourceBuilder dataSourceBuilder=new DataSourceBuilder();
        dataSourceBuilder=dataSourceBuilder.setId(null).setType(dataSourceType).setMetadata(METADATA.FREQUENCY,String.valueOf(frequency)).setPlatform(platform);
        dataSourceBuilder = dataSourceBuilder.setDataDescriptors(createDataDescriptors());
        dataSourceBuilder = dataSourceBuilder.setMetadata(METADATA.FREQUENCY, String.valueOf(frequency));
        dataSourceBuilder = dataSourceBuilder.setMetadata(METADATA.NAME, dataSourceType.toLowerCase());
        dataSourceBuilder = dataSourceBuilder.setMetadata(METADATA.UNIT, "");
        dataSourceBuilder = dataSourceBuilder.setMetadata(METADATA.DESCRIPTION, dataSourceType.toLowerCase());
        dataSourceBuilder = dataSourceBuilder.setMetadata(METADATA.DATA_TYPE, DataTypeInt.class.getName());
        return dataSourceBuilder;
    }

    public boolean register(Platform platform) {
        DataSourceBuilder dataSourceBuilder=createDatSourceBuilder(platform);
        dataSourceClient = DataKitHandler.getInstance(context).register(dataSourceBuilder);
        return dataSourceClient != null;
    }
    ArrayList<HashMap<String, String>>  createDataDescriptors() {
        ArrayList<HashMap<String, String>> dataDescriptors = new ArrayList<>();
        HashMap<String, String> dataDescriptor = new HashMap<>();
        dataDescriptor.put(METADATA.NAME, dataSourceType.toLowerCase());
        dataDescriptor.put(METADATA.MIN_VALUE, String.valueOf(minValue));
        dataDescriptor.put(METADATA.MAX_VALUE, String.valueOf(maxValue));
        dataDescriptor.put(METADATA.FREQUENCY, String.valueOf(frequency));
        dataDescriptor.put(METADATA.DESCRIPTION, dataSourceType.toLowerCase());
        dataDescriptor.put(METADATA.DATA_TYPE, int.class.getName());
        dataDescriptors.add(dataDescriptor);
        return dataDescriptors;
    }

}
