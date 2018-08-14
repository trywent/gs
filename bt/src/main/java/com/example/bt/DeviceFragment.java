package com.example.bt;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link DeviceFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link DeviceFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DeviceFragment extends AFragment implements AdapterView.OnItemSelectedListener , CompoundButton.OnCheckedChangeListener
    ,View.OnClickListener,AdapterView.OnItemClickListener {
    Switch bluetoothSwitch;
    Button scan;
    Button connect;
    Button disconnect;
    Button unpair;
    TextView connectedDevice;
    ListView devices;
    ArrayAdapter<BluetoothDevice> mAdapter;
    List<BluetoothDevice> cachedDevcie;
    View root;
    View selectedView;
    BluetoothDevice curbd;

    public DeviceFragment() {
        // Required empty public constructor
        super();
        cachedDevcie = new ArrayList<BluetoothDevice>();

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if(root!=null){
            connectedDevice.setText(btm.getState());
            return root;
        }
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_device, container, false);
        bluetoothSwitch  = v.findViewById(R.id.switch1);
        bluetoothSwitch.setOnCheckedChangeListener(this);

        scan = v.findViewById(R.id.scan);
        scan.setOnClickListener(this);

        connect = v.findViewById(R.id.conn);
        connect.setOnClickListener(this);

        disconnect = v.findViewById(R.id.disconn);
        disconnect.setOnClickListener(this);
        unpair = v.findViewById(R.id.unpair);
        unpair.setOnClickListener(this);

        connectedDevice = v.findViewById(R.id.textView4);
        connectedDevice.setText(btm.getState());

        devices = v.findViewById(R.id.listview1);
        devices.setOnItemSelectedListener(this);
        devices.setOnItemClickListener(this);
        mAdapter = new MyAdapter<BluetoothDevice>(getContext(),R.layout.mlist);
        devices.setAdapter(mAdapter);
        if(btm.isEnabled()) {
            bluetoothSwitch.setChecked(true);
            bluetoothSwitch.setText("bluetooth on");
        }else{
            bluetoothSwitch.setChecked(false);
            bluetoothSwitch.setText("bluetooth off");
        }
        if(!cachedDevcie.isEmpty()){
            for(BluetoothDevice d:cachedDevcie){
                mAdapter.add(d);
            }
        }else {
            Set<BluetoothDevice> devices = btm.getPairedDevice();
            if (devices != null) {
                for (Iterator<BluetoothDevice> it = devices.iterator(); it.hasNext(); ) {
                    BluetoothDevice d = it.next();
                    addDevice(d);
                    if (d.getBondState() == BluetoothDevice.BOND_BONDED) {
                        connectedDevice.setText(btm.getState());//d.getName() + "\r\n" + d.getAddress());
                    }
                }
            }
        }
        root = v;
        return v;
    }

    //listview

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    //bluetooth switch
    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        btm.setBluetooth(b);
        bluetoothSwitch.setText(b?"bluetooth on":"bluetooth off");
    }
    //button
    @Override
    public void onClick(View view) {
        if(view.getId()==R.id.scan){
            mAdapter.clear();
            cachedDevcie.clear();
            btm.startDiscover();
        }else if(view.getId()==R.id.conn){
            if(curbd==null)
                return;
            btm.connectDevice(curbd);
        }else if(view.getId()==R.id.disconn){
            btm.disconDevice(false);
        }else if(view.getId()==R.id.unpair){
            btm.disconDevice(true);
        }

    }
    public void updateState(){
        connectedDevice.setText(btm.getState());
    }
    public void addDevice(BluetoothDevice device){
        for(BluetoothDevice d:cachedDevcie){
            String addr = device.getAddress();
            if(d.getAddress().equals(addr))
                return;
        }
        cachedDevcie.add(device);
        mAdapter.add(device);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        if(selectedView!=null)
            selectedView.setBackgroundColor(Color.WHITE);
        selectedView = view;
        selectedView.setBackgroundColor(Color.GRAY);
        curbd = (BluetoothDevice)adapterView.getItemAtPosition(i);
    }

    class MyAdapter<T> extends ArrayAdapter{
        int viewId;
        public MyAdapter(Context ctx,int res){
            super(ctx,res);
            viewId = res;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            BluetoothDevice d = (BluetoothDevice) getItem(position);
            View v = LayoutInflater.from(getContext()).inflate(viewId, null);
            TextView txtView = v.findViewById(R.id.textView5);
            txtView.setText(d.getName()+"  addr:"+d.getAddress());
            return txtView;
        }
    }

}
