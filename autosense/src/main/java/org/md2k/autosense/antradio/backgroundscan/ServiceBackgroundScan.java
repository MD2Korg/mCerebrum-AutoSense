package org.md2k.autosense.antradio.backgroundscan;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

import com.dsi.ant.AntService;
import com.dsi.ant.channel.AntChannel;
import com.dsi.ant.channel.AntChannelProvider;
import com.dsi.ant.channel.Capabilities;
import com.dsi.ant.channel.ChannelNotAvailableException;
import com.dsi.ant.channel.PredefinedNetwork;

import org.md2k.autosense.BuildConfig;
import org.md2k.autosense.antradio.ChannelInfo;
import org.md2k.datakitapi.source.platform.PlatformType;

import java.util.ArrayList;
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

public class ServiceBackgroundScan extends Service
{
    private static final String TAG = "ChannelService";
    private Object mCreateChannel_LOCK = new Object();
    ArrayList<ChannelInfo> mChannelInfoList = new ArrayList<>();
    SharedPreferences sharedPreferences;

    ChannelChangedListener mListener;

    private boolean mAntRadioServiceBound;
    private AntService mAntRadioService = null;
    private AntChannelProvider mAntChannelProvider = null;

    private boolean mAllowAcquireBackgroundScanChannel = false;
    private boolean mBackgroundScanAcquired = false;
    private boolean mBackgroundScanInProgress = false;
    private boolean mActivityIsRunning = false;

    int CHANNEL_PROOF_DEVICE_TYPE = (byte)0x01;
    int CHANNEL_PROOF_TRANSMISSION_TYPE = 0;

    int WILDCARD_SEARCH_DEVICE_NUMBER = 0;

    int CHANNEL_PROOF_PERIOD = 0x04EC;//8070;
    int CHANNEL_PROOF_FREQUENCY = 0x50;

    private ChannelControllerBackgroundScan mBackgroundScanController;
    private ServiceConnection mAntRadioServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            mAntRadioService = new AntService(service);
            try {
                mAntChannelProvider = mAntRadioService.getChannelProvider();
                boolean mChannelAvailable = (getNumberOfChannelsAvailable() > 0);
                boolean legacyInterfaceInUse = mAntChannelProvider.isLegacyInterfaceInUse();
                // If there are channels OR legacy interface in use, allow
                // acquire background scan
                // If no channels available AND legacy interface is not in
// use, disallow acquire background scan
                mAllowAcquireBackgroundScanChannel = mChannelAvailable || legacyInterfaceInUse;
                // Attempting to acquire a background scan channel when connected
                // to ANT Radio Service
                if (mAllowAcquireBackgroundScanChannel) {
                    acquireBackgroundScanningChannel();
                    if(mListener != null) {
                        mListener.onAllowStartScan(!mBackgroundScanInProgress && mBackgroundScanAcquired);
                    }
                }
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ChannelNotAvailableException e) {
                // If channel is not available, do not allow to start scan
                if(mListener != null) {
                    mListener.onAllowStartScan(false);
                }
            }
        }
        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            die("Binder Died");
            mAntChannelProvider = null;
            mAntRadioService = null;
            mListener.onAllowStartScan(false);
            mAllowAcquireBackgroundScanChannel = false;
        }
    };

    public interface ChannelChangedListener
    {
        void onChannelChanged(ChannelInfo newInfo);
        void onAllowStartScan(boolean allowStartScan);
    }
    public class ChannelServiceComm extends Binder
    {
        public void setOnChannelChangedListener(ChannelChangedListener listener)
        {
            mListener = listener;
        }
        public ArrayList<ChannelInfo> getCurrentChannelInfoForAllChannels()
        {
            return mChannelInfoList;
        }
        public void setActivityIsRunning(boolean isRunning) {
            mActivityIsRunning = isRunning;
            if(isRunning && mAllowAcquireBackgroundScanChannel) {
                try {
                    // If activity has started running; try and acquire
                    acquireBackgroundScanningChannel();
                    mListener.onAllowStartScan(!mBackgroundScanInProgress && mBackgroundScanAcquired);
                } catch (ChannelNotAvailableException e) {
                    mListener.onAllowStartScan(false);
                }
            }
        }
    }
    private void closeBackgroundScanChannel()
    {
        if(mBackgroundScanController != null)
        {
            mBackgroundScanController.close();
            mBackgroundScanAcquired = false;
        }
    }

    AntChannel acquireChannel() throws ChannelNotAvailableException
    {
        AntChannel mAntChannel = null;
        if(null != mAntChannelProvider)
        {
            try
            {
                /*
                 * In order to acquire a channel that is capable of background
                 * scanning, a Capabilities object must be created with the
                 * background scanning feature set to true. Passing this
                 * Capabilities object to acquireChannel() will return a channel
                 * that is capable of being assigned (via Extended Assignment)
                 * as a background scanning channel.
                 */
                Capabilities capableOfBackgroundScan = new Capabilities();
                capableOfBackgroundScan.supportBackgroundScanning(true);
                mAntChannel = mAntChannelProvider.acquireChannel(this, PredefinedNetwork.PUBLIC,
                        capableOfBackgroundScan);

                // Get background scan status
                mBackgroundScanInProgress = mAntChannel.getBackgroundScanState().isInProgress();

            } catch (RemoteException e)
            {
                die("ACP Remote Ex");
            }
        }
        return mAntChannel;
    }

    public void acquireBackgroundScanningChannel() throws ChannelNotAvailableException
    {
        synchronized(mCreateChannel_LOCK)
        {
            // We only want one channel; don't attempt if already acquired
            if (!mBackgroundScanAcquired) {

                // Acquire a channel, if no exception then set background scan
                // acquired to true
                AntChannel antChannel = acquireChannel();
                mBackgroundScanAcquired = true;

                if (null != antChannel)
                {
                    ChannelControllerBackgroundScan.ChannelBroadcastListener broadcastListener = new ChannelControllerBackgroundScan.ChannelBroadcastListener()
                    {

                        @Override
                        public void onBroadcastChanged(ChannelInfo newInfo)
                        {
                            // Pass on the received channel info to activity for display
                            mListener.onChannelChanged(newInfo);
                        }

                        @Override
                        public void onBackgroundScanStateChange(boolean backgroundScanInProgress, boolean backgroundScanIsConfigured) {
                            if(mListener == null) return;

                            mBackgroundScanInProgress = backgroundScanInProgress;
                            // Allow starting background scan if no scan in progress
                            mListener.onAllowStartScan(!mBackgroundScanInProgress && mBackgroundScanAcquired);
                        }

                        @Override
                        public void onChannelDeath() {
                            // Cleanup Background Scan Channel
                            closeBackgroundScanChannel();

                            if(mListener == null) return;

                            mListener.onAllowStartScan(false);
                        }
                    };

                    mBackgroundScanController = new ChannelControllerBackgroundScan(ServiceBackgroundScan.this,antChannel, broadcastListener,CHANNEL_PROOF_DEVICE_TYPE,CHANNEL_PROOF_TRANSMISSION_TYPE,WILDCARD_SEARCH_DEVICE_NUMBER,CHANNEL_PROOF_PERIOD,CHANNEL_PROOF_FREQUENCY);
                    mBackgroundScanController.openBackgroundScanningChannel();
                }
            }
        }
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (sharedPreferences.getString("platformType", "").equals(PlatformType.AUTOSENSE_CHEST)){
            CHANNEL_PROOF_DEVICE_TYPE = (byte)0x01;
            CHANNEL_PROOF_TRANSMISSION_TYPE = 0;
            WILDCARD_SEARCH_DEVICE_NUMBER = 0;
            CHANNEL_PROOF_PERIOD = 0x04EC;//8070;
            CHANNEL_PROOF_FREQUENCY = 0x50;
        }
        else if (sharedPreferences.getString("platformType", "").equals(PlatformType.AUTOSENSE_WRIST)){
            CHANNEL_PROOF_DEVICE_TYPE = (byte)0x03;
            CHANNEL_PROOF_TRANSMISSION_TYPE = 0;
            WILDCARD_SEARCH_DEVICE_NUMBER = 0;
            CHANNEL_PROOF_PERIOD = 1638;
            CHANNEL_PROOF_FREQUENCY = 0x50;
        }
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }
    @Override
    public IBinder onBind(Intent arg0)
    {
        return new ChannelServiceComm();
    }
    
    // Receives Channel Provider state changes
    private final BroadcastReceiver mChannelProviderStateChangedReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            // Only respond to state changes if activity is running.
            if(!mActivityIsRunning) return;
            
            if(AntChannelProvider.ACTION_CHANNEL_PROVIDER_STATE_CHANGED.equals(intent.getAction())) {
                int numChannels = intent.getIntExtra(AntChannelProvider.NUM_CHANNELS_AVAILABLE, 0);
                boolean legacyInterfaceInUse = intent.getBooleanExtra(AntChannelProvider.LEGACY_INTERFACE_IN_USE, false);
                
                if(numChannels > 0) {
                    // retrieve the number of channels with the background scanning capability
                    numChannels = getNumberOfChannelsAvailable();
                }
                
                if(mAllowAcquireBackgroundScanChannel) {
                    // Was a acquire channel allowed
                    // If no channels available AND legacy interface is not in use, disallow acquiring of channels
                    if(0 == numChannels && !legacyInterfaceInUse) {
                        // not any more
                        mAllowAcquireBackgroundScanChannel = false;
                    }
                } else {
                    // Acquire channels not allowed
                    // If there are channels OR legacy interface in use, allow acquiring of channels
                    if(numChannels > 0 || legacyInterfaceInUse) {
                        // now there are
                        mAllowAcquireBackgroundScanChannel = true;
                    }
                }
                
                if(null != mListener) {
                    if(mAllowAcquireBackgroundScanChannel) {
                        try {
                            // Try and acquire a channel to be used for background scanning
                            // If successful, allow user to start scan
                            acquireBackgroundScanningChannel();
                            mListener.onAllowStartScan(!mBackgroundScanInProgress && mBackgroundScanAcquired);
                        } catch (ChannelNotAvailableException e) {
                            // Channel is not yet available; disallow user to start scan
                            mListener.onAllowStartScan(false);
                        }
                    }
                }
            }
        }
    };
    
    private int getNumberOfChannelsAvailable() {
        if(null != mAntChannelProvider) {
            
            // In order to get the number of channels that are capable of
            // background scanning, a Capabilities object must be created with
            // the background scanning feature set to true.
            Capabilities capabilities = new Capabilities();
            capabilities.supportBackgroundScanning(true);

            try {
                // By passing in Capabilities object this will return the number
                // of channels capable of background scanning.
                int numChannels = mAntChannelProvider.getNumChannelsAvailable(capabilities);
                
                Log.i(TAG, "Number of channels with background scanning capabilities: " + numChannels);
                
                return numChannels;
            } catch (RemoteException e) {
                Log.i(TAG, "", e);
            }
        }
        return 0;
    }
    private void doBindAntRadioService()
    {
        if(BuildConfig.DEBUG) Log.v(TAG, "doBindAntRadioService");
        
        // Start listing for channel available intents
        registerReceiver(mChannelProviderStateChangedReceiver, new IntentFilter(AntChannelProvider.ACTION_CHANNEL_PROVIDER_STATE_CHANGED));
        
        mAntRadioServiceBound = AntService.bindService(this, mAntRadioServiceConnection);
    }
    
    private void doUnbindAntRadioService()
    {
        if(BuildConfig.DEBUG) Log.v(TAG, "doUnbindAntRadioService");
        
        // Stop listing for channel available intents
        try{
            unregisterReceiver(mChannelProviderStateChangedReceiver);
        } catch (IllegalArgumentException exception) {
            if(BuildConfig.DEBUG) Log.d(TAG, "Attempting to unregister a never registered Channel Provider State Changed receiver.");
        }
        
        if(mAntRadioServiceBound)
        {
            try
            {
                unbindService(mAntRadioServiceConnection);
            }
            catch(IllegalArgumentException e)
            {
                // Not bound, that's what we want anyway
            }

            mAntRadioServiceBound = false;
        }
    }

    @Override
    public void onCreate()
    {

        super.onCreate();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        mAntRadioServiceBound = false;
        
        doBindAntRadioService();
    }
    
    @Override
    public void onDestroy()
    {
        closeBackgroundScanChannel();
        doUnbindAntRadioService();
        mAntChannelProvider = null;
        
        super.onDestroy();
    }

    static void die(String error)
    {
        Log.e(TAG, "DIE: " + error);
    }

}
