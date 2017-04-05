package todo.mqtt.com.mqtttodo;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.IBinder;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Locale;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import todo.mqtt.com.mqtttodo.MQTTService.LocalBinder;

public class MainActivity extends Activity {

    public static final String ERROR_DETECTED = "No NFC tag detected!";
    public static final String WRITE_SUCCESS = "Text written to the NFC tag successfully!";
    public static final String WRITE_ERROR = "Error during writing, is the NFC tag close enough to your device?";
    NfcAdapter nfcAdapter;
    PendingIntent pendingIntent;
    IntentFilter writeTagFilters[];
    boolean writeMode;
    Tag myTag;
    public static Context context;

    TextView tvNFCContent;

    public static CheckBox llamada;
    CheckBox carro;
    CheckBox trabajo;
    CheckBox dormir;
    public static TextView conn;
    public static Button button1;

    MQTTService mServer;
    boolean mBounded;

    public static String MY_PREFS = "MY_PREFS";
    public static SharedPreferences mySharedPreferences;
    int prefMode = Activity.MODE_PRIVATE;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

        startService(new Intent(getBaseContext(), MQTTService.class));

        tvNFCContent = (TextView) findViewById(R.id.nfc_contents);
        llamada = (CheckBox) findViewById(R.id.checkBox);
        carro = (CheckBox) findViewById(R.id.checkBox2);
        trabajo = (CheckBox) findViewById(R.id.checkBox3);
        dormir = (CheckBox) findViewById(R.id.checkBox4);

        conn = (TextView) findViewById(R.id.conStatus);
        button1 = (Button) findViewById(R.id.button2);
        button1.setOnClickListener(mButton1_OnClickListener);


        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            // Stop here, we definitely need NFC
            Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
            finish();
        }
        readFromIntent(getIntent());

        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        writeTagFilters = new IntentFilter[] { tagDetected };

    }


    /******************************************************************************
     **********************************Read From NFC Tag***************************
     ******************************************************************************/
    private void readFromIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] msgs = null;
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
            }
            buildTagViews(msgs);
        }
    }
    private void buildTagViews(NdefMessage[] msgs) {
        if (msgs == null || msgs.length == 0) return;

        String text = "";
//        String tagId = new String(msgs[0].getRecords()[0].getType());
        byte[] payload = msgs[0].getRecords()[0].getPayload();
        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16"; // Get the Text Encoding
        int languageCodeLength = payload[0] & 0063; // Get the Language Code, e.g. "en"
        // String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");

        try {
            // Get the Text
            text = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
        } catch (UnsupportedEncodingException e) {
            Log.e("UnsupportedEncoding", e.toString());
        }

        tvNFCContent.setText("NFC Content: " + text);


        if(text.toString().contains("Carro")){
            carro.setChecked(true);
            if(mServer.mClient.isConnected()){
                mServer.sendMessage("Estoy en el carro");
            }else{
                Toast.makeText(MainActivity.this, "Hay problemas con el Raspberry o desconectado", Toast.LENGTH_SHORT).show();
            }
        }else if(text.toString().contains("Trabajo")){
            trabajo.setChecked(true);
            if(mServer.mClient.isConnected()){
                mServer.sendMessage("Sali del trabajo");
            }else{
                Toast.makeText(MainActivity.this, "Hay problemas con el Raspberry o desconectado", Toast.LENGTH_SHORT).show();
            }
        }else if(text.toString().contains("Dormir")){
            dormir.setChecked(true);
            if(mServer.mClient.isConnected()){
                mServer.sendMessage("Me voy a dormir");
            }else{
                Toast.makeText(MainActivity.this, "Hay problemas con el Raspberry o desconectado", Toast.LENGTH_SHORT).show();
            }
        }
    }


    /******************************************************************************
     **********************************Write to NFC Tag****************************
     ******************************************************************************/
    private void write(String text, Tag tag) throws IOException, FormatException {
        NdefRecord[] records = { createRecord(text) };
        NdefMessage message = new NdefMessage(records);
        // Get an instance of Ndef for the tag.
        Ndef ndef = Ndef.get(tag);
        // Enable I/O
        ndef.connect();
        // Write the message
        ndef.writeNdefMessage(message);
        // Close the connection
        ndef.close();
    }
    private NdefRecord createRecord(String text) throws UnsupportedEncodingException {
        String lang       = "en";
        byte[] textBytes  = text.getBytes();
        byte[] langBytes  = lang.getBytes("US-ASCII");
        int    langLength = langBytes.length;
        int    textLength = textBytes.length;
        byte[] payload    = new byte[1 + langLength + textLength];

        // set status byte (see NDEF spec for actual bits)
        payload[0] = (byte) langLength;

        // copy langbytes and textbytes into payload
        System.arraycopy(langBytes, 0, payload, 1,              langLength);
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);

        NdefRecord recordNFC = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,  NdefRecord.RTD_TEXT,  new byte[0], payload);

        return recordNFC;
    }



    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        readFromIntent(intent);
        if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())){
            myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        }
    }

    @Override
    public void onPause(){
        super.onPause();
//        SharedPreferences.Editor editor = mySharedPreferences.edit();
//        editor.putBoolean("key1", llamada.isChecked());
//        editor.putBoolean("key2", carro.isChecked());
//        editor.putBoolean("key3", trabajo.isChecked());
//        editor.putBoolean("key4", dormir.isChecked());
//        editor.commit(); // persist the values
        WriteModeOff(); //NFC
    }

    @Override
    public void onResume(){
        super.onResume();
//        mySharedPreferences = getSharedPreferences(MY_PREFS, prefMode);
//        llamada.setChecked(mySharedPreferences.getBoolean("key1",false));
//        carro.setChecked(mySharedPreferences.getBoolean("key2",false));
//        trabajo.setChecked(mySharedPreferences.getBoolean("key3",false));
//        dormir.setChecked(mySharedPreferences.getBoolean("key4",false));
        WriteModeOn(); //NFC
    }


    /******************************************************************************
     **********************************Enable Write********************************
     ******************************************************************************/
    private void WriteModeOn(){
        writeMode = true;
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, writeTagFilters, null);
    }
    /******************************************************************************
     **********************************Disable Write*******************************
     ******************************************************************************/
    private void WriteModeOff(){
        writeMode = false;
        nfcAdapter.disableForegroundDispatch(this);
    }


    //Lanzar el servicio de MQTT
    public void startClicked(View view) {
        startService(new Intent(getBaseContext(), MQTTService.class));
    }


    //On click listener for button1
    final View.OnClickListener mButton1_OnClickListener = new View.OnClickListener() {
        public void onClick(final View v) {

            if(mServer.mClient.isConnected()){
                mServer.sendMessage("Probando");
                carro.setChecked(true);
            }else{
                System.out.println("No funciona");
            }
        }
    };


    //Binder stuff for Service
    @Override
    protected void onStart() {
        super.onStart();
        Intent mIntent = new Intent(this, MQTTService.class);
        bindService(mIntent, mConnection, BIND_AUTO_CREATE);
    };

    ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceDisconnected(ComponentName name) {
            Toast.makeText(MainActivity.this, "Service is disconnected", Toast.LENGTH_SHORT).show();
            mBounded = false;
            mServer = null;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            Toast.makeText(MainActivity.this, "Service is connected", Toast.LENGTH_SHORT).show();
            mBounded = true;
            LocalBinder mLocalBinder = (LocalBinder)service;
            mServer = mLocalBinder.getServerInstance();
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        if(mBounded) {
            unbindService(mConnection);
            mBounded = false;
        }
    };

}
