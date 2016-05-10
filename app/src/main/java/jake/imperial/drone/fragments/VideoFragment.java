package jake.imperial.drone.fragments;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;


import org.apache.http.message.BasicNameValuePair;

import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.callback.WritableCallback;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.http.AsyncHttpClient;
//import com.koushikdutta.async.http.BasicNameValuePair;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.cookie.CookieMiddleware;

import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
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

        String url = "http://" + app.getDomain() + "/api/images/latest";
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

        ((Button) rootView.findViewById(R.id.stream_audio)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAudioStream();
            }
        });

        return rootView;
    }

    @Override
    public void onDestroyView(){
        super.onDestroyView();
        //stopRepeatingTask();
    }

    private Runnable updateView = new Runnable() {
        @Override
        public void run() {
            //if(app != null) {
                domain = app.getDomain();
                String url = "http://" + domain + "/getLatestImage";
                Log.d(TAG, "Updating image from " + url);

                // null check on context?

                Ion.with(getContext())
                        .load(url)
                        .noCache()
                        .withBitmap()
                        .error(R.drawable.control_pad_button)
                        .crossfade(true)
                        .intoImageView(imageView);
            //}
            //mHandler.postDelayed(updateView, mInterval);
        }
    };

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
        String url = "http://" + domain + "/api/images/latest";
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
        HttpCookie cookie = cookies.get(0);

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
            //new Thread(updateViewNew).start();
            updateImageView();
        }
    }

    private void startAudioStream(){
        Log.d(TAG, ".startAudioStream() entered");
        AudioTrack audioTrack = app.getAudioTrack();
        if(audioTrack == null){

            // The formatting must be assumed/predetermined
            int buffSize = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, 8 * buffSize, AudioTrack.MODE_STREAM );
            audioTrack.play();
            app.setAudioTrack(audioTrack);
        }

        WebSocketClient client = app.getClient();
        if(client == null){
            String uri = "ws://" + app.getDomain() + "/api/audio/stream/listen";

            Log.d(TAG, "Trying to connect to " + uri);
            URI url;
            try{
                url = new URI(uri);
            }catch(Exception e){
                return;
            }
            List<HttpCookie> cookies = ((CookieManager)CookieManager.getDefault()).getCookieStore().get(url);
            HttpCookie cookie = cookies.get(0);

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
            client.connect();

            app.setClient(client);
        }

    }
}


