package tech.dronescan.rfid;

import java.io.IOException;

import tech.dronescan.util.ConfigurationUtils;

/**
 * Singleton for configuration
 *
 */
public class Configuration extends ConfigurationUtils {

	private static final String configFile = "rfidReader.properties";

	private static Configuration instance = null;

	public Configuration(String configurationResource) {
		try {
			ConfigurationUtils.load(this, configurationResource);
		} catch (IOException e) {
			throw new RuntimeException("Error loading configuration properties from file " + configurationResource, e);
		}
	}
	
	public static synchronized Configuration getInstance() {
		if (instance == null) 
			instance = new Configuration(configFile);
		
		return instance;
	}
	
	/* PROPERTIES ***************************************************************************/
	
	public int maxTagsPerFile = 5000;		//max tags per data file before moving to transfer dir
	public long maxTimePerFile = 900000; 	//max milli-seconds per data file before moving to trasfer dir
	
	public int readerSession = 0;			//See com.thingmagic.Gen2$Session
	

}
