package org.md2k.autosense.data_quality;

import org.md2k.utilities.data_format.DATA_QUALITY;

import java.util.Arrays;


public class ACLQualityCalculation {


	public final static double MINIMUM_EXPECTED_SAMPLES=3*(0.33)*10.33;  //33% of a 3 second window with 10.33 sampling frequency

	public final static float MAGNITUDE_VARIANCE_THRESHOLD=(float) 1.40;   //this threshold comes from the data we collect by placing the wrist sensor on table. It compares with the wrist accelerometer on-body from participant #11 (smoking pilot study) 

	private static final String TAG = "ACLQualityCalculation";


	// ===========================================================
	public ACLQualityCalculation(){
		// ===========================================================
//		if(Log.DEBUG) Log.d(TAG,"starting");
	}

	// ===========================================================
	public int currentQuality(int[] x, int[] y, int[] z){       //three axes accelerometer data received here.
		int len_x=x.length;
		int len_y=y.length;
		int len_z=z.length;
		
		if(len_x<MINIMUM_EXPECTED_SAMPLES || len_y<MINIMUM_EXPECTED_SAMPLES || len_z<MINIMUM_EXPECTED_SAMPLES)
			return DATA_QUALITY.BAND_OFF;
					
		int minLength=0;

		/* calculate magnitude for each 3 seconds window*/

		//find the minimum length
		if(len_x<=len_y && len_x<=len_z)
			minLength=len_x;
		else if(len_y<=len_x && len_y<=len_z)
			minLength=len_y;
		else if(len_z<=len_x && len_z<=len_y)
			minLength=len_z;

		double []magnitude=new double[minLength];
		for(int i=0;i<minLength;i++){
			magnitude[i]=Math.sqrt(x[i]*x[i]+y[i]*y[i]+z[i]*z[i]);
		}

		if(getStdDev(magnitude)<MAGNITUDE_VARIANCE_THRESHOLD)
			return DATA_QUALITY.BAND_OFF;
		
		return DATA_QUALITY.GOOD;
	}
	
	public int currentQuality(int[] x){       //just receive x axis, in fact it should work with any single axis.
		int len_x=x.length;
		if(len_x==0) return DATA_QUALITY.BAND_OFF;
				
//		if(len_x<MINIMUM_EXPECTED_SAMPLES)
//			return DATA_QUALITY.BAND_OFF;
					
		if(getStdDev(x)<MAGNITUDE_VARIANCE_THRESHOLD)
			return DATA_QUALITY.NOT_WORN;
		
		return DATA_QUALITY.GOOD;
	}


	public static double getMean(double[] data)
	{
		double sum = 0.0;
		for(double a : data)
			sum += a;
		return sum/data.length;
	}
	
	public static double getMean(int[] data)
	{
		double sum = 0.0;
		for(double a : data)
			sum += a;
		return sum/data.length;
	}

	public static double getVariance(int[] data)
	{
		double mean = getMean(data);
		double temp = 0;
		for(double a :data)
			temp += (mean-a)*(mean-a);
		return temp/data.length;
	}

	public static double getStdDev(int[] data)
	{
		return Math.sqrt(getVariance(data));
	}
	
	public static double getVariance(double[] data)
	{
		double mean = getMean(data);
		double temp = 0;
		for(double a :data)
			temp += (mean-a)*(mean-a);
		return temp/data.length;
	}

	public static double getStdDev(double[] data)
	{
		return Math.sqrt(getVariance(data));
	}

	// getting the maximum value
	public static int getMaxValue(int[] array){  
		int maxValue = array[0];  
		for(int i=1;i < array.length;i++){  
			if(array[i] > maxValue){  
				maxValue = array[i];  

			}  
		}  
		return maxValue;  
	}  

	// getting the miniumum value
	public static int getMinValue(int[] array){  
		int minValue = array[0];  
		for(int i=1;i<array.length;i++){  
			if(array[i] < minValue){  
				minValue = array[i];  
			}  
		}  
		return minValue;  
	} 
	//getting first difference
	public static int[] getFirstDiff(int[] array){  		
		int[] diff=new int[array.length-1];

		for(int i=1;i<array.length;i++){  
			diff[i-1]=Math.abs(array[i]-array[i-1]);
		}  
		return diff;  
	} 
	//getting median of an array
	public static float getMedian(int[] m) {
		//sort the array
		Arrays.sort(m);
		int middle = m.length/2;
		if (m.length%2 == 1) {
			return m[middle];
		} else {
			return (m[middle-1] + m[middle]) / 2;
		}
	}
}
