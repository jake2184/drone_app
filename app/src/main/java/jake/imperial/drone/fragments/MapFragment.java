package jake.imperial.drone.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import org.json.JSONException;
import org.json.JSONObject;

import jake.imperial.drone.DroneApplication;
import jake.imperial.drone.R;
import jake.imperial.drone.utils.Constants;


public class MapFragment extends Fragment implements OnMapReadyCallback{
    private static final String TAG = MapFragment.class.getName();
    private DroneApplication app;
    private BroadcastReceiver broadcastReceiver;
    private MapView mapView;

    private int mInterval = 5000;
    private Handler mHandler;
    private RequestQueue requestQueue;

    private Marker droneMarker;

    public static MapFragment newInstance() {
        return new MapFragment();
    }


    private GoogleMap mMap;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, ".onMapReady()");
        mMap = googleMap;

        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            // Show rationale and request permission.
        }



        // Change to load latest from drone.
        mMap.moveCamera(CameraUpdateFactory.zoomTo(12));

        LatLng London = new LatLng(51.5, -0.12);
        mMap.addMarker(new MarkerOptions().position(London).title("Marker in London"));

        mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(51.485138, -0.187755)));

        droneMarker = mMap.addMarker(new MarkerOptions()
            .position(new LatLng(0, 0))
            .title("Drone")
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.drone_icon2))
        );

        startDronePositionUpdate();

    }

    @Override
    public void onResume(){
        Log.d(TAG, ".onResume() entered()");
        super.onResume();
        app = (DroneApplication) getActivity().getApplication();
        app.setCurrentRunningActivity(TAG);
        mapView.onResume();
        if (broadcastReceiver == null) {
            Log.d(TAG, ".onResume() - Registering LogBroadcastReceiver");
            broadcastReceiver = new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d(TAG, ".onReceive() - Received intent for logBroadcastReceiver");
                    processIntent(intent);
                }
            };
        }
        getActivity().getApplicationContext().registerReceiver(broadcastReceiver,
                new IntentFilter(Constants.APP_ID + Constants.INTENT_MAP));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        View rootView =  inflater.inflate(R.layout.fragment_map, container, false);

        mapView = (MapView) rootView.findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        mHandler = new Handler();

        requestQueue = Volley.newRequestQueue(getContext());

        return rootView;
    }

    @Override
    public void onDestroyView(){
        super.onDestroyView();
        stopDronePositionUpdate();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory(){
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onPause(){
        super.onPause();
        mapView.onPause();
    }

    private Runnable updateDronePosition = new Runnable() {
        @Override
        public void run() {
            if(app != null){
                String domain = app.getDomain();
                String url = "http://" + domain + "/getLatestGPS";
                Log.d(TAG, "Updating drone position from " + url);

                JsonObjectRequest request  = new JsonObjectRequest(Request.Method.GET, url, null,
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                try {
                                    double lat = response.getDouble("lat");
                                    double lon = response.getDouble("lon");
                                    LatLng dronePos = new LatLng (lat, lon);

                                    droneMarker.setPosition(dronePos);
                                    Log.d(TAG, "New drone position is: " + String.valueOf(lat) + "," + String.valueOf(lon) );

                                }catch (JSONException e){

                                }
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                });
                // Add the request to the RequestQueue.
                requestQueue.add(request);

            }
            mHandler.postDelayed(updateDronePosition, mInterval);
        }
    };

    private void startDronePositionUpdate() {
        updateDronePosition.run();
    }

    private void stopDronePositionUpdate() {
        mHandler.removeCallbacks(updateDronePosition);
    }

    private void processIntent(Intent intent){
        String data = intent.getStringExtra(Constants.INTENT_DATA);
        assert data != null;
        if (data.equals(Constants.TEXT_EVENT)) {
            // Log them somehow?
        } else if (data.equals(Constants.ALERT_EVENT)) {
            String message = intent.getStringExtra(Constants.INTENT_DATA_MESSAGE);
            new AlertDialog.Builder(getActivity())
                    .setTitle("Bing")
                    .setMessage(message)
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        }
                    }).show();
        }

    }
}