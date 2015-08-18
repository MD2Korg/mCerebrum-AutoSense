package org.md2k.autosense.devices;

import org.md2k.datakitapi.DataKitApi;
import org.md2k.datakitapi.datatype.DataType;
import org.md2k.datakitapi.source.datasource.DataSource;
import org.md2k.datakitapi.source.datasource.DataSourceBuilder;
import org.md2k.datakitapi.source.datasource.DataSourceClient;
import org.md2k.datakitapi.source.platform.Platform;

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
public class AutoSenseDataSource {
    private String dataSourceType;
    DataSourceClient dataSourceClient;
    DataKitApi mDataKitApi;

    public AutoSenseDataSource(String dataSourceType) {
        this.dataSourceType = dataSourceType;
    }
    public void sendMessage(DataType data) {
        mDataKitApi.insert(dataSourceClient, data);
    }

    public String getDataSourceType() {
        return dataSourceType;
    }
    public DataSourceClient getDataSourceClient(){
        return dataSourceClient;
    }
    public DataSourceBuilder getDataSourceBuilder() {
        DataSourceBuilder dataSourceBuilder = new DataSourceBuilder().setId(null).setType(dataSourceType);
        return dataSourceBuilder;
    }
    public boolean register(DataKitApi dataKitApi, final Platform platform) {
        mDataKitApi = dataKitApi;
        DataSourceBuilder dataSourceBuilder = getDataSourceBuilder();
        dataSourceBuilder = dataSourceBuilder.setPlatform(platform);
        DataSource dataSource = dataSourceBuilder.build();
        dataSourceClient = dataKitApi.register(dataSource).await();
        return true;
    }
}
