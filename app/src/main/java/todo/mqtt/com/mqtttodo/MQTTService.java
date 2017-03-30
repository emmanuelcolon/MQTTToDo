package todo.mqtt.com.mqtttodo;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Random;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.internal.MemoryPersistence;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPublish;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Vibrator;
import android.widget.TextView;
import android.widget.Toast;

public class MQTTService extends Service implements MqttCallback {

	//private static final String SERVER = "tcp://192.168.50.155:1883";
    private static final String SERVER = "tcp://raspberrypi.mshome.net:1883";
	private static final String TOPIC = "hello/world";

	private MqttClient mClient;

    private final Handler handler = new Handler(MainActivity.context.getMainLooper());

	public MQTTService() {
		try {
			final String clientID = String.valueOf(new Random(System
					.currentTimeMillis()).nextInt());
			mClient = new MqttClient(SERVER, clientID, new MemoryPersistence());
			mClient.setCallback(this);

		} catch (MqttException e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Toast.makeText(getApplicationContext(),
				"Service start command received!", Toast.LENGTH_SHORT).show();

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					if (!mClient.isConnected())
						mClient.connect();

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

}
