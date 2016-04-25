
package jake.imperial.drone;

import android.app.Application;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;

import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONObject;

import java.util.ArrayList;

import jake.imperial.drone.utils.Constants;

public class DroneApplication extends Application{
    private final static String TAG = DroneApplication.class.getName();

    // Current activity of the application, updated whenever activity is changed
    private String currentRunningActivity;

    // Values needed for connecting to IoT
    private String domain;
    private String organization;

    private String deviceId;
    private String APIKey;
    private String APIToken;
    private Constants.ConnectionType connectionType;


    // Application state variables
    private boolean connected = false;
    private int publishCount = 0;
    private int receiveCount = 0;
    private int unreadCount = 0;

    private int color = Color.WHITE;

    // Message log for log activity
    private ArrayList<String> messageLog = new ArrayList<>();
    private ArrayList<MarkerOptions> markerList = new ArrayList<>();

    private JSONObject sensorData = new JSONObject();


    /**
     * Called when the application is created. Initializes the application.
     */
    @Override
    public void onCreate() {
        Log.d(TAG, ".onCreate() entered");
        super.onCreate();
    }

    // Getters and Setters
    public String getCurrentRunningActivity() { return currentRunningActivity; }

    public void setCurrentRunningActivity(String currentRunningActivity) { this.currentRunningActivity = currentRunningActivity; }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getAPIKey() {
        return APIKey;
    }

    public void setAPIKey(String APIKey) {
        this.APIKey = APIKey;
    }

    public String getAPIToken() {
        return APIToken;
    }

    public void setAPIToken(String APIToken) {
        this.APIToken = APIToken;
    }

    public void setConnectionType(Constants.ConnectionType type) {
        this.connectionType = type;
    }

    public Constants.ConnectionType getConnectionType() {
        return this.connectionType;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public int getPublishCount() {
        return publishCount;
    }

    public void setPublishCount(int publishCount) {
        this.publishCount = publishCount;
    }

    public int getReceiveCount() {
        return receiveCount;
    }

    public void setReceiveCount(int receiveCount) {
        this.receiveCount = receiveCount;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public String APIKey(){
        return "a-gqn7ig-cchluabzjp";
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public ArrayList<String> getMessageLog() {
        return messageLog;
    }

    public ArrayList<MarkerOptions> getMarkerList(){
        return markerList;
    }

    public JSONObject getSensorData() {
        return sensorData;
    }

    public void setSensorData(JSONObject sensorData) {
        this.sensorData = sensorData;
    }

}
