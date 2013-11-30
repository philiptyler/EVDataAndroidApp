package cp.obd.evdatautility;

import android.util.Log;

public class VCDataStream extends EVDataStream {
	
	@Override
	public boolean addToFile(Long val, String timeStamp) {
		long value = val.longValue();
		
		//calculate voltage
		long tempValue = (value >> 14) & 0x3FC;
		tempValue = tempValue | ((value >> 30) & 0x3);
		val = Long.valueOf(tempValue >> 1);
		Log.e("STREAM", "In VC data Stream add to file");
		toPost = "Battery Voltage: " + val.toString() + " V";
		toPost += "\nBattery Current: ";
		
		super.addToFile(val, timeStamp);
		
		//calculate current
		
		if ((value & 0x80) == 0x80) {
			tempValue = (value ^ 0xFF) & 0x7F;
			tempValue = tempValue << 3;
			tempValue = tempValue | (((value >> 13) & 0x7) ^ 0x7);
			val = Long.valueOf(tempValue >> 1);
			toPost += val.toString() + " A";
		}
		else {
			tempValue = (value & 0xFF) << 3;
			tempValue = tempValue | ((value >> 13) & 0x7);
			val = Long.valueOf(tempValue >> 1);
			toPost += val.toString() + " A";
		}
		return super.addToFile(val, timeStamp);
	}
}
