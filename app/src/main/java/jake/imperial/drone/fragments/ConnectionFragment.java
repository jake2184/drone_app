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
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jake.imperial.drone.DroneApplication;
import jake.imperial.drone.R;
import jake.imperial.drone.utils.Constants;
import jake.imperial.drone.utils.MqttHandler;
import jake.imperial.drone.utils.TopicFactory;


public class ConnectionFragment extends Fragment {
    private final static String TAG = ConnectionFragment.class.getName();
    private DroneApplication app;
    private BroadcastReceiver broadcastReceiver;
    private RequestQueue requestQueue;


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
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(broadcastReceiver, intentFilter);
        requestQueue = Volley.newRequestQueue(getContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        app = (DroneApplication)getActivity().getApplication();

        final View rootView = inflater.inflate(R.layout.fragment_connection, container, false);

        SharedPreferences settings = getActivity().getPreferences(0);

        ((EditText)rootView.findViewById(R.id.domain)).setText(settings.getString("domain", ""));
        ((EditText)rootView.findViewById(R.id.username)).setText(settings.getString("username", ""));
        ((EditText)rootView.findViewById(R.id.password)).setText(settings.getString("password", ""));
        ((EditText)rootView.findViewById(R.id.organisation)).setText(settings.getString("organisation", ""));
        ((EditText)rootView.findViewById(R.id.deviceID)).setText(settings.getString("device_id", ""));
        ((EditText)rootView.findViewById(R.id.api_key)).setText(settings.getString("api_key", ""));
        ((EditText)rootView.findViewById(R.id.api_token)).setText(settings.getString("api_token", ""));

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
        String username = ((EditText) rootView.findViewById(R.id.username)).getText().toString();
        String password = ((EditText) rootView.findViewById(R.id.password)).getText().toString();
        String organisation = ((EditText)rootView.findViewById(R.id.organisation)).getText().toString();
        String deviceID = ((EditText)rootView.findViewById(R.id.deviceID)).getText().toString();
        String APIKey = ((EditText)rootView.findViewById(R.id.api_key)).getText().toString();
        String APIToken = ((EditText)rootView.findViewById(R.id.api_token)).getText().toString();

        app.setDomain(domain);

        SharedPreferences.Editor settings = getActivity().getPreferences(0).edit();
        settings.putString("domain", domain);
        settings.putString("username", username);
        settings.putString("password", password);
        settings.putString("organisation", organisation);
        settings.putString("device_id", deviceID);
        settings.putString("api_key", APIKey);
        settings.putString("api_token", APIToken);
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
        app.setUsername(((EditText)getActivity().findViewById(R.id.username)).getText().toString());
        app.setPassword(((EditText)getActivity().findViewById(R.id.password)).getText().toString());
        app.setOrganization(((EditText) getActivity().findViewById(R.id.organisation)).getText().toString());
        app.setDeviceId(((EditText) getActivity().findViewById(R.id.deviceID)).getText().toString());
        app.setAPIKey(((EditText) getActivity().findViewById(R.id.api_key)).getText().toString());
        app.setAPIToken(((EditText) getActivity().findViewById(R.id.api_token)).getText().toString());

        if (checkCanConnect()) {
            app.getDomain();
            String url = "http://" + app.getDomain() + "/login";
            LoginRequest loginRequest = new LoginRequest(url, app.getUsername(), app.getPassword(), new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.d(TAG, response);

                    try {
                        URI uri = new URI("http://" + app.getDomain());
                        List<HttpCookie> bing = ((CookieManager) CookieManager.getDefault()).getCookieStore().get(uri);
                        Log.d(TAG, "coo");
                    } catch (Exception e){
                        Log.d(TAG, e.toString());
                    }

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d(TAG, error.toString());

                    if(error instanceof AuthFailureError) {
                        Log.d(TAG, "Authentication error");
                    } else if(error instanceof TimeoutError){
                        Log.d(TAG, "Connection timeout error");
                    } else if(error instanceof NetworkError) {
                        Log.d(TAG, "Network Error");
                    }


                }
            });


            requestQueue.add(loginRequest);





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
                //MqttHandler.getInstance(getContext()).subscribe(TopicFactory.getEventTopic("pi", "drone", "sensors"),0);
                MqttHandler.getInstance(getContext()).subscribe(TopicFactory.getEventTopic("node", "server", "image"), 0);
                //MqttHandler.getInstance(getContext()).subscribe(TopicFactory.getEventTopic("node", "server", "event"), 0);

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
                    app.getAPIKey() == null || app.getAPIKey().equals("") ||
                    app.getAPIToken() == null || app.getAPIToken().equals(""))
            {
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

    private class LoginRequest extends StringRequest{

        public LoginRequest(String url, String username, String password, Response.Listener<String> listener, Response.ErrorListener errorListener) {
            super(Request.Method.POST, url, listener, errorListener);
            if (username != null && password != null) {
                String loginEncoded = new String(Base64.encode((username + ":" + password).getBytes(), Base64.NO_WRAP));
                this.headers.put("Authorization", "Basic " + loginEncoded);
            }
        }

        private Map<String, String> headers = new HashMap<String, String>();

        @Override
        public Map<String, String> getHeaders() throws AuthFailureError {
            return headers;
        }

        public void setHeader(String title, String content) {
            headers.put(title, content);
        }

    }
}
