package org.xgmtk.xmpp.sample;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.jivesoftware.smack.XMPPException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xgmtk.xmpp.sample.client.SimpleClient;
import org.xgmtk.xmpp.sample.server.ComponentContainer;
import org.xgmtk.xmpp.sample.server.Echo;
import org.xgmtk.xmpp.sample.server.EchoComponent;
import org.xmpp.component.ComponentException;
import org.xmpp.packet.JID;


public class TestSamples {
	private static File CONFIG_DIR;
	private static File CLIENT_CONFIG;
	private static File SVC_CONFIG;
	private static AtomicInteger count;

	public static String getNewId(String prefix){
		int n = count.getAndIncrement();
		return prefix+n;
	}
	
	@BeforeClass
	public static void setupStatic(){
		File wdir = new File(System.getProperty("user.dir"));
		CONFIG_DIR = new File(wdir, "resource");
		CLIENT_CONFIG = new File(CONFIG_DIR, "client.properties");
		SVC_CONFIG = new File(CONFIG_DIR, ComponentContainer.DEFAULT_CONFIG_FILE_NAME);
		count = new AtomicInteger(0);
	}

	private EchoComponent svc;
	private SimpleClient client;
	private JID svcJID;
	private String compName;
	private ComponentContainer container;

	@Before
	public void setup() throws IOException, XMPPException, ComponentException{
		this.container = new ComponentContainer(SVC_CONFIG);

		this.compName = "echo";
		this.svc = new EchoComponent(this.compName, 
				container.getIntegralProperty(this.compName, "interval"),
				container.getMaxThreadpoolSizeProperty(this.compName),
				container.getMaxQueueSizeProperty(this.compName));
	
		this.container.startComponent(this.compName, this.svc);
		this.svcJID = this.svc.getJID();
		
		this.client = new SimpleClient(CLIENT_CONFIG, getNewId(SimpleClient.class.getSimpleName()));
		this.client.connect();
	}
	
	@After
	public void cleanup() throws ComponentException{
		this.container.stopComponent(this.compName);
		this.client.disconnect();
	}
	
	@Test
	public void test0() throws IOException{
		String jid = this.svcJID.toString();
		this.client.sendMessage(jid, "Hello.");
		this.client.sendMessage(jid, "World!");
		this.client.sendMessage("echo@"+jid, "Hello Echo!");
		Echo.commandLoop();
	}
}
