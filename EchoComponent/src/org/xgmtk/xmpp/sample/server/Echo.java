package org.xgmtk.xmpp.sample.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Main class for "echo" external server component.
 * @author kando
 *
 */
public class Echo{
	/**
	 * Command string to quit program.
	 */
	private static final String QUIT_COMMAND = "quit";
	
	/**
	 * The main method.
	 * This method takes no argument.
	 * This method would find configuration file at "~/resource/component.properties".
	 * This method would wait to finish until entering "quit" command from standard input stream.
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception{
		//Locate configuration file
		File wdir = new File(new File(System.getProperty("user.dir")), "resource");
		File defaultSettingFile = new File(wdir, ComponentContainer.DEFAULT_CONFIG_FILE_NAME);
		
		//Initialize ComponentContainer
		ComponentContainer container = new ComponentContainer(defaultSettingFile);
	
		//Initialize EchoComponent.
		String compName = "echo";
		EchoComponent component = new EchoComponent(compName,
				container.getIntegralProperty(compName, "interval"),
				container.getMaxThreadpoolSizeProperty(compName), container.getMaxQueueSizeProperty(compName));
		
		//Start EchoComponent
		container.startComponent(compName, component);
		
		//Wait the "quit" command.
		commandLoop();
		
		//Stop EchoComponent
		container.stopComponent(compName);
		
		//Finish
		System.exit(0);
	}

	/**
	 * Execute the loop waiting the "quit" command.
	 * @throws IOException
	 */
	public static void commandLoop() throws IOException {
		//Print a help text.
		System.out.println("Type \""+QUIT_COMMAND+"\" and enter key to quit");
		
		BufferedReader lineReader = new BufferedReader(new InputStreamReader(System.in));
		String lineInput = lineReader.readLine();
		while(lineInput != null){
			if(lineInput.equals(QUIT_COMMAND)){
				break;
			}
			lineInput = lineReader.readLine();
		}
	}
}
