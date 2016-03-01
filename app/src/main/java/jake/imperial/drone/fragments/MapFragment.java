package jake.imperial.drone.fragments;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import jake.imperial.drone.DroneApplication;
import jake.imperial.drone.R;
import jake.imperial.drone.utils.Constants;

/**
 * Created by root on 29/02/16.
 */
public class MapFragment extends Fragment implements OnMapReadyCallback{
    private static final String TAG = MapFragment.class.getName();
    protected DroneApplication app;
    protected BroadcastReceiver broadcastReceiver;
    protected MapView mapView;
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




        // Change to load latest from drone.
        LatLng London = new LatLng(51.5, -0.12);
        mMap.addMarker(new MarkerOptions().position(London).title("Marker in London"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(London));
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

        return rootView;
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
