package jake.imperial.drone.fragments;


import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
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
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TimePicker;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;


import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import jake.imperial.drone.DroneApplication;
import jake.imperial.drone.R;
import jake.imperial.drone.utils.Constants;

import com.androidplot.xy.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class GraphFragment extends Fragment {
    private static final String TAG = GraphFragment.class.getName();
    private DroneApplication app;
    private BroadcastReceiver broadcastReceiver;

    private RequestQueue requestQueue;
    private AtomicInteger queryNumber;
    private AtomicInteger responseCount;
    private AtomicInteger responseErrors;

    private boolean liveData = true;


    private XYPlot linePlot;


    public GraphFragment() {
    }

    public static GraphFragment newInstance() {
        return new GraphFragment();
    }

    @Override
    public void onResume() {
        Log.d(TAG, ".onResume() entered()");
        super.onResume();
        app = (DroneApplication) getActivity().getApplication();


        if (broadcastReceiver == null) {
            Log.d(TAG, ".onResume() - Registering GraphBroadcastReceiver");
            broadcastReceiver = new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d(TAG, intent.toString());
                    if (app.getCurrentRunningActivity().equals(TAG)) {
                        Log.d(TAG, ".onReceive() - Received intent for GraphBroadcastReceiver");
                        processIntent(intent);
                    }
                }
            };
        }

        IntentFilter intentFilter = new IntentFilter(Constants.APP_ID + "." + Constants.SENSOR_EVENT);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(broadcastReceiver, intentFilter);

        app.resetFormatter();
        for (SimpleXYSeries series : app.getSensorData().values()) {
            linePlot.addSeries(series, app.getFormatter());
        }

        linePlot.redraw();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        liveData = getActivity().getPreferences(0).getBoolean("mqtt_live_data", false);

        View rootView = inflater.inflate(R.layout.fragment_graph, container, false);

        requestQueue = Volley.newRequestQueue(getContext());
        queryNumber = new AtomicInteger(0);

        final CheckBox mqtt_live_data = (CheckBox) rootView.findViewById(R.id.mqtt_live_data);
        mqtt_live_data.setChecked(liveData);
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
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                liveData = false;
                mqtt_live_data.setChecked(false);
                loadSensorData3();
            }
        });


        linePlot = (XYPlot) rootView.findViewById(R.id.linePlot);

        linePlot.setDomainStep(XYStepMode.SUBDIVIDE, 10);
        linePlot.setRangeStep(XYStepMode.SUBDIVIDE, 10);
        linePlot.setDomainValueFormat(new Format() {
            private SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

            @Override
            public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
                Date date = new Date(((Number) obj).longValue());
                return dateFormat.format(date, toAppendTo, pos);
            }

            @Override
            public Object parseObject(String source, ParsePosition pos) {
                return null;
            }
        });
        linePlot.setRangeValueFormat(new DecimalFormat("###.#"));


        linePlot.getGraphWidget().setDomainLabelOrientation(-45);
        linePlot.getLegendWidget().setPadding(10, 10, 10, 10);
        linePlot.getGraphWidget().setPadding(10, 10, 10, 10);
        //linePlot.getLegendWidget().setSize(new SizeMetrics(10, SizeLayoutType.ABSOLUTE, 10, SizeLayoutType.ABSOLUTE));
        //linePlot.getLegendWidget().position(100, XLayoutStyle.ABSOLUTE_FROM_LEFT, 100, YLayoutStyle.ABSOLUTE_FROM_TOP, AnchorPosition.LEFT_TOP);


        //startLoadingSensorData();
        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        //stopLoadingSensorData();
    }

    private void clearGraph() {
        app.getSensorData().clear();
        app.resetFormatter();
        linePlot.clear();
        linePlot.redraw();
    }

    private void runDatabaseQuery(String url) {
        Log.d(TAG, "Querying database with uri: " + url);
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        Log.d(TAG, "Received response for sensorData");

                        for (int i = 0; i < response.length(); i++) {
                            try {
                                responseCount.incrementAndGet();
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
                            }catch (JSONException e) {
                                responseErrors.incrementAndGet();
                                Log.d(TAG, e.toString());
                            }
                        }
                        linePlot.redraw();
                        queryNumber.decrementAndGet();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                queryNumber.decrementAndGet();
                Log.d(TAG, "Query " + error.toString());
            }
        });

        // Query can take a while if data is large
        // Should really fix how the server handles it, setting up streams.
        request.setRetryPolicy(new DefaultRetryPolicy(5000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        // Add the request to the RequestQueue.
        requestQueue.add(request);
    }

    private void loadSensorData3() {

        final Dialog dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.time_choice_dialog);
        dialog.setTitle("Select time range:");

        SharedPreferences settings = getActivity().getPreferences(0);
        final CheckBox temp_check = (CheckBox) dialog.findViewById(R.id.temperature_check);
        final CheckBox alt_check = (CheckBox) dialog.findViewById(R.id.altitude_check);
        final CheckBox air_check = (CheckBox) dialog.findViewById(R.id.airpurity_check);
        temp_check.setChecked(settings.getBoolean("temperature_check", false));
        alt_check.setChecked(settings.getBoolean("altitude_check", false));
        air_check.setChecked(settings.getBoolean("airpurity_check", false));

        dialog.findViewById(R.id.accept).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePicker datePicker = (DatePicker) dialog.findViewById(R.id.from_date_picker);
                TimePicker timePicker = (TimePicker) dialog.findViewById(R.id.from_time_picker);
                GregorianCalendar date = new GregorianCalendar(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth(),
                        timePicker.getCurrentHour(), timePicker.getCurrentMinute(), 0);
                long fromTime = date.getTimeInMillis();

                datePicker = (DatePicker) dialog.findViewById(R.id.till_date_picker);
                timePicker = (TimePicker) dialog.findViewById(R.id.till_time_picker);
                date = new GregorianCalendar(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth(),
                        timePicker.getCurrentHour(), timePicker.getCurrentMinute(), 0);
                long tillTime = date.getTimeInMillis();

                if (tillTime - fromTime > 7 * 24 * 60 * 60 * 1000) {
                    new AlertDialog.Builder(getActivity())
                            .setTitle("Error:")
                            .setMessage("Date range is too large.")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                }
                            }).show();
                    return;
                }

                SharedPreferences.Editor sett = getActivity().getPreferences(0).edit();
                sett.putString("dialog_data_from", String.valueOf(fromTime));
                sett.putString("dialog_data_till", String.valueOf(tillTime));
                sett.putBoolean("temperature_check", temp_check.isChecked());
                sett.putBoolean("altitude_check", alt_check.isChecked());
                sett.putBoolean("airpurity_check", air_check.isChecked());
                sett.commit();

                dialog.dismiss();

                new dlTask().execute();

            }
        });

        dialog.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        long fromTime = Long.parseLong(settings.getString("dialog_data_from", "0"));
        long tillTime = Long.parseLong(settings.getString("dialog_data_till", String.valueOf(System.currentTimeMillis())));


        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(fromTime);

        DatePicker datePicker = (DatePicker) dialog.findViewById(R.id.from_date_picker);
        TimePicker timePicker = (TimePicker) dialog.findViewById(R.id.from_time_picker);

        datePicker.init(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH), null);
        timePicker.setCurrentHour(c.get(Calendar.HOUR));
        timePicker.setCurrentMinute(c.get(Calendar.MINUTE));

        c.setTimeInMillis(tillTime);

        datePicker = (DatePicker) dialog.findViewById(R.id.till_date_picker);
        timePicker = (TimePicker) dialog.findViewById(R.id.till_time_picker);

        datePicker.init(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH), null);
        timePicker.setCurrentHour(c.get(Calendar.HOUR));
        timePicker.setCurrentMinute(c.get(Calendar.MINUTE));


        dialog.show();
    }

    class dlTask extends AsyncTask<Void, Void, Void> {
        ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            dialog = ProgressDialog.show(getContext(), "", "Loading sensor data..", true, false);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            dialog.dismiss();
            if(app.getSensorData().isEmpty()){

                new AlertDialog.Builder(getActivity())
                        .setTitle("No data found.")
                        .setMessage("Response Count: " + responseCount + "     Response Errors: " + responseErrors)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        }).show();
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            SharedPreferences settings = getActivity().getPreferences(0);
            String timeFrom = settings.getString("dialog_data_from", "0");
            String timeTill = settings.getString("dialog_data_till", String.valueOf(System.currentTimeMillis()));
            boolean temperature = settings.getBoolean("temperature_check", false);
            boolean altitude = settings.getBoolean("altitude_check", false);
            boolean airpurity = settings.getBoolean("airpurity_check", false);

            clearGraph();

            String domain = app.getDomain();
            responseCount = new AtomicInteger(0);
            responseErrors = new AtomicInteger(0);
            String url = "http://" + domain + "/api/sensors/" + timeFrom + "/" + timeTill + "/";
            if (temperature) {
                queryNumber.incrementAndGet();
                runDatabaseQuery(url + "temperature");
            }
            if (altitude) {
                queryNumber.incrementAndGet();
                runDatabaseQuery(url + "altitude");
            }
            if (airpurity) {
                queryNumber.incrementAndGet();
                runDatabaseQuery(url + "airPurity");
            }

            while (queryNumber.get() != 0) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    continue;
                }
            }
            return null;
        }
    }

    private void processIntent(Intent intent) {

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
        } else if (data.equals(Constants.SENSOR_EVENT) && liveData) {
            linePlot.redraw();
        } else if (data.equals(Constants.SENSOR_TYPE_EVENT) && liveData) {
            String type = intent.getStringExtra(Constants.INTENT_DATA_SENSORTYPE);
            linePlot.addSeries(app.getSensorData().get(type), app.getFormatter());
            linePlot.redraw();
        }
    }



















    private Runnable querySensorDatabase = new Runnable() {
        @Override
        public void run() {

            SharedPreferences settings = getActivity().getPreferences(0);
            String timeFrom = settings.getString("dialog_data_from", "0");
            String timeTill = settings.getString("dialog_data_till", String.valueOf(System.currentTimeMillis()));
            boolean temperature = settings.getBoolean("temperature_check", false);
            boolean altitude = settings.getBoolean("altitude_check", false);
            boolean airpurity = settings.getBoolean("airpurity_check", false);

            clearGraph();

            String domain = app.getDomain();


            //String url = "http://" + domain + "/getSensorData?timeFrom=" + timeFrom + "&timeTill=" + timeTill;

            String url = "http://" + domain + "/api/sensors/" + timeFrom + "/" + timeTill + "/";
            if (temperature) {
                queryNumber.incrementAndGet();
                runDatabaseQuery(url + "/temperature");
            }
            if (altitude) {
                queryNumber.incrementAndGet();
                runDatabaseQuery(url + "/altitude");
            }
            if (airpurity) {
                queryNumber.incrementAndGet();
                runDatabaseQuery(url + "/airPurity");
            }

            while (queryNumber.get() != 0) {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    Log.d(TAG, e.getMessage());
                }
            }
        }
    };


    private void loadSensorData() {
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
        dialogBuilder.setPositiveButton("Get Data", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences.Editor sett = settings.edit();
                sett.putString("dialog_data_from", fromTime.getText().toString());
                sett.putString("dialog_data_till", tillTime.getText().toString());
                sett.putBoolean("temperature_check", temp_check.isChecked());
                sett.putBoolean("altitude_check", alt_check.isChecked());
                sett.putBoolean("airpurity_check", air_check.isChecked());
                sett.commit();
                new Thread(querySensorDatabase).start();
            }
        });
        dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });


        AlertDialog b = dialogBuilder.create();
        b.show();


    }


    private void loadSensorData2() {
        DatePickerDialog dialog = new DatePickerDialog(getContext(), android.R.style.Theme_Holo_Dialog, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                int cal = getContext().getResources().getIdentifier("android:", null, null);


            }
        }, 2000, 1, 1) {
            @Override
            protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);

            }
        };
        dialog.getDatePicker().setCalendarViewShown(false);
        dialog.show();
    }
}