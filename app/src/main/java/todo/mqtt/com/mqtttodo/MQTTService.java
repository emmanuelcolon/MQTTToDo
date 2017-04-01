package todo.mqtt.com.mqtttodo;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Random;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
//import org.eclipse.paho.client.mqttv3.internal.MemoryPersistence;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Vibrator;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import android.os.Binder;

public class MQTTService extends Service implements MqttCallback {

	//private static final String SERVER = "tcp://192.168.50.155:1883";
    private static final String SERVER = "tcp://raspberrypi.mshome.net:1883";
	private static final String TOPIC = "hello/world";
    MqttMessage msg1 = new MqttMessage("Hello, I am Android Mqtt Client.".getBytes());
    IBinder mBinder = new LocalBinder();

	public MqttClient mClient;

    private final Handler handler = new Handler(MainActivity.context.getMainLooper());

	public MQTTService() {
		try {
			final String clientID = String.valueOf(new Random(System
					.currentTimeMillis()).nextInt());
			mClient = new MqttClient(SERVER, clientID, new MemoryPersistence());
			mClient.setCallback(this);

//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    MainActivity.button1.setOnClickListener(mButton1_OnClickListener);
//                }
//            });

		} catch (MqttException e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
	}

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

//	@Override
//	public IBinder onBind(Intent intent) {
//		return null;
//	}

    public class LocalBinder extends Binder {
        public MQTTService getServerInstance() {
            return MQTTService.this;
        }
    }

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Toast.makeText(getApplicationContext(),
				"Service start command received!", Toast.LENGTH_SHORT).show();

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					if (!mClient.isConnected()){
                        mClient.connect();
                    }

					mClient.subscribe(TOPIC);

					System.out.println("Client connected and subscribed.");

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MainActivity.conn.setText("Client connected and subscribed.");
                        }
                    });

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
        
		return START_STICKY;
	}

    private void runOnUiThread(Runnable runnable) {
        handler.post(runnable);
    }

    @Override
	public void connectionLost(Throwable throwable) {
        System.err.println("Connection lost!");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MainActivity.conn.setText("Connection lost!");
            }
        });
	}

    @Override
    public void messageArrived(final String topic, final MqttMessage message) throws Exception {

        System.out.println("Message Arrived: " + topic + " - " + message);


        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(),
                        "Message Arrived: " + topic + " - " + message, Toast.LENGTH_SHORT).show();

                if(message.toString().contains("Llamada Despertadora")){
                    MainActivity.llamada.setChecked(true);
                    SharedPreferences.Editor editor = MainActivity.mySharedPreferences.edit();
                    editor.putBoolean("key1", MainActivity.llamada.isChecked());
                    editor.commit();
                }
            }
        });

        final Vibrator vibrator = (Vibrator) getApplicationContext()
                .getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(500);

    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        System.out.println("Delivery Complete!");
    }

    public void sendMessage(String msg){
        MqttMessage mensaje = new MqttMessage(msg.getBytes());
        try {
            mClient.publish("hello/world1", mensaje);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

//    //On click listener for button1
//    final View.OnClickListener mButton1_OnClickListener = new View.OnClickListener() {
//        public void onClick(final View v) {
//            try {
//                mClient.publish("hello/world1", msg1);
//            } catch (MqttException e) {
//                e.printStackTrace();
//            }
//        }
//    };


    /*paho MQTT v1.0.1
    @Override
	public void deliveryComplete(MqttDeliveryToken topic) {
		System.out.println("Delivery Complete: " + topic);
	}

	@Override
	public void messageArrived(final MqttTopic topic, final MqttMessage msg)
			throws Exception {
		System.out.println("Message Arrived: " + topic + " - " + msg);


        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(),
                        "Message Arrived: " + topic + " - " + msg, Toast.LENGTH_SHORT).show();
            }
        });

		final Vibrator vibrator = (Vibrator) getApplicationContext()
				.getSystemService(Context.VIBRATOR_SERVICE);
		vibrator.vibrate(500);
	}

	*/

}
