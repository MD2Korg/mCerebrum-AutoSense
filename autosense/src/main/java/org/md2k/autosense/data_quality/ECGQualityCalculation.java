package org.md2k.autosense.data_quality;


import org.md2k.utilities.data_format.DATA_QUALITY;

import java.util.Arrays;

/*
 * Copyright (c) 2016, The University of Memphis, MD2K Center
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

public class ECGQualityCalculation {
    // ===========================================================
    private static final int BUFF_LENGTH = 3;
    // ========
    private static final int ACCEPTABLE_OUTLIER_PERCENT = 50;
    private static final int OUTLIER_THRESHOLD_HIGH = 4000;
    private static final int OUTLIER_THRESHOLD_LOW = 20;
    private static final int BAD_SEGMENTS_THRESHOLD = 2;
    private static final int SLOPE_THRESHOLD = 50;
    private static final int RANGE_THRESHOLD = 200;
    private final static int ECK_THRESHOLD_BAND_LOOSE = 47;
    private static final String TAG = "ECKQualityCalculation";
    private static int[] envelBuff;
    private static int envelHead;
    private static int[] classBuff;
    //private final static int ECK_THRESHOLD_BAND_OFF = 20;
    private static int classHead;
    // ========
    private static int large_stuck = 0;
    private static int small_stuck = 0;
    private static int large_flip = 0;
    private static int small_flip = 0;
    private static int max_value = 0;
    private static int min_value = 0;
    private static int segment_class = 0;
    private static int discontinuous = 0;
    // ========
    private static int SEGMENT_GOOD = 0;
    private static int SEGMENT_BAD = 1;
    // ========
    private static int bad_segments = 0;
    private static int amplitude_small = 0;

    public ECGQualityCalculation() {
        envelBuff = new int[BUFF_LENGTH];
        classBuff = new int[BUFF_LENGTH];
        for (int i = 0; i < BUFF_LENGTH; i++) {
            envelBuff[i] = 2 * ECK_THRESHOLD_BAND_LOOSE;
            classBuff[i] = 0;
        }
        envelHead = 0;
        classHead = 0;
    }

    // getting the maximum value
    public static int getMaxValue(int[] array) {
        int maxValue = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] > maxValue) {
                maxValue = array[i];

            }
        }
        return maxValue;
    }

    // getting the miniumum value
    public static int getMinValue(int[] array) {
        int minValue = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] < minValue) {
                minValue = array[i];
            }
        }
        return minValue;
    }

    //getting first difference
    public static int[] getFirstDiff(int[] array) {
        int[] diff = new int[array.length - 1];

        for (int i = 1; i < array.length; i++) {
            diff[i - 1] = Math.abs(array[i] - array[i - 1]);
        }
        return diff;
    }

    //getting median of an array
    public static float getMedian(int[] m) {
        //sort the array
        Arrays.sort(m);
        int middle = m.length / 2;
        if (m.length % 2 == 1) {
            return m[middle];
        } else {
            return (m[middle - 1] + m[middle]) / 2;
        }
    }

    // ===========================================================
    private void classifyDataPoints(int[] data) {
        // ===========================================================
        large_stuck = 0;
        small_stuck = 0;
        large_flip = 0;
        small_flip = 0;
        discontinuous = 0;
        max_value = data[0];
        min_value = data[0];
        for (int i = 0; i < data.length; i++) {
            int im = ((i == 0) ? (data.length - 1) : (i - 1));
            int ip = ((i == data.length - 1) ? (0) : (i + 1));
            boolean stuck = ((data[i] == data[im]) && (data[i] == data[ip]));
            boolean flip = ((Math.abs(data[i] - data[im]) > 4000) || (Math.abs(data[i] - data[ip]) > 4000));
            boolean disc = ((Math.abs(data[i] - data[im]) > 100) || (Math.abs(data[i] - data[ip]) > 100));
            if (disc) discontinuous++;
            if (data[i] > OUTLIER_THRESHOLD_HIGH) {
                if (stuck) large_stuck++;
                if (flip) large_flip++;
            } else if (data[i] < OUTLIER_THRESHOLD_LOW) {
                if (stuck) small_stuck++;
                if (flip) small_flip++;
            } else {
                if (data[i] > max_value) max_value = data[i];
                if (data[i] < min_value) min_value = data[i];
            }
        }
    }

    // ===========================================================
    private void classifySegment(int[] data) {
        // ===========================================================
        int outliers = large_stuck + large_flip + small_stuck + small_flip;
        if (100 * outliers > ACCEPTABLE_OUTLIER_PERCENT * data.length) {
            segment_class = SEGMENT_BAD;
        } else {
            segment_class = SEGMENT_GOOD;
        }
    }

    // ===========================================================
    private void classifyBuffer() {
        // ===========================================================
        bad_segments = 0;
        amplitude_small = 0;
        //amplitude_very_small=0;
        for (int i = 1; i < envelBuff.length; i++) {
            if (classBuff[i] == SEGMENT_BAD) bad_segments++;
            //if(envelBuff[i]<ECK_THRESHOLD_BAND_OFF) amplitude_very_small++;
            if (envelBuff[i] < ECK_THRESHOLD_BAND_LOOSE) amplitude_small++;
        }
    }

    // ===========================================================
    public int currentQuality(int[] data) {
        // ===========================================================
        if (data.length <=10) return DATA_QUALITY.BAND_OFF;
        classifyDataPoints(data);
        /*large_stuck=0;
        small_stuck=0;
		large_flip=0;
		small_flip=0;*/

        classifySegment(data);
        //segment_class=SEGMENT_GOOD;

        classBuff[(classHead++) % classBuff.length] = segment_class;
        envelBuff[(envelHead++) % envelBuff.length] = max_value - min_value;
        classifyBuffer();

        if (bad_segments > BAD_SEGMENTS_THRESHOLD) {
            return DATA_QUALITY.BAND_OFF;
            //}else if(2*amplitude_very_small>envelBuff.length){
            //return DATA_QUALITY_BAND_OFF;
        } else if (2 * amplitude_small > envelBuff.length) {
            return DATA_QUALITY.BAND_LOOSE;
        }
        //return DATA_QUALITY_GOOD;

        //Added by mahbub
        //if this function returns good quality, then it is further checked by the second function developed by Rummana
        //fitler_bad_ecg_v2(int[] data);
        //assume that 3 seconds data is received by this function. now segment them into one second window and returns the status as good if just one window give good quality result

        return fitler_bad_ecg_v2(data);
    }

    private int fitler_bad_ecg_v2(int[] data) {
        //we are supposed to receive data for 1 second = 64 samples
        int max = getMaxValue(data);
        int min = getMinValue(data);
        int range = max - min;
        float median_slope = getMedian(getFirstDiff(data));
        if (median_slope > SLOPE_THRESHOLD || range < RANGE_THRESHOLD)
            return DATA_QUALITY.BAND_LOOSE;
        if (max > OUTLIER_THRESHOLD_HIGH)
            return DATA_QUALITY.BAND_OFF;
        return DATA_QUALITY.GOOD;
    }
}
