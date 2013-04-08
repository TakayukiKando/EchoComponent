package org.xgmtk.xmpp.sample.client;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;

public class SimpleClient {
	//private class ChatListener implements 
	private String server;
	private String username;
	private String password;
	private String resource;
	private boolean debug;
	private XMPPConnection connection;
	private String JID;
	private ChatManager chatMan;
	private Map<String, Map<String,Chat>> chats;

	public SimpleClient(File propertyFile, String resource) throws IOException{
		Properties props = new Properties();
		FileReader r = new FileReader(propertyFile);
		props.load(r);
		this.server = props.getProperty("server");
		this.username = props.getProperty("username");
		this.password = props.getProperty("password");
		this.resource = props.getProperty("resource");
		if(this.resource == null){
			this.resource = props.getProperty("resource");
		}
		this.debug = Boolean.parseBoolean(props.getProperty("debug"));
		this.connection = null;
		this.JID = null;
		this.chats = new HashMap<>();
	}
	
	public void connect() throws XMPPException{
		if(server == null || username == null || password == null || resource == null){
			throw new IllegalArgumentException("unsuitable service/login information is specified.");
		}
		boolean savedFlag = XMPPConnection.DEBUG_ENABLED;
		XMPPConnection.DEBUG_ENABLED = this.debug;
		this.connection = new XMPPConnection(server);
		XMPPConnection.DEBUG_ENABLED = savedFlag;
		this.connection.connect();
		this.connection.login(username, password, resource);
		this.JID = this.connection.getUser();
		this.chatMan = this.connection.getChatManager();
		this.chatMan.addChatListener(new ChatManagerListener(){

			@Override
			public void chatCreated(Chat chat, boolean createdLocally) {
				addChat(chat);
			}
			
		});
	}
	
	protected void addChat(Chat chat) {
		String participant = chat.getParticipant();
		String threadID = chat.getThreadID();
		Map<String,Chat> map = this.chats.get(participant);
		if(map == null){
			map = new HashMap<>();
			this.chats.put(participant, map);
		}
		if(map.containsKey(threadID)){
			System.err.println("Chat object (participant: \""+participant+"\", threadID: \""+threadID+"\") is already exist, and overwritten.");
		}
		map.put(threadID, chat);
		chat.addMessageListener(new MessageListener(){
			@Override
			public void processMessage(Chat chat, Message message) {
				System.err.println("User: \""+JID+"\" recieved message: \""+message.getBody()+"\" (from: \""+message.getFrom()+"\", threadID: \""+message.getThread()+"\").");
			}
		});
	}

	public void sendMessage(String toJID, String message){
		Message m = new Message();
		m.setTo(toJID);
		m.setBody(message);
		this.connection.sendPacket(m);
	}

	public void disconnect() {
		this.connection.disconnect();
	}
}
