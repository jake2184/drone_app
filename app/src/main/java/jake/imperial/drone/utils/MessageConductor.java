
package jake.imperial.drone.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import jake.imperial.drone.fragments.ConnectionFragment;
import jake.imperial.drone.fragments.ControlFragment;
import jake.imperial.drone.DroneApplication;
import jake.imperial.drone.fragments.LogFragment;
import jake.imperial.drone.fragments.VideoFragment;

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
        }  else if (topic.contains(Constants.TEXT_EVENT)) {
            int unreadCount = app.getUnreadCount();
            app.setUnreadCount(++unreadCount);

            // save payload in an arrayList
            //List messageRecvd = new ArrayList<String>();
            //messageRecvd.add(payload);

            app.getMessageLog().add(d.getString("text"));

            String runningActivity = app.getCurrentRunningActivity();
            if (runningActivity != null && runningActivity.equals(ControlFragment.class.getName())) {
                Intent actionIntent = new Intent(Constants.APP_ID + Constants.INTENT_CONTROL);
                actionIntent.putExtra(Constants.INTENT_DATA, Constants.TEXT_EVENT);
                context.sendBroadcast(actionIntent);
            }

            Intent unreadIntent;
            if (runningActivity.equals(ControlFragment.class.getName())) {
                unreadIntent = new Intent(Constants.APP_ID + Constants.INTENT_CONTROL);
            } else if (runningActivity.equals(ConnectionFragment.class.getName())) {
                unreadIntent = new Intent(Constants.APP_ID + Constants.INTENT_CONNECTION);
            } else if (runningActivity.equals(LogFragment.class.getName())) {
                unreadIntent = new Intent(Constants.APP_ID + Constants.INTENT_LOG);
            } else if (runningActivity.equals(VideoFragment.class.getName())) {
                unreadIntent = new Intent(Constants.APP_ID + Constants.INTENT_VIDEO);
            }else {
                return;
            }

            String messageText = d.getString("text");
            if (messageText != null) {
                unreadIntent.putExtra(Constants.INTENT_DATA, Constants.UNREAD_EVENT);
                context.sendBroadcast(unreadIntent);
            }
        } else if (topic.contains(Constants.ALERT_EVENT)) {
            // save payload in an arrayList
            int unreadCount = app.getUnreadCount();
            app.setUnreadCount(++unreadCount);
            Log.d(TAG, "Alert");
            List messageRecvd = new ArrayList<>();
            messageRecvd.add(payload);

            app.getMessageLog().add(d.getString("text"));

            String runningActivity = app.getCurrentRunningActivity();
            Log.d(TAG, runningActivity);
            if (runningActivity != null) {
                if (runningActivity.equals(ControlFragment.class.getName())) {
                    Intent actionIntent = new Intent(Constants.APP_ID + Constants.INTENT_CONTROL);
                    actionIntent.putExtra(Constants.INTENT_DATA, Constants.TEXT_EVENT);
                    context.sendBroadcast(actionIntent);
                }

                Intent alertIntent;
                if (runningActivity.equals(ControlFragment.class.getName())) {
                    alertIntent = new Intent(Constants.APP_ID + Constants.INTENT_CONTROL);
                } else if (runningActivity.equals(ConnectionFragment.class.getName())) {
                    alertIntent = new Intent(Constants.APP_ID + Constants.INTENT_CONTROL);
                } else if (runningActivity.equals(ControlFragment.class.getName())) {
                    alertIntent = new Intent(Constants.APP_ID + Constants.INTENT_DRONE);
                }  else {
                    return;
                }

                String messageText = d.getString("text");
                Log.d(TAG, messageText);
                if (messageText != null) {
                    alertIntent.putExtra(Constants.INTENT_DATA, Constants.ALERT_EVENT);
                    alertIntent.putExtra(Constants.INTENT_DATA_MESSAGE, d.getString("text"));
                    context.sendBroadcast(alertIntent);
                }
            }
        }else {
            Log.d(TAG, "No known action for " +
                    topic + " " + payload);
        }
    }
}
