package org.md2k.autosense.antradio.connection;

import android.content.Context;

import org.md2k.autosense.antradio.ChannelInfo;
import org.md2k.autosense.devices.AutoSensePlatform;
import org.md2k.datakitapi.DataKitAPI;
import org.md2k.datakitapi.datatype.DataTypeDoubleArray;
import org.md2k.datakitapi.exception.DataKitException;
import org.md2k.datakitapi.source.datasource.DataSourceType;
import org.md2k.utilities.Report.Log;

import java.util.LinkedList;
import java.util.Queue;

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
public class DataExtractorChest {

    /** The ECG channel for AutoSense. */
    static final byte ECG_CHANNEL = (byte) 0;
    /** The ACCELX channel for AutoSense. */
    static final byte ACCELX_CHANNEL = (byte) 1;
    /** The ACCELY channel for AutoSense. */
    static final byte ACCELY_CHANNEL = (byte) 2;
    /** The ACCELZ channel for AutoSense. */
    static final byte ACCELZ_CHANNEL = (byte) 3;
    /** The GSR channel for AutoSense. */
    static final byte RIP_BASELINE_CHANNEL = (byte) 4;
    /** The RIP channel for AutoSense. */
    static final byte RIP_CHANNEL = (byte) 7;
    /** The SKIN, AMBIENCE, BATTERY channels for AutoSense. */
    static final byte MISC_CHANNEL = (byte) 8;
    private static final String TAG = DataExtractorChest.class.getSimpleName();
    DataKitAPI dataKitAPI;

    //Matlab code:  Fs = 64/3; b = fir1(128,0.3*2/Fs,bartlett(129));
    double[] b = new double[]{0, 0.0620, 0.1248, 0.1879, 0.2508, 0.1879, 0.1248, 0.0620, 0};
    Queue ripQ = new LinkedList();

    double ref = 0;
    double mean_ref = 0;
    int mean_ref_count = 0;
    double mean_rip = 0;
    int mean_rip_count = 0;
    double R_36 = 10000;
    double R_37 = 10000;
    double R_40 = 604000;  // for the modified mote
    double G1 = R_40 / (R_36 + R_37);

    double[] convolve1D(double[] in, double[] kernel) {
        double[] out = new double[in.length];

        for (int i = 0; i < kernel.length - 1; ++i) {
            out[i] = 0;
            for (int j = i, k = 0; j >= 0; --j, ++k)
                out[i] += in[j] * kernel[k];
        }

        for (int i = kernel.length - 1; i < in.length; ++i) {
            out[i] = 0;
            for (int j = i, k = 0; k < kernel.length; --j, ++k)
                out[i] += in[j] * kernel[k];
        }

        return out;
    }

    double recover_RIP_rawWithMeasuredREF(double sample) {
        ripQ.add(sample);
        if (ripQ.size() < b.length) return sample;

        Double[] rip = (Double[]) ripQ.toArray(new Double[b.length]);
        double[] ref_synn_noM = new double[b.length];
        double[] RIP_synn_noM = new double[b.length];
        for (int i=0; i<b.length; i++) {
            ref_synn_noM[i] = ref - mean_ref;
            RIP_synn_noM[i] = (rip[i]-mean_rip)/G1;
        }

        double[] ref_synn_noM_LP = convolve1D(ref_synn_noM, b);
        double[] RIP_raw_measuredREF = new double[b.length];

        for (int i=0; i<b.length; i++) {
            RIP_raw_measuredREF[i] = (ref_synn_noM_LP[i] + RIP_synn_noM[i])*G1;
        }
        ripQ.poll();
        return RIP_raw_measuredREF[b.length/2];
    }

    DataExtractorChest(Context context) {
        dataKitAPI = DataKitAPI.getInstance(context);
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

    public int[] getSample(byte[] ANTRxMessage) {
        return decodeAutoSenseSamples(ANTRxMessage);
    }

    private long[] correctTimeStamp(AutoSensePlatform autoSensePlatform, String dataSourceType, long timestamp) {
        long diff=(long)(1000.0/autoSensePlatform.getAutoSenseDataSource(dataSourceType).getFrequency());
        long timestamps[]=new long[5];
        for (int i=0;i<5;i++)
            timestamps[i]=timestamp-(4-i)*diff;
        return timestamps;
    }

    public void prepareAndSendToDataKit(Context context, ChannelInfo newInfo) throws DataKitException {
        int samples[] = getSample(newInfo.broadcastData);
        String dataSourceType = getDataSourceType(newInfo.broadcastData);

        if(dataSourceType!=null){
            if(dataSourceType.equals("BATTERY_SKIN_AMBIENT")){
                dataKitAPI.insertHighFrequency(newInfo.autoSensePlatform.getAutoSenseDataSource(DataSourceType.BATTERY).getDataSourceClient(), new DataTypeDoubleArray(newInfo.timestamp, samples[0]));
//                dataKitAPI.insertHighFrequency(newInfo.autoSensePlatform.getAutoSenseDataSource(DataSourceType.SKIN_TEMPERATURE).getDataSourceClient(), new DataTypeDoubleArray(newInfo.timestamp, samples[1]));
//                dataKitAPI.insertHighFrequency(newInfo.autoSensePlatform.getAutoSenseDataSource(DataSourceType.AMBIENT_TEMPERATURE).getDataSourceClient(), new DataTypeDoubleArray(newInfo.timestamp, samples[2]));
            } else if(dataSourceType.equals(DataSourceType.RESPIRATION_BASELINE)) {
                long timestamps[]=correctTimeStamp(newInfo.autoSensePlatform,dataSourceType,newInfo.timestamp);
                for (int i=0; i<5; i++) {
                    updateRef(samples[i]);
                    dataKitAPI.insertHighFrequency(newInfo.autoSensePlatform.getAutoSenseDataSource(dataSourceType).getDataSourceClient(), new DataTypeDoubleArray(timestamps[i], samples[i]));
                }
            } else{
                long timestamps[]=correctTimeStamp(newInfo.autoSensePlatform,dataSourceType,newInfo.timestamp);
                int modifiedSamples[] = new int[5];
                if (dataSourceType.equals(DataSourceType.RESPIRATION)) {
                    for (int i=0; i<5; i++) {
                        updateRip(samples[i]);
                        modifiedSamples[i] = (int) recover_RIP_rawWithMeasuredREF(samples[i]);
                    }
                }
                for (int i = 0; i < 5; i++) {
                    //TODO: sample correction
                    if (dataSourceType.equals(DataSourceType.RESPIRATION)) {
                        dataKitAPI.insertHighFrequency(newInfo.autoSensePlatform.getAutoSenseDataSource(DataSourceType.RESPIRATION_RAW).getDataSourceClient(), new DataTypeDoubleArray(timestamps[i], samples[i]));
                        dataKitAPI.insertHighFrequency(newInfo.autoSensePlatform.getAutoSenseDataSource(DataSourceType.RESPIRATION).getDataSourceClient(), new DataTypeDoubleArray(timestamps[i], modifiedSamples[i]));
                    } else {
                        dataKitAPI.insertHighFrequency(newInfo.autoSensePlatform.getAutoSenseDataSource(dataSourceType).getDataSourceClient(), new DataTypeDoubleArray(timestamps[i], samples[i]));
                    }
                    switch (dataSourceType) {
                        case DataSourceType.RESPIRATION:
                            newInfo.autoSensePlatform.dataQualities.get(0).add(modifiedSamples[i]);
                            newInfo.autoSensePlatform.dataQualities.get(1).add(modifiedSamples[i]);
                            break;
                        case DataSourceType.ECG:
                            newInfo.autoSensePlatform.dataQualities.get(2).add(samples[i]);
                            break;
                    }
                }
            }
        }
    }

    static int MAX_VAL = 2000000000;
    static int RESTART_VAL = 20000000;

    private void updateRip(int sample) {
        mean_rip = (mean_rip*mean_rip_count+sample)/(mean_rip_count+1);
        mean_rip_count++;
        if (mean_rip_count>MAX_VAL) mean_rip_count = RESTART_VAL;
    }

    private void updateRef(int sample) {
        ref = sample;
        mean_ref = (mean_ref*mean_ref_count+sample)/(mean_ref_count+1);
        mean_ref_count++;
        if (mean_ref_count>MAX_VAL) mean_ref_count = RESTART_VAL;

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
            case RIP_BASELINE_CHANNEL:
                return DataSourceType.RESPIRATION_BASELINE;
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
