package com.example.bt;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaMetadata;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements AFragment.OnFragmentInteractionListener{
    private static final String TAG = "BTM";
    //private TextView mTextMessage;
    DeviceFragment device;
    MusicFragment music;
    PhoneFragment phone;
    FragmentManager mFragmentManager;
    BtManager btm;
    Handler mHandler;
    boolean isResumed = false;

    public MainActivity(){}
    void switchFragment(Fragment fg){
        mFragmentManager.beginTransaction().replace(R.id.fragment, fg).commit();
    }
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_device:
                   // mTextMessage.setText(R.string.title_device);
                    if(device==null) {
                        device = new DeviceFragment();//AFragment.newInstance("device","1");
                    }
                    switchFragment(device);
                    return true;
                case R.id.navigation_music:
                    //mTextMessage.setText(R.string.title_music);
                    if(music==null) {
                        music = new MusicFragment();//AFragment.newInstance("music","2");
                        music.setBtManager(btm);
                    }
                    switchFragment(music);
                    return true;
                case R.id.navigation_phone:
                    //mTextMessage.setText(R.string.title_phone);
                    if(phone==null) {
                        phone = new PhoneFragment();//AFragment.newInstance("phone","3");
                        phone.setBtManager(btm);
                    }
                    switchFragment(phone);
                    return true;
            }
            return false;
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btm = BtManager.getInstance(this);
        mHandler = new MyHandler();
        //mTextMessage = (TextView) findViewById(R.id.message);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        int[][] states = new int[][]{
                new int[]{-android.R.attr.state_checked},
                new int[]{android.R.attr.state_checked}
        };
        int[] colors = new int[]{Color.BLACK,Color.GRAY};
        ColorStateList csl = new ColorStateList(states, colors);
        navigation.setItemIconTintList(csl);
        navigation.setItemTextColor(csl);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        mFragmentManager = getSupportFragmentManager();
        //Fragment fg = mFragmentManager.findFragmentById(R.id.fragment);
        //mFragmentManager.beginTransaction().remove(fg).commit();
        device = new DeviceFragment();
        music = new MusicFragment();
        music.setBtManager(btm);
        phone = new PhoneFragment();
        phone.setBtManager(btm);
        switchFragment(device);

        requestPermission();
    }
    protected void onPause(){
        super.onPause();
        music.stopPlay();
        isResumed = false;
    }
    protected void onResume(){
        super.onResume();
        isResumed = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        btm.release();
    }
    public void onBackPressed(){
        finish();
    }
    @Override
    public void onFragmentInteraction(Uri uri) {

    }
    static final int DEVICE_ADD=1;
    static final int DEVICE_STATE_CHANGE=2;
    static final int MUSIC_STATE_CHANGE=3;
    static final int PHONE_STATE_CHANGE=4;
    static final int PHONEBOOTK_STATE_CHANGE=5;
    static final int PHONEBOOK_DOWNLOAD=6;
    static final int MUSIC_METADATA=7;
    class MyHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {

            int event = msg.what;
            switch (event){
                case DEVICE_ADD:
                    BluetoothDevice d = (BluetoothDevice) msg.obj;
                    device.addDevice(d);
                    break;
                case DEVICE_STATE_CHANGE:
                    device.updateState();
                    break;
                case MUSIC_STATE_CHANGE:
                    music.updateState((PlaybackState)msg.obj);
                    break;
                case MUSIC_METADATA:
                    music.updateInfo((MediaMetadata)msg.obj);
                    break;
                case PHONE_STATE_CHANGE:
                    phone.updateInfo();
                    if(isResumed)
                        switchFragment(phone);
                    break;
                case PHONEBOOTK_STATE_CHANGE:
                    phone.updateInfo();
                    break;
                case PHONEBOOK_DOWNLOAD:
                    phone.updatePhoneBook();
                    break;
            }
            //super.handleMessage(msg);
        }
    }
    public void sendMsg(int what,Object obj){
        if(obj!=null){
            mHandler.sendMessage(mHandler.obtainMessage(what,obj));
        }else{
            mHandler.sendMessage(mHandler.obtainMessage(what));
        }

    }
    private void requestPermission(){
        String[] per = new String[]{
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.WRITE_CONTACTS,
                Manifest.permission.CALL_PHONE,
        };
        requestPermissions(per,1);
    }

}
