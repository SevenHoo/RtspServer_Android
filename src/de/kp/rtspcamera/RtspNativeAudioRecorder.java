package de.kp.rtspcamera;

import java.io.IOException;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import de.kp.net.rtp.recorder.RtspAudioRecorder;
import de.kp.net.rtsp.RtspConstants;
import de.kp.net.rtsp.server.RtspServer;

public class RtspNativeAudioRecorder extends Activity {

	private String TAG = "RTSPNativeCamera";

// default RTSP command port is 554
//	private int SERVER_PORT = 8080;
	
	private RtspAudioRecorder audioPlayer;

	private Button butn;

	private RtspServer streamer;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.d(TAG, "onCreate");

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		Window win = getWindow();
		win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		win.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.audiorecorder);
		butn = (Button)findViewById(R.id.startButn);
		butn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				audioPlayer.start();		
			}
		});
		
		audioPlayer = new RtspAudioRecorder(this);
		audioPlayer.open();

	}

	@Override
	public void onResume() {
		Log.d(TAG, "onResume");
		

		// starts the RTSP Server

		try {

			// initialize video encoder to be used
			// for SDP file generation
			RtspConstants.VideoEncoder rtspVideoEncoder = (MediaConstants.H264_CODEC == true) ? RtspConstants.VideoEncoder.H264_ENCODER
					: RtspConstants.VideoEncoder.H263_ENCODER;

			if (streamer == null) {
				streamer = new RtspServer(RtspConstants.SERVER_PORT, rtspVideoEncoder);
				new Thread(streamer).start();
			}

			Log.d(TAG, "RtspServer started");

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		super.onResume();

	}

	@Override
	public void onPause() {

		// stop RTSP server
		if (streamer != null)
			streamer.stop();
		streamer = null;

		super.onPause();
	}
	
	
	

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		audioPlayer.stop();

	}


}
