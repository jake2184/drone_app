
package jake.imperial.drone;

import android.app.Application;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioTrack;
import android.util.Log;

import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.HashMap;

import jake.imperial.drone.utils.WebSocketClient;


/**
 * Main Application class. Works for persistent storage that fragments can interact with
 */
public class DroneApplication extends Application{
    private final static String TAG = DroneApplication.class.getName();

    /**
     * Current visible fragment, updated whenever the fragment is changed
     */
    private String currentRunningActivity;

    // Values needed for connecting to IoT
    private String domain;
    private String organization;
    private String deviceId;
    private String APIKey;
    private String APIToken;

    // Values to authorise connections to the server
    private String username;
    private String password;


    // Application state variables
    private boolean connected = false;
    private int publishCount = 0;
    private int receiveCount = 0;
    private int unreadCount = 0;

    private int color = Color.WHITE;

    // Message log for log activity
    private ArrayList<String> messageLog = new ArrayList<>();

    private ArrayList<MarkerOptions> markerList = new ArrayList<>();
    private HashMap<String, LatLng> latestPosition = new HashMap<>();



    private String currentDrone = "";

    private int formatLevel = 1;
    private ArrayList<String> droneNames = new ArrayList<>();


    /**
     * WebSocket client for audio streaming
     */
    private WebSocketClient client;
    /**
     * For playing an audio stream recorded by a drone
     */
    private AudioTrack audioTrack;
    /**
     * Storage for graph data. Is persistent throughout GraphFragment lifecycle
     */
    private HashMap<String, SimpleXYSeries> sensorData = new HashMap<>();

    public void setDronePosition(String droneName, LatLng latestPosition) {
        this.latestPosition.put(droneName, latestPosition);
    }

    public LatLng getDronePosition(String droneName) {
        return latestPosition.get(droneName);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public ArrayList<String> getDroneNames() {
        return droneNames;
    }

    public void setDroneNames(ArrayList<String> droneNames) {
        this.droneNames = droneNames;
    }


    public String getCurrentDrone() {
        return currentDrone;
    }

    public void setCurrentDrone(String currentDrone) {
        this.currentDrone = currentDrone;
    }


    /**
     * Called when the application is created. Initializes the application.
     */
    @Override
    public void onCreate() {
        Log.d(TAG, ".onCreate() entered");
        super.onCreate();
    }


    /**
     * @return the name of the current visible fragment
     */
    public String getCurrentRunningActivity() { return currentRunningActivity; }

    /**
     * @param currentRunningActivity the name of the currently visible fragment
     */
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

    public ArrayList<MarkerOptions> getMarkerList(){
        return markerList;
    }

    public HashMap<String, SimpleXYSeries> getSensorData(){
        return sensorData;
    }

    public void clearSensorData(){
        sensorData.clear();
        resetFormatter();
    }

    /**
     * @return line formatter for the GraphFragment, dependent on existing number of datasets
     */
    public LineAndPointFormatter getFormatter(){
        int index = formatLevel++;
        int colour;
        switch(index){
            case 1:
                colour = Color.rgb(0xff, 0x0, 0x0);
                break;
            case 2:
                colour = Color.rgb(0x0, 0xff, 0x0);
                break;
            case 3:
                colour = Color.rgb(0x0, 0x0, 0xff);
                break;
            default:
                colour = Color.rgb(0,0,0);
        }


        LineAndPointFormatter formatter = new LineAndPointFormatter(colour, null, null, null);
        formatter.getLinePaint().setStrokeJoin(Paint.Join.ROUND);
        formatter.getLinePaint().setStrokeWidth(2);
        return formatter;
    }

    public void resetFormatter(){formatLevel = 1;}

    public AudioTrack getAudioTrack() {
        return audioTrack;
    }

    public void setAudioTrack(AudioTrack audioTrack) {
        this.audioTrack = audioTrack;
    }

    public WebSocketClient getClient() {
        return client;
    }

    public void setClient(WebSocketClient client) {
        this.client = client;
    }

    public void resetAudio(){
        setClient(null);
        setAudioTrack(null);
    }


}
