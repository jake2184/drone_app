
package jake.imperial.drone;

import android.app.Application;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;

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
    private String authToken;
    private Constants.ConnectionType connectionType;

    private SharedPreferences settings;

    // Application state variables
    private boolean connected = false;
    private int publishCount = 0;
    private int receiveCount = 0;
    private int unreadCount = 0;

    private int color = Color.WHITE;

    // Message log for log activity
    private ArrayList<String> messageLog = new ArrayList<>();


    /**
     * Called when the application is created. Initializes the application.
     */
    @Override
    public void onCreate() {
        Log.d(TAG, ".onCreate() entered");
        super.onCreate();

        settings = getSharedPreferences(Constants.SETTINGS, 0);


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

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
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

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public ArrayList<String> getMessageLog() {
        return messageLog;
    }

}
