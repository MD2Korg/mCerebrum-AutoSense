package org.md2k.autosense.antradio.connection;

import android.content.Context;

import org.md2k.autosense.antradio.ChannelInfo;
import org.md2k.autosense.devices.AutoSensePlatform;
import org.md2k.datakitapi.DataKitAPI;
import org.md2k.datakitapi.datatype.DataTypeDoubleArray;
import org.md2k.datakitapi.exception.DataKitException;
import org.md2k.datakitapi.source.datasource.DataSourceType;

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
public class DataExtractorWrist {

    static final byte NINE_AXIS_ACCL_X_CHANNEL = (byte) 0;
    static final byte NINE_AXIS_ACCL_Y_CHANNEL = (byte) 7;
    static final byte NINE_AXIS_ACCL_Z_CHANNEL = (byte) 1;
    static final byte NINE_AXIS_GYRO_X_CHANNEL = (byte) 2;
    static final byte NINE_AXIS_GYRO_Y_CHANNEL = (byte) 3;
    static final byte NINE_AXIS_GYRO_Z_CHANNEL = (byte) 4;
    static final byte NINE_AXIS_NULL_PACKET_CHANNEL = (byte) 15;
    private static final String TAG = DataExtractorWrist.class.getSimpleName();
    DataKitAPI dataKitAPI;

    DataExtractorWrist(Context context) {
        dataKitAPI = DataKitAPI.getInstance(context);
    }

    private static int[] decodeAutoSenseSamples(byte[] ANTRxMessage) {
        int[] samples = new int[5];
           /* Decode 5 samples of 12 bits each */
        samples[0] = (short) (((((short) ANTRxMessage[1] & 0x00FF) << 4) | (((short) ANTRxMessage[2] & 0x00FF) >>> 4)) & 0x0FFF);
        samples[1] = (short) (((((short) ANTRxMessage[2] & 0x00FF) << 8) | ((short) ANTRxMessage[3] & 0x00FF)) & 0x0FFF);
        samples[2] = (short) (((((short) ANTRxMessage[4] & 0x00FF) << 4) | (((short) ANTRxMessage[5] & 0x00FF) >>> 4)) & 0x0FFF);
        samples[3] = (short) (((((short) ANTRxMessage[5] & 0x00FF) << 8) | ((short) ANTRxMessage[6] & 0x00FF)) & 0x0FFF);
        samples[4] = (short) (((((short) ANTRxMessage[7] & 0x00FF) << 4) | (((short) ANTRxMessage[8] & 0x00FF) >>> 4)) & 0x0FFF);

        return samples;
    }

    public int[] getSample(byte[] ANTRxMessage) {
        int[] samples = decodeAutoSenseSamples(ANTRxMessage);
        return convertSamplesToTwosComplement(samples);
    }

    private int[] convertSamplesToTwosComplement(int[] samples) {
        for (int i = 0; i < samples.length; i++) {
            samples[i] = TwosComplement(samples[i], 12);
        }
        return samples;
    }

    public int TwosComplement(int x, int nBits) {
        int msb = x >> (nBits - 1);
        if (msb == 1) {
            return -1 * ((~x & ((1 << nBits) - 1)) + 1);
        } else {
            return x;
        }
    }

    private long[] correctTimeStamp(AutoSensePlatform autoSensePlatform, String dataSourceType, long timestamp) {
        long diff = (long) (1000.0 / autoSensePlatform.getAutoSenseDataSource(dataSourceType).getFrequency());
        long timestamps[] = new long[5];
        for (int i = 0; i < 5; i++)
            timestamps[i] = timestamp - (4 - i) * diff;
        return timestamps;
    }

    public void prepareAndSendToDataKit(Context context, ChannelInfo newInfo) throws DataKitException {

        int samples[] = getSample(newInfo.broadcastData);
        String dataSourceType = getDataSourceType(newInfo.broadcastData);

        double conversionFactor = 1.0;
        if (isAccelerometerData(dataSourceType)) {
            conversionFactor = 1.0 / 1024;

        } else if (isGyroscopeData(dataSourceType)) {
            conversionFactor = 250.0 / 2048;
        }

        if (dataSourceType != null) {
            long timestamps[] = correctTimeStamp(newInfo.autoSensePlatform, dataSourceType, newInfo.timestamp);
            for (int i = 0; i < samples.length; i++) {
                dataKitAPI.insertHighFrequency(newInfo.autoSensePlatform.getAutoSenseDataSource(dataSourceType).getDataSourceClient(), new DataTypeDoubleArray(timestamps[i], samples[i] * conversionFactor));

                switch (dataSourceType) {
                    case DataSourceType.ACCELEROMETER_X:
                        newInfo.autoSensePlatform.dataQuality.get(0).add(samples[i]);
                        break;
                }

            }
        }
    }

    private boolean isGyroscopeData(String dataSourceType) {
        if (DataSourceType.GYROSCOPE_X.equals(dataSourceType) || DataSourceType.GYROSCOPE_Y.equals(dataSourceType) || DataSourceType.GYROSCOPE_Z.equals(dataSourceType)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isAccelerometerData(String dataSourceType) {
        if (DataSourceType.ACCELEROMETER_X.equals(dataSourceType) || DataSourceType.ACCELEROMETER_Y.equals(dataSourceType) || DataSourceType.ACCELEROMETER_Z.equals(dataSourceType)) {
            return true;
        } else {
            return false;
        }
    }

    public String getDataSourceType(byte[] ANTRxMessage) {
        byte mSequenceNumber = (byte) (ANTRxMessage[8] & 0x0F);
        switch (mSequenceNumber) {
            case NINE_AXIS_ACCL_X_CHANNEL:
                return DataSourceType.ACCELEROMETER_X;
            case NINE_AXIS_ACCL_Y_CHANNEL:
                return DataSourceType.ACCELEROMETER_Y;
            case NINE_AXIS_ACCL_Z_CHANNEL:
                return DataSourceType.ACCELEROMETER_Z;
            case NINE_AXIS_GYRO_X_CHANNEL:
                return DataSourceType.GYROSCOPE_X;
            case NINE_AXIS_GYRO_Y_CHANNEL:
                return DataSourceType.GYROSCOPE_Y;
            case NINE_AXIS_GYRO_Z_CHANNEL:
                return DataSourceType.GYROSCOPE_Z;
            default:
                return null;
        }
    }
}
