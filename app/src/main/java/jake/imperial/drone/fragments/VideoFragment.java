package jake.imperial.drone.fragments;


import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.koushikdutta.ion.Ion;

import jake.imperial.drone.DroneApplication;
import jake.imperial.drone.R;
import jake.imperial.drone.utils.Constants;


public class VideoFragment extends Fragment {
    private static final String TAG = VideoFragment.class.getName();
    private DroneApplication app;
    private BroadcastReceiver broadcastReceiver;

    private Handler mHandler;
    private int mInterval = 10000;
    private ImageView imageView;
    private String domain = "";

    public VideoFragment() {
    }

    public static VideoFragment newInstance() {
        return new VideoFragment();
    }

    @Override
    public void onResume(){
        Log.d(TAG, ".onResume() entered()");
        super.onResume();
        app = (DroneApplication) getActivity().getApplication();

        if (broadcastReceiver == null) {
            Log.d(TAG, ".onResume() - Registering VideoBroadcastReceiver");
            broadcastReceiver = new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    if(app.getCurrentRunningActivity().equals(TAG)) {
                        Log.d(TAG, ".onReceive() - Received intent for VideoBroadcastReceiver");
                        processIntent(intent);
                    }
                }
            };
        }

        IntentFilter intentFilter = new IntentFilter(Constants.APP_ID + "." + Constants.ALERT_EVENT);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(broadcastReceiver, intentFilter);

        domain = app.getDomain();
        String url = "http://"+domain+"/getLatestImage";
        Ion     .with(getContext())
                .load(url)
                .noCache()
                .withBitmap()
                .error(R.drawable.control_pad_button)
                .crossfade(true)
                .intoImageView(imageView);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_video, container, false);
        imageView = (ImageView) rootView.findViewById(R.id.imageview);
        mHandler = new Handler();

        //startRepeatingTask();
        return rootView;
    }

    @Override
    public void onDestroyView(){
        super.onDestroyView();
        stopRepeatingTask();
    }

    private Runnable updateView = new Runnable() {
        @Override
        public void run() {
            if(app != null) {
                domain = app.getDomain();
                String url = "http://" + domain + "/getLatestImage";
                Log.d(TAG, "Updating image from " + url);

                // null check on context?

                Ion.with(getContext())
                        .load(url)
                        .noCache()
                        .withBitmap()
                        .error(R.drawable.control_pad_button)
                        .crossfade(true)
                        .intoImageView(imageView);
            }
            mHandler.postDelayed(updateView, mInterval);
        }
    };

    private void startRepeatingTask() {
        updateView.run();
    }

    private void stopRepeatingTask() {
        mHandler.removeCallbacks(updateView);
    }

    private void processIntent(Intent intent){
        if(!app.getCurrentRunningActivity().equals(TAG)){return;}
        String data = intent.getStringExtra(Constants.INTENT_DATA);
        assert data != null;
        if (data.equals(Constants.ALERT_EVENT)) {
            String message = intent.getStringExtra(Constants.INTENT_DATA_MESSAGE);
            new AlertDialog.Builder(getActivity())
                    .setTitle("Alert:")
                    .setMessage(message)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        }
                    }).show();
        }
    }
}


