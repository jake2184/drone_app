
package jake.imperial.drone.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Path;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.androidplot.xy.SimpleXYSeries;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.JsonArray;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Iterator;

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
            app.setUnreadCount(app.getUnreadCount() + 1);

            String messageText = top.getString("text");
            JSONObject location = top.getJSONObject("location");

            app.getMessageLog().add("[" + new Timestamp(new Date().getTime()) + "]: " + messageText);

            app.getMarkerList().add(new MarkerOptions()
                    .position(new LatLng(location.getDouble("lat"), location.getDouble("lon")))
                    .title(messageText)
                    //.icon // somehow select depending on thingymagig
                    );

            Intent logIntent = new Intent(Constants.APP_ID + "." + Constants.LOG_EVENT);
            if (messageText != null) {
                logIntent.putExtra(Constants.INTENT_DATA, Constants.LOG_EVENT); // Un needed?
                logIntent.putExtra(Constants.INTENT_DATA_MESSAGE, top.getString("text"));
                logIntent.putExtra(Constants.INTENT_DATA_LOC_LAT, location.getDouble("lat"));
                logIntent.putExtra(Constants.INTENT_DATA_LOC_LON, location.getDouble("lon"));
                Log.d(TAG, String.valueOf(location.toString()));
                LocalBroadcastManager.getInstance(context).sendBroadcast(logIntent);
            }

            // Does Log need revalidating?
            // Should make GPS global. Cos if map not running it not receive

        }else if (topic.contains("event")){
            Log.d(TAG, "Received event");
            app.setUnreadCount(app.getUnreadCount() + 1);

            String messageText = top.getString("text");
            JSONArray location = top.getJSONArray("location");
            double lat = location.getDouble(0);
            double lon = location.getDouble(1);

            app.getMessageLog().add("[" + new Timestamp(new Date().getTime()) + "]: " + messageText + "  at lat:" + String.valueOf(lat) + " lon: " + String.valueOf(lon));

            app.getMarkerList().add(new MarkerOptions()
                            .position(new LatLng(lat, lon))
                            .title(messageText)
                    //.icon // somehow select depending on thingymagig
            );

            Intent logIntent = new Intent(Constants.APP_ID + "." + Constants.LOG_EVENT);

//            logIntent.putExtra(Constants.INTENT_DATA, Constants.LOG_EVENT); // Un needed?
//            logIntent.putExtra(Constants.INTENT_DATA_MESSAGE, top.getString("text"));
//            logIntent.putExtra(Constants.INTENT_DATA_LOC_LAT, location.getDouble(0));
//            logIntent.putExtra(Constants.INTENT_DATA_LOC_LON, location.getDouble(1));

            logIntent.putExtra(Constants.INTENT_DATA, app.getMarkerList().size() - 1);

            Log.d(TAG, String.valueOf(location.toString()));
            LocalBroadcastManager.getInstance(context).sendBroadcast(logIntent);
        }else if (topic.contains(Constants.ALERT_EVENT)) {
            JSONObject d = top.getJSONObject("d");
            app.setUnreadCount(app.getUnreadCount() + 1);

            String runningActivity = app.getCurrentRunningActivity();
            if (runningActivity != null) {
                Intent alertIntent = new Intent(Constants.APP_ID + "." + Constants.ALERT_EVENT);
                String messageText = d.getString("text");
                if (messageText != null) {
                    alertIntent.putExtra(Constants.INTENT_DATA, Constants.ALERT_EVENT);
                    alertIntent.putExtra(Constants.INTENT_DATA_MESSAGE, d.getString("text"));
                    LocalBroadcastManager.getInstance(context).sendBroadcast(alertIntent);
                }
            }
        } else if (topic.contains(Constants.IMAGE_EVENT)){
            Log.d(TAG, "New image uploaded to server");
            Intent newImageIntent = new Intent(Constants.APP_ID + "." + Constants.IMAGE_EVENT);
            LocalBroadcastManager.getInstance(context).sendBroadcast(newImageIntent);


        } else if (topic.contains(Constants.SENSOR_EVENT)){

            int fourthSlash = ordinalIndexOf(topic, '/', 3);
            int fifthSlash = ordinalIndexOf(topic, '/', 4);
            String droneName = topic.substring(fourthSlash+1, fifthSlash);

            JSONObject readings = new JSONObject(payload);
            long time = readings.getLong("time");
            JSONArray position = readings.getJSONArray("location");

            // Update position information
            LatLng latLon = new LatLng(position.getDouble(0), position.getDouble(1));
            app.setDronePosition(droneName, latLon);

            // Notify of new position information
            Intent positionIntent = new Intent(Constants.APP_ID + "." + Constants.INTENT_POSITION);
            positionIntent.putExtra(Constants.INTENT_DATA_MESSAGE, droneName);
            LocalBroadcastManager.getInstance(context).sendBroadcast(positionIntent);

            readings.remove("time");
            readings.remove("location");

            Iterator<String> iter = readings.keys();
            while(iter.hasNext()) {

                String type = iter.next();
                Double reading;
                try {
                    reading = readings.getDouble(type);
                } catch (JSONException e){
                    continue; // Sensor reading was null
                }
                SimpleXYSeries series = app.getSensorData(droneName).get(type);
                if (series != null) {

                    // Could still show > MAX_GRAPH_SIZE over different series
                    if(series.size() >= Constants.MAX_GRAPH_SIZE){
                        series.removeFirst();
                    }

                    series.addLast(time, reading);

                } else {
                    series = new SimpleXYSeries(type);
                    series.addLast(time, reading);

                    app.getSensorData(droneName).put(type, series);

                    Intent sensorTypeIntent = new Intent(Constants.APP_ID + "." + Constants.SENSOR_EVENT);
                    sensorTypeIntent.putExtra(Constants.INTENT_DATA, Constants.SENSOR_TYPE_EVENT);
                    sensorTypeIntent.putExtra(Constants.INTENT_DATA_MESSAGE, droneName);
                    sensorTypeIntent.putExtra(Constants.INTENT_DATA_SENSORTYPE, type);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(sensorTypeIntent);


                }
            }
            Intent sensorDataIntent = new Intent(Constants.APP_ID + "." + Constants.SENSOR_EVENT);
            sensorDataIntent.putExtra(Constants.INTENT_DATA, Constants.SENSOR_EVENT);
            sensorDataIntent.putExtra(Constants.INTENT_DATA_MESSAGE, droneName);
            LocalBroadcastManager.getInstance(context).sendBroadcast(sensorDataIntent);
        } else {
            Log.d(TAG, "No known action for " + topic + " " + payload);
        }
    }

    // https://stackoverflow.com/questions/3976616/how-to-find-nth-occurrence-of-character-in-a-string

    public static int ordinalIndexOf(String str, char c, int n) {
        int pos = str.indexOf(c, 0);
        while (n-- > 0 && pos != -1)
            pos = str.indexOf(c, pos+1);
        return pos;
    }
}
