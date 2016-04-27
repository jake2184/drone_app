package jake.imperial.drone.fragments;


import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.androidplot.xy.XYPlot;


import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import jake.imperial.drone.DroneApplication;
import jake.imperial.drone.R;
import jake.imperial.drone.utils.Constants;
import jake.imperial.drone.utils.MqttHandler;

import com.androidplot.xy.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class GraphFragment extends Fragment {
    private static final String TAG = GraphFragment.class.getName();
    private DroneApplication app;
    private BroadcastReceiver broadcastReceiver;

    private String domain = "";
    private RequestQueue requestQueue;
    private boolean liveData = false;


    private XYPlot linePlot;


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


        if (broadcastReceiver == null) {
            Log.d(TAG, ".onResume() - Registering GraphBroadcastReceiver");
            broadcastReceiver = new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d(TAG, intent.toString());
                    if(app.getCurrentRunningActivity().equals(TAG)) {
                        Log.d(TAG, ".onReceive() - Received intent for GraphBroadcastReceiver");
                        processIntent(intent);
                    }
                }
            };
        }

        IntentFilter intentFilter = new IntentFilter(Constants.APP_ID + "." + Constants.SENSOR_EVENT);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(broadcastReceiver, intentFilter);

        app.resetFormatter();
        for(SimpleXYSeries series: app.getSensorData().values()){
            linePlot.addSeries(series, app.getFormatter());
        }

        linePlot.redraw();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {

        liveData = getActivity().getPreferences(0).getBoolean("mqtt_live_data", false);

        View rootView = inflater.inflate(R.layout.fragment_graph, container, false);

        requestQueue = Volley.newRequestQueue(getContext());

        final CheckBox mqtt_live_data = (CheckBox) rootView.findViewById(R.id.mqtt_live_data);

        mqtt_live_data.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                liveData = isChecked;
            }
        });

        Button button = (Button) rootView.findViewById(R.id.clear_sensor_data_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearGraph();

            }
        });
        button = (Button) rootView.findViewById(R.id.load_saved_data);
        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                liveData = false;
                mqtt_live_data.setChecked(false);
                loadSensorData();
            }
        });


        linePlot = (XYPlot) rootView.findViewById(R.id.linePlot);


        linePlot.setDomainStep(XYStepMode.SUBDIVIDE, 10);

        linePlot.setRangeStep(XYStepMode.SUBDIVIDE, 10);

        linePlot.setDomainValueFormat(new Format(){
            private SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

            @Override
            public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos){
                Date date = new Date(((Number) obj).longValue());
                return dateFormat.format(date, toAppendTo, pos);
            }
            @Override
            public Object parseObject(String source, ParsePosition pos){
                return null;
            }
        });
        linePlot.setRangeValueFormat(new DecimalFormat("###.#"));
        linePlot.getGraphWidget().setDomainLabelOrientation(-45);
        linePlot.getLegendWidget().setPadding(10, 10, 10, 10);
        linePlot.getGraphWidget().setPadding(10,10,10,10);
        //linePlot.getLegendWidget().setSize(new SizeMetrics(10, SizeLayoutType.ABSOLUTE, 10, SizeLayoutType.ABSOLUTE));
        //linePlot.getLegendWidget().position(100, XLayoutStyle.ABSOLUTE_FROM_LEFT, 100, YLayoutStyle.ABSOLUTE_FROM_TOP, AnchorPosition.LEFT_TOP);


        //startLoadingSensorData();
        return rootView;
    }

    @Override
    public void onDestroyView(){
        super.onDestroyView();
        //stopLoadingSensorData();
    }

    private void clearGraph(){
        app.getSensorData().clear();
        app.resetFormatter();
        linePlot.clear();
        linePlot.redraw();
    }

    private Runnable querySensorDatabase = new Runnable() {
        @Override
        public void run() {
            SharedPreferences settings = getActivity().getPreferences(0);
            try {

                String timeFrom = settings.getString("dialog_data_from", "0");
                String timeTill = settings.getString("dialog_data_till", String.valueOf(System.currentTimeMillis()));

                boolean temperature = settings.getBoolean("temperature_check", false);
                boolean altitude = settings.getBoolean("altitude_check", false);
                boolean airpurity = settings.getBoolean("airpurity_check", false);

                clearGraph();

                domain = app.getDomain();

                String url = "http://" + domain + "/getSensorData?timeFrom=" + timeFrom + "&timeTill=" + timeTill;
                if(temperature){
                    runDatabaseQuery(url + "&type=temperature");
                }
                if(altitude){
                    runDatabaseQuery(url + "&type=altitude");
                }
                if(airpurity){
                    runDatabaseQuery(url + "&type=airPurity");
                }




            } catch (Exception e) {
                Log.d(TAG, e.getMessage());
            }
        }
    };


    private void runDatabaseQuery(String url){
        Log.d(TAG, "Querying database with uri: " + url);
        // Do I want a ProgressDialog?
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            Log.d(TAG, "Received response for sensorData");

                            for (int i = 0; i < response.length(); i++) {
                                JSONObject item = response.getJSONObject(i);

                                // Item of format {time:111,temp:43,alt:47}
                                long time = item.getLong("time");
                                JSONArray position = item.getJSONArray("location");
                                item.remove("time");
                                item.remove("location");

                                Iterator<String> iter = item.keys();
                                while (iter.hasNext()) {

                                    String type = iter.next();
                                    Double reading = item.getDouble(type);

                                    SimpleXYSeries series = app.getSensorData().get(type);
                                    if (series != null) {
                                        series.addLast(time, reading);
                                    } else {
                                        series = new SimpleXYSeries(type);
                                        series.addLast(time, reading);

                                        linePlot.addSeries(series, app.getFormatter());
                                        app.getSensorData().put(type, series);
                                    }
                                }


                            }
                            linePlot.redraw();

                        } catch (JSONException e) {
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

    private void loadSensorData(){
        // Get current settings
        final SharedPreferences settings = getActivity().getPreferences(0);


        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.fragment_sensor_dialog, null);
        dialogBuilder.setView(dialogView);

        final EditText fromTime = (EditText) dialogView.findViewById(R.id.dialog_data_from);
        final EditText tillTime = (EditText) dialogView.findViewById(R.id.dialog_data_till);

        final CheckBox temp_check = (CheckBox) dialogView.findViewById(R.id.temperature_check);
        final CheckBox alt_check = (CheckBox) dialogView.findViewById(R.id.altitude_check);
        final CheckBox air_check = (CheckBox) dialogView.findViewById(R.id.airpurity_check);

        fromTime.setText(settings.getString("dialog_data_from", ""));
        tillTime.setText(settings.getString("dialog_data_till", ""));

        temp_check.setChecked(settings.getBoolean("temperature_check", false));
        alt_check.setChecked(settings.getBoolean("altitude_check", false));
        air_check.setChecked(settings.getBoolean("airpurity_check", false));


        dialogBuilder.setTitle("Enter data parameters:");
        dialogBuilder.setPositiveButton("Get Data", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences.Editor sett = settings.edit();
                sett.putString("dialog_data_from" , fromTime.getText().toString());
                sett.putString("dialog_data_till", tillTime.getText().toString());
                sett.putBoolean("temperature_check", temp_check.isChecked());
                sett.putBoolean("altitude_check", alt_check.isChecked());
                sett.putBoolean("airpurity_check", air_check.isChecked());
                sett.commit();
                new Thread(querySensorDatabase).start();
            }
        });
        dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });





        AlertDialog b = dialogBuilder.create();
        b.show();

    }

    private void processIntent(Intent intent){

        String data = intent.getStringExtra(Constants.INTENT_DATA);
        assert data != null;
        Log.d(TAG, data);
        if (data.equals(Constants.ALERT_EVENT)) {
            String message = intent.getStringExtra(Constants.INTENT_DATA_MESSAGE);
            new AlertDialog.Builder(getActivity())
                    .setTitle("Alert:")
                    .setMessage(message)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        }
                    }).show();
        } else if (data.equals(Constants.SENSOR_EVENT) && liveData){
            linePlot.redraw();
        } else if (data.equals(Constants.SENSOR_TYPE_EVENT) && liveData){
            String type = intent.getStringExtra(Constants.INTENT_DATA_SENSORTYPE);
            linePlot.addSeries(app.getSensorData().get(type), app.getFormatter());
            linePlot.redraw();
        }
    }

}


