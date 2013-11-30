package cp.obd.evdatautility;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.Hashtable;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.*;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.BufferType;

public class BlueToothActivity extends Activity implements OnClickListener {
	private final static Integer VC_ID = Integer.valueOf(475);
	private final static Integer RPM_ID = Integer.valueOf(474);
	private final static Integer SOC_ID = Integer.valueOf(1468);
	private final static Integer GPS_ID = Integer.valueOf(0);
	private final static int REQUEST_ENABLE_BT = 0xB7B7;
	private final static int MESSAGE_SIZE = 11;
	private final static int SYNC_BYTE = 83;
	public static enum State { ERROR, INIT, CONNECTED, COLLECTING, COMPILING };  
	
	//Entry point for all Bluetooth interaction.
	private static BluetoothAdapter btAdapter;
	private static BluetoothDevice btDevice;
	private static BluetoothSocket btSocket;
	private static InputStream btInput;
	private static Intent emailIntent;
	private ConnectThread collectTask;
	private ProgressBar collectingBar;
	private ImageButton collectButton;
	private State currentState;
	public Time currentTime;
	private UUID randUUID;
	private static Hashtable<Integer, EVDataStream> fileWriters;
	
	public State getState() {
		return currentState;
	}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.activity_blue_tooth);
    	currentState = State.INIT;
    	currentTime = new Time();
    	btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter != null) {
        	if (!btAdapter.isEnabled()) {
        		askUserBluetoothEnable();
        	}
        }
        else {
        	showDialog(getString(R.string.CannotInitialize));
        }
    	Log.e("ERROR", "IN showBluetoothLayout");
    	randUUID = UUID.fromString("a2a1191b-25a2-4af9-90f3-3b85595981d1");
    	collectButton = (ImageButton) findViewById(R.id.CollectButton);
    	collectButton.setOnClickListener(this);
    	collectingBar = (ProgressBar) findViewById(R.id.ReceivingBar);
    }
    
    protected void onStart() {
    	super.onStart();
    	if (btSocket == null || !btSocket.isConnected()) {
    		int numConnectTries = 0;
    		do {
    			connect();
    			numConnectTries++;
    			Log.e("CONNECT", numConnectTries+ ": trying to connect..");
    		} while (btSocket == null && numConnectTries < 10);
    	}
    	
    	if (btSocket != null && btSocket.isConnected()) {
    		currentState = State.CONNECTED;
        	collectButton.setOnClickListener(this);
    		collectButton.setImageResource(R.drawable.collect);
    	}
    	else {
    		currentState = State.ERROR;
    		showDialog(getString(R.string.cannotConnect));
    	}
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    }
    
    @Override
    public void onStop() {
    	super.onStop();
    	currentState = State.ERROR;
    	stopCollection();
    	try {
			Log.e("Socket", "Attempting to close socket in onStart()");
			if (btSocket != null) {
				btSocket.close();
			}
		} catch (IOException e) {
			Log.e("Socket", "Cannot close socket in onStart()");
		}
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    }
    
    public void askUserBluetoothEnable() {
    	Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.blue_tooth, menu);
        return true;
    }
    
    protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
        if (requestCode == REQUEST_ENABLE_BT && resultCode != RESULT_OK) {
            showDialog(getString(R.string.notEnabled));
        }
    }
    
    public void connect() {
    	BluetoothSocket tmp = null;
    	Set<BluetoothDevice> mDeviceSet;
        
    	// Use a temporary object that is later assigned to mSocket,
        // because mSocket is final
        mDeviceSet = btAdapter.getBondedDevices();
        btSocket = null;
        
        if (mDeviceSet.size() > 0) {
        	for(BluetoothDevice device : mDeviceSet) {
        		try {
        			Method m = device.getClass().getMethod("createRfcommSocket", new Class[] { int.class });
        			tmp = (BluetoothSocket)m.invoke(device, 1);	
        	        btSocket = tmp;
        	        btDevice = device;
        	        break;
    	        } catch (NoSuchMethodException e) {
    	        	showDialog("Invalid Bluetooth Device"); 
    	        } catch (IllegalArgumentException e) {
    	        	showDialog("Invalid Bluetooth Device"); 
				} catch (IllegalAccessException e) {
    	        	showDialog("Invalid Bluetooth Device"); 
				} catch (InvocationTargetException e) {
    	        	showDialog("Invalid Bluetooth Device"); 
				}
        	}
        }
        
        if (btSocket != null) {
        	// Get a BluetoothSocket to connect with the given BluetoothDevice
	        btAdapter.cancelDiscovery();
	        
	        try {
	            // Connect the device through the socket. This will block
	            // until it succeeds or throws an exception
	            btSocket.connect();
	        } catch (IOException connectException) {
	        	Log.e("CONNECT", "Error on connect()");
	            // Unable to connect; close the socket and get out
	            try {
	        		Log.e("SOCKET", "Closing socket in onStop");
	                btSocket.close();
	            } catch (IOException closeException) {
		        	Log.e("CONNECT", "Error on close() after connect()");
		        }
	            return;
	        }
        }
    }
    
    @Override
	public void onClick(View view) {
		switch (view.getId()) {
		case (R.id.CollectButton):
			if (currentState == State.COLLECTING) {
				stopCollection();
			}
			else if (currentState == State.CONNECTED){
				this.beginCollection();
			}
			else {
				showDialog("Cannot Collect data because device is not connected.");
			}
		default:
		}
    }
    
    public void beginCollection() {
    	try {
			btInput = btSocket.getInputStream();
		} catch (IOException e) {
			currentState = State.ERROR;
			showDialog("Cannot get input steam");
			return;
		}
    	
    	Log.e("COLLECTION", "About to begin collecting data");
    	collectTask = new ConnectThread(this);
    	collectTask.execute();
    	currentState = State.COLLECTING;
    	collectButton.setImageResource(R.drawable.stop);
    	collectingBar.setVisibility(View.VISIBLE);
    }
    
    public void endCollection() {
    	currentState = State.CONNECTED;
    	collectingBar.setVisibility(View.INVISIBLE);
    	collectButton.setImageResource(R.drawable.collect);
    	for (Integer msgId : fileWriters.keySet()) {
    		//fileWriters.get(msgId).cleanUp();
    	}
    	
    }
    
    private void showDialog(String msg) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Error:");
        dialog.setMessage(msg);
        dialog.setPositiveButton("OK", new 
            DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                	try {
                		AlertDialog calledDialog = (AlertDialog)dialog;
                		calledDialog.dismiss();
                		checkError();
                	}
                	catch (Exception e) {
                		Log.e("ERROR", "Couldn't cast/close Dialog");
                		Log.e("ERROR", e.getMessage());
                	}
                }
            }
        );

        dialog.show();
    }
    
    private synchronized void stopCollection() {
    	if (collectTask != null && collectTask.getStatus() == AsyncTask.Status.RUNNING) {
    		collectTask.cancel(true);
    		try {
    			synchronized(collectTask) {
    				collectTask.wait();
    			}
			} catch (InterruptedException e) {
				Log.e("STOP", "Cannot wait() on collectTask");
			}
    	}
    }
    
    public void checkError() {
    	if (currentState == State.ERROR) {
    		finish();
    	}
    }
    
    private class ConnectThread extends AsyncTask<Void, Integer, Long> {
    	private BlueToothActivity uiActivity;
    	
    	public ConnectThread(BlueToothActivity btActivity) {
    		uiActivity = btActivity;
    	}
    	
    	@Override
    	protected void onPreExecute() {
			fileWriters = new Hashtable<Integer, EVDataStream>();
			VCDataStream vcStream = new VCDataStream();
			vcStream.init("VoltsAmps");
			vcStream.setTextView((TextView)findViewById(R.id.voltageView));
			RPMDataStream rpmStream = new RPMDataStream();
			rpmStream.init("Rpm");
			rpmStream.setTextView((TextView)findViewById(R.id.rpmView));
			SOCDataStream socStream = new SOCDataStream();
			socStream.init("Soc");
			socStream.setTextView((TextView)findViewById(R.id.socView));
			GPSDataStream gpsStream = new GPSDataStream();
			gpsStream.init(uiActivity);
			fileWriters.put(VC_ID, vcStream);
			fileWriters.put(RPM_ID, rpmStream);
			fileWriters.put(SOC_ID, socStream);
			fileWriters.put(GPS_ID, gpsStream);
		}
    	
		protected Long doInBackground(Void...v) {
			byte[] buffer = new byte[MESSAGE_SIZE-1];
			int bytes = 0;
			long numReceived = 0, val;
			
			try {
				do {
					while (btInput.available() < MESSAGE_SIZE);
					bytes = btInput.read(buffer,0,1);
					
					if (bytes < 0)
						return Long.valueOf(-1);
					else if (bytes == 0)
						return Long.valueOf(0);
					
					else if (buffer[0] == SYNC_BYTE) {
						btInput.read(buffer);
						bytes = (buffer[1] & 0xF0) >> 4;
						val = generateLong(buffer, 2, bytes);
						processMsg(buffer, val);
						numReceived++;
					}
				}
				while (!isCancelled() && bytes > 0);
				
			} catch (IOException e) {
				Log.e("FILE", "Bluetoothstream reader error");
				Log.e("FILE", e.getMessage());
			}
			
			synchronized(collectTask) {
				collectTask.notify();
			}
			return Long.valueOf(numReceived);
		}
		
		private void processMsg(byte[] msg, long val) {
			int tempInt = (int) (msg[1] & 0xF) + 1;
			Integer msgId = Integer.valueOf((tempInt << 8) + msg[0]);
			Log.e("INT", msgId.toString());
			
			if (!fileWriters.containsKey(msgId)) {
				EVDataStream newDataStream = new EVDataStream();
				if (newDataStream.init(msgId.toString())) {
					fileWriters.put(msgId, newDataStream);
				}
				else {
					return;
				}
			}
			
			currentTime.setToNow();
			fileWriters.get(msgId).addToFile(Long.valueOf(val), 
					currentTime.toString().substring(0, 15));
			publishProgress(msgId);
		}
		
		private long generateLong(byte[] bytes, int offset, int numBytes) {
			long val = 0;
			if (numBytes > 8) {
				numBytes = 8;
			}
			
			for(int i = offset; i < offset+numBytes; i++) {
				Log.e("VAL","i:"+i+". val:"+bytes[i]);
				val += bytes[i] << ((i-offset)*8);
			}
			
			return Long.valueOf(val);
		}
		
		@Override
		protected void onPostExecute(Long num) {
			if (num.longValue() > 0) {
				createEmail();
				uiActivity.endCollection();
			}
			else {
				uiActivity.showDialog(getString(R.string.cannotConnect));
				uiActivity.currentState = State.INIT;
			}
		}
		
		@Override
		protected void onCancelled() {
			if (!fileWriters.isEmpty() && uiActivity.getState() == State.COLLECTING) { 
				createEmail();
			}
			uiActivity.endCollection();
		}
		
		private void createEmail() {
			uiActivity.currentState = State.COMPILING;
			File tempFile;
			ArrayList<Uri> uris = new ArrayList<Uri>();
			
			String address = "philip.b.tyler@gmail.com";
			String subject = "DATA TEST";
			String emailtext = "Please check the attached csv file";

			emailIntent = new Intent(android.content.Intent.ACTION_SEND_MULTIPLE);
			
			emailIntent.setType("plain/text");
			emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[] { address });
			emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
			emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, emailtext);
			emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

			for (Integer msgId : fileWriters.keySet()) {
				try {
					Log.e("fileWriter", "attaching file "+msgId);
					tempFile = fileWriters.get(msgId).endStream();
					if (tempFile != null) {
						tempFile.setReadable(true, false);
						Log.e("FILE", "File size is " + tempFile.length());
						uris.add(Uri.fromFile(tempFile));
					}
				} catch (IOException e) {
					Log.e("FILE", "Cannot close stream " + msgId.toString());
				}
			}

			emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
			uiActivity.startActivity(emailIntent);
		}
		
		protected void onProgressUpdate(Integer...num) {
			EVDataStream toUpdate = fileWriters.get(num[0]);
			TextView toSet = toUpdate.getTextView();
			if (toSet != null) {
				toSet.setText(toUpdate.getPost());
			}
		}
    }  
}
