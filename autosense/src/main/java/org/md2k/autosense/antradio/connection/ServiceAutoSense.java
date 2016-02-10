package org.md2k.autosense.antradio.connection;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.dsi.ant.AntService;
import com.dsi.ant.channel.AntChannel;
import com.dsi.ant.channel.AntChannelProvider;
import com.dsi.ant.channel.ChannelNotAvailableException;
import com.dsi.ant.channel.PredefinedNetwork;

import org.md2k.autosense.BuildConfig;
import org.md2k.autosense.Constants;
import org.md2k.autosense.antradio.ChannelInfo;
import org.md2k.autosense.devices.AutoSensePlatform;
import org.md2k.datakitapi.datatype.DataTypeByteArray;
import org.md2k.datakitapi.source.platform.PlatformType;
import org.md2k.datakitapi.time.DateTime;

import java.util.HashMap;

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

public class ServiceAutoSense extends Service {
    private static final String TAG = ServiceAutoSense.class.getSimpleName();
    HashMap<String, ChannelController> mChannelControllerList = new HashMap<>();
    ChannelChangedListener mListener;
    DataExtractorChest dataExtractorChest;
    DataExtractorWrist dataExtractorWrist;
    HashMap<String, Integer> hm = new HashMap<>();
    long starttimestamp = 0;
    private Object mCreateChannel_LOCK = new Object();
    private boolean mAntRadioServiceBound;
    private AntService mAntRadioService = null;
    private AntChannelProvider mAntChannelProvider = null;
    private boolean mAllowAddChannel = false;
    /**
     * Receives AntChannelProvider state changes being sent from ANT Radio Service
     */
    private final BroadcastReceiver mChannelProviderStateChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AntChannelProvider.ACTION_CHANNEL_PROVIDER_STATE_CHANGED.equals(intent.getAction())) {
                boolean update = false;
                // Retrieving the data contained in the intent
                int numChannels = intent.getIntExtra(AntChannelProvider.NUM_CHANNELS_AVAILABLE, 0);
                boolean legacyInterfaceInUse = intent.getBooleanExtra(AntChannelProvider.LEGACY_INTERFACE_IN_USE, false);

                if (mAllowAddChannel) {
                    // Was a acquire channel allowed
                    // If no channels available AND legacy interface is not in use, disallow acquiring of channels
                    if (0 == numChannels && !legacyInterfaceInUse) {
                        mAllowAddChannel = false;
                        update = true;
                    }
                } else {
                    // Acquire channels not allowed
                    // If there are channels OR legacy interface in use, allow acquiring of channels
                    if (numChannels > 0 || legacyInterfaceInUse) {
                        mAllowAddChannel = true;
                        update = true;
                    }
                }

                if (update && (null != mListener)) {
                    // AllowAddChannel has been changed, sending event callback
                    mListener.onAllowAddChannel(mAllowAddChannel);
                }
            }
        }
    };
    private ServiceConnection mAntRadioServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // Must pass in the received IBinder object to correctly construct an AntService object
            mAntRadioService = new AntService(service);

            try {
                // Getting a channel provider in order to acquire channels
                mAntChannelProvider = mAntRadioService.getChannelProvider();

                // Initial check for number of channels available
                boolean mChannelAvailable = mAntChannelProvider.getNumChannelsAvailable() > 0;
                Log.d(TAG, "Channel Available: " + mAntChannelProvider.getNumChannelsAvailable());
                // Initial check for if legacy interface is in use. If the
                // legacy interface is in use, applications can free the ANT
                // radio by attempting to acquire a channel.
                boolean legacyInterfaceInUse = mAntChannelProvider.isLegacyInterfaceInUse();

                // If there are channels OR legacy interface in use, allow adding channels
                // If no channels available AND legacy interface is not in use, disallow adding channels
                mAllowAddChannel = mChannelAvailable || legacyInterfaceInUse;

                if (mAllowAddChannel) {
                    if (null != mListener) {
                        // Send an event that indicates if adding channels is allowed
                        mListener.onAllowAddChannel(true);
                    }
                }

            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            die("Binder Died");

            mAntChannelProvider = null;
            mAntRadioService = null;

            if (mAllowAddChannel) {
                mListener.onAllowAddChannel(false);
            }
            mAllowAddChannel = false;
        }

    };

    static void die(String error) {
        Log.e(TAG, "DIE: " + error);
    }

    private void closeChannel(AutoSensePlatform autoSensePlatform) {
        synchronized (mChannelControllerList) {
            ChannelController channelController = mChannelControllerList.get(autoSensePlatform.getPlatformType() + ":" + autoSensePlatform.getDeviceId());
            channelController.close();
            mChannelControllerList.remove(autoSensePlatform.getPlatformType() + ":" + autoSensePlatform.getDeviceId());
        }
    }

    private void closeAllChannels() {
        synchronized (mChannelControllerList) {
            for (Object o : mChannelControllerList.entrySet()) {
                HashMap.Entry pair = (HashMap.Entry) o;
                ChannelController channel = (ChannelController) pair.getValue();
                channel.close();
            }
            mChannelControllerList.clear();
        }
    }

    AntChannel acquireChannel() throws ChannelNotAvailableException {
        Log.d(TAG, "mAntChannelProvider=" + mAntChannelProvider);
        AntChannel mAntChannel = null;
        if (null != mAntChannelProvider) {
            try {
                /*
                 * If applications require a channel with specific capabilities
                 * (event buffering, background scanning etc.), a Capabilities
                 * object should be created and then the specific capabilities
                 * required set to true. Applications can specify both required
                 * and desired Capabilities with both being passed in
                 * acquireChannel(context, PredefinedNetwork,
                 * requiredCapabilities, desiredCapabilities).
                 */
                Log.d(TAG, "acquireChannel...");

                mAntChannel = mAntChannelProvider.acquireChannel(this, PredefinedNetwork.PUBLIC);
                Log.d(TAG, "mAntChannel=" + mAntChannel);
                Log.d(TAG, "...acquireChannel");
            } catch (RemoteException e) {
                die("ACP Remote Ex");
            }
        }
        return mAntChannel;
    }

    private ChannelInfo createNewChannel(final AutoSensePlatform autoSensePlatform) throws ChannelNotAvailableException {
        hm.clear();
        starttimestamp = DateTime.getDateTime();
        ChannelController channelController = null;

        synchronized (mCreateChannel_LOCK) {
            // Acquiring a channel from ANT Radio Service
            Log.d(TAG, "create_new_channel....");
            AntChannel antChannel = acquireChannel();
            Log.d(TAG, "create_new_channel....antChannel=" + antChannel);

            if (null != antChannel) {
                // Constructing a controller that will manage and control the channel
                channelController = new ChannelController(antChannel, autoSensePlatform,
                        new ChannelController.ChannelBroadcastListener() {
                            @Override
                            public void onBroadcastChanged(ChannelInfo newInfo) {
                                // Sending a channel changed event when message from ANT is received
                                if (newInfo.status == 1) {
                                    mListener.onChannelChanged(newInfo);
                                    return;
                                }
                                if (newInfo.autoSensePlatform.getPlatformType().equals(PlatformType.AUTOSENSE_CHEST)) {
                                    dataExtractorChest.prepareAndSendToDataKit(ServiceAutoSense.this, newInfo);
                                } else if (newInfo.autoSensePlatform.getPlatformType().equals(PlatformType.AUTOSENSE_WRIST)) {
                                    dataExtractorWrist.prepareAndSendToDataKit(ServiceAutoSense.this, newInfo);
                                }
                                Intent intent = new Intent("autosense");
                                // You can also include some extra data.
                                intent.putExtra("operation", "data");
                                intent.putExtra("deviceId", autoSensePlatform.getDeviceId());
                                intent.putExtra("platformType", autoSensePlatform.getPlatformType());
                                intent.putExtra("dataSourceType", "autosense");
                                if (!hm.containsKey(autoSensePlatform.getDeviceId())) {
                                    hm.put(autoSensePlatform.getDeviceId(), 0);
                                }
                                hm.put(autoSensePlatform.getDeviceId(), hm.get(autoSensePlatform.getDeviceId()) + 1);
                                intent.putExtra("count", hm.get(autoSensePlatform.getDeviceId()));
                                intent.putExtra("timestamp", DateTime.getDateTime());
                                intent.putExtra("starttimestamp", starttimestamp);
                                intent.putExtra("data", new DataTypeByteArray(newInfo.timestamp, newInfo.broadcastData));
                                LocalBroadcastManager.getInstance(ServiceAutoSense.this).sendBroadcast(intent);

                                mListener.onChannelChanged(newInfo);
                            }
                        });
                mChannelControllerList.put(autoSensePlatform.getPlatformType() + ":" + autoSensePlatform.getDeviceId(), channelController);
                Log.d(TAG, autoSensePlatform.getPlatformType() + ":" + autoSensePlatform.getDeviceId() + " ->channelController=" + channelController);
            }
        }

        if (null == channelController) return null;

        return channelController.getCurrentInfo();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return new ChannelServiceComm();
    }

    private void doBindAntRadioService() {
        if (BuildConfig.DEBUG) Log.v(TAG, "doBindAntRadioService");

        // Start listing for channel available intents
        registerReceiver(mChannelProviderStateChangedReceiver, new IntentFilter(AntChannelProvider.ACTION_CHANNEL_PROVIDER_STATE_CHANGED));

        // Creating the intent and calling context.bindService() is handled by
        // the static bindService() method in AntService
        mAntRadioServiceBound = AntService.bindService(this, mAntRadioServiceConnection);
    }

    private void doUnbindAntRadioService() {
        if (BuildConfig.DEBUG) Log.v(TAG, "doUnbindAntRadioService");

        // Stop listing for channel available intents
        try {
            unregisterReceiver(mChannelProviderStateChangedReceiver);
        } catch (IllegalArgumentException exception) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Attempting to unregister a never registered Channel Provider State Changed receiver.");
        }

        if (mAntRadioServiceBound) {
            try {
                unbindService(mAntRadioServiceConnection);
            } catch (IllegalArgumentException e) {
                // Not bound, that's what we want anyway
            }

            mAntRadioServiceBound = false;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        dataExtractorChest = new DataExtractorChest(getApplicationContext());
        dataExtractorWrist = new DataExtractorWrist(getApplicationContext());

        mAntRadioServiceBound = false;

        doBindAntRadioService();
    }

    @Override
    public void onDestroy() {
        closeAllChannels();

        doUnbindAntRadioService();
        mAntChannelProvider = null;

        super.onDestroy();
    }

    public interface ChannelChangedListener {
        /**
         * Occurs when a Channel's Info has changed (i.e. a newly created
         * channel, channel has transmitted or received data, or if channel has
         * been closed.
         *
         * @param newInfo The channel's updated info
         */
        void onChannelChanged(ChannelInfo newInfo);

        /**
         * Occurs when there is adding a channel is being allowed or disallowed.
         *
         * @param addChannelAllowed True if adding channels is allowed. False, otherwise.
         */
        void onAllowAddChannel(boolean addChannelAllowed);
    }

    /**
     * The interface used to communicate with the ChannelService
     */
    public class ChannelServiceComm extends Binder {
        /**
         * Sets the listener to be used for channel changed event callbacks.
         *
         * @param listener The listener that will receive events
         */
        void setOnChannelChangedListener(ChannelChangedListener listener) {
            mListener = listener;
        }


        public ChannelInfo addNewChannel(AutoSensePlatform autoSensePlatform) throws ChannelNotAvailableException {
            return createNewChannel(autoSensePlatform);
        }

        /**
         * Closes all channels currently added.
         */
        void clearAllChannels() {
            closeAllChannels();
        }

        void clearChannel(AutoSensePlatform autoSensePlatform) {
            closeChannel(autoSensePlatform);
        }
    }

}
