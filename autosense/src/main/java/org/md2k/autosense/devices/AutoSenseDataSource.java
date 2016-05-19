package org.md2k.autosense.devices;

import android.content.Context;

import org.md2k.datakitapi.DataKitAPI;
import org.md2k.datakitapi.datatype.DataTypeInt;
import org.md2k.datakitapi.exception.DataKitException;
import org.md2k.datakitapi.source.METADATA;
import org.md2k.datakitapi.source.datasource.DataSourceBuilder;
import org.md2k.datakitapi.source.datasource.DataSourceClient;
import org.md2k.datakitapi.source.platform.Platform;

import java.io.Serializable;
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
public class AutoSenseDataSource implements Serializable{
    int minValue;
    int maxValue;
    String name;
    private String dataSourceType;
    private double frequency;
    private DataSourceClient dataSourceClient;
    private Context context;

    public AutoSenseDataSource(Context context, String dataSourceType, String name, double frequency,int minValue,int maxValue) {
        this.context = context;
        this.dataSourceType = dataSourceType;
        this.frequency=frequency;
        this.maxValue=maxValue;
        this.minValue=minValue;
        this.name=name;
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
        dataSourceBuilder = dataSourceBuilder.setMetadata(METADATA.NAME, name);
        dataSourceBuilder = dataSourceBuilder.setMetadata(METADATA.UNIT, "");
        dataSourceBuilder = dataSourceBuilder.setMetadata(METADATA.DESCRIPTION, dataSourceType.toLowerCase());
        dataSourceBuilder = dataSourceBuilder.setMetadata(METADATA.DATA_TYPE, DataTypeInt.class.getName());
        return dataSourceBuilder;
    }

    public boolean register(Platform platform) throws DataKitException {
        DataSourceBuilder dataSourceBuilder=createDatSourceBuilder(platform);
        dataSourceClient = DataKitAPI.getInstance(context).register(dataSourceBuilder);
        return dataSourceClient != null;
    }

    public void unregister() throws DataKitException {
        if (dataSourceClient != null)
            DataKitAPI.getInstance(context).unregister(dataSourceClient);
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
