package cp.obd.evdatautility;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;

public class EVDataStream {
	protected File dataFile;
	protected String dashUnits;
	protected BufferedWriter fos;
	protected String toPost;
	protected TextView textView;
	
	public void setTextView(TextView tv) {
		textView = tv;
	}
	
	public TextView getTextView() {
		return textView;
	}
	
	public String getPost() {
		return toPost;
	}
	
	public boolean init(String filePrefix) {
		try {
			File root = Environment.getExternalStorageDirectory();
			dataFile = new File(root, filePrefix+".csv");
			fos = new BufferedWriter(new FileWriter(dataFile));
			fos.append("Value: "+filePrefix);
			fos.append(',');
			fos.append("Time Stamp");
			fos.append("\r\n");
		} catch (Exception e) {
			Log.e("FILE", "Cannot create temp file for " + filePrefix);
			return false;
		}
		
		return true;
	}
	
	public boolean addToFile(Long val, String timeStamp) {
		Log.e("STREAM", "VAL:"+val+", TIMESTAMP:"+timeStamp+", SELF:"+this.toString());
		try {
			if (fos == null) {
				Log.e("NULLCHECK", "fos is null");
			}
			fos.append(val.toString());
			fos.append(',');
			fos.append(timeStamp);
			fos.append("\r\n");
		} catch (IOException e) {
			Log.e("FILE", "Cannot append val " + val);
			return false;
		}
		
		return true;
	}
	
	public File endStream() throws IOException {
		try {
			fos.flush();
			fos.close();
			return dataFile;
		} catch (Exception e) {
			return null;
		}
	}
	
	public void cleanUp() {
		dataFile.delete();
	}
}
