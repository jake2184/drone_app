package jake.imperial.drone.fragments;


import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.androidplot.Plot;
import com.androidplot.ui.AnchorPosition;
import com.androidplot.ui.DynamicTableModel;
import com.androidplot.ui.SizeLayoutType;
import com.androidplot.ui.SizeMetrics;
import com.androidplot.ui.XLayoutStyle;
import com.androidplot.ui.YLayoutStyle;
import com.androidplot.xy.XYPlot;


import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import jake.imperial.drone.DroneApplication;
import jake.imperial.drone.R;
import jake.imperial.drone.utils.Constants;
import jake.imperial.drone.utils.MqttHandler;
import jake.imperial.drone.utils.TopicFactory;

import com.androidplot.xy.*;
import com.github.mikephil.charting.data.LineData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class GraphFragment extends Fragment {
    private static final String TAG = GraphFragment.class.getName();
    private DroneApplication app;
    private BroadcastReceiver broadcastReceiver;

    private Handler mHandler;
    private int mInterval = 10000;
    private String domain = "";
    private RequestQueue requestQueue;
    private long lastSuccessfulRequest = 0;

    private class PlotUpdater implements Observer {
        Plot plot;

        public PlotUpdater(Plot plot){
            this.plot = plot;
        }
        @Override
        public void update(Observable o,Object arg){
            plot.redraw();
        }
    }

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
        app.setCurrentRunningActivity(TAG);


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
        //lineChart = (LineChart) rootView.findViewById(R.id.lineChart);
        requestQueue = Volley.newRequestQueue(getContext());

        RadioGroup sensorSource = (RadioGroup) rootView.findViewById(R.id.sensor_source);
        sensorSource.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId){
                    case R.id.load_saved_data:
                        // TODO Stop MQTT
                        MqttHandler mqttHandler = MqttHandler.getInstance(getContext());
                        mqttHandler.unsubscribe(TopicFactory.getEventTopic("pi", "drone", "sensors"));
                        startLoadingSensorData();
                        break;
                    case R.id.mqtt_live_data:
                        // TODO Start MQTT
                        stopLoadingSensorData();
                        mqttHandler = MqttHandler.getInstance(getContext());
                        mqttHandler.subscribe(TopicFactory.getEventTopic("pi", "drone", "sensors"), 0);
                        break;
                }
            }
        });

        Button button = (Button) rootView.findViewById(R.id.clear_sensor_data_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, ".clear() line plot data");
                app.getSensorData().clear();
                linePlot.clear();
                linePlot.redraw();
            }
        });


        linePlot = (XYPlot) rootView.findViewById(R.id.linePlot);


        linePlot.setDomainStep(XYStepMode.SUBDIVIDE, 10);

        linePlot.setRangeStepMode(XYStepMode.INCREMENT_BY_VAL);
        linePlot.setRangeStepValue(5);

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


    private Runnable updateData = new Runnable() {
        @Override
        public void run() {
            if(app != null) {
                domain = app.getDomain();

                // TODO remove this line
                domain = "192.168.1.76:8080";

                String url = "http://" + domain + "/getSensorData?fromTime=" + lastSuccessfulRequest;
                final long lastRequest = System.currentTimeMillis();
                Log.d(TAG, "Updating sensor data from " + url);

                JsonArrayRequest request  = new JsonArrayRequest(Request.Method.GET, url, null,
                        new Response.Listener<JSONArray>() {
                            @Override
                            public void onResponse(JSONArray response) {
                                try {
                                    Log.d(TAG, "Received response for sensorData");

                                    for(int i=0; i<response.length(); i++){
                                        JSONObject item = response.getJSONObject(i);

                                        // Item of format {time:111,temp:43,alt:47}
                                        long time = item.getLong("time");
                                        JSONArray position = item.getJSONArray("location");
                                        item.remove("time");
                                        item.remove("location");

                                        Iterator<String> iter = item.keys();
                                        while(iter.hasNext()) {

                                            String type = iter.next();
                                            Double reading = item.getDouble(type);

                                            SimpleXYSeries series = app.getSensorData().get(type);
                                            if (series != null) {
                                                series.addLast(time, reading);
                                            } else {
                                                series = new SimpleXYSeries(type);
                                                series.addLast(time, reading);

                                                LineAndPointFormatter formatter1 = new LineAndPointFormatter(Color.rgb(0, 0, 0), null, null, null);
                                                formatter1.getLinePaint().setStrokeJoin(Paint.Join.ROUND);
                                                formatter1.getLinePaint().setStrokeWidth(2);


                                                linePlot.addSeries(series, formatter1);
                                                app.getSensorData().put(type, series);
                                            }
                                        }


                                    }
                                    linePlot.redraw();
                                    lastSuccessfulRequest = lastRequest;

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



    private void startLoadingSensorData() {
        updateData.run();
    }

    private void stopLoadingSensorData() {
        mHandler.removeCallbacks(updateData);
    }


    private void subscribeToSensorData(){
        MqttHandler mqttHandler = MqttHandler.getInstance(getContext());

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


