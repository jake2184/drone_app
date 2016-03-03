package jake.imperial.drone.fragments;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import java.sql.Timestamp;
import java.util.Date;

import jake.imperial.drone.DroneApplication;
import jake.imperial.drone.R;
import jake.imperial.drone.utils.Constants;
import jake.imperial.drone.utils.MqttHandler;


public class ConnectionFragment extends Fragment {
    private final static String TAG = ConnectionFragment.class.getName();
    private DroneApplication app;
    private BroadcastReceiver broadcastReceiver;


    public ConnectionFragment() {
    }

    public static ConnectionFragment newInstance() {
        return new ConnectionFragment();
    }

    @Override
    public void onResume(){
        Log.d(TAG, ".onResume() entered");
        super.onResume();
        app = (DroneApplication) getActivity().getApplication();
        //app.setCurrentRunningActivity(TAG);

        if (broadcastReceiver == null) {
            Log.d(TAG, ".onResume() - Registering ConnectionBroadcastReceiver");
            broadcastReceiver = new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d(TAG, ".onReceive() - Received intent for ConnectionBroadcastReceiver");
                    processIntent(intent);
                }
            };
        }
        IntentFilter intentFilter = new IntentFilter(Constants.APP_ID + "." + Constants.ALERT_EVENT);
        getActivity().getApplicationContext().registerReceiver(broadcastReceiver, intentFilter);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        app = (DroneApplication)getActivity().getApplication();

        final View rootView = inflater.inflate(R.layout.fragment_connection, container, false);

        SharedPreferences settings = getActivity().getPreferences(0);

        ((EditText)rootView.findViewById(R.id.domain)).setText(settings.getString("domain", ""));
        ((EditText)rootView.findViewById(R.id.organisation)).setText(settings.getString("organisation", ""));
        ((EditText)rootView.findViewById(R.id.auth_token)).setText(settings.getString("auth_token", ""));
        ((EditText)rootView.findViewById(R.id.deviceID)).setText(settings.getString("device_id", ""));

        Button submit = (Button)rootView.findViewById(R.id.submit);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditSettings(rootView);
            }
        });
        Button connect = (Button)rootView.findViewById(R.id.connect);
        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Connect();
            }
        });
        return rootView;
    }

    private void EditSettings(View rootView){
        String domain = ((EditText) rootView.findViewById(R.id.domain)).getText().toString();
        String organisation = ((EditText)rootView.findViewById(R.id.organisation)).getText().toString();
        String auth_token = ((EditText)rootView.findViewById(R.id.auth_token)).getText().toString();
        String deviceID = ((EditText)rootView.findViewById(R.id.deviceID)).getText().toString();

        app.setDomain(domain);

        SharedPreferences.Editor settings = getActivity().getPreferences(0).edit();
        settings.putString("domain", domain);
        settings.putString("organisation", organisation);
        settings.putString("auth_token", auth_token);
        settings.putString("device_id", deviceID);
        settings.commit();
        new AlertDialog.Builder(getActivity())
                .setTitle("Credentials Saved")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                }).show();
    }

    private void Connect() {
        Log.d(TAG, ".Connect() entered");

        MqttHandler mqttHandle = MqttHandler.getInstance(getActivity().getApplicationContext());
        app.setDomain(((EditText)getActivity().findViewById(R.id.domain)).getText().toString());
        app.setDeviceId(((EditText) getActivity().findViewById(R.id.deviceID)).getText().toString());
        app.setOrganization(((EditText) getActivity().findViewById(R.id.organisation)).getText().toString());
        app.setAuthToken(((EditText) getActivity().findViewById(R.id.auth_token)).getText().toString());

        if (checkCanConnect()) {
            if(mqttHandle.connect()){
                Log.d(TAG, "Connection successful");
                app.getMessageLog().add("[" + new Timestamp((new Date()).getTime()) + "]: Connected successfully");
                new AlertDialog.Builder(getActivity())
                        .setTitle("Connected Successfully")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Do nothing.
                            }
                        }).show();
            }
        } else {
            displaySetPropertiesDialog();
        }
    }

    private boolean checkCanConnect() {
        if (app.getOrganization().equals(Constants.QUICKSTART)) {
            app.setConnectionType(Constants.ConnectionType.QUICKSTART);
            if (app.getDeviceId() == null || app.getDeviceId().equals("")) {
                return false;
            }
        } else if (app.getOrganization().equals(Constants.M2M)) {
            app.setConnectionType(Constants.ConnectionType.M2M);
            if (app.getDeviceId() == null || app.getDeviceId().equals("")) {
                return false;
            }
        } else {
            app.setConnectionType(Constants.ConnectionType.IOTF);
            if (app.getOrganization() == null || app.getOrganization().equals("") ||
                    app.getDeviceId() == null || app.getDeviceId().equals("") ||
                    app.getAuthToken() == null || app.getAuthToken().equals("")) {
                return false;
            }
        }
        return true;
    }


    private void displaySetPropertiesDialog() {
        new AlertDialog.Builder(getActivity())
                .setTitle("Can't connect, please verify details")
                //.setMessage(getResources().getString(R.string.connect_props_text))
                .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing.
                    }
                }).show();
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
        } else {
            Log.d(TAG, data);
        }

    }
}
