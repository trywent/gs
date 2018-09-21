package com.example.bt;

import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadata;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;


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

    View root;
    Button pre,play,next;
    TextView title;
    TextView info;
    SeekBar progress;
    int trackNum=0;
    int trackTotal=0;
    long duration = 0;

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        if(root!=null)
            return root;
        root =  inflater.inflate(R.layout.fragment_music, container, false);
        pre = root.findViewById(R.id.pre);
        play = root.findViewById(R.id.play);
        next = root.findViewById(R.id.next);
        title = root.findViewById(R.id.title);
        info = root.findViewById(R.id.info);
        progress = root.findViewById(R.id.seekBar);
        progress.setMax(100);
        progress.setMin(0);

        pre.setOnClickListener(this);
        play.setOnClickListener(this);
        next.setOnClickListener(this);

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
        if(trackNum!=curTrackNum||trackTotal!=curTrackTotal||duration!=curDuration){
            trackNum = curTrackNum;
            trackTotal = curTrackTotal;
            duration = curDuration;
            String artist = data.getString(MediaMetadata.METADATA_KEY_ARTIST);
            String trackTitle = data.getString(MediaMetadata.METADATA_KEY_TITLE);
            String album = data.getString(MediaMetadata.METADATA_KEY_ALBUM);
            if(info!=null)
                info.setText("\t歌曲名:"+trackTitle+"\n"+"\t歌手:"+artist+"\n"+"\t专辑名:"+album +"\n\t歌曲长度:"+duration);
            if(title!=null)
                title.setText(" ");
        }else{
            String trackTitle = data.getString(MediaMetadata.METADATA_KEY_TITLE);
            if(title!=null)
            title.setText(trackTitle);
        }
    }
    public void updateState(PlaybackState state){
        if(play==null||progress==null)
            return;
        if(state.getState()==PlaybackState.STATE_PLAYING){
            play.setText("pause");
            playing = true;
        }
        else{
            play.setText("play");
            playing = false;
        }
        long pos = state.getPosition();
        if(duration>0&&pos>0){
            long val = (100*pos)/duration;
            //Log.i("BTM","duration "+state.getPosition()+"val "+val);
            progress.setProgress((int)val);
        }
    }
    @Override
    public void onClick(View view) {
        if(view.getId()==R.id.play){
            //playing = btm.isPlaying();
            if(playing){
                btm.pause();
                //play.setText("play");
            }else{
                btm.play();
                //play.setText("pause");
            }

        }else if(view.getId()==R.id.pre){
            btm.prev();
        }else if(view.getId()==R.id.next){
            btm.next();
        }
        /*new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                btm.getElement();
            }
        }, 500);*/

    }
}
