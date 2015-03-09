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

import com.orangelabs.rcs.core.ims.protocol.rtp.MediaRegistry;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h263.H263Config;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h263.encoder.NativeH263Encoder;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h263.encoder.NativeH263EncoderParams;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.H264Config;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.encoder.NativeH264Encoder;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.H263VideoFormat;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.H264VideoFormat;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.VideoFormat;
import com.orangelabs.rcs.core.ims.protocol.rtp.media.MediaException;
import com.orangelabs.rcs.core.ims.protocol.rtp.media.MediaInput;
import com.orangelabs.rcs.core.ims.protocol.rtp.media.MediaSample;
import com.orangelabs.rcs.service.api.client.media.IMediaEventListener;
import com.orangelabs.rcs.service.api.client.media.IMediaPlayer;
import com.orangelabs.rcs.service.api.client.media.MediaCodec;
import com.orangelabs.rcs.service.api.client.media.video.VideoCodec;
import com.orangelabs.rcs.utils.FifoBuffer;
import com.orangelabs.rcs.utils.logger.Logger;

import android.hardware.Camera;
import android.os.SystemClock;
import android.util.Log;

import java.util.Vector;

/**
 * Live RTP audio player. Supports only H.263 and H264 QCIF formats.
 */
public class RtspAudioRecorder implements Camera.PreviewCallback {

    /**
     * Video format
     */
    private VideoFormat videoFormat;

    /**
     * Local RTP port
     */
    private int localRtpPort;

    /**
     * RTP sender session
     */
    private MediaRtpSender rtpMediaSender = null;

    /**
     * RTP media input
     */
    private MediaRtpInput rtpInput = null;

    /**
     * Last audio data
     */
    private AudioBuffer audioBuffer = null;

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


    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

	private String TAG = "RtspAudioRecorder";

    /**
     * Constructor
     */
    public RtspAudioRecorder() {
    	if(audioBuffer == null)
    		audioBuffer = new AudioBuffer();
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
     * Return the video start time
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
            
        	rtpMediaSender = new MediaRtpSender(videoFormat);            
            rtpMediaSender.prepareSession(rtpInput);
        
        } catch (Exception e) {
        	
            if (logger.isActivated()) {
                logger.debug("Player error: " + e.getMessage());
            }
        	
            return;
        }

        // Player is opened
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
    public synchronized void start() {
		Log.d(TAG , "start");
   	
        if ((opened == false) || (started == true)) {
            return;
        }

        started = true;

        // Start RTP layer
        rtpMediaSender.startSession();
        
        // Start capture
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
     * Preview frame from the camera
     *
     * @param data Frame
     * @param camera Camera
     */
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (frameBuffer != null)
            frameBuffer.setFrame(data);
    }

    /**
     * Audio buffer
     */
    private class AudioBuffer {
        /**
         * YUV frame where frame size is always (videoWidth*videoHeight*3)/2
         */
        private byte frame[] = new byte[1024];

        /**
         * Set the last captured frame
         *
         * @param frame Frame
         */
        public void setFrame(byte[] frame) {
            this.frame = frame;
        }

        /**
         * Return the last captured frame
         *
         * @return Frame
         */
        public byte[] getFrame() {
            return frame;
        }
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
            int timeToSleep = 1000 / selectedVideoCodec.getFramerate();
            int timestampInc = 90000 / selectedVideoCodec.getFramerate();
            byte[] audioData;
            byte[] encodedFrame;
            long encoderTs = 0;
            long oldTs = System.currentTimeMillis();

            while (started) {
                // Set timestamp
                long time = System.currentTimeMillis();
                encoderTs = encoderTs + (time - oldTs);

                // Get data to encode
                audioData = audioBuffer.getFrame();
                
                // Encode frame
                int encodeResult;
                if (selectedVideoCodec.getCodecName().equalsIgnoreCase(H264Config.CODEC_NAME)) {
                    encodedFrame = NativeH264Encoder.EncodeFrame(frameData, encoderTs);
                    encodeResult = NativeH264Encoder.getLastEncodeStatus();
                } else {
                    encodedFrame = NativeH263Encoder.EncodeFrame(frameData, encoderTs);
                    encodeResult = 0;
                }

        		System.out.println("RtpVideoRecorder: captureThread: encodeResult == " + encodeResult);

        		/*
        		 * accept additional status 
        		 * EAVCEI_MORE_NAL     --  there is more NAL to be retrieved
        		 */
                if ((encodeResult == 0 || encodeResult == 6) && encodedFrame.length > 0) {
                	
                	if (encodeResult == 6)
                		System.out.println("RtpVideoRecorder: captureThread: Status == EAVCEI_MORE_NAL");
                	
                    // Send encoded frame                	
                    rtpInput.addFrame(encodedFrame, timeStamp += timestampInc);
                }

                // Sleep between frames if necessary
                long delta = System.currentTimeMillis() - time;
                if (delta < timeToSleep) {
                    try {
                        Thread.sleep((timeToSleep - delta) - (((timeToSleep - delta) * 10) / 100));
                    } catch (InterruptedException e) {
                    }
                }

                // Update old timestamp
                oldTs = time;
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
        public void addAudioData(byte[] data, long timestamp) {
            if (fifo != null) {
                fifo.addObject(new MediaSample(data, timestamp));
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
