package com.example.bt;

import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadata;
import android.media.browse.MediaBrowser;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MusicFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MusicFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MusicFragment extends AFragment implements View.OnClickListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    Handler mHandler;
    View root;
    Button pre,play,next,butt;
    TextView title;
    TextView info;
    SeekBar progress;
    ArrayAdapter<MediaBrowser.MediaItem> mAdapter;
    ListView playlist;
    String artist;
    String trackTitle;
    String album;
    int trackNum=0;
    int trackTotal=0;
    long duration = 0;
    long curPos = 0,timerVal = 0;

    boolean playing=false;

    public MusicFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MusicFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static MusicFragment newInstance(String param1, String param2) {
        MusicFragment fragment = new MusicFragment();
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
    public void onStop(){
        super.onStop();
    }
    public void onDestroy(){
        super.onDestroy();
    }
    public void onDetach(){
        super.onDetach();
        //if(playing)
        //    btm.pause();
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        if(root!=null) {
            btm.play();
            return root;
        }
        mHandler = new MHander();
        root =  inflater.inflate(R.layout.fragment_music, container, false);
        pre = root.findViewById(R.id.pre);
        play = root.findViewById(R.id.play);
        next = root.findViewById(R.id.next);
        butt = root.findViewById(R.id.button2);
        title = root.findViewById(R.id.title);
        info = root.findViewById(R.id.info);
        mAdapter = new MyAdapter<MediaBrowser.MediaItem>(getContext(),R.layout.mlist);
        playlist = root.findViewById(R.id.playlist);
        playlist.setAdapter(mAdapter);
        progress = root.findViewById(R.id.seekBar);
        progress.setMax(100);
        progress.setMin(0);

        pre.setOnClickListener(this);
        play.setOnClickListener(this);
        next.setOnClickListener(this);
        butt.setOnClickListener(this);
        btm.play();
        return root;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    public void updateInfo(MediaMetadata data) {
        int curTrackNum = (int)data.getLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER);
        int curTrackTotal = (int)data.getLong(MediaMetadata.METADATA_KEY_NUM_TRACKS);
        int curDuration = (int)data.getLong(MediaMetadata.METADATA_KEY_DURATION);
        if(trackNum!=curTrackNum||trackTotal!=curTrackTotal){
            Log.i("BTM","duration "+duration+" curdur "+curDuration+" track "+trackNum+" total "+trackTotal);
            trackNum = curTrackNum;
            trackTotal = curTrackTotal;
            duration = curDuration;
            artist = data.getString(MediaMetadata.METADATA_KEY_ARTIST);
            trackTitle = data.getString(MediaMetadata.METADATA_KEY_TITLE);
            album = data.getString(MediaMetadata.METADATA_KEY_ALBUM);
            int s = (int)(duration/1000.0);
            int min = (int)(s/60.0);
            int sec = s%60;
            if(info!=null)
                info.setText("\t歌曲名:"+trackTitle+"\n"+"\t歌手:"+artist+"\n"+"\t专辑名:"+album +"\n\t歌曲长度:"+min+"m"+sec+"s");
            if(title!=null)
                title.setText(" ");
        }else if((curDuration>0 && duration != curDuration)){//有些播放器时间第二次才正确.
            curPos = 0;
            duration = curDuration;
            int s = (int)(duration/1000.0);
            int min = (int)(s/60.0);
            int sec = s%60;
            if(info!=null)
                info.setText("\t歌曲名:"+trackTitle+"\n"+"\t歌手:"+artist+"\n"+"\t专辑名:"+album +"\n\t歌曲长度:"+min+"m"+sec+"s");
        }else{
            String trackTitle = data.getString(MediaMetadata.METADATA_KEY_TITLE);
            if(title!=null)
            title.setText(trackTitle);
        }
    }
    public void updateState(PlaybackState state){
        if(play==null||progress==null)
            return;
        //Log.i("BTM","state "+state.getState());
        if(state.getState()==PlaybackState.STATE_PLAYING){
            play.setText("pause");
            playing = true;
            mHandler.sendMessageDelayed(mHandler.obtainMessage(UPDATE),1000);
        }
        else{
            play.setText("play");
            playing = false;
        }
        curPos = state.getPosition();
        if(duration>0 && curPos>0){
            long val = (100*curPos)/duration;
            //Log.i("BTM","duration "+state.getPosition()+"val "+val);
            progress.setProgress((int)val);
        }
    }
    @Override
    public void onClick(View view) {
        if(view.getId()==R.id.play){
            //playing = btm.isPlaying();
            if(playing){
                //btm.stop();
                playing = false;
                btm.pause();
                //play.setText("play");
            }else{
                playing = true;
                btm.play();
                mHandler.sendMessageDelayed(mHandler.obtainMessage(UPDATE),1000);
                //play.setText("pause");
            }

        }else if(view.getId()==R.id.pre){
            btm.prev();
        }else if(view.getId()==R.id.next){
            btm.next();
        }else if(view.getId()==R.id.button2){
            btm.pause();
            //btm.getPlaylist();
        }
        /*new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                btm.getElement();
            }
        }, 500);*/

    }
    class MyAdapter<T> extends ArrayAdapter {
        int viewId;
        public MyAdapter(Context ctx,int res){
            super(ctx,res);
            viewId = res;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            MediaBrowser.MediaItem media = (MediaBrowser.MediaItem) getItem(position);
            View v = LayoutInflater.from(getContext()).inflate(viewId, null);
            TextView txtView = v.findViewById(R.id.textView5);
            txtView.setText(media.getDescription().getTitle());
            return txtView;
        }
    }
    public void stopPlay(){
        if(playing)
            btm.pause();
    }
    //
    static final int UPDATE = 1;
    class MHander extends Handler{
        public void handleMessage(Message msg) {
            if(msg.what == UPDATE){
                Log.i("BTM","update "+timerVal +" curPos:"+curPos+ " playing:"+playing);
                if(timerVal >curPos || !playing) return;
                curPos +=1000;
                timerVal = curPos;
                progress.setProgress((int)((100*curPos)/duration));
                this.removeMessages(UPDATE);
                this.sendMessageDelayed(mHandler.obtainMessage(UPDATE),1000);
            }
        }
    }
}
