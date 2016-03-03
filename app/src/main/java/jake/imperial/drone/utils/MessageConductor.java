
package jake.imperial.drone.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import jake.imperial.drone.fragments.ControlFragment;
import jake.imperial.drone.DroneApplication;

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
        JSONObject d = top.getJSONObject("d");

        if (topic.contains(Constants.COLOR_EVENT)) {
            Log.d(TAG, "Color Event");
            int r = d.getInt("r");
            int g = d.getInt("g");
            int b = d.getInt("b");
            // alpha value received is 0.0 < a < 1.0 but Color.agrb expects 0 < a < 255
            int alpha = (int)(d.getDouble("alpha")*255.0);
            if ((r > 255 || r < 0) ||
                    (g > 255 || g < 0) ||
                    (b > 255 || b < 0) ||
                    (alpha > 255 || alpha < 0)) {
                return;
            }

            app.setColor(Color.argb(alpha, r, g, b));

            String runningActivity = app.getCurrentRunningActivity();
            if (runningActivity != null && runningActivity.equals(ControlFragment.class.getName())) {
                Intent actionIntent = new Intent(Constants.APP_ID + Constants.INTENT_DRONE);
                actionIntent.putExtra(Constants.INTENT_DATA, Constants.COLOR_EVENT);
                context.sendBroadcast(actionIntent);
            }
        }  else if (topic.contains(Constants.LOG_EVENT)) {
            app.setUnreadCount(app.getUnreadCount()+1);

            app.getMessageLog().add(d.getString("text"));

            String runningActivity = app.getCurrentRunningActivity();
            if (runningActivity != null) {
                Intent logIntent = new Intent(Constants.APP_ID + "." + Constants.LOG_EVENT);
                String messageText = d.getString("text");
                JSONObject location = d.getJSONObject("location");
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

            app.setUnreadCount(app.getUnreadCount() + 1);
            app.getMessageLog().add(d.getString("text"));

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
        }else {
            Log.d(TAG, "No known action for " + topic + " " + payload);
        }
    }
}
