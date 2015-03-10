package de.kp.net.rtp.recorder;

import java.io.IOException;
import java.util.Vector;

import de.kp.net.rtp.RtpPacket;
import de.kp.net.rtp.RtpSocket;

/**
 * This class is responsible for sending
 * RTP packets via RTP to a set of registered
 * consumers.
 * 
 * @author Stefan Krusche (krusche@dr-kruscheundpartner.de)
 *
 */
public class AudioStreamSender {

	private static AudioStreamSender instance = new AudioStreamSender();
	
	/*
	 * This vector holds a set of RTP sockets that
	 * are registered consumers of RTP packets.
	 */
	private Vector<RtpSocket> receivers;
	
	private AudioStreamSender() {
		receivers = new Vector<RtpSocket>();
	}
	
	public int getReceiverCount() {
		return receivers.size();
	}
	
	public static AudioStreamSender getInstance() {
		if (instance == null) instance = new AudioStreamSender();
		return instance;
	}
	
	/**
	 * Register RTP packet consumer
	 * 
	 * @param receiver
	 */
	public void addReceiver(RtpSocket receiver) {
		receivers.add(receiver);
	}
	
	/**
	 * De-register RTP packet consumer
	 * @param receiver
	 */
	public void removeReceiver(RtpSocket receiver) {
		receivers.remove(receiver);
	}
	
	/**
	 * Send RTP packet to all registered RTP
	 * packet consumers.
	 * 
	 * @param rtpPacket
	 * @throws IOException
	 */
	public synchronized void send(RtpPacket rtpPacket) throws IOException {

		for (RtpSocket receiver:receivers) {
			receiver.send(rtpPacket);
		}
		
	}

	/**
	 * Send RTP packet to all registered RTP
	 * packet consumers.
	 * 
	 * @param rtpPacket
	 * @throws IOException
	 */
	public synchronized void send(byte[] data) throws IOException {

		for (RtpSocket receiver:receivers) {
			receiver.send(data);
		}
		
	}

	/**
	 * De-register all registered RTP consumers
	 */
	public void clear() {
		receivers.clear();
	}
	
	public void stop() {
		// TODO
	}
}

