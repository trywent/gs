package com.example.bt;

import java.util.Iterator;
import java.util.List;
import java.util.Set;


import android.bluetooth.BluetoothA2dpSink;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAvrcp;
import android.bluetooth.BluetoothAvrcpController;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadsetClient;
import android.bluetooth.BluetoothHeadsetClientCall;
import android.bluetooth.BluetoothPbapClient;
import android.bluetooth.BluetoothProfile;
import android.os.Handler;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.HandlerThread;
import android.util.Log;


public class BtManager implements BluetoothProfile.ServiceListener{
    public static final String TAG = "BTM";
    private BluetoothAdapter mAdapter;
    //current remote device processed
    public BluetoothDevice mDevice;
    private static BtManager instance=null;
    Handler mHandler;
    MainActivity mctx;

    BluetoothA2dpSink a2dpsink;
    BluetoothAvrcpController avrcp;
    BluetoothHeadsetClient hfpclient;
    BluetoothPbapClient pbap;


    public BtManager(MainActivity context){
        HandlerThread ht = new HandlerThread("btm");
        ht.start();
        mctx = context;
        mHandler = new Handler(ht.getLooper());
        registerReceiver();
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        if(!mAdapter.isEnabled()) {
            mAdapter.enable();
        }
        mAdapter.getProfileProxy(context, this,BluetoothProfile.A2DP_SINK);
        mAdapter.getProfileProxy(context, this,BluetoothProfile.AVRCP_CONTROLLER);
        mAdapter.getProfileProxy(context, this,BluetoothProfile.PBAP_CLIENT);
        mAdapter.getProfileProxy(context, this,BluetoothProfile.HEADSET_CLIENT);
    }
    public static BtManager getInstance(MainActivity context){
        if(instance==null) {
            instance = new BtManager(context);
        }
        return instance;
    }
    public boolean isEnabled(){
        return mAdapter.isEnabled();
    }
    public void setBluetooth(boolean on){
        if(on){
            mAdapter.enable();
        }else{
            mAdapter.disable();
        }
    }
    public boolean startDiscover(){
        if(!mAdapter.isDiscovering()&&!isDeviceBusy()){
            Log.i(TAG,"startDiscovery");
            return mAdapter.startDiscovery();
        }
        return false;
    }

    public void release(){
        Log.i(TAG,"btm release");
        mctx.unregisterReceiver(mReceiver);
        mAdapter.closeProfileProxy(BluetoothProfile.A2DP_SINK,a2dpsink);
        mAdapter.closeProfileProxy(BluetoothProfile.AVRCP_CONTROLLER,avrcp);
        mAdapter.closeProfileProxy(BluetoothProfile.PBAP_CLIENT,pbap);
        mAdapter.closeProfileProxy(BluetoothProfile.HEADSET_CLIENT,hfpclient);
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
    String stateToStr(int state){
        switch(state){
            case 0:return "disconnected";
            case 1:return "connecting";
            case 2:return "connected";
            case 3:return "disconnecting";
            case 10:return "none";
            case 11:return "bounding";
            case 12:return "bounded";
            default:break;
        }
        return "null";
    }

    public String getState(){
        if(mDevice==null)
            return "no device connected";
        StringBuilder sb = new StringBuilder();
        sb.append("name: "+mDevice.getName());
        sb.append("\r\naddr: "+mDevice.getAddress());
        sb.append("\r\nstate: "+stateToStr(mDevice.getBondState()));
        sb.append("\r\nad2psink: "+(a2dpsink==null?"a2dp is null":stateToStr(a2dpsink.getConnectionState(mDevice))));
        sb.append("\r\nhfpclient: "+(hfpclient==null?"hfpclient is null":stateToStr(hfpclient.getConnectionState(mDevice))));
        sb.append("\r\navrcp: "+(avrcp==null?"avrcp is null":stateToStr(avrcp.getConnectionState(mDevice))));
        sb.append("\r\npbap: "+(pbap==null?"pbap is null":stateToStr(pbap.getConnectionState(mDevice))));
        return sb.toString();
    }
    //blueooth device

    public void connectDevice(BluetoothDevice bd){
        if(mAdapter.isDiscovering()){
            mAdapter.cancelDiscovery();
        }
        if(mDevice!=null&&mDevice!=bd){
            if(mDevice.getBondState()==BluetoothDevice.BOND_BONDING)
                mDevice.cancelBondProcess();

            disconDevice(false);
        }
        mDevice = bd;
        Log.i(TAG,"connectDevice state 0x"+Integer.toHexString(bd.getBondState()));
        if(bd.getBondState()!=BluetoothDevice.BOND_BONDED&&bd.getBondState()!=BluetoothDevice.BOND_BONDING) {
            bd.createBond();
        }else if(bd.getBondState()==BluetoothDevice.BOND_BONDED){
            connectProfile();
        }
    }
    public void disconDevice(boolean unPair){

        if(a2dpsink!=null&&a2dpsink.getConnectionState(mDevice)!=BluetoothA2dpSink.STATE_DISCONNECTED){
            a2dpsink.disconnect(mDevice);
        }
        if(hfpclient!=null&&hfpclient.getConnectionState(mDevice)!=BluetoothHeadsetClient.STATE_DISCONNECTED){
            hfpclient.disconnect(mDevice);
        }
        if(pbap!=null&&pbap.getConnectionState(mDevice)!=BluetoothPbapClient.STATE_DISCONNECTED)
            pbap.disconnect(mDevice);

        if(unPair) {
            mDevice.removeBond();
        }
    }
    public boolean isDeviceBusy(){
        return mDevice!=null&&mDevice.getBondState()==BluetoothDevice.BOND_BONDING;
    }

    public Set<BluetoothDevice> getPairedDevice(){
        return mAdapter.getBondedDevices();
    }

    public BluetoothDevice getBoundedDevice(){
        Set<BluetoothDevice> devices = mAdapter.getBondedDevices();
        BluetoothDevice device=null;
        for(Iterator<BluetoothDevice> it = devices.iterator(); it.hasNext();) {
            device = it.next();
            if(device.getBondState()==BluetoothDevice.BOND_BONDED) {
                break;
            }
            device= null;
        }
        return device;
    }
    public boolean isSameDeivce(BluetoothDevice bd){
        return mDevice!=null&&bd!=null&&mDevice.getAddress().equals(bd.getAddress());
    }

    //profile avrcp a2dpsink pbap hfpclient
    public void connectProfile(){
        Log.i(TAG,"connectProfile");
        if(a2dpsink!=null)
            a2dpsink.connect(mDevice);
        if(hfpclient!=null)
            hfpclient.connect(mDevice);
    }
    //a2dpclient
    public boolean isPlaying(){
        if(a2dpsink==null)
            return false;

        boolean play=  a2dpsink.isA2dpPlaying(mDevice);
        Log.i(TAG,"isplaying :"+play);
        return play;
    }

    //avrcp
    public void play(){
        if(avrcp==null)
            return;
        avrcp.sendPassThroughCmd(mDevice,BluetoothAvrcp.PASSTHROUGH_ID_PLAY, BluetoothAvrcp.PASSTHROUGH_STATE_PRESS);
        avrcp.sendPassThroughCmd(mDevice,BluetoothAvrcp.PASSTHROUGH_ID_PLAY, BluetoothAvrcp.PASSTHROUGH_STATE_RELEASE);
    }
    public void pause(){
        if(avrcp==null)
            return;
        avrcp.sendPassThroughCmd(mDevice,BluetoothAvrcp.PASSTHROUGH_ID_PAUSE, BluetoothAvrcp.PASSTHROUGH_STATE_PRESS);
        avrcp.sendPassThroughCmd(mDevice,BluetoothAvrcp.PASSTHROUGH_ID_PAUSE, BluetoothAvrcp.PASSTHROUGH_STATE_RELEASE);
    }
    public void prev(){
        if(avrcp==null)
            return;
        avrcp.sendPassThroughCmd(mDevice,BluetoothAvrcp.PASSTHROUGH_ID_BACKWARD, BluetoothAvrcp.PASSTHROUGH_STATE_PRESS);
        avrcp.sendPassThroughCmd(mDevice,BluetoothAvrcp.PASSTHROUGH_ID_BACKWARD, BluetoothAvrcp.PASSTHROUGH_STATE_RELEASE);
    }
    public void next(){
        if(avrcp==null)
            return;
        avrcp.sendPassThroughCmd(mDevice,BluetoothAvrcp.PASSTHROUGH_ID_FORWARD, BluetoothAvrcp.PASSTHROUGH_STATE_PRESS);
        avrcp.sendPassThroughCmd(mDevice,BluetoothAvrcp.PASSTHROUGH_ID_FORWARD, BluetoothAvrcp.PASSTHROUGH_STATE_RELEASE);
    }
    public void getElement(){
        avrcp.getElementAttrCmd();
    }
    //hfpclient
    public void switchAudio(){
        if(hfpclient==null)
            return;
        int state = hfpclient.getAudioState(mDevice);
        if(state==BluetoothHeadsetClient.STATE_AUDIO_DISCONNECTED){
            hfpclient.connectAudio(mDevice);
        }else{
            hfpclient.disconnectAudio(mDevice);
        }
    }
    public void call(String num){
        if(hfpclient==null)
            return;
        hfpclient.dial(mDevice,num);
    }
    public void handup(){
        if(hfpclient==null)
            return;
        List<BluetoothHeadsetClientCall> cur = hfpclient.getCurrentCalls(mDevice);
        for(BluetoothHeadsetClientCall call:cur){
            if(call.getState()==BluetoothHeadsetClientCall.CALL_STATE_INCOMING){
                hfpclient.rejectCall(mDevice);
            }else{
                hfpclient.terminateCall(mDevice,call);
            }
        }
    }
    public void accept(){
        if(hfpclient==null)
            return;
        hfpclient.acceptCall(mDevice,0);
    }

    //pbap
    public void downLoadPhonebook(){
        if(pbap!=null)
            pbap.connect(mDevice);
    }
    @Override
    public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
        switch (i){
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
    }

    @Override
    public void onServiceDisconnected(int i) {
        switch (i){
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


    //
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED.equals(action)){
                BluetoothDevice dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(dev!=mDevice)
                    return;
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE,-1);
                mctx.sendMsg(MainActivity.DEVICE_STATE_CHANGE,null);
            }else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
                Log.i(TAG,"discovery started ");
            }else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED .equals(action)){
                Log.i(TAG,"discovery finish ");
            }else if(BluetoothDevice.ACTION_FOUND .equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.i(TAG,"found device "+device);
                mctx.sendMsg(MainActivity.DEVICE_ADD,device);

            }else if(BluetoothDevice.ACTION_BOND_STATE_CHANGED .equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String state = intent.getStringExtra(BluetoothDevice.EXTRA_BOND_STATE );
            }else if(BluetoothDevice.ACTION_NAME_CHANGED  .equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME );
            }else if(BluetoothA2dpSink.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {//a2dpclitent
                mctx.sendMsg(MainActivity.DEVICE_STATE_CHANGE,null);
            }else if(BluetoothA2dpSink.ACTION_PLAYING_STATE_CHANGED.equals(action)){

            }else if(BluetoothAvrcpController.ACTION_CONNECTION_STATE_CHANGED.equals(action)){//avrcp
                mctx.sendMsg(MainActivity.DEVICE_STATE_CHANGE,null);
            }else if(BluetoothAvrcpController.ACTION_TRACK_EVENT.equals(action)){

            }else if("com.android.getelementattrrsp".equals(action)){
                String artist = intent.getStringExtra("artist");
                String trackTitle = intent.getStringExtra("trackTitle");
                String album = intent.getStringExtra("album");
                Log.i(TAG,"getElementAttrRsp,artist: " + artist + ",trackTitle: " + trackTitle + ",album: " + album);
            }else if(BluetoothHeadsetClient.ACTION_CONNECTION_STATE_CHANGED.equals(action)){//hfpclient
                mctx.sendMsg(MainActivity.DEVICE_STATE_CHANGE,null);
            }else if(BluetoothHeadsetClient.ACTION_AUDIO_STATE_CHANGED.equals(action)){
                mctx.sendMsg(MainActivity.DEVICE_STATE_CHANGE,null);
            }else if(BluetoothHeadsetClient.ACTION_CALL_CHANGED.equals(action)){

            }else if(BluetoothPbapClient.ACTION_CONNECTION_STATE_CHANGED.equals(action)){//pbap

            }
        }
    };
    public void registerReceiver(){
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

        mctx.registerReceiver(mReceiver,filter,null,mHandler);
    }
}
