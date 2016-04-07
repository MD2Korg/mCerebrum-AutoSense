package org.md2k.autosense.data_quality;

import android.content.Context;

import org.md2k.datakitapi.DataKitAPI;
import org.md2k.datakitapi.datatype.DataTypeInt;
import org.md2k.datakitapi.source.datasource.DataSourceBuilder;
import org.md2k.datakitapi.source.datasource.DataSourceClient;
import org.md2k.datakitapi.source.platform.Platform;
import org.md2k.datakitapi.time.DateTime;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Copyright (c) 2015, The University of Memphis, MD2K Center
 * - Syed Monowar Hossain <monowar.hossain@gmail.com>
 * All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <p>
 * * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * <p>
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <p>
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
    abstract public DataSourceBuilder createDatSourceBuilder(Platform platform);
    abstract public ArrayList<HashMap<String, String>> createDataDescriptors();

    DataQuality(Context context) {
        this.context=context;
        samples = new ArrayList<>();
    }

    public abstract int getStatus();

    public void add(int sample) {
        samples.add(sample);
    }
    public void insertToDataKit(int sample){
        DataTypeInt dataTypeInt = new DataTypeInt(DateTime.getDateTime(), sample);
        DataKitAPI.getInstance(context).insert(dataSourceClient, dataTypeInt);
    }
    public boolean register(Platform platform) {
        DataSourceBuilder dataSourceBuilder = createDatSourceBuilder(platform);
        dataSourceClient = DataKitAPI.getInstance(context).register(dataSourceBuilder);
        return dataSourceClient != null;
    }

    public void unregister(){
        if (dataSourceClient != null)
            DataKitAPI.getInstance(context).unregister(dataSourceClient);
    }
}
