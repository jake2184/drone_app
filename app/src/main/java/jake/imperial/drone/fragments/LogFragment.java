package jake.imperial.drone.fragments;


import android.app.AlertDialog;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import jake.imperial.drone.DroneApplication;
import jake.imperial.drone.R;
import jake.imperial.drone.utils.Constants;


public class LogFragment extends ListFragment {
    private static final String TAG = LogFragment.class.getName();
    private DroneApplication app;
    private BroadcastReceiver broadcastReceiver;

    private ArrayAdapter<String> listAdapter;

    public LogFragment() {
    }

    public static LogFragment newInstance() {
        return new LogFragment();
    }

    @Override
    public void onResume(){
        Log.d(TAG, ".onResume() entered()");
        super.onResume();
        app = (DroneApplication) getActivity().getApplication();
        app.setCurrentRunningActivity(TAG);
        listAdapter = new ArrayAdapter<>(getContext(), R.layout.list_item, app.getMessageLog());
        setListAdapter(listAdapter);
        if (broadcastReceiver == null) {
            Log.d(TAG, ".onResume() - Registering LogBroadcastReceiver");
            broadcastReceiver = new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d(TAG, ".onReceive() - Received intent for LogBroadcastReceiver");
                    processIntent(intent);
                }
            };
        }

        IntentFilter intentFilter = new IntentFilter(Constants.APP_ID + "." + Constants.ALERT_EVENT);
        intentFilter.addAction(Constants.APP_ID + "." + Constants.LOG_EVENT);
        getActivity().getApplicationContext().registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_log, container, false);

    }

    private void processIntent(Intent intent){
        String data = intent.getStringExtra(Constants.INTENT_DATA);
        assert data != null;
        if (data.equals(Constants.TEXT_EVENT)) {
            listAdapter.notifyDataSetInvalidated();
        } else if (data.equals(Constants.ALERT_EVENT) && app.getCurrentRunningActivity().equals(TAG)) {
            listAdapter.notifyDataSetInvalidated();
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


