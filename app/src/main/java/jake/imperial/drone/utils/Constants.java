
package jake.imperial.drone.utils;

public class Constants {

    public final static String APP_ID = "jake.imperial.drone";

    public final static String SETTINGS_MQTT_SERVER = "messaging.internetofthings.ibmcloud.com";
    public final static String SETTINGS_MQTT_PORT = "1883";
    public final static String SETTINGS_USERNAME = "use-token-auth";

    public enum ActionStateStatus {
        CONNECTING, DISCONNECTING, SUBSCRIBE, UNSUBSCRIBE, PUBLISH
    }

    public final static String DEVICE_TYPE = "Android";

    // IoT topic formats
    public final static String EVENT_TOPIC = "iot-2/evt/";
    public final static String COMMAND_TOPIC = "iot-2/cmd/";
    public final static String FORMAT_TOPIC = "/fmt/json";

    // IoT events and commands
    public final static String TEXT_EVENT = "text";
    public final static String ALERT_EVENT = "alert";
    public final static String UNREAD_EVENT = "unread";
    public final static String LOG_EVENT = "log";
    public final static String IMAGE_EVENT = "image";
    public final static String SENSOR_EVENT = "sensors";
    public final static String SENSOR_TYPE_EVENT = "sensorType";
    public final static String CONNECTIVITY_MESSAGE = "connectivityMessage";

    public final static String ACTION_INTENT_CONNECTIVITY_MESSAGE_RECEIVED = Constants.APP_ID + "." + "CONNECTIVITY_MESSAGE_RECEIVED";
    // Fragment intents
    public final static String INTENT_CONNECTION = "INTENT_CONNECTION";

    public final static String INTENT_DRONE = "INTENT_DRONE";
    public final static String INTENT_DATA = "data";
    public final static String INTENT_POSITION = "positionUpdate";

    // MQTT action intent data
    public final static String INTENT_DATA_CONNECT = "connect";
    public final static String INTENT_DATA_FAILURE = "failure";
    public final static String INTENT_DATA_SUCCESS = "success";
    public final static String INTENT_DATA_DISCONNECT = "disconnect";
    public final static String INTENT_DATA_PUBLISHED = "publish";
    public final static String INTENT_DATA_RECEIVED = "receive";
    public final static String INTENT_DATA_MESSAGE = "message";
    public final static String INTENT_DATA_LOC_LAT = "lat";
    public final static String INTENT_DATA_LOC_LON = "lon";
    public final static String INTENT_DATA_SENSORTYPE = "sensorType";

    public final static int ERROR_BROKER_UNAVAILABLE = 3;

}
