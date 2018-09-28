package com.example.bt;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadsetClientCall;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link PhoneFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link PhoneFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PhoneFragment extends AFragment implements View.OnClickListener,AdapterView.OnItemClickListener,View.OnLongClickListener{
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static String TAG = BtManager.TAG;
    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    static final int MSG_UDPATE = 1;
    static final int MSG_CONTACT = 2;

    private OnFragmentInteractionListener mListener;
    View root;
    Button bt1,bt2,bt3,bt4,bt5,bt6,bt7,bt8,bt9,bt10,bt11,bt12;
    Button del;
    Button call;
    Button hangup;
    Button audio;
    TextView number;
    ListView contacts;
    ListView recentsCall;
    View selectedView;
    StringBuilder builder;
    MyAdapter mAdapter;
    H mhandler;

    static final String[] PROJECTION = new String[] {ContactsContract.RawContacts._ID,
            ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY};

    // This is the select criteria
    static final String SELECTION = "((" +
            ContactsContract.RawContacts.ACCOUNT_NAME + " NOTNULL) AND (" +
            ContactsContract.RawContacts.ACCOUNT_NAME + " != '' ))";

    public PhoneFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment PhoneFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static PhoneFragment newInstance(String param1, String param2) {
        PhoneFragment fragment = new PhoneFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        if(root!=null){
            //btm.pause();
            return root;
        }
        builder = new StringBuilder();
        View view =  inflater.inflate(R.layout.fragment_phone, container, false);
        bt1 = view.findViewById(R.id.num1);
        bt2 = view.findViewById(R.id.num2);
        bt3 = view.findViewById(R.id.num3);
        bt4 = view.findViewById(R.id.num4);
        bt5 = view.findViewById(R.id.num5);
        bt6 = view.findViewById(R.id.num6);
        bt7 = view.findViewById(R.id.num7);
        bt8 = view.findViewById(R.id.num8);
        bt9 = view.findViewById(R.id.num9);
        bt10 = view.findViewById(R.id.num10);
        bt11 = view.findViewById(R.id.num11);
        bt12 = view.findViewById(R.id.num12);
        del = view.findViewById(R.id.delete);
        call = view.findViewById(R.id.call);
        hangup = view.findViewById(R.id.hangup);
        audio = view.findViewById(R.id.audio);
        number = view.findViewById(R.id.textView3);
        contacts = view.findViewById(R.id.contact);
        recentsCall = view.findViewById(R.id.recent);

        contacts.setOnItemClickListener(this);
        bt1.setOnClickListener(this);
        bt2.setOnClickListener(this);
        bt3.setOnClickListener(this);
        bt4.setOnClickListener(this);
        bt5.setOnClickListener(this);
        bt6.setOnClickListener(this);
        bt7.setOnClickListener(this);
        bt8.setOnClickListener(this);
        bt9.setOnClickListener(this);
        bt10.setOnClickListener(this);
        bt11.setOnClickListener(this);
        bt12.setOnClickListener(this);
        del.setOnClickListener(this);
        del.setOnLongClickListener(this);
        call.setOnClickListener(this);
        hangup.setOnClickListener(this);
        audio.setOnClickListener(this);

        number.setBackgroundColor(Color.WHITE);
        number.setTextSize(30);
        root = view;

        mAdapter = new MyAdapter<String[][]>(getContext(),R.layout.mlist);
        contacts.setAdapter(mAdapter);
        btm.downLoadPhonebook();

        mhandler = new H();
        Message msg = mhandler.obtainMessage(MSG_CONTACT);
        mhandler. sendMessageDelayed(msg,10000);
        updateInfo();
        btm.pause();
        return view;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onClick(View view) {
        char c=0;
        if(view.getId()==R.id.num1){
            c = '1';
        }else if(view.getId()==R.id.num2){
            c = '2';
        }else if(view.getId()==R.id.num3){
            c = '3';
        }else if(view.getId()==R.id.num4){
            c = '4';
        }else if(view.getId()==R.id.num5){
            c = '5';
        }else if(view.getId()==R.id.num6){
            c = '6';
        }else if(view.getId()==R.id.num7){
            c = '7';
        }else if(view.getId()==R.id.num8){
            c = '8';
        }else if(view.getId()==R.id.num9){
            c = '9';
        }else if(view.getId()==R.id.num10){
            c = '*';
        }else if(view.getId()==R.id.num11){
            c = '0';
        }else if(view.getId()==R.id.num12){
            c = '#';
        }else if(view.getId()==R.id.delete){
            int index = builder.length()-1;
            if(index>=0)
                builder.deleteCharAt(index);
        }else if(view.getId()==R.id.call){
            String num = number.getText().toString();
            btm.call(num);
            return;
        }else if(view.getId()==R.id.hangup){
            btm.handup();
            return;
        }else if(view.getId()==R.id.audio){
            btm.switchAudio();
            return;
        }
        if(c!=0)
            builder.append(c);
        int length = builder.length();
        number.setText(builder.subSequence(0,length));
        if(btm.getCallState()!= BluetoothHeadsetClientCall.CALL_STATE_TERMINATED)
            btm.dail((byte)c);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        if(selectedView!=null)
            selectedView.setBackgroundColor(Color.WHITE);
        selectedView = view;
        selectedView.setBackgroundColor(Color.GRAY);
        String[][] info  = (String[][])adapterView.getItemAtPosition(i);
        number.setText(info[0][1]);
    }

    public void updateInfo(){
        if(root==null){
            return;
        }
        if(btm.getCallState()!= BluetoothHeadsetClientCall.CALL_STATE_TERMINATED){
            call.setBackgroundColor(Color.GREEN);
            hangup.setBackgroundColor(Color.RED);
            audio.setBackgroundColor(Color.WHITE);
        }else{
            call.setBackgroundColor(Color.WHITE);
            hangup.setBackgroundColor(Color.WHITE);
            audio.setBackgroundColor(Color.WHITE);
        }
        if(btm.getCallState()!= BluetoothHeadsetClientCall.CALL_STATE_INCOMING){
            String n = btm.getNum();
            if(n!=null)
            number.setText(n);
        }

    }
    public void getPhoneBook(){
        Message msg = mhandler.obtainMessage(MSG_CONTACT);
        mhandler.removeMessages(MSG_CONTACT);
        mhandler. sendMessage(msg);
    }

    @Override
    public boolean onLongClick(View view) {
        if(view.getId()==R.id.delete){

        }
        return true;
    }

    class H extends Handler{
        @Override
        public void handleMessage(Message msg) {
            int what = msg.what;
            switch (what){
                case MSG_UDPATE:
                    String[][] info = (String[][])msg.obj;
                    mAdapter.add(info);
                    break;
                case MSG_CONTACT://start get contact from provider
                    new DownloadAsynTask().execute();
                    break;
                default:break;

            }
        }
    }
    class MyAdapter<T> extends ArrayAdapter {
        int viewId;
        public MyAdapter(Context ctx,int res){
            super(ctx,res);
            viewId = res;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            String[][] s = (String[][]) getItem(position);
            //Log.i(TAG,"view name"+s[0][0]+"   num:"+s[0][1]);
            View v = LayoutInflater.from(getContext()).inflate(viewId, null);
            TextView txtView = v.findViewById(R.id.textView5);
            txtView.setText(s[0][0]+"  "+s[0][1]);
            return txtView;
        }
    }
    private class DownloadAsynTask extends AsyncTask<Void, Integer, Boolean> {


        public DownloadAsynTask() {

        }

        protected void onPreExecute() {
        }

        @Override
        protected void onPostExecute(Boolean result) {

        }

        @Override
        protected void onProgressUpdate(Integer... progresses) {

        }

        @Override
        protected Boolean doInBackground(Void... paramArrayOfParams) {
            Context mContext = getContext();
            if(mContext==null)
                return true;
            //联系人的Uri，也就是content://com.android.contacts/contacts
            Uri uri = ContactsContract.RawContacts.CONTENT_URI;
            //指定获取_id和display_name两列数据，display_name即为姓名
            String[] projection = new String[] {
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
            };
            //根据Uri查询相应的ContentProvider，cursor为获取到的数据集
            Cursor cursor = mContext.getContentResolver().query(uri, projection, null, null, null);
            String[][] arr = new String[cursor.getCount()][2];

            int i = 0;
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Long id = cursor.getLong(0);
                    //获取姓名
                    String name = cursor.getString(1);
                    //指定获取NUMBER这一列数据
                    String[] phoneProjection = new String[] {
                            ContactsContract.CommonDataKinds.Phone.NUMBER
                    };
                    arr[i][0] = name;

                    //根据联系人的ID获取此人的电话号码
                    Cursor phonesCusor = mContext.getContentResolver().query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            phoneProjection,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=" + id,
                            null,
                            null);

                    //因为每个联系人可能有多个电话号码，所以需要遍历
                    if (phonesCusor != null && phonesCusor.moveToFirst()) {
                        do {
                            String num = phonesCusor.getString(0);
                            arr[i][1] =  num;
                        }while (phonesCusor.moveToNext());
                    }
                    Message msg = mhandler.obtainMessage(MSG_UDPATE,new String[][]{arr[i]});
                    mhandler.sendMessage(msg);
                    i++;
                } while (cursor.moveToNext());
            }
            return true;
        }
    }
}
