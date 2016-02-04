package org.md2k.autosense.antradio.connection;

import android.content.Context;

import org.md2k.autosense.antradio.ChannelInfo;
import org.md2k.autosense.devices.AutoSensePlatform;
import org.md2k.datakitapi.DataKitAPI;
import org.md2k.datakitapi.datatype.DataTypeInt;
import org.md2k.datakitapi.source.datasource.DataSourceType;

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
public class DataExtractorChest {

    private static final String TAG = DataExtractorChest.class.getSimpleName();
    /** The ECG channel for AutoSense. */
    static final byte ECG_CHANNEL = (byte) 0;
    /** The ACCELX channel for AutoSense. */
    static final byte ACCELX_CHANNEL = (byte) 1;
    /** The ACCELY channel for AutoSense. */
    static final byte ACCELY_CHANNEL = (byte) 2;
    /** The ACCELZ channel for AutoSense. */
    static final byte ACCELZ_CHANNEL = (byte) 3;
    /** The GSR channel for AutoSense. */
    static final byte GSR_CHANNEL = (byte) 4;
    /** The RIP channel for AutoSense. */
    static final byte RIP_CHANNEL = (byte) 7;
    /** The SKIN, AMBIENCE, BATTERY channels for AutoSense. */
    static final byte MISC_CHANNEL = (byte) 8;

    DataKitAPI dataKitAPI;
    DataExtractorChest(Context context){
        dataKitAPI=DataKitAPI.getInstance(context);
    }

    private int[] decodeAutoSenseSamples(byte[] ANTRxMessage)
    {
        int[] samples = new int[5];
           /* Decode 5 samples of 12 bits each */
        samples[0] = (short)(( (((short)ANTRxMessage[1] & 0x00FF) << 4) | (((short)ANTRxMessage[2] & 0x00FF) >>> 4) ) & 0x0FFF);
        samples[1] = (short)(( (((short)ANTRxMessage[2] & 0x00FF) << 8) | ((short)ANTRxMessage[3] & 0x00FF) ) & 0x0FFF);
        samples[2] = (short)(( (((short)ANTRxMessage[4] & 0x00FF) << 4) | (((short)ANTRxMessage[5] & 0x00FF) >>> 4) ) & 0x0FFF);
        samples[3] = (short)(( (((short)ANTRxMessage[5] & 0x00FF) << 8) | ((short)ANTRxMessage[6] & 0x00FF) ) & 0x0FFF);
        samples[4] = (short)(( (((short)ANTRxMessage[7] & 0x00FF) << 4) | (((short)ANTRxMessage[8] & 0x00FF) >>> 4) ) & 0x0FFF);

        return samples;
    }
    public int[] getSample(byte[] ANTRxMessage){
        return decodeAutoSenseSamples(ANTRxMessage);
    }
    private long[] correctTimeStamp(AutoSensePlatform autoSensePlatform, String dataSourceType, long timestamp){
        long diff=(long)(1000.0/autoSensePlatform.getAutoSenseDataSource(dataSourceType).getFrequency());
        long timestamps[]=new long[5];
        for (int i=0;i<5;i++)
            timestamps[i]=timestamp-(4-i)*diff;
        return timestamps;
    }
    public void prepareAndSendToDataKit(Context context, ChannelInfo newInfo){
        int samples[]= getSample(newInfo.broadcastData);
        String dataSourceType= getDataSourceType(newInfo.broadcastData);

        if(dataSourceType!=null){
            if(dataSourceType.equals("BATTERY_SKIN_AMBIENT")){
                dataKitAPI.insert(newInfo.autoSensePlatform.getAutoSenseDataSource(DataSourceType.BATTERY).getDataSourceClient(),new DataTypeInt(newInfo.timestamp,samples[0]));
                dataKitAPI.insert(newInfo.autoSensePlatform.getAutoSenseDataSource(DataSourceType.SKIN_TEMPERATURE).getDataSourceClient(),new DataTypeInt(newInfo.timestamp,samples[1]));
                dataKitAPI.insert(newInfo.autoSensePlatform.getAutoSenseDataSource(DataSourceType.AMBIENT_TEMPERATURE).getDataSourceClient(),new DataTypeInt(newInfo.timestamp,samples[2]));
            }
            else{
                long timestamps[]=correctTimeStamp(newInfo.autoSensePlatform,dataSourceType,newInfo.timestamp);
                for(int i=0;i<5;i++)
                    dataKitAPI.insert(newInfo.autoSensePlatform.getAutoSenseDataSource(dataSourceType).getDataSourceClient(),new DataTypeInt(timestamps[i],samples[i]));
            }
        }
    }

    public String getDataSourceType(byte[] ANTRxMessage) {
        final byte[] fixed_send_order = {0, 1, 0, 2, 0, 7, 0, 3, 0, 4, 0, 7, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 8};
        byte mSequenceNumber = (byte) (ANTRxMessage[8] & 0x0F);
        switch (fixed_send_order[mSequenceNumber]) {
            case ECG_CHANNEL:
                return DataSourceType.ECG;
            case ACCELX_CHANNEL:
                return DataSourceType.ACCELEROMETER_X;
            case ACCELY_CHANNEL:
                return DataSourceType.ACCELEROMETER_Y;
            case ACCELZ_CHANNEL:
                return DataSourceType.ACCELEROMETER_Z;
            case GSR_CHANNEL:
                return DataSourceType.GALVANIC_SKIN_RESPONSE;
            case RIP_CHANNEL:
                return DataSourceType.RESPIRATION;
            case MISC_CHANNEL:
                return "BATTERY_SKIN_AMBIENT";
//                    Packetizer.getInstance().addPacket(samples, SAMPLE_NO, 8, timestamp);
                //try {
                //textBATTERY.setText("Battery: " + (float)samples[0]/4096*3*2 + "V");
                //textSKIN.setText("Skin Temperature: " + samples[1]);
                //textAMBIENT.setText("Ambient Temperature: " + samples[2]);
                //Log.d(TAG, "Battery: " + (float)samples[0]/4096*3*2 + "V");
                //Log.d(TAG, "Skin Temperature: " + samples[1]);
                //Log.d(TAG, "Ambient Temperature: " + samples[2]);
                //}
                //catch(Exception e)
                //{
                //	Log.e(TAG, "antDecodeAutoSense: Caught exception " + e);
                //}
                //Log.d(TAG, "SKIN TEMP: " + samples[1]);
                //Log.d(TAG, "AMBIENT TEMP: " + samples[2]);
                //Log.d(TAG, "BATTERY: " + samples[0]);
            default:
                return null;
        }
    }
}
