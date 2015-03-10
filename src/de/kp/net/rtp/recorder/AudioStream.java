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

import com.orangelabs.rcs.core.ims.protocol.rtp.media.MediaInput;
import com.orangelabs.rcs.core.ims.protocol.rtp.media.MediaSample;
import com.orangelabs.rcs.core.ims.protocol.rtp.stream.ProcessorInputStream;
import com.orangelabs.rcs.core.ims.protocol.rtp.util.Buffer;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Media capture stream
 * 
 * @author huyq
 */
public class AudioStream implements ProcessorInputStream {
	/**
     * Media player
     */
	private MediaInput player;

    /**
     * Sequence number
     */
    private long seqNo = 0;

    /**
     * Input buffer
     */
	private Buffer buffer = new Buffer();

	/**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
	 * Constructor
	 * 
     * @param player Media player
	 */
    public AudioStream(MediaInput player) {
		this.player = player;
	}
    
    
    /**
	 * Open the input stream
	 * 
     * @throws Exception
	 */	
    public void open() throws Exception {
    	try {
	    	player.open();
			if (logger.isActivated()) {
				logger.debug("Media capture stream openned");
			}
    	} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Media capture stream failed", e);
			}
			throw e;
    	}
	}    	
	
    /**
     * Close the input stream
     */
    public void close() {
		player.close();
		if (logger.isActivated()) {
			logger.debug("Media capture stream closed");
		}
    }
    
    /**
     * Read from the stream
     * 
     * @return Buffer
     * @throws Exception
     */
    public Buffer read() throws Exception {
    	// Read a new sample from the media player
    	MediaSample sample = player.readSample();
    	if (sample == null) {
    		return null;
    	}
    	
    	// Create a buffer
	    buffer.setData(sample.getData());   	
	    buffer.setLength(sample.getLength());
    	buffer.setSequenceNumber(seqNo++);
    	buffer.setFlags(Buffer.FLAG_SYSTEM_TIME | Buffer.FLAG_LIVE_DATA);
    	buffer.setTimeStamp(sample.getTimeStamp());
    	return buffer;  
    }    
}
