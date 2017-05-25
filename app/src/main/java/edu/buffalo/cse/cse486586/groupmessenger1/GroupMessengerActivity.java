package edu.buffalo.cse.cse486586.groupmessenger1;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;

import static edu.buffalo.cse.cse486586.groupmessenger1.DatabaseHelper.TABLE_NAME;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 */
public class GroupMessengerActivity extends Activity {
    private static final String TAG = GroupMessengerActivity.class.getSimpleName();
    private static final int SERVER_PORT = 10000;
    private static ArrayList<Integer> remotePorts;
    private static DatabaseHelper databaseHelper;
    private ServerTask serverTask;
    private ServerSocket serverSocket;
    private int msgCounter;
    private ContentResolver contentResolver;
    private Uri uri;

    @Override
    protected void onDestroy() {
        serverTask.cancel(true);
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        databaseHelper = new DatabaseHelper(getApplicationContext());
        msgCounter = 0;
        contentResolver = getContentResolver();
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority("edu.buffalo.cse.cse486586.groupmessenger1.provider");
        uriBuilder.scheme("content");
        uri = uriBuilder.build();

        //Setting the serverSocket
        try {
            serverSocket = new ServerSocket(SERVER_PORT);
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Initializing Own Port and calculating ports for other devices
        remotePorts = new ArrayList<Integer>();

        int i = 5554;
        while (i < 5564) {
            remotePorts.add(i * 2);
            i += 2;
        }

        serverTask = new ServerTask();
        serverTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);

        final EditText editText = (EditText) findViewById(R.id.editText1);

        Button sendButton = (Button) findViewById(R.id.button4);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String msg = editText.getText().toString();
                new ClientTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, msg);
                editText.setText("");
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    /***
     * ServerTask is an AsyncTask that should handle incoming messages. It is created by
     * ServerTask.executeOnExecutor() call in SimpleMessengerActivity.
     * <p>
     * Please make sure you understand how AsyncTask works by reading
     * http://developer.android.com/reference/android/os/AsyncTask.html
     *
     * @author stevko
     */

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        ServerSocket serverSocket;
        SQLiteDatabase sqLiteDatabase;

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            /*TODO: Implement Server Task to receive multiple messages together*/
            serverSocket = sockets[0];
            sqLiteDatabase = databaseHelper.getWritableDatabase();
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                    DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                    String msg = dataInputStream.readUTF();
                    dataOutputStream.writeUTF("OK");
                    Log.d("MSG RECEIVED", msg);
                    publishProgress(msg);
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }


            return null;
        }

        protected void onProgressUpdate(String... strings) {
            ContentValues values = new ContentValues();
            values.put("key", Integer.toString(msgCounter));
            values.put("value", strings[0]);
            msgCounter++;
            contentResolver.insert(uri,values);
            return;
        }

        @Override
        protected void onCancelled() {
            if (!serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            sqLiteDatabase.close();
            super.onCancelled();
        }
    }

    /***
     * ClientTask is an AsyncTask that should send a string over the network.
     * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
     * an enter key press event.
     *
     * @author stevko
     */
    private class ClientTask extends AsyncTask<String, Void, Void> {

        /*TODO: Implement Client Task to send messages*/
        @Override
        protected Void doInBackground(String... msgs) {
            String msgToSend = msgs[0];
            try {
                for (int port : remotePorts) {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            port);
                    DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                    dataOutputStream.writeUTF(msgToSend);
                    dataOutputStream.flush();
                    Log.d("MSG SENT", msgToSend);
                    DataInputStream dataInputStream = new DataInputStream((socket.getInputStream()));
                    String resp = dataInputStream.readUTF();
                    if (resp.equals("OK"))
                        socket.close();

                }
            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }

            return null;
        }
    }
}
