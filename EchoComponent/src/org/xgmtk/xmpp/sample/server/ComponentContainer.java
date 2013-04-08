package org.xgmtk.xmpp.sample.server;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.jivesoftware.whack.ExternalComponentManager;
import org.xmpp.component.Component;
import org.xmpp.component.ComponentException;

/**
 * Container of Component object.
 * An instance of this class reads a configuration file, 
 * accepts a Component object,
 * and runs the accepted component object as external server component.
 * This container can run several components for one host.
 * @author kando
 *
 */
public class ComponentContainer {
	/**
	 * Default name of configuration file.
	 */
	public static final String DEFAULT_CONFIG_FILE_NAME ="component.properties";
	
	/**
	 * Utility method to load configuration file.
	 * The configuration file is a Java property file.
	 * 
	 * @param propertyFile
	 * @return A Property object contains parameters of settings as properties.
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private static Properties loadSettings(File propertyFile) throws FileNotFoundException,
	IOException {
		InputStream is = new BufferedInputStream(new FileInputStream(propertyFile));
		Properties settings = new Properties();
		settings.load(is);
		is.close();
		return settings;
	}

	private Properties props;
	private ExternalComponentManager exCompoMan;
	private String host;
	
	/**
	 * Initializer
	 * @param propertyFile A property file contains parameters of settings as properties.
	 * @throws IOException
	 */
	public ComponentContainer(File propertyFile) throws IOException{
		this.props = loadSettings(propertyFile);
		this.host = this.props.getProperty("host");
		this.exCompoMan = new ExternalComponentManager(this.host);
	}

	/**
	 * Get the maximum value of thread pool size from property file.
	 * A result is an integral value from the property named compName+".max.threadpool.size"
	 * @param compName The name of the component.
	 * @return The maximum value of  thread pool size
	 * @throws NumberFormatException
	 */
	public int getMaxThreadpoolSizeProperty(String compName) throws NumberFormatException{
		return getIntegralProperty(compName, "max.threadpool.size");
	}
	
	/**
	 * Get the maximum value of queue size from property file.
	 * A result is an integral value from the property named compName+".max.queue.size"
	 * @param compName The name of the component.
	 * @return The maximum value of queue size.
	 * @throws NumberFormatException
	 */
	public int getMaxQueueSizeProperty(String compName) throws NumberFormatException{
		return getIntegralProperty(compName, "max.queue.size");
	}

	/**
	 * Get the property value from property file.
	 * A result is an integral value from the property named compName+"."+propertyName .
	 * @param compName The name of the component.
	 * @param propertyName The name of property.
	 * @return The integral property value.
	 * @throws NumberFormatException
	 */
	public int getIntegralProperty(String compName, String propertyName) throws NumberFormatException {
		return Integer.parseInt(this.props.getProperty(compName+"."+propertyName));
	}
	
	/**
	 * Accept and run the given component.
	 * The name of the component is used as a sub-domain name
	 * and a prefix of the name of the property of the secret key.
	 * @param compName The name of the component.
	 * @param component The Component object to run
	 * @throws IOException
	 * @throws ComponentException
	 */
	public void startComponent(String compName, Component component)
			throws IOException, ComponentException{
		String secretKey = this.props.getProperty(compName+".secret.key");
		this.exCompoMan.setSecretKey(compName, secretKey);
		this.exCompoMan.getLog().info("host: \""+this.host+"\", name: \""+compName+"\", secret: \""+secretKey+"\"");
		this.exCompoMan.addComponent(compName, component);
	}

	/**
	 * Stop the component which is specified by its name.
	 * @param compName The name of the component.
	 * @throws ComponentException
	 */
	public void stopComponent(String compName) throws ComponentException{
		this.exCompoMan.removeComponent(compName);
	}
}
