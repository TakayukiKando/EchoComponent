package org.xgmtk.xmpp.sample.server;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import org.xmpp.component.AbstractComponent;
import org.xmpp.component.ComponentException;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Presence;
import org.xmpp.packet.Presence.Show;

/**
 * This server component serves echo and time signal facilities.
 * @author kando
 *
 */
public class EchoComponent extends AbstractComponent {
	/**
	 * The dscription of the component.
	 */
	private static final String COMPONENT_DESCRIPTION = "This service echoes user's messages back to sender and sends time signal messages.";
	
	/**
	 * Timer.
	 */
	private static final Timer TIMER = new Timer();
	
	/**
	 * Definition of 1 second in milliseconds.
	 */
	private static final long SECOND = 1000;
	
	/**
	 * Definition of 1 minute in seconds.
	 */
	private static final long MINUTE = 60 * SECOND;
	
	/**
	 * A record of a user client information to send messages.
	 * @author kando
	 *
	 */
	protected class AddressInfo{
		/**
		 * The reciever's Jabber ID
		 */
		public final JID clientJID;
		
		/**
		 * The sender's Jabber ID
		 */
		public final JID receptionistJID;
		
		/**
		 * Thread ID
		 */
		public final String threadID;
		
		/**
		 * Initializer.
		 * @param clientJID The reciever's Jabber ID
		 * @param receptionistJID The sender's Jabber ID
		 * @param threadID Thread ID
		 */
		public AddressInfo(JID clientJID, JID receptionistJID, String threadID){
			this.clientJID = clientJID;
			this.receptionistJID = receptionistJID;
			this.threadID = threadID;
		}
	}
	
	private String name;
	private Map<String, AddressInfo> addresses;
	/**
	 * Initializer.
	 * The arguments of this can get from
	 * {@link org.xgmtk.xmpp.sample.server.ComponentContainer#getMaxThreadpoolSizeProperty(String)}
	 * and {@link org.xgmtk.xmpp.sample.server.ComponentContainer#getMaxQueueSizeProperty(String)}.
	 * @param compName The name of component.
	 * @param interval Time signal interval in minutes.
	 * @param maxThreadpoolSize The maximum value of thread pool size.
	 * @param maxQueueSize The maximum value of queue size.
	 */
	public EchoComponent(String compName, long interval, int maxThreadpoolSize, int maxQueueSize) {
		super(maxThreadpoolSize, maxQueueSize, false);
		this.name = compName;
		this.addresses = new ConcurrentHashMap<>();
		this.setTimeSignal(interval*MINUTE);
	}

	/* (”ñ Javadoc)
	 * @see org.xmpp.component.AbstractComponent#getName()
	 */
	@Override
	public String getName() {
		return this.name;
	}

	/* (”ñ Javadoc)
	 * @see org.xmpp.component.AbstractComponent#getDescription()
	 */
	@Override
	public String getDescription() {
		return COMPONENT_DESCRIPTION;
	}

	/**
	 * The timer set to the specified interval.
	 * The timer runs a thread periodically.
	 * The thread forms a message string and passes it to the method {@link #sendMessageToAll(String)}.
	 * @param interval
	 */
	private void setTimeSignal(long interval) {
		Calendar local_calendar =  Calendar.getInstance();
		local_calendar.set(Calendar.MINUTE, 0);
		local_calendar.set(Calendar.SECOND, 0);
		local_calendar.set(Calendar.MILLISECOND, 0);
		Date startTime = local_calendar.getTime();
		TIMER.scheduleAtFixedRate(new TimerTask(){
			@Override
			public void run() {
				DateFormat dateTimeFormat = DateFormat.getDateTimeInstance();
				String message = "* Time signal: "+dateTimeFormat.format(Calendar.getInstance().getTime());
				sendMessageToAll(message);
			}
		}, startTime, interval);
		DateFormat dateTimeFormat = DateFormat.getDateTimeInstance();
		log.info("Start time signale(interval: "+((double)interval / MINUTE)+" minutes) at : "+dateTimeFormat.format(startTime));
	}

	/**
	 * Create a Message packet and send it.
	 * @param to The receiver's Jabber ID.
	 * @param from The sender's Jabber ID.
	 * @param subject The subject of the message.
	 * @param messageText The message text.
	 * @param threadID  The thread ID.
	 * @return true, if the method has sent message successfully.
	 */
	private boolean sendMessage(JID to, JID from, String subject,
			String messageText, String threadID) {
		Message msg = new Message();
				msg.setFrom(from);
				msg.setTo(to);
				msg.setBody(messageText);
				msg.setSubject(subject);
				msg.setThread(threadID);
				msg.setType(Message.Type.chat);
		try {
			compMan.sendPacket(this, msg);
		} catch (ComponentException e) {
			return false;
		}
		return true;
	}

	/**
	 * Add address information of client, which would receive messages, to address list.
	 * @param address The address information of client receiving time signal.
	 */
	private void addAddress(AddressInfo address){
		String clientJID = address.clientJID.toString();
		if(!this.addresses.containsKey(clientJID)){
			this.addresses.put(clientJID, address);
		}
	}

	/**
	 * Send a message to the all clients in the address list.
	 * @param message
	 */
	private void sendMessageToAll(String message) {
		List<AddressInfo> failedAddresses = new ArrayList<>();
		for(AddressInfo address : this.addresses.values()){
			if(!this.sendMessage(address.clientJID, address.receptionistJID, "Time signal", message, address.threadID)){
				log.warn("\t Failed to send a message to: \""+address.clientJID+"\"");
				failedAddresses.add(address);
			}
		}
		for(AddressInfo address : failedAddresses){
			this.addresses.remove(address.clientJID.toString());
		}
	}

	/* (”ñ Javadoc)
	 * @see org.xmpp.component.AbstractComponent#handlePresence(org.xmpp.packet.Presence)
	 */
	@Override
	protected void handlePresence(Presence presence) {
		JID sender = presence.getFrom();
		JID receiver = presence.getTo();
		String id = presence.getID();
		Show show = presence.getShow();
		String status = presence.getStatus();
		int priority = presence.getPriority();
		
		log.info("\t Echo component is recieved a presence info from: \""+sender +"\", from:\""+receiver
				+"\", id:\""+id+"\", show mode: \""+show
				+"\", priority: \""+priority+"\", status: \""+status+"\"");
		
		super.handlePresence(presence);
	}

	/* (”ñ Javadoc)
	 * @see org.xmpp.component.AbstractComponent#handleMessage(org.xmpp.packet.Message)
	 */
	@Override
	protected void handleMessage(Message message) {
		JID sender = message.getFrom();
		JID recv = message.getTo();
		String messageText = message.getBody();
		String subject = message.getSubject();
		String threadID = message.getThread();
		
		log.info("\t Echo component is recieved a message from: \""+sender+"\", to: \""+recv+"\", message text:\""+messageText+"\"");
		if(sendMessage(sender, recv, subject, messageText, threadID)){
			addAddress(new AddressInfo(sender, recv, threadID));
		}else{
			log.warn("\t Failed to send a message to: \""+sender+"\"");
		}
		
		super.handleMessage(message);
	}
}
