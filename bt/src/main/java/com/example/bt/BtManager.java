package com.example.bt;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


import android.annotation.NonNull;
import android.annotation.Nullable;
import android.bluetooth.BluetoothA2dpSink;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAvrcp;
import android.bluetooth.BluetoothAvrcpController;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadsetClient;
import android.bluetooth.BluetoothHeadsetClientCall;
import android.bluetooth.BluetoothPbapClient;
import android.bluetooth.BluetoothProfile;
import android.content.ComponentName;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.HandlerThread;
import android.util.Log;


public class BtManager implements BluetoothProfile.ServiceListener {
    public static final String TAG = "BTM";
    private BluetoothAdapter mAdapter;
    //current remote device processed
    public BluetoothDevice mDevice;
    private static BtManager instance = null;
    Handler mHandler;
    MainActivity mctx;
    AudioManager mAudioManager;
    private MediaPlayer mPlayer;

    BluetoothA2dpSink a2dpsink;
    BluetoothAvrcpController avrcp;
    BluetoothHeadsetClient hfpclient;
    BluetoothPbapClient pbap;

    boolean connectting;

    public BtManager(MainActivity context) {
        HandlerThread ht = new HandlerThread("btm");
        ht.start();
        mctx = context;
        mHandler = new Handler(ht.getLooper());
        registerReceiver();
        mAudioManager = (AudioManager) mctx.getSystemService(Context.AUDIO_SERVICE);

        mAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mAdapter.isEnabled()) {
            Log.i(TAG, "enable bluetooth");
            mAdapter.enable();
        }
        mAdapter.getProfileProxy(context, this, BluetoothProfile.A2DP_SINK);
        mAdapter.getProfileProxy(context, this, BluetoothProfile.AVRCP_CONTROLLER);
        mAdapter.getProfileProxy(context, this, BluetoothProfile.PBAP_CLIENT);
        mAdapter.getProfileProxy(context, this, BluetoothProfile.HEADSET_CLIENT);
        mAdapter.setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, 120);
        //avrcp
        mConnectionCallback = new BtMediaCallback();
        mMediaBrowser = new MediaBrowser(getContext(),new ComponentName(BT_BROWSED_PACKAGE, BT_BROWSED_SERVICE),
                mConnectionCallback, null);
        mMediaBrowser.connect();
    }

    public static BtManager getInstance(MainActivity context) {
        if (instance == null) {
            instance = new BtManager(context);
        }
        return instance;
    }

    public boolean isEnabled() {
        return mAdapter.isEnabled();
    }

    public void setBluetooth(boolean on) {
        if (on) {
            mAdapter.enable();
        } else {
            mAdapter.disable();
        }
    }

    public String getName() {
        return mAdapter.getName();
    }

    public void setName(String name) {
        Log.i(TAG, "set name " + name);
        mAdapter.setName(name);
    }

    public boolean startDiscover() {
        if (mAdapter.isDiscovering()) {
            mAdapter.cancelDiscovery();
        }
        if(isDeviceBusy()){
            mDevice.cancelBondProcess();
        }
        if (!mAdapter.isDiscovering() && !isDeviceBusy()) {
            Log.i(TAG, "startDiscovery");
            return mAdapter.startDiscovery();
        }
        return false;
    }

    public void release() {
        Log.i(TAG, "btm release");
        mctx.unregisterReceiver(mReceiver);
        mAdapter.closeProfileProxy(BluetoothProfile.A2DP_SINK, a2dpsink);
        mAdapter.closeProfileProxy(BluetoothProfile.AVRCP_CONTROLLER, avrcp);
        mAdapter.closeProfileProxy(BluetoothProfile.PBAP_CLIENT, pbap);
        mAdapter.closeProfileProxy(BluetoothProfile.HEADSET_CLIENT, hfpclient);
        if(mPlayer!=null){
            mPlayer.stop();
            mPlayer.release();
        }

        instance = null;
    }

    /*
     * device state
     * BOND_NONE = 10 BOND_BONDING = 11 BOND_BONDED = 12
     *
     *
     * profile state
     * STATE_DISCONNECTED  = 0;
     * STATE_CONNECTING    = 1;
     * STATE_CONNECTED     = 2;
     * STATE_DISCONNECTING = 3;
     *
     *
     * */
    String stateToStr(int state) {
        switch (state) {
            case 0:
                return "disconnected";
            case 1:
                return "connecting";
            case 2:
                return "connected";
            case 3:
                return "disconnecting";
            case 10:
                return "none";
            case 11:
                return "bounding";
            case 12:
                return "bounded";
            default:
                break;
        }
        return "null";
    }

    public String getState() {
        if (mDevice == null)
            return "no device connected";
        StringBuilder sb = new StringBuilder();
        sb.append("name: " + mDevice.getName());
        sb.append("\r\naddr: " + mDevice.getAddress());
        sb.append("\r\nstate: " + stateToStr(mDevice.getBondState()));
        sb.append("\r\nad2psink: " + (a2dpsink == null ? "a2dp is null" : stateToStr(a2dpsink.getConnectionState(mDevice))));
        sb.append("\r\nhfpclient: " + (hfpclient == null ? "hfpclient is null" : stateToStr(hfpclient.getConnectionState(mDevice))));
        sb.append("\r\navrcp: " + (avrcp == null ? "avrcp is null" : stateToStr(avrcp.getConnectionState(mDevice))));
        sb.append("\r\npbap: " + (pbap == null ? "pbap is null" : stateToStr(pbap.getConnectionState(mDevice))));
        return sb.toString();
    }
    //blueooth device

    public void connectDevice(BluetoothDevice bd) {
        if (connectting)
            return;
        connectting = true;
        if (mAdapter.isDiscovering()) {
            mAdapter.cancelDiscovery();
        }
        if (mDevice != null && bd != null && mDevice != bd) {
            if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING)
                mDevice.cancelBondProcess();
            disconDevice(false,null);
        }
        mDevice = bd != null ? bd : mDevice;
        Log.i(TAG, "connectDevice" + mDevice.getName() + " state " + mDevice.getBondState());
        if (mDevice.getBondState() != BluetoothDevice.BOND_BONDED && mDevice.getBondState() != BluetoothDevice.BOND_BONDING) {
            mDevice.createBond();
        } else if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
            connectProfile();
        }
        connectting = false;
    }

    public void disconDevice(boolean unPair,BluetoothDevice bd) {
        BluetoothDevice device;
        if(bd!=null)
            device = bd;
        else
            device = mDevice;

        if (a2dpsink != null && a2dpsink.getConnectionState(device) != BluetoothA2dpSink.STATE_DISCONNECTED) {
            a2dpsink.disconnect(device);
        }
        if (hfpclient != null && hfpclient.getConnectionState(device) != BluetoothHeadsetClient.STATE_DISCONNECTED) {
            hfpclient.disconnect(device);
        }
        if (pbap != null && pbap.getConnectionState(device) != BluetoothPbapClient.STATE_DISCONNECTED)
            pbap.disconnect(device);

        if (unPair) {
            Log.i(TAG, "unpair"+device.getName());
            device.removeBond();
        }
    }

    public boolean isDeviceBusy() {
        return mDevice != null && mDevice.getBondState() == BluetoothDevice.BOND_BONDING;
    }

    public Set<BluetoothDevice> getPairedDevice() {
        return mAdapter.getBondedDevices();
    }

    public BluetoothDevice getBoundedDevice() {
        Set<BluetoothDevice> devices = mAdapter.getBondedDevices();
        BluetoothDevice device = null;
        for (Iterator<BluetoothDevice> it = devices.iterator(); it.hasNext(); ) {
            device = it.next();
            if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                break;
            }
            device = null;
        }
        return device;
    }

    public boolean isSameDeivce(BluetoothDevice bd) {
        return mDevice != null && bd != null && mDevice.getAddress().equals(bd.getAddress());
    }

    public boolean isconneted(BluetoothDevice bd) {
        if (bd == null)
            return false;
        boolean hfp = hfpclient != null ? hfpclient.getConnectionState(bd) == BluetoothProfile.STATE_CONNECTED : false;
        boolean avr = false;//avrcp != null ? avrcp.getConnectionState(bd) == BluetoothProfile.STATE_CONNECTED : false;
        boolean a2dp = a2dpsink != null ? a2dpsink.getConnectionState(bd) == BluetoothProfile.STATE_CONNECTED : false;
        return hfp || avr || a2dp;
    }

    public void autoConnect() {
        Log.i(TAG, "autoConnect");
        Set<BluetoothDevice> blueSet = getPairedDevice();
        BluetoothDevice first=null;
        if (blueSet != null && !blueSet.isEmpty()) {
            BluetoothDevice bd = null;
            for (Iterator<BluetoothDevice> it = blueSet.iterator(); it.hasNext(); ) {
                bd = (BluetoothDevice) it.next();
                if(first==null)
                    first = bd;
                Log.i(TAG, "get Device: "+bd);
                if (isconneted(bd)) {
                    connectDevice(bd);
                    return;
                }
            }
            connectDevice(first);
        }
    }

    //profile avrcp a2dpsink pbap hfpclient
    public void connectProfile() {
        Log.i(TAG, "connectProfile");
        int state;
        if (a2dpsink != null) {
            state = a2dpsink.getConnectionState(mDevice);Log.i(TAG, "connectProfile a2dp state "+state);
            if (state == BluetoothProfile.STATE_DISCONNECTED || state == BluetoothProfile.STATE_DISCONNECTING) {
                a2dpsink.connect(mDevice);
                Log.i(TAG, "connectProfile a2dp");
            }
        }

        if (hfpclient != null) {
            state = hfpclient.getConnectionState(mDevice);Log.i(TAG, "connectProfile hfpclient state "+state);
            if (state == BluetoothProfile.STATE_DISCONNECTED || state == BluetoothProfile.STATE_DISCONNECTING) {
                hfpclient.connect(mDevice);
                Log.i(TAG, "connectProfile hfpclient");
            }

        }

    }

    //a2dpclient
    public boolean isPlaying() {
        if (a2dpsink == null)
            return false;

        boolean play = a2dpsink.isA2dpPlaying(mDevice);
        Log.i(TAG, "isplaying :" + play);
        return play;
    }

    //avrcp
    Context getContext(){
        return mctx;
    }
    private MediaBrowser mMediaBrowser = null;
    private MediaController mMediaController = null;
    private static final String BT_BROWSED_PACKAGE = "com.android.bluetooth";
    private static final String BT_BROWSED_SERVICE = "com.android.bluetooth.avrcpcontroller.BluetoothMediaBrowserService";

    private MediaBrowser.ConnectionCallback mConnectionCallback = null;
    class BtMediaCallback extends  MediaBrowser.ConnectionCallback{
        @Override
        public void onConnected() {
            MediaSession.Token token = mMediaBrowser.getSessionToken();
            Log.d(TAG, "onConnected: session token " + token);
            if (token == null) {
                throw new IllegalArgumentException("No Session token");
            }
            //get mediaContoller
            mMediaController = new MediaController(getContext(), token);
            mMediaController.registerCallback(new MediaController.Callback() {
                @Override
                public void onSessionDestroyed() {
                    Log.d(TAG, "onSessionDestroyed");
                }

                @Override
                public void onSessionEvent(@NonNull String event, @Nullable Bundle extras) {
                    Log.d(TAG, "onSessionEvent event: " + event);
                }

                @Override
                public void onPlaybackStateChanged(@Nullable PlaybackState state) {
                    Log.d(TAG, "onPlaybackStateChanged state: " + state);
                    if(state !=null)
                        mctx.sendMsg(MainActivity.MUSIC_STATE_CHANGE, state);
                }

                @Override
                public void onMetadataChanged(@Nullable MediaMetadata metadata) {
                    Log.d(TAG, "onMetadataChanged metadata: " + metadata);
                    if (metadata != null) {
                            mctx.sendMsg(MainActivity.MUSIC_METADATA, metadata);
                    }
                }

                @Override
                public void onQueueChanged(@Nullable List<MediaSession.QueueItem> queue) {
                    Log.d(TAG, "onQueueChanged");
                }

                @Override
                public void onQueueTitleChanged(@Nullable CharSequence title) {
                    Log.d(TAG, "onQueueTitleChanged title: " + title);
                }

                @Override
                public void onExtrasChanged(@Nullable Bundle extras) {
                    Log.d(TAG, "onExtrasChanged extras: " + extras);
                }

                @Override
                public void onAudioInfoChanged(MediaController.PlaybackInfo info) {
                    Log.d(TAG, "onAudioInfoChanged info: " + info);
                }
            });
        }

        @Override
        public void onConnectionFailed() {
            Log.d(TAG, "onConnectionFailed");
            mMediaController = null;
        }

        @Override
        public void onConnectionSuspended() {
            Log.d(TAG, "onConnectionSuspended");
        }
    };
    PlaybackState getPlaybackState(){
        if(mMediaController != null){
            return mMediaController.getPlaybackState();
        }
        return null;
    }
    public void play() {
        if(mMediaController != null){
            mMediaController.getTransportControls().play();
            return;
        }
        if (avrcp == null)
            return;
        avrcp.sendPassThroughCmd(mDevice, BluetoothAvrcp.PASSTHROUGH_ID_PLAY, BluetoothAvrcp.PASSTHROUGH_STATE_PRESS);
        avrcp.sendPassThroughCmd(mDevice, BluetoothAvrcp.PASSTHROUGH_ID_PLAY, BluetoothAvrcp.PASSTHROUGH_STATE_RELEASE);
    }

    public void pause() {
        if(mMediaController != null){
            mMediaController.getTransportControls().pause();
            return;
        }
        if (avrcp == null)
            return;
        avrcp.sendPassThroughCmd(mDevice, BluetoothAvrcp.PASSTHROUGH_ID_PAUSE, BluetoothAvrcp.PASSTHROUGH_STATE_PRESS);
        avrcp.sendPassThroughCmd(mDevice, BluetoothAvrcp.PASSTHROUGH_ID_PAUSE, BluetoothAvrcp.PASSTHROUGH_STATE_RELEASE);
    }
    public void stop() {
        if(mMediaController != null){
            mMediaController.getTransportControls().stop();
            return;
        }
        if (avrcp == null)
            return;
        avrcp.sendPassThroughCmd(mDevice, BluetoothAvrcp.PASSTHROUGH_ID_STOP, BluetoothAvrcp.PASSTHROUGH_STATE_PRESS);
        avrcp.sendPassThroughCmd(mDevice, BluetoothAvrcp.PASSTHROUGH_ID_STOP, BluetoothAvrcp.PASSTHROUGH_STATE_RELEASE);
    }
    public void prev() {
        if(mMediaController != null){
            mMediaController.getTransportControls().skipToPrevious();
            return;
        }
        if (avrcp == null)
            return;
        avrcp.sendPassThroughCmd(mDevice, BluetoothAvrcp.PASSTHROUGH_ID_BACKWARD, BluetoothAvrcp.PASSTHROUGH_STATE_PRESS);
        avrcp.sendPassThroughCmd(mDevice, BluetoothAvrcp.PASSTHROUGH_ID_BACKWARD, BluetoothAvrcp.PASSTHROUGH_STATE_RELEASE);
    }

    public void next() {
        if(mMediaController != null){
            mMediaController.getTransportControls().skipToNext();
            return;
        }
        if (avrcp == null)
            return;
        avrcp.sendPassThroughCmd(mDevice, BluetoothAvrcp.PASSTHROUGH_ID_FORWARD, BluetoothAvrcp.PASSTHROUGH_STATE_PRESS);
        avrcp.sendPassThroughCmd(mDevice, BluetoothAvrcp.PASSTHROUGH_ID_FORWARD, BluetoothAvrcp.PASSTHROUGH_STATE_RELEASE);
    }

    public void getElement() {
        avrcp.getElementAttrCmd();
    }

    public void getPlaylist(){
        if(mMediaController != null){
            return;
        }
        avrcp.getNowPlayingList(mDevice,0,20);
    }

    //hfpclient
    public void switchAudio() {
        if (hfpclient == null)
            return;
        int state = hfpclient.getAudioState(mDevice);
        if (state == BluetoothHeadsetClient.STATE_AUDIO_DISCONNECTED) {
            hfpclient.connectAudio(mDevice);
            Log.i(TAG, "connectAudio");
        } else if (state == BluetoothHeadsetClient.STATE_AUDIO_CONNECTED) {
            hfpclient.disconnectAudio(mDevice);
            Log.i(TAG, "disconnectAudio");
        }
    }

    public void call(String num) {
        if (hfpclient == null)
            return;
        hfpclient.dial(mDevice, num);
    }

    public void handup() {
        if (hfpclient == null)
            return;
        List<BluetoothHeadsetClientCall> cur = hfpclient.getCurrentCalls(mDevice);
        for (BluetoothHeadsetClientCall call : cur) {
            if (call.getState() == BluetoothHeadsetClientCall.CALL_STATE_INCOMING) {
                hfpclient.rejectCall(mDevice);
            } else {
                hfpclient.terminateCall(mDevice, call);
            }
        }
    }

    public void accept() {
        if (hfpclient == null)
            return;

        List<BluetoothHeadsetClientCall> cur = hfpclient.getCurrentCalls(mDevice);
        if (cur == null)
            return;
        for (BluetoothHeadsetClientCall call : cur) {
            if (call.getState() == BluetoothHeadsetClientCall.CALL_STATE_INCOMING) {
                break;
            }
            return;
        }
        hfpclient.acceptCall(mDevice, 0);
    }

    /*
     * 获取电话状态
     * BluetoothHeadsetClientCall.CALL_STATE_ACTIVE
     * CALL_STATE_HELD             保留当前不挂断,接听另一个电话
     * CALL_STATE_DIALING
     * CALL_STATE_INCOMING
     * CALL_STATE_TERMINATED
     *
     * */
    public int getCallState() {
        if (hfpclient == null)
            return BluetoothHeadsetClientCall.CALL_STATE_TERMINATED;

        List<BluetoothHeadsetClientCall> cur = hfpclient.getCurrentCalls(mDevice);
        if (cur == null)
            return BluetoothHeadsetClientCall.CALL_STATE_TERMINATED;
        for (BluetoothHeadsetClientCall call : cur) {
            int state = call.getState();
            if (state != BluetoothHeadsetClientCall.CALL_STATE_TERMINATED) {
                return state;
            }
        }
        return BluetoothHeadsetClientCall.CALL_STATE_TERMINATED;
    }

    public void dail(byte code) {
        if (hfpclient == null)
            return;
        hfpclient.sendDTMF(mDevice, code);
    }

    public String getNum() {
        if (hfpclient == null)
            return null;
        List<BluetoothHeadsetClientCall> cur = hfpclient.getCurrentCalls(mDevice);
        if (cur == null)
            return null;
        for (BluetoothHeadsetClientCall call : cur) {
            int state = call.getState();
            if (state != BluetoothHeadsetClientCall.CALL_STATE_INCOMING) {
                return call.getNumber();
            }
        }
        return null;
    }
    private void playRing(Boolean play){
        if (play) {
            Log.d(TAG,"start ring");
            if (mPlayer==null) {
                AudioAttributes attr = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build();
                int session = mAudioManager.generateAudioSessionId();
                mPlayer = MediaPlayer.create(mctx,R.raw.ring,attr,session);
                //mPlayer.setLooping(true);
                mPlayer.start();
            }else if(!mPlayer.isPlaying()){
                mPlayer.start();
            }
        } else {
            if (mPlayer!=null) {
                Log.d(TAG,"stopping ring");
                mPlayer.stop();
                mPlayer.release();
                mPlayer=null;
            }
        }
    }

    //pbap
    long pbapDownloading =0;
    public void removePhonebook(){
        if (pbap == null)
            return;
        pbap.disconnect(mDevice);
    }
    public void downLoadPhonebook() {
        int state = pbap.getConnectionState(mDevice);
        if (pbap == null||(state!=BluetoothProfile.STATE_DISCONNECTED && (SystemClock.uptimeMillis()-pbapDownloading)<60000*5))
            return;
        Log.i(TAG,"download phonebook");
        pbapDownloading = SystemClock.uptimeMillis();
        pbap.disconnect(mDevice);
        pbap.connect(mDevice);
    }

    @Override
    public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
        switch (i) {
            case BluetoothProfile.A2DP_SINK:
                a2dpsink = (BluetoothA2dpSink) bluetoothProfile;
                break;
            case BluetoothProfile.AVRCP_CONTROLLER:
                avrcp = (BluetoothAvrcpController) bluetoothProfile;
                break;
            case BluetoothProfile.PBAP_CLIENT:
                pbap = (BluetoothPbapClient) bluetoothProfile;
                break;
            case BluetoothProfile.HEADSET_CLIENT:
                hfpclient = (BluetoothHeadsetClient) bluetoothProfile;
                break;
        }
        if (a2dpsink != null && avrcp != null && hfpclient != null) {
            autoConnect();
            mctx.sendMsg(MainActivity.DEVICE_STATE_CHANGE, null);
        }
    }

    @Override
    public void onServiceDisconnected(int i) {
        switch (i) {
            case BluetoothProfile.A2DP_SINK:
                a2dpsink = null;
                break;
            case BluetoothProfile.AVRCP_CONTROLLER:
                avrcp = null;
                break;
            case BluetoothProfile.PBAP_CLIENT:
                pbap = null;
                break;
            case BluetoothProfile.HEADSET_CLIENT:
                hfpclient = null;
                break;
        }
    }


    /*
     * 处理蓝牙状态
     * 连接:
     *
     * hfpclient:
     *
     * a2dpsink:
     *
     * avrcp:
     *
     * pbap:
     *
     * */
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {//profile 连接广播
                BluetoothDevice dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, -1);
                mctx.sendMsg(MainActivity.DEVICE_STATE_CHANGE, null);
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Log.i(TAG, "discovery started ");
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.i(TAG, "discovery finish ");
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.i(TAG, "found device " + device);
                mctx.sendMsg(MainActivity.DEVICE_ADD, device);

            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,-1);
                if (device!=null&&state==BluetoothDevice.BOND_BONDED&&!isconneted(mDevice))
                    connectDevice(device);
                mctx.sendMsg(MainActivity.DEVICE_STATE_CHANGE, null);
            } else if (BluetoothDevice.ACTION_NAME_CHANGED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
                mctx.sendMsg(MainActivity.DEVICE_STATE_CHANGE, null);
            }
            //a2dpsink
            if (BluetoothA2dpSink.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED);
                Log.i(TAG,"a2dp update state "+state);
                mctx.sendMsg(MainActivity.DEVICE_STATE_CHANGE, null);
            } else if (BluetoothA2dpSink.ACTION_PLAYING_STATE_CHANGED.equals(action)) {

            }
            //avrcp
            if (BluetoothAvrcpController.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
                mctx.sendMsg(MainActivity.DEVICE_STATE_CHANGE, null);
            } else if (BluetoothAvrcpController.ACTION_TRACK_EVENT.equals(action)) {
                MediaMetadata data = intent.getParcelableExtra("android.bluetooth.avrcp-controller.profile.extra.METADATA");
                PlaybackState state = intent.getParcelableExtra("android.bluetooth.avrcp-controller.profile.extra.PLAYBACK");
                //Log.i(TAG,"metadata "+data);
                if (data != null)
                    mctx.sendMsg(MainActivity.MUSIC_METADATA, data);
                if(state !=null)
                    mctx.sendMsg(MainActivity.MUSIC_STATE_CHANGE, state);
            } else if ("com.android.getelementattrrsp".equals(action)) {
                String artist = intent.getStringExtra("artist");
                String trackTitle = intent.getStringExtra("trackTitle");
                String album = intent.getStringExtra("album");
                Log.i(TAG, "getElementAttrRsp,artist: " + artist + ",trackTitle: " + trackTitle + ",album: " + album);
                MediaMetadata data = new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
                        .putString(MediaMetadata.METADATA_KEY_TITLE, trackTitle)
                        .putString(MediaMetadata.METADATA_KEY_ALBUM, album).build();
                mctx.sendMsg(MainActivity.MUSIC_METADATA, data);
            }
            //hfpclient
            if (BluetoothHeadsetClient.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED);
                int reason = intent.getIntExtra("disconnect_reason",0);
                Log.i(TAG,"hfpclient update state "+state+" reason "+reason);
                /*if (state == BluetoothProfile.STATE_CONNECTED) {
                    if (!isconneted(mDevice))
                        autoConnect();
                }*/
                mctx.sendMsg(MainActivity.DEVICE_STATE_CHANGE, null);
            } else if (BluetoothHeadsetClient.ACTION_AUDIO_STATE_CHANGED.equals(action)) {
                mctx.sendMsg(MainActivity.DEVICE_STATE_CHANGE, null);
            } else if (BluetoothHeadsetClient.ACTION_CALL_CHANGED.equals(action)) {
                BluetoothHeadsetClientCall c = intent.getParcelableExtra(BluetoothHeadsetClient.EXTRA_CALL);
                //Log.i(TAG,"action "+c.toString());
                mctx.sendMsg(MainActivity.PHONE_STATE_CHANGE
                        , null);
                if (getCallState() == BluetoothHeadsetClientCall.CALL_STATE_INCOMING) {
                    playRing(true);
                } else {
                    playRing(false);
                }
            }
            //pbap
            if (BluetoothPbapClient.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
                mctx.sendMsg(MainActivity.DEVICE_STATE_CHANGE, null);
            } else if ("com.lsec.pbap_downloaded".equals(action)) {
                Log.i(TAG,"action "+action);
                pbapDownloading = 0;
                mctx.sendMsg(MainActivity.PHONEBOOK_DOWNLOAD, null);
            } else if ("action.download.complete".equals(action)) {
                pbapDownloading = 0;
                mctx.sendMsg(MainActivity.PHONEBOOK_DOWNLOAD, null);
            } else if ("action.download.exception".equals(action)) {

            }
        }
    };

    public void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_NAME_CHANGED);
        //a2dpclient
        filter.addAction(BluetoothA2dpSink.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothA2dpSink.ACTION_PLAYING_STATE_CHANGED);
        //avrcp
        filter.addAction(BluetoothAvrcpController.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothAvrcpController.ACTION_TRACK_EVENT);
        filter.addAction("com.android.getelementattrrsp");
        //hfpclient
        filter.addAction(BluetoothHeadsetClient.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothHeadsetClient.ACTION_AUDIO_STATE_CHANGED);
        filter.addAction(BluetoothHeadsetClient.ACTION_CALL_CHANGED);
        //pbap
        filter.addAction(BluetoothPbapClient.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction("com.lsec.pbap_downloaded");
        filter.addAction("action.download.complete");
        filter.addAction("action.download.exception");
        mctx.registerReceiver(mReceiver, filter, null, mHandler);
    }
}
