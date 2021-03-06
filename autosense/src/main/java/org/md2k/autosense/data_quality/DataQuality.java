package org.md2k.autosense.data_quality;

import android.content.Context;

import org.md2k.datakitapi.DataKitAPI;
import org.md2k.datakitapi.datatype.DataTypeInt;
import org.md2k.datakitapi.datatype.DataTypeIntArray;
import org.md2k.datakitapi.exception.DataKitException;
import org.md2k.datakitapi.source.datasource.DataSourceBuilder;
import org.md2k.datakitapi.source.datasource.DataSourceClient;
import org.md2k.datakitapi.source.platform.Platform;
import org.md2k.datakitapi.time.DateTime;

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
public abstract class DataQuality {
    Context context;
    ArrayList<Integer> samples;
    DataSourceClient dataSourceClient;
    DataSourceBuilder dataSourceBuilder;

    DataQuality(Context context) {
        this.context = context;
        samples = new ArrayList<>();
    }

    abstract public DataSourceBuilder createDatSourceBuilder(Platform platform);

    abstract public ArrayList<HashMap<String, String>> createDataDescriptors();

    public abstract int getStatus();

    public synchronized void add(int sample) {

        samples.add(sample);
    }

    public void insertToDataKit(int sample) throws DataKitException {
        DataTypeInt dataTypeInt = new DataTypeInt(DateTime.getDateTime(), sample);
        DataKitAPI.getInstance(context).insert(dataSourceClient, dataTypeInt);
        int[] intArray=new int[7];
        for(int i=0;i<7;i++) intArray[i]=0;
        int value=dataTypeInt.getSample();
        intArray[value]=3000;
        DataKitAPI.getInstance(context).setSummary(dataSourceClient, new DataTypeIntArray(dataTypeInt.getDateTime(), intArray));
    }

    public boolean register(Platform platform) throws DataKitException {
        dataSourceBuilder = createDatSourceBuilder(platform);
        dataSourceClient = DataKitAPI.getInstance(context).register(dataSourceBuilder);
        return dataSourceClient != null;
    }


    public void unregister() throws DataKitException {
        if (dataSourceClient != null)
            DataKitAPI.getInstance(context).unregister(dataSourceClient);
        dataSourceClient = null;

    }
}
