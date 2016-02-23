
package jake.imperial.drone.utils;

public class Constants {

    public final static String APP_ID = "jake.imperial.drone";
    public final static String SETTINGS = APP_ID+".Settings";

    public final static String SETTINGS_MQTT_SERVER = "messaging.internetofthings.ibmcloud.com";
    public final static String SETTINGS_MQTT_PORT = "1883";
    public final static String SETTINGS_USERNAME = "use-token-auth";

    public final static String M2M = "m2m";
    public final static String M2M_DEMO_SERVER = "messagesight.demos.ibm.com";
    public final static String M2M_CLIENTID = "d:m2m:";

    public static final String QUICKSTART = "quickstart";
    public final static String QUICKSTART_SERVER = "184.172.124.189";

    public static final String LOGIN_LABEL = "LOGIN";
    public static final String IOT_LABEL = "IOT";
    public static final String LOG_LABEL = "LOG";

    public enum ConnectionType {
        M2M, QUICKSTART, IOTF
    }

    public enum ActionStateStatus {
        CONNECTING, DISCONNECTING, SUBSCRIBE, UNSUBSCRIBE, PUBLISH
    }

    // IoT properties
    public final static String AUTH_TOKEN = "authtoken";
    public final static String DEVICE_ID = "deviceid";
    public final static String ORGANIZATION = "organization";
    public final static String DEVICE_TYPE = "Android";

    // IoT topic formats
    public final static String EVENT_TOPIC = "iot-2/evt/";
    public final static String COMMAND_TOPIC = "iot-2/cmd/";
    public final static String FORMAT_TOPIC = "/fmt/json";

    // IoT events and commands
    public final static String COLOR_EVENT = "color";
    public final static String TEXT_EVENT = "text";
    public final static String ALERT_EVENT = "alert";
    public final static String UNREAD_EVENT = "unread";
    public final static String STATUS_EVENT = "status";

    public final static String CONNECTIVITY_MESSAGE = "connectivityMessage";
    public final static String ACTION_INTENT_CONNECTIVITY_MESSAGE_RECEIVED = Constants.APP_ID + "." + "CONNECTIVITY_MESSAGE_RECEIVED";

    // Fragment intents
    public final static String INTENT_CONNECTION = "INTENT_CONNECTION";
    public final static String INTENT_DRONE = "INTENT_DRONE";
    public final static String INTENT_CONTROL = "INTENT_CONTROL";
    public final static String INTENT_VIDEO = "INTENT_VIDEO";
    public final static String INTENT_LOG = "INTENT_LOG";
    public final static String INTENT_DATA = "data";

    // MQTT action intent data
    public final static String INTENT_DATA_CONNECT = "connect";
    public final static String INTENT_DATA_DISCONNECT = "disconnect";
    public final static String INTENT_DATA_PUBLISHED = "publish";
    public final static String INTENT_DATA_RECEIVED = "receive";
    public final static String INTENT_DATA_MESSAGE = "message";

    public final static int ERROR_BROKER_UNAVAILABLE = 3;

    // Location Services
    public final static int LOCATION_MIN_TIME = 30000;
    public final static float LOCATION_MIN_DISTANCE = 5;
}
