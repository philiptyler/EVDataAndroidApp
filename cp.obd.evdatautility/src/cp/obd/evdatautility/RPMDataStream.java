package cp.obd.evdatautility;

import android.util.Log;

public class RPMDataStream extends EVDataStream {
	
	@Override
	public boolean addToFile(Long val, String timeStamp) {
		long value = val.longValue();
		long tempValue = (value >> 40) & 0xFF;  //changed shift count
		tempValue = tempValue | ((value >> 24) & 0x7F00); //changed shift count
		val = Long.valueOf(tempValue >> 1);
		Log.e("STREAM", "In RPM data Stream add to file");
		toPost = "Engine RPM: " + val.toString();
		return super.addToFile(val, timeStamp);	
	}
}
