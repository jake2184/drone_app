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
import android.widget.Button;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.github.mikephil.charting.charts.LineChart;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Objects;

import jake.imperial.drone.DroneApplication;
import jake.imperial.drone.R;
import jake.imperial.drone.utils.Constants;


public class GraphFragment extends Fragment {
    private static final String TAG = GraphFragment.class.getName();
    private DroneApplication app;
    private BroadcastReceiver broadcastReceiver;

    private Handler mHandler;
    private int mInterval = 10000;
    private String domain = "";
    private RequestQueue requestQueue;
    private LineChart lineChart;


    public GraphFragment() {
    }

    public static GraphFragment newInstance() {
        return new GraphFragment();
    }

    @Override
    public void onResume(){
        Log.d(TAG, ".onResume() entered()");
        super.onResume();
        app = (DroneApplication) getActivity().getApplication();
        //app.setCurrentRunningActivity(TAG);

        if (broadcastReceiver == null) {
            Log.d(TAG, ".onResume() - Registering GraphBroadcastReceiver");
            broadcastReceiver = new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    if(app.getCurrentRunningActivity().equals(TAG)) {
                        Log.d(TAG, ".onReceive() - Received intent for GraphBroadcastReceiver");
                        processIntent(intent);
                    }
                }
            };
        }

        IntentFilter intentFilter = new IntentFilter(Constants.APP_ID + "." + Constants.ALERT_EVENT);
        getActivity().getApplicationContext().registerReceiver(broadcastReceiver, intentFilter);




    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_graph, container, false);
        mHandler = new Handler();
        lineChart = (LineChart) rootView.findViewById(R.id.lineChart);
        requestQueue = Volley.newRequestQueue(getContext());

        Button button = (Button) rootView.findViewById(R.id.load_saved_data);
        button.setOnClickListener(new View.OnClickListener(){
            boolean selected = false;
            @Override
            public void onClick(View v){
                if(!selected) {
                    v.setBackgroundColor(getResources().getColor(R.color.control_blue));
                    startRepeatingTask();
                } else{
                    v.setBackgroundColor(getResources().getColor(R.color.common_google_signin_btn_text_dark_disabled));
                    stopRepeatingTask();
                }
                selected = !selected;
            }
        });

        //startRepeatingTask();
        return rootView;
    }

    @Override
    public void onDestroyView(){
        super.onDestroyView();
        stopRepeatingTask();
    }

    private Runnable updateData = new Runnable() {
        @Override
        public void run() {
            if(app != null) {
                domain = app.getDomain();

                domain = "192.168.1.76:8080";

                String url = "http://" + domain + "/getSensorData";
                Log.d(TAG, "Updating sensor data from " + url);

                // null check on context?


                JsonArrayRequest request  = new JsonArrayRequest(Request.Method.GET, url, null,
                        new Response.Listener<JSONArray>() {
                            @Override
                            public void onResponse(JSONArray response) {
                                try {
                                    Log.d(TAG, "Received response for sensorData");
                                    JSONObject sensorData = app.getSensorData();

                                    for(int i=0; i<response.length(); i++){
                                        JSONObject item = response.getJSONObject(i);

                                        if(sensorData.has(item.getString("readingType"))) {
                                            JSONArray array = sensorData.getJSONArray(item.getString("readingType"));
                                            array.put(item);
                                        }else {
                                            JSONArray array = new JSONArray();
                                            array.put(item);
                                            sensorData.put(item.getString("readingType"), array);
                                        }
                                    }

                                }catch (JSONException e){
                                    Log.d(TAG, e.toString());
                                }
                            }
                        }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.d(TAG, error.toString());
                        }
                });
                // Add the request to the RequestQueue.
                requestQueue.add(request);

            }
            mHandler.postDelayed(updateData, mInterval);
        }

    };

    private void startRepeatingTask() {
        updateData.run();
    }

    private void stopRepeatingTask() {
        mHandler.removeCallbacks(updateData);
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


