/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package de.kp.net.rtp.recorder;


import com.orangelabs.rcs.core.ims.protocol.rtp.media.MediaException;
import com.orangelabs.rcs.core.ims.protocol.rtp.media.MediaInput;
import com.orangelabs.rcs.core.ims.protocol.rtp.media.MediaSample;
import com.orangelabs.rcs.utils.FifoBuffer;
import com.orangelabs.rcs.utils.logger.Logger;

import android.R.integer;
import android.content.Context;
import android.content.res.AssetManager;
import android.hardware.Camera;
import android.os.SystemClock;
import android.text.InputFilter.LengthFilter;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Live RTP audio player. Supports only H.263 and H264 QCIF formats.
 */
public class RtspAudioRecorder {


    /**
     * Local RTP port
     */
    private int localRtpPort;

    /**
     * RTP sender session
     */
    private AudioRtpSender rtpMediaSender = null;

    /**
     * RTP media input
     */
    private MediaRtpInput rtpInput = null;

    /**
     * Last audio data
     */
    private byte[] audioBuffer = null;
    
    private static final int AUDIO_BUFFER_LEN = 1024 * 10;

    /**
     * Is player opened
     */
    private boolean opened = false;

    /**
     * Is player started
     */
    private boolean started = false;

    /**
     * Audio start time
     */
    private long audioStartTime = 0L;

    /*
     * AudioFile in assets folds
     */
    private String fileName = "demo.m4a";
    private InputStream input;
    private BufferedInputStream bis;

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

	private String TAG = "RtspAudioRecorder";

    /**
     * Constructor
     */
    public RtspAudioRecorder(Context context) {
    	if(audioBuffer == null)
    		audioBuffer = new byte[AUDIO_BUFFER_LEN];
     AssetManager assetManager = context.getAssets();
 	 try {
		input = assetManager.open(fileName);
		bis = new BufferedInputStream(input);
	} catch (IOException e) {
		// TODO Auto-generated catch block
		Log.e(TAG, "open audio file failed");
		e.printStackTrace();
	}
    }

    /**
     * Returns the local RTP port
     *
     * @return Port
     */
    public int getLocalRtpPort() {
        return localRtpPort;
    }

    /**
     * Return the audio start time
     *
     * @return Milliseconds
     */
    public long getVideoStartTime() {
        return audioStartTime;
    }

    /**
     * Is player opened
     *
     * @return Boolean
     */
    public boolean isOpened() {
        return opened;
    }

    /**
     * Is player started
     *
     * @return Boolean
     */
    public boolean isStarted() {
        return started;
    }

    
    public void open() {

    	if (opened) {
            // Already opened
            return;
        }

        // Init the RTP layer
        try {

        	rtpInput = new MediaRtpInput();
            rtpInput.open();
            
        	rtpMediaSender = new AudioRtpSender();            
            rtpMediaSender.prepareSession(rtpInput);
        
        } catch (Exception e) {
        	
            if (logger.isActivated()) {
                logger.debug("Player error: " + e.getMessage());
            }
        	
            return;
        }

        // audioInput is opened
        opened = true;

    }

    /**
     * Close the player
     */
    public void close() {
        if (!opened) {
            // Already closed
            return;
        }
        // Close the RTP layer
        rtpInput.close();
        rtpMediaSender.stopSession();

        // Player is closed
        opened = false;

    }

    /**
     * Start the player
     */
    public synchronized void start(){
		Log.e(TAG , "start");
   	
        if ((opened == false) || (started == true)) {
            return;
        }

        started = true;

        // Start RTP layer
        rtpMediaSender.startSession();
        
        // Start capture audio data,read into rtpInput
        captureThread.start();

        // Player is started
        audioStartTime = SystemClock.uptimeMillis();

    }

    /**
     * Stop the player
     */
    public void stop() {
        
    	if ((opened == false) || (started == false)) { 
            return;
        }

        // Stop capture
        try {
            captureThread.interrupt();

        } catch (Exception e) {
        }

        // Player is stopped
        audioStartTime = 0L;
        started = false;

    }

    /**
     * Video capture thread
     */
    private Thread captureThread = new Thread() {
        /**
         * Timestamp
         */
        private long timeStamp = 0;

        /**
         * Processing
         */
        public void run() {
//            if (rtpInput == null) {
//                return;
//            }
            int timestampInc = 0;
            while (started) {
                // Set timestamp
                long time = System.currentTimeMillis();
                // Get data to encode
                try {
                    int readLength;
					while ((readLength = input.read(audioBuffer)) != -1) {
						rtpInput.addAudioData(audioBuffer, readLength,timeStamp += timestampInc);
					}
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}  

            }
        }
    };

    /**
     * Media RTP input
     */
    private static class MediaRtpInput implements MediaInput {
        /**
         * Received frames
         */
        private FifoBuffer fifo = null;

        /**
         * Constructor
         */
        public MediaRtpInput() {
        }

        /**
         * Add a new audio data
         *
         * @param data Data
         * @param timestamp Timestamp
         */
        public void addAudioData(byte[] data, int offset,long timestamp) {
        	byte [] usefulData = Arrays.copyOf(data, offset);    	
            if (fifo != null) {
                fifo.addObject(new MediaSample(usefulData, timestamp));
            }
        }

        /**
         * Open the player
         */
        public void open() {
            fifo = new FifoBuffer();
        }

        /**
         * Close the player
         */
        public void close() {
            if (fifo != null) {
                fifo.close();
                fifo = null;
            }
        }

        /**
         * Read a media sample (blocking method)
         *
         * @return Media sample
         * @throws MediaException
         */
        public MediaSample readSample() throws MediaException {
            try {
                if (fifo != null) {
                    return (MediaSample)fifo.getObject();
                } else {
                    throw new MediaException("Media input not opened");
                }
            } catch (Exception e) {
                throw new MediaException("Can't read media sample");
            }
        }
    }
}
