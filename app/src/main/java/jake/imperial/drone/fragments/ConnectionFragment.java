package jake.imperial.drone.fragments;

import android.app.AlertDialog;
import android.app.ProgressDialog;
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
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.JsonArray;

import org.json.JSONArray;
import org.json.JSONException;

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
        IntentFilter intentFilter = new IntentFilter(Constants.APP_ID + "." + Constants.INTENT_CONNECTION);
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

        final ProgressDialog dialog = ProgressDialog.show(getContext(), "", "Trying to connect..", true, false);


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
            final String url = "http://" + app.getDomain() + "/login";
            LoginRequest loginRequest = new LoginRequest(url, app.getUsername(), app.getPassword(), new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.d(TAG, response);
                    String droneInfoUri = "http://" + app.getDomain() + "/api/drones";
                    JsonArrayRequest droneInfoRequest = new JsonArrayRequest(droneInfoUri, new Response.Listener<JSONArray>() {
                        @Override
                        public void onResponse(JSONArray response) {
                            Log.d(TAG, response.toString());
                            String droneName = "";
                            for(int i=0; i<response.length(); i++){
                                try {
                                    droneName = response.getJSONObject(i).getString("name");

                                    MqttHandler.getInstance(getContext()).subscribe(TopicFactory.getEventTopic("pi", droneName , "sensors"), 0);
                                    MqttHandler.getInstance(getContext()).subscribe(TopicFactory.getEventTopic("node", droneName, "image"), 0);
                                    MqttHandler.getInstance(getContext()).subscribe(TopicFactory.getEventTopic("node", droneName, "event"), 0);


                                    app.getDroneNames().add(droneName);
                                } catch (JSONException e){
                                    Log.d(TAG, "Badly formed JSON");
                                }
                            }
                            app.setCurrentDrone(droneName);
                            dialog.cancel();


                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            dialog.cancel();
                            Log.d(TAG, error.toString());
                            String errorType;
                            if(error instanceof AuthFailureError) {
                                errorType = "Authentication Error";
                            } else if(error instanceof TimeoutError){
                                errorType = "Connection Timeout Error";
                            } else if(error instanceof NetworkError) {
                                errorType = "Network Error";
                            } else {
                                errorType = "Unknown Error";
                            }

                            Log.e(TAG, errorType);
                            new AlertDialog.Builder(getActivity())
                                    .setTitle("Failed To Connect to server")
                                    .setMessage(errorType)
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            // Do nothing.
                                        }
                                    }).show();
                        }
                    });
                    requestQueue.add(droneInfoRequest);




                    //dialog.cancel();

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    dialog.cancel();
                    Log.d(TAG, error.toString());
                    String errorType;
                    if(error instanceof AuthFailureError) {
                        errorType = "Authentication Error";
                    } else if(error instanceof TimeoutError){
                        errorType = "Connection Timeout Error";
                    } else if(error instanceof NetworkError) {
                        errorType = "Network Error";
                    } else {
                        errorType = "Unknown Error";
                    }

                    Log.e(TAG, errorType);
                    new AlertDialog.Builder(getActivity())
                        .setTitle("Failed To Connect to server")
                        .setMessage(errorType)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Do nothing.
                            }
                        }).show();
                }
            });

            requestQueue.add(loginRequest);

            if(mqttHandle.connect()){
                Log.d(TAG, "client.connect() returned true");
            }

        } else {
            displaySetPropertiesDialog();
        }
    }

    private boolean checkCanConnect() {
        return !(app.getOrganization() == null || app.getOrganization().equals("") ||
                app.getDeviceId() == null || app.getDeviceId().equals("") ||
                app.getAPIKey() == null || app.getAPIKey().equals("") ||
                app.getAPIToken() == null || app.getAPIToken().equals(""));

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

        String data = intent.getStringExtra(Constants.INTENT_DATA);
        assert data != null;
        if (data.equals(Constants.INTENT_DATA_FAILURE)) {
            Log.e(TAG, "Connection unsuccessful");
            new AlertDialog.Builder(getActivity())
                    .setTitle("Failed to connect to MQTT")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        }
                    }).show();
        } else if(data.equals(Constants.INTENT_DATA_SUCCESS)){
            Log.d(TAG, "Connection successful");
            app.getMessageLog().add("[" + new Timestamp((new Date()).getTime()) + "]: Connected successfully");
//            MqttHandler.getInstance(getContext()).subscribe(TopicFactory.getEventTopic("pi", "drone", "sensors"),0);
//            MqttHandler.getInstance(getContext()).subscribe(TopicFactory.getEventTopic("node", "server", "image"), 0);
//            MqttHandler.getInstance(getContext()).subscribe(TopicFactory.getEventTopic("node", "server", "event"), 0);

            // Trigger image to load
            Intent newImageIntent = new Intent(Constants.APP_ID + "." + Constants.IMAGE_EVENT);
            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(newImageIntent);
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
