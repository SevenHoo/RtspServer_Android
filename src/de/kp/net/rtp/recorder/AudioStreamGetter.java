package de.kp.net.rtp.recorder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class AudioStreamGetter {

	private static final String ROOTDIR = "F:/work/WebServer/Resource";
	private InputStream inputStream;
	private BufferedInputStream bis;
	private byte[] buffer;
	
	private OutputStream outStream;
	private BufferedOutputStream bos;
	
	File file;

	public AudioStreamGetter(String fileName) {
		file = new File(ROOTDIR + "/" + fileName);
		if(file.exists())
		{
			try {
				inputStream = new FileInputStream(file);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		bis = new BufferedInputStream(inputStream);
		buffer = new byte[1024];
		
	}
	
	public static String readFileFromAssets(Context context, String fileName) throws IOException, IllegalArgumentException {
	    if (null == context || TextUtils.isEmpty( fileName )){
	      throw new IllegalArgumentException( "bad arguments!" );
	    }
	    
	    AssetManager assetManager = context.getAssets();
	    InputStream input = assetManager.open(fileName);
	    ByteArrayOutputStream output = new ByteArrayOutputStream();
	    byte[] buffer = new byte[1024];
	    int length = 0;
	    while ((length = input.read(buffer)) != -1) {
	      output.write(buffer, 0, length);
	    }
	    output.close();
	    input.close();
	    
	    return output.toString();
	}

	public byte[] getAudioStream() {
		try {
			bis.read(buffer);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return buffer;
	}
	
	public void sendAudioStream() throws IOException
	{
		byte [] bytes = new byte[1024];
		int readLength;
		while ((readLength = bis.read(bytes)) != -1) {
			    bos.write(bytes, 0, readLength);
			   }
	}
	
	public void release() throws IOException
	{
		   inputStream.close();
		   bis.close();
		   bos.flush();
		   outStream.close();
		   bos.close();
	}

}
