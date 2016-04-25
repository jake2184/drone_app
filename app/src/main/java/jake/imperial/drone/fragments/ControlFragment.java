package jake.imperial.drone.fragments;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import jake.imperial.drone.DroneApplication;
import jake.imperial.drone.R;
import jake.imperial.drone.utils.Constants;
import jake.imperial.drone.utils.MqttHandler;
import jake.imperial.drone.utils.RepeatListener;
import jake.imperial.drone.utils.TopicFactory;


public class ControlFragment extends Fragment {
    private static final String TAG = ControlFragment.class.getName();
    private DroneApplication app;
    private BroadcastReceiver broadcastReceiver;

    public ControlFragment() {
    }

    public static ControlFragment newInstance() {
        return new ControlFragment();
    }

    @Override
    public void onResume(){
        Log.d(TAG, ".onResume() entered");
        super.onResume();
        app = (DroneApplication) getActivity().getApplication();
        //app.setCurrentRunningActivity(TAG);

        if (broadcastReceiver == null) {
            Log.d(TAG, ".onResume() - Registering ControlBroadcastReceiver");
            broadcastReceiver = new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    if(app.getCurrentRunningActivity().equals(TAG)) {
                        Log.d(TAG, ".onReceive() - Received intent for ControlBroadcastReceiver");
                        processIntent(intent);
                    }
                }
            };
        }

        IntentFilter intentFilter = new IntentFilter(Constants.APP_ID + "." + Constants.ALERT_EVENT);
        getActivity().getApplicationContext().registerReceiver(broadcastReceiver, intentFilter);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_control, container, false);

        Button button = (Button) rootView.findViewById(R.id.lhs_up);
        button.setOnTouchListener(new RepeatListener(100, 100, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateCommand("lhs_up");
            }
        }));

        button = (Button) rootView.findViewById(R.id.lhs_left);
        button.setOnTouchListener(new RepeatListener(100, 100, new View.OnClickListener() {
             @Override
             public void onClick(View v) {
                 generateCommand("lhs_left");
             }
         }));
        button = (Button) rootView.findViewById(R.id.lhs_right);
        button.setOnTouchListener(new RepeatListener(100, 100, new View.OnClickListener() {
             @Override
             public void onClick(View v) {
                 generateCommand("lhs_right");
             }
         }));
        button = (Button) rootView.findViewById(R.id.lhs_down);
        button.setOnTouchListener(new RepeatListener(100, 100, new View.OnClickListener() {
             @Override
             public void onClick(View v) {
                 generateCommand("lhs_down");
             }
         }));
        button = (Button) rootView.findViewById(R.id.rhs_up);
        button.setOnTouchListener(new RepeatListener(100, 100, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateCommand("rhs_up");
            }
        }));
        button = (Button) rootView.findViewById(R.id.rhs_left);
        button.setOnTouchListener(new RepeatListener(100, 100, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateCommand("rhs_left");
            }
        }));
        button = (Button) rootView.findViewById(R.id.rhs_right);
        button.setOnTouchListener(new RepeatListener(100, 100, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateCommand("rhs_right");
            }
        }));
        button = (Button) rootView.findViewById(R.id.rhs_down);
        button.setOnTouchListener(new RepeatListener(100, 100, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateCommand("rhs_down");
            }
        }));

        return rootView;
    }

    private void generateCommand(String button){
        String messageData = TopicFactory.getTextMessage(button);
        MqttHandler mqtt = MqttHandler.getInstance(getActivity().getApplicationContext());
        mqtt.publish(TopicFactory.getEventTopic(Constants.DEVICE_TYPE, "phone", Constants.TEXT_EVENT), messageData, false, 0);
    }

    private void processIntent(Intent intent) {
        if(!app.getCurrentRunningActivity().equals(TAG)){return;}
        String data = intent.getStringExtra(Constants.INTENT_DATA);
        assert data != null;
        if (data.equals(Constants.ALERT_EVENT)) {
            String message = intent.getStringExtra(Constants.INTENT_DATA_MESSAGE);
            new AlertDialog.Builder(getActivity())
                    .setTitle("Alert:")
                    .setMessage(message)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        }
                    }).show();
        }
    }
}
