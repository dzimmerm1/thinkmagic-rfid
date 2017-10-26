package tech.dronescan;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thingmagic.TagReadData;
import com.thingmagic.Gen2;
import com.thingmagic.Reader;
import com.thingmagic.ReaderException;
import com.thingmagic.SimpleReadPlan;
import com.thingmagic.TMConstants;
import com.thingmagic.TagProtocol;

/**
 * RFID reader main class
 *
 */
public class MercuryScanner {
	private static final Logger log = LoggerFactory.getLogger(MercuryScanner.class);
	private static final String IP_ADDR = "192.168.254.102";
	private static final int[] antennaList = {1};
	private static final int READ_TIME_MS = 15000; 

	private Reader reader;

	public static void main(String[] args) {
		new MercuryScanner().run();
	}
	
	private void run() {
		try {
			log.error("Starting MercuryScanner");
			connect(IP_ADDR);
			setRegion();
			startReading();
			Thread.sleep(READ_TIME_MS);
		} catch (Exception e) {
			log.error("Fatal Exception", e);
		} finally {
			shutdown();
		}		
		System.exit(0);
	}
	
	private void startReading() throws ReaderException, IOException {
		log.info("Starting reader");
		
        SimpleReadPlan plan = new SimpleReadPlan(antennaList, TagProtocol.GEN2, null, null, 1000);
        reader.paramSet(TMConstants.TMR_PARAM_READ_PLAN, plan);
        reader.addReadExceptionListener(new TagReadExceptionHandler());
        reader.paramSet("/reader/gen2/BLF", Gen2.LinkFrequency.LINK250KHZ);
        reader.paramSet("/reader/gen2/tari", Gen2.Tari.TARI_25US);
        reader.paramSet("/reader/gen2/tagEncoding", Gen2.TagEncoding.M2);
        reader.paramSet("/reader/gen2/session", Gen2.Session.S0);
        
        
        // Create and add tag listener
        reader.addReadListener(new TagReadHandler());
        
//        readData();
        
        reader.startReading();   
		
	}

	private void readData() throws ReaderException, IOException {
        TagReadData[] tags = reader.read(READ_TIME_MS);
        TagReadHandler handler = new TagReadHandler();
        for (TagReadData tag : tags)
        	handler.tagRead(reader, tag);
		
	}

	private void connect(String ip) throws ReaderException {
		reader = Reader.create("tmr://" + ip);
		reader.connect();
		log.info("Reader Connected to {}", ip);
	}

	private void shutdown() {
		log.info("Shutting down reader");
		
		if (reader != null) {
			reader.stopReading();
			reader.destroy();
		}
			
	}

	private void setRegion() throws ReaderException {
		if (Reader.Region.UNSPEC == (Reader.Region) reader.paramGet("/reader/region/id")) {
			Reader.Region[] supportedRegions = (Reader.Region[]) reader
					.paramGet(TMConstants.TMR_PARAM_REGION_SUPPORTEDREGIONS);
			if (supportedRegions.length < 1) {
				throw new RuntimeException("Reader doesn't support any regions");
			} else {
				reader.paramSet("/reader/region/id", supportedRegions[0]);
			}
		}
	}
}
