package cp.obd.evdatautility;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import cp.obd.evdatautility.MyLocation;
import cp.obd.evdatautility.MyLocation.LocationResult;
import android.location.Location;
import android.os.Environment;
import android.text.format.Time;
import android.util.Log;

public class GPSDataStream extends EVDataStream {
	private Time currentTime;
	protected MyLocation myLocation;
	
	@Override
	public boolean init(String filePrefix) {
		try {
			File root = Environment.getExternalStorageDirectory();
			dataFile = new File(root, filePrefix+".csv");
			fos = new BufferedWriter(new FileWriter(dataFile));
			fos.append("Latitude");
			fos.append(',');
			fos.append("Longitude");
			fos.append(',');
			fos.append("Altitude");
			fos.append(',');
			fos.append("Time Stamp");
			fos.append("\r\n");
		} catch (Exception e) {
			Log.e("FILE", "Cannot create temp file for " + filePrefix);
			return false;
		}
		
		return true;
	}
	
	public void init(BlueToothActivity activity) {
		LocationResult locationResult = new LocationResult(){
		    @Override
		    public void gotLocation(Location location){
		    	if (location != null) {
		    		try {
		    			addToFile(location);
		    		}
		    		catch (Exception e) {
		    			Log.e("GPS", "CANNOT WRITE LOCATION");
		    		}
				}
		    }
		};
		myLocation = new MyLocation();
		myLocation.getLocation(activity, locationResult);
		currentTime = new Time();
		init("Gps");
	}

	public void addToFile(Location loc) throws IOException {
		fos.append(String.valueOf(loc.getLatitude()));
		fos.append(',');
		fos.append(String.valueOf(loc.getLongitude()));
		fos.append(',');
		fos.append(String.valueOf(loc.getAltitude()));
		fos.append(',');
		currentTime.setToNow();
		fos.append(currentTime.toString().substring(0, 15));
		fos.append("\r\n");
	}
	
	public File endStream() throws IOException {
		myLocation.cancel();
		return super.endStream();
	}
}
