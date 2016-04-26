
package jake.imperial.drone.utils;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.androidplot.xy.SimpleXYSeries;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Iterator;

import jake.imperial.drone.fragments.ControlFragment;
import jake.imperial.drone.DroneApplication;
import jake.imperial.drone.fragments.GraphFragment;

/**
 * Steer incoming MQTT messages to the proper activities based on their content.
 */
public class MessageConductor {

    private final static String TAG = MessageConductor.class.getName();
    private static MessageConductor instance;
    private Context context;
    private DroneApplication app;

    private MessageConductor(Context context) {
        this.context = context;
        app = (DroneApplication) context.getApplicationContext();
    }

    public static MessageConductor getInstance(Context context) {
        if (instance == null) {
            instance = new MessageConductor(context);
        }
        return instance;
    }

    /**
     * Steer incoming MQTT messages to the proper activities based on their content.
     *
     * @param payload The log of the MQTT message.
     * @param topic The topic the MQTT message was received on.
     * @throws JSONException If the message contains invalid JSON.
     */
    public void steerMessage(String payload, String topic) throws JSONException {
        Log.d(TAG, ".steerMessage() entered");
        Log.d(TAG, payload);
        JSONObject top = new JSONObject(payload);


        if (topic.contains(Constants.LOG_EVENT)) {
            JSONObject d = top.getJSONObject("d");
            app.setUnreadCount(app.getUnreadCount() + 1);

            String messageText = d.getString("text");
            JSONObject location = d.getJSONObject("location");

            app.getMessageLog().add("[" + new Timestamp(new Date().getTime()) + "]: " + messageText);
            app.getMarkerList().add(new MarkerOptions()
                    .position(new LatLng(location.getDouble("lat"), location.getDouble("lon")))
                    .title(messageText)
                    //.icon // somehow select depending on thingymagig
                    );

            String runningActivity = app.getCurrentRunningActivity();
            if (runningActivity != null) {
                Intent logIntent = new Intent(Constants.APP_ID + "." + Constants.LOG_EVENT);
                if (messageText != null) {
                    logIntent.putExtra(Constants.INTENT_DATA, Constants.LOG_EVENT); // Un needed?
                    logIntent.putExtra(Constants.INTENT_DATA_MESSAGE, d.getString("text"));
                    logIntent.putExtra(Constants.INTENT_DATA_LOC_LAT, location.getDouble("lat"));
                    logIntent.putExtra(Constants.INTENT_DATA_LOC_LON, location.getDouble("lon"));
                    Log.d(TAG, String.valueOf(location.toString()));
                    context.sendBroadcast(logIntent);
                }
            }
            // Does Log need revalidating?
            // Should make GPS global. Cos if map not running it not receive

            } else if (topic.contains(Constants.ALERT_EVENT)) {
                JSONObject d = top.getJSONObject("d");
                app.setUnreadCount(app.getUnreadCount() + 1);

                String runningActivity = app.getCurrentRunningActivity();
                if (runningActivity != null) {
                   Intent alertIntent = new Intent(Constants.APP_ID + "." + Constants.ALERT_EVENT);
                   String messageText = d.getString("text");
                   if (messageText != null) {
                       alertIntent.putExtra(Constants.INTENT_DATA, Constants.ALERT_EVENT);
                       alertIntent.putExtra(Constants.INTENT_DATA_MESSAGE, d.getString("text"));
                       context.sendBroadcast(alertIntent);
                   }
                }
            } else if (topic.contains(Constants.SENSOR_EVENT)){
                Log.d(TAG, "he");
                JSONObject readings = new JSONObject(payload);
                long time = readings.getLong("time");
                JSONArray position = readings.getJSONArray("location");

                // Send out position information
                Intent positionIntent = new Intent(Constants.APP_ID + "." + Constants.INTENT_POSITION);

                LatLng latLon = new LatLng(position.getDouble(0), position.getDouble(1));
                app.setLatestPosition(latLon);
                positionIntent.putExtra(Constants.INTENT_DATA, Constants.INTENT_POSITION);
                context.sendBroadcast(positionIntent);

                readings.remove("time");
                readings.remove("location");

                Iterator<String> iter = readings.keys();
                while(iter.hasNext()) {

                    String type = iter.next();
                    Double reading = readings.getDouble(type);

                    SimpleXYSeries series = app.getSensorData().get(type);
                    if (series != null) {
                        series.addLast(time, reading);

                    } else {
                        series = new SimpleXYSeries(type);
                        series.addLast(time, reading);

                        app.getSensorData().put(type, series);

                        String runningActivity = app.getCurrentRunningActivity();
                        if(runningActivity != null && runningActivity.equals(GraphFragment.class.getName())){
                            Intent sensorTypeIntent = new Intent(Constants.APP_ID + "." + Constants.SENSOR_EVENT);
                            sensorTypeIntent.putExtra(Constants.INTENT_DATA, Constants.SENSOR_TYPE_EVENT);
                            sensorTypeIntent.putExtra(Constants.INTENT_DATA_SENSORTYPE, type);
                            context.sendBroadcast(sensorTypeIntent);

                        }

                    }
                }
                String runningActivity = app.getCurrentRunningActivity();
                if(runningActivity != null && runningActivity.equals(GraphFragment.class.getName())){
                    Intent sensorDataIntent = new Intent(Constants.APP_ID + "." + Constants.SENSOR_EVENT);
                    sensorDataIntent.putExtra(Constants.INTENT_DATA, Constants.SENSOR_EVENT);
                    context.sendBroadcast(sensorDataIntent);
                }
        } else {
            Log.d(TAG, "No known action for " + topic + " " + payload);
        }
    }
}
