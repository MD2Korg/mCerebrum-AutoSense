package org.md2k.autosense.devices;

import android.content.Context;

import org.md2k.datakitapi.source.datasource.DataSourceBuilder;
import org.md2k.datakitapi.source.datasource.DataSourceClient;
import org.md2k.utilities.datakit.DataKitHandler;

import java.io.Serializable;

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

    public AutoSenseDataSource(Context context, String dataSourceType, double frequency) {
        this.context = context;
        this.dataSourceType = dataSourceType;
        this.frequency=frequency;
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

    public DataSourceBuilder createDatSourceBuilder(DataSourceBuilder dataSourceBuilder){
        return dataSourceBuilder.setId(null).setType(dataSourceType).setMetadata("frequency",String.valueOf(frequency));
    }

    public boolean register(DataSourceBuilder dataSourceBuilder) {
        dataSourceBuilder=createDatSourceBuilder(dataSourceBuilder);
        dataSourceClient = DataKitHandler.getInstance(context).register(dataSourceBuilder);
        return dataSourceClient != null;
    }
}
