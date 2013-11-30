package cp.obd.evdatautility;

import android.util.Log;

public class SOCDataStream extends EVDataStream {

	@Override
	public boolean addToFile(Long val, String timeStamp) {
		long value = val.longValue();
		long tempValue = (value << 2) & 0x3FC;
		tempValue = tempValue | ((value >> 14) & 0x3);
		val = Long.valueOf(tempValue);
		Log.e("STREAM", "In RPM data Stream add to file");
		toPost = "State of Charge: " + val.toString();
		return super.addToFile(val, timeStamp);	
	}
}
