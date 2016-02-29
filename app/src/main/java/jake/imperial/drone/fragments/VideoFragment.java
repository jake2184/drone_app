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
    protected DroneApplication app;
    protected BroadcastReceiver broadcastReceiver;

    private Handler mHandler;
    private int mInterval = 10000;
    private ImageView imageView;

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
        app.setCurrentRunningActivity(TAG);

        if (broadcastReceiver == null) {
            Log.d(TAG, ".onResume() - Registering LogBroadcastReceiver");
            broadcastReceiver = new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d(TAG, ".onReceive() - Received intent for videoBroadcastReceiver");
                    processIntent(intent);
                }
            };
        }

        getActivity().getApplicationContext().registerReceiver(broadcastReceiver,
                new IntentFilter(Constants.APP_ID + Constants.INTENT_CONTROL));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_video, container, false);
        imageView = (ImageView) rootView.findViewById(R.id.imageview);
        mHandler = new Handler();
        startRepeatingTask();
        return rootView;
    }

    Runnable updateView = new Runnable() {
        @Override
        public void run() {

            Log.d(TAG, "Updating image");

            Long tsLong = System.currentTimeMillis()/1000;
            String ts = tsLong.toString();
            Ion     .with(getContext())
                    .load("http://drone-nodes.eu-gb.mybluemix.net/getLatestImage?time=" + ts)
                    //.setHeader("Cache-Control", "no-cache")
                    //.addHeader("Cache-Control", "no-cache")
                    .noCache()
                    .withBitmap()
                    .error(R.drawable.control_pad_button)
                    .crossfade(true)
                    .intoImageView(imageView);

            mHandler.postDelayed(updateView, mInterval);
        }
    };

    void startRepeatingTask() {
        updateView.run();
    }

    void stopRepeatingTask() {
        mHandler.removeCallbacks(updateView);
    }

    private void processIntent(Intent intent){
        String data = intent.getStringExtra(Constants.INTENT_DATA);
        assert data != null;
        if (data.equals(Constants.TEXT_EVENT)) {
            // Log them somehow?
        } else if (data.equals(Constants.ALERT_EVENT)) {
            String message = intent.getStringExtra(Constants.INTENT_DATA_MESSAGE);
            new AlertDialog.Builder(getActivity())
                    .setTitle("Bing")
                    .setMessage(message)
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        }
                    }).show();
        }
    }
}


