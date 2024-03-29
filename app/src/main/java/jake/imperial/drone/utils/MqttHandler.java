
package jake.imperial.drone.utils;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import jake.imperial.drone.fragments.ConnectionFragment;
import jake.imperial.drone.fragments.ControlFragment;
import jake.imperial.drone.DroneApplication;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.json.JSONException;

/**
 * This class provides a wrapper around the MQTT client API's and implements
 * the MqttCallback interface.
 */
public class MqttHandler implements MqttCallback {

    private final static String TAG = MqttHandler.class.getName();
    private static MqttHandler instance;
    private MqttAndroidClient client;
    private Context context;
    private DroneApplication app;

    private MqttHandler(Context context) {
        this.context = context;
        this.app = (DroneApplication) context.getApplicationContext();
        this.client = null;
    }

    /**
     * @param context The application context for the object.
     * @return The MqttHandler object for the application.
     */
    public static MqttHandler getInstance(Context context) {
        Log.d(TAG, ".getInstance() entered");
        if (instance == null) {
            instance = new MqttHandler(context);
        }
        return instance;
    }

    /**
     * Connect MqttAndroidClient to the MQTT server
     * @return true if the client connects successfully
     */
    public boolean connect() {
        Log.d(TAG, ".connect() entered");

        // check if client is already connected
        if (!isMqttConnected()) {
            String serverHost;
            String serverPort = Constants.SETTINGS_MQTT_PORT;
            String clientId;
            serverHost = app.getOrganization() + "." + Constants.SETTINGS_MQTT_SERVER;
            //clientId = "d:" + app.getOrganization() + ":" + Constants.DEVICE_TYPE + ":" + app.getDeviceId();
            // Register the client as an application
            clientId = "a:" + app.getOrganization() + ":" + app.getDeviceId();

            Log.d(TAG, ".initMqttConnection() - Host name: " + serverHost + ", Port: " + serverPort
                    + ", client id: " + clientId);


            String connectionUri = "tcp://" + serverHost + ":" + serverPort;
            if (client != null) {
                client.unregisterResources();
                client = null;
            }
            client = new MqttAndroidClient(context, connectionUri, clientId);
            client.setCallback(this);

            // create ActionListener to handle connection results
            ActionListener listener = new ActionListener(context, Constants.ActionStateStatus.CONNECTING);
            // create MqttConnectOptions and set the clean session flag
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);

            options.setUserName(app.getAPIKey());
            options.setPassword(app.getAPIToken().toCharArray());


            try {
                // connect
                client.connect(options, context, listener);
                return true;
            } catch (MqttException e) {
                Log.e(TAG, "Exception caught while attempting to connect to server ", e.getCause());
                Log.e(TAG, e.getMessage() + " " + options + " " + clientId);
                if (e.getReasonCode() == (Constants.ERROR_BROKER_UNAVAILABLE)) {
                    // error while connecting to the broker - send an intent to inform the user
                    Intent actionIntent = new Intent(Constants.ACTION_INTENT_CONNECTIVITY_MESSAGE_RECEIVED);
                    actionIntent.putExtra(Constants.CONNECTIVITY_MESSAGE, Constants.ERROR_BROKER_UNAVAILABLE);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(actionIntent);
                }
                return false;
            }
        }
        return true;
    }

    /**
     * Disconnect MqttAndroidClient from the MQTT server
     */
    public void disconnect() {
        Log.d(TAG, ".disconnect() entered");

        // check if client is actually connected
        if (isMqttConnected()) {
            ActionListener listener = new ActionListener(context, Constants.ActionStateStatus.DISCONNECTING);
            try {
                // disconnect
                client.disconnect(context, listener);
            } catch (MqttException e) {
                Log.e(TAG, "Exception caught while attempting to disconnect from server", e.getCause());
            }
        }
    }

    /**
     * Subscribe MqttAndroidClient to a topic
     *
     * @param topic to subscribe to
     * @param qos   to subscribe with
     */
    public void subscribe(String topic, int qos) {
        Log.d(TAG, ".subscribe() entered");

        // check if client is connected
        if (isMqttConnected()) {
            try {
                // create ActionListener to handle subscription results
                ActionListener listener = new ActionListener(context, Constants.ActionStateStatus.SUBSCRIBE);
                Log.d(TAG, ".subscribe() - Subscribing to: " + topic + ", with QoS: " + qos);
                client.subscribe(topic, qos, context, listener);
            } catch (MqttException e) {
                Log.e(TAG, "Exception caught while attempting to subscribe to topic " + topic, e.getCause());
            }
        } else {
            connectionLost(null);
        }
    }

    /**
     * Unsubscribe MqttAndroidClient from a topic
     *
     * @param topic to unsubscribe from
     */
    public void unsubscribe(String topic) {
        Log.d(TAG, ".unsubscribe() entered");

        // check if client is connected
        if (isMqttConnected()) {
            try {
                // create ActionListener to handle unsubscription results
                ActionListener listener = new ActionListener(context, Constants.ActionStateStatus.UNSUBSCRIBE);
                client.unsubscribe(topic, context, listener);
            } catch (MqttException e) {
                Log.e(TAG, "Exception caught while attempting to unsubscribe from topic " + topic, e.getCause());
            }
        } else {
            connectionLost(null);
        }
    }

    /**
     * Publish message to a topic
     *
     * @param topic    to publish the message to
     * @param message  JSON object representation as a string
     * @param retained true if retained flag is requred
     * @param qos      quality of service (0, 1, 2)
     */
    public void publish(String topic, String message, boolean retained, int qos) {
        Log.d(TAG, ".publish() entered");

        // check if client is connected
        if (isMqttConnected()) {
            // create a new MqttMessage from the message string
            MqttMessage mqttMsg = new MqttMessage(message.getBytes());
            // set retained flag
            mqttMsg.setRetained(retained);
            // set quality of service
            mqttMsg.setQos(qos);
            try {
                // create ActionListener to handle message published results
                ActionListener listener = new ActionListener(context, Constants.ActionStateStatus.PUBLISH);
                Log.d(TAG, ".publish() - Publishing " + message + " to: " + topic + ", with QoS: " + qos + " with retained flag set to " + retained);
                client.publish(topic, mqttMsg, context, listener);

                int count = app.getPublishCount();
                app.setPublishCount(++count);

                String runningActivity = app.getCurrentRunningActivity();
                if (runningActivity != null && runningActivity.equals(ControlFragment.class.getName())) {
                    Intent actionIntent = new Intent(Constants.APP_ID + Constants.INTENT_DRONE);
                    actionIntent.putExtra(Constants.INTENT_DATA, Constants.INTENT_DATA_PUBLISHED);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(actionIntent);
                }
            } catch (MqttPersistenceException e) {
                Log.e(TAG, "MqttPersistenceException caught while attempting to publish a message", e.getCause());
            } catch (MqttException e) {
                Log.e(TAG, "MqttException caught while attempting to publish a message", e.getCause());
            }
        } else {
            connectionLost(null);
        }
    }

    /**
     * Handle loss of connection from the MQTT server.
     * @param throwable throws
     */
    @Override
    public void connectionLost(Throwable throwable) {
        Log.e(TAG, ".connectionLost() entered");

        if (throwable != null) {
            throwable.printStackTrace();
        }

        app.setConnected(false);

        String runningActivity = app.getCurrentRunningActivity();
        if (runningActivity != null && runningActivity.equals(ConnectionFragment.class.getName())) {
            Intent actionIntent = new Intent(Constants.APP_ID + Constants.INTENT_CONNECTION);
            actionIntent.putExtra(Constants.INTENT_DATA, Constants.INTENT_DATA_DISCONNECT);
            LocalBroadcastManager.getInstance(context).sendBroadcast(actionIntent);
        }
    }

    /**
     * Process incoming messages to the MQTT client.
     *
     * @param topic       The topic the message was received on.
     * @param mqttMessage The message that was received
     * @throws Exception  Exception that is thrown if the message is to be rejected.
     */
    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
        Log.d(TAG, ".messageArrived() entered");

        int receiveCount = app.getReceiveCount();
        app.setReceiveCount(++receiveCount);

        String payload = new String(mqttMessage.getPayload());
        Log.d(TAG, ".messageArrived - Message received on topic " + topic
                + ": message is " + payload);
        try {
            // send the message through the application logic
            MessageConductor.getInstance(context).steerMessage(payload, topic);
        } catch (JSONException e) {
            Log.e(TAG, ".messageArrived() - Exception caught while steering a message", e.getCause());
            e.printStackTrace();
        }
    }

    /**
     * Handle notification that message delivery completed successfully.
     *
     * @param iMqttDeliveryToken The token corresponding to the message which was delivered.
     */
    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        Log.d(TAG, ".deliveryComplete() entered");
    }

    /**
     * Checks if the MQTT client has an active connection
     *
     * @return True if client is connected, false if not.
     */
    private boolean isMqttConnected() {
        Log.d(TAG, ".isMqttConnected() entered");
        boolean connected = false;
        try {
            if ((client != null) && (client.isConnected())) {
                connected = true;
            }
        } catch (Exception e) {
            // swallowing the exception as it means the client is not connected
        }
        Log.d(TAG, ".isMqttConnected() - returning " + connected);
        return connected;
    }
}
