package jake.imperial.drone.fragments;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;


import org.apache.http.message.BasicNameValuePair;

import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.cookie.CookieMiddleware;

import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;

import jake.imperial.drone.DroneApplication;
import jake.imperial.drone.R;
import jake.imperial.drone.utils.Constants;
import jake.imperial.drone.utils.WebSocketClient;


public class VideoFragment extends Fragment {
    private static final String TAG = VideoFragment.class.getName();
    private DroneApplication app;
    private BroadcastReceiver broadcastReceiver;

    private ImageView imageView;
    private String domain = "";

    public VideoFragment() {
    }

    public static VideoFragment newInstance() {
        return new VideoFragment();
    }

    @Override
    public void onResume(){
        Log.d(TAG, ".onResume() entered()");
        super.onResume();
        app = (DroneApplication) getActivity().getApplication();

        if (broadcastReceiver == null) {
            Log.d(TAG, ".onResume() - Registering VideoBroadcastReceiver");
            broadcastReceiver = new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d(TAG, ".onReceive() - Received intent for VideoBroadcastReceiver");
                    processIntent(intent);
                }
            };
        }

        IntentFilter intentFilter = new IntentFilter(Constants.APP_ID + "." + Constants.IMAGE_EVENT);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(broadcastReceiver, intentFilter);

        String url = "http://" + app.getDomain() + "/api/" + app.getCurrentDrone() + "/images/latest";
        Ion     .with(getContext())
                .load(url)
                .noCache()
                .withBitmap()
                .error(R.drawable.control_pad_button)
                .crossfade(true)
                .intoImageView(imageView);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_video, container, false);
        imageView = (ImageView) rootView.findViewById(R.id.imageview);

        ((CheckBox) rootView.findViewById(R.id.stream_audio)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                toggleAudioStream(isChecked);
            }
        });

        return rootView;
    }

    @Override
    public void onDestroyView(){
        super.onDestroyView();
    }

    private Runnable updateViewNew = new Runnable() {
        @Override
        public void run() {
            domain = app.getDomain();
            String url = "http://" + domain + "/api/images/latest";
            Log.d(TAG, "Updating image from " + url);

            Ion.with(getContext())
                    .load(url)
                    .noCache()
                    .withBitmap()
                    .error(R.drawable.control_pad_button)
                    .crossfade(true)
                    .intoImageView(imageView);

        }
    };

    private void updateImageView(){
        domain = app.getDomain();
        String url = "http://" + domain + "/api/" + app.getCurrentDrone() + "/images/latest";
        Log.d(TAG, "Updating image from " + url);

        CookieMiddleware middle = Ion.getDefault(getContext()).getCookieMiddleware();
        URI uri;
        try{
            uri = new URI(url);
        }catch(Exception e){
            return;
        }

        List<HttpCookie> bing = middle.getCookieStore().get(uri);
        CookieStore store = middle.getCookieStore();

        List<HttpCookie> cookies = ((CookieManager)CookieManager.getDefault()).getCookieStore().get(uri);
        try {
            HttpCookie cookie = cookies.get(0);
        } catch (IndexOutOfBoundsException e){
            return;
        }
        Ion.with(getContext())
                .load(url)
                .noCache()
                //.setHeader("Cookie", cookie.toString())
                .withBitmap()
                .error(R.drawable.control_pad_button)
                .crossfade(true)
                .intoImageView(imageView);



    }

    private void processIntent(Intent intent) {
        if (intent.getAction().contains(Constants.IMAGE_EVENT)) {
            updateImageView();
        }
    }

    private void toggleAudioStream(boolean enabled){
        Log.d(TAG, ".toggleAudioStream() entered");

        AudioTrack audioTrack = app.getAudioTrack();
        WebSocketClient client = app.getClient();

        if(!enabled){
            if(audioTrack != null) {
                audioTrack.stop();
                audioTrack.flush();
            }
            if(client != null){
                client.disconnect();
            }
            return;
        }

        if(audioTrack == null){
            // The formatting must be assumed/predetermined
            int buffSize = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, 8 * buffSize, AudioTrack.MODE_STREAM );
            app.setAudioTrack(audioTrack);
        }
        audioTrack.play();

        if(client == null){
            String uri = "ws://" + app.getDomain() + "/api/" + app.getCurrentDrone() + "/audio/stream/listen";

            Log.d(TAG, "Trying to connect to " + uri);
            URI url;
            try{
                url = new URI(uri);
            }catch(Exception e){
                return;
            }
            List<HttpCookie> cookies = ((CookieManager)CookieManager.getDefault()).getCookieStore().get(url);
            HttpCookie cookie;
            try {
                cookie = cookies.get(0);
            } catch (IndexOutOfBoundsException e){
                return;
            }

            List<BasicNameValuePair> extraHeaders = Arrays.asList(new BasicNameValuePair("Cookie", "session="+cookie.getValue()));
            client = new WebSocketClient(URI.create(uri), new WebSocketClient.Listener(){

                @Override
                public void onConnect() {
                    Log.d(TAG, "WebSocket Connected");
                }

                @Override
                public void onMessage(String message) {
                    Log.d(TAG, String.format("Got WebSocket string message: %s", message));
                }

                @Override
                public void onMessage(byte[] data) {
                    short[] shortData = new short[data.length / 2];
                    ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortData);
                    app.getAudioTrack().write(shortData, 0, shortData.length);
                }

                @Override
                public void onDisconnect(int code, String reason) {
                    Log.d(TAG, String.format("Disconnected from WebSocket. Code: %d Reason: %s", code, reason));
                }

                @Override
                public void onError(Exception error) {
                    Log.e(TAG, "WebSocket Error: ", error);
                }
            }, extraHeaders);

            app.setClient(client);
        }
        client.connect();

    }
}


