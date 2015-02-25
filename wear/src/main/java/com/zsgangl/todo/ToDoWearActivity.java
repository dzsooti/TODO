package com.zsgangl.todo;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.List;

import todo.zsgangl.com.todo.R;

public class ToDoWearActivity extends Activity implements View.OnClickListener,
    MessageApi.MessageListener, GoogleApiClient.ConnectionCallbacks {

    private final String TAG = "ToDoWearActivity";
    private static final int SPEECH_REQUEST_CODE = 0;
    private GoogleApiClient mGoogleApiClient;
    private TextView mCreateTextView;
    private Node mPeerNode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.layout_todo_wear);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.e(TAG, "onConnectionFailed: " + result);
                    }
                })
                .addApi(Wearable.API)
                .build();


        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mCreateTextView = (TextView) stub.findViewById(R.id.text);
                mCreateTextView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        displaySpeechRecognizer();
                    }
                });
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
    }

    // Create an intent that can start the Speech Recognizer activity
    private void displaySpeechRecognizer() {

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        // Start the activity, the intent will be populated with the speech text
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    // This callback is invoked when the Speech Recognizer returns.
    // This is where you process the intent and extract the speech text from the intent.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);

            createToDo(spokenText);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void createToDo(String todo) {


    }

    @Override
    public void onConnected(Bundle bundle) {

        Log.v(TAG, "connected to Google Play Services on Wear!");
        Wearable.MessageApi.addListener(mGoogleApiClient, this).setResultCallback(resultCallback);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onMessageReceived(final MessageEvent messageEvent) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(messageEvent.getPath().endsWith("connected")){

                }

            }
        });
        Log.e(TAG, "Message received on wear: " + messageEvent.getPath());

    }

    private ResultCallback<Status> resultCallback = new ResultCallback<Status>() {
        @Override
        public void onResult(Status status) {
            Log.e(TAG, "Status: " + status.getStatus().isSuccess());
            new AsyncTask<Void, Void, Void>(){
                @Override
                protected Void doInBackground(Void... params) {
                    sendStartMessage();
                    return null;
                }
            }.execute();
        }
    };

    private void sendStartMessage() {

        NodeApi.GetConnectedNodesResult rawNodes =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
        for (final Node node : rawNodes.getNodes()) {
            Log.v(TAG, "Node: " + node.getId());
            PendingResult<MessageApi.SendMessageResult> result = Wearable.MessageApi.sendMessage(
                    mGoogleApiClient,
                    node.getId(),
                    "/start",
                    null
            );
            result.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                @Override
                public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                    // The message is done sending.
                    // This doesn't mean it worked, though.
                    Log.v(TAG, "Our callback is done.");
                    mPeerNode = node; // Save the node that worked so we don't have to loop again.
                }
            });
        }
    }

    @Override
    public void onClick(View v) {

    }
}
