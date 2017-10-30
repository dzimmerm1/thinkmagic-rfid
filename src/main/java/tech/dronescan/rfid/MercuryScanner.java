package tech.dronescan.rfid;

import java.io.IOException;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private String host = "localhost";  //192.168.254.102
	private int[] antennaList;
	private long duration = -1;
	private Configuration config = Configuration.getInstance();

	private Reader reader;

	public static void main(String[] args) {
		if (args.length != 3) {
			System.out.println("Usage: MercuryScanner [hostname] [antenna list as 1,2,...] [duration in ms.  Use -1 for none]");
			System.exit(0);
		}
		String host = args[0];
		String ants[] = args[1].split(",");
		long duration = Long.parseLong(args[2]);
		System.out.println("Using " + host + " as host and " + Arrays.toString(ants) + " as antenna list.  Running for " + duration + "ms.");
		
		int[] antennaList = new int[ants.length];
		for (int i = 0; i < ants.length; i++)
			antennaList[i] = Integer.parseInt(ants[i]);
		
		MercuryScanner scanner = new MercuryScanner(host, antennaList, duration);
		scanner.run();
	}
	
	public MercuryScanner(String host, int[] antennas, long duration) {
		this.host = host;
		this.antennaList = antennas;
		this.duration = duration;
	}

	private void run() {
		try {
			log.info("Starting MercuryScanner");

			connect(host);
			setRegion();
			TagReadHandler handler = setupReader();

			// add shutdown hook
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					shutdownReader();				
					handler.shutdown();
				}
			});

			// start reading
			reader.startReading();

			if (duration >= 0) {
				Thread.sleep(duration);
				System.exit(0);
			}
			
		} catch (Exception e) {
			log.error("Fatal Exception", e);
			System.exit(-1);
		}
		
	}

	private TagReadHandler setupReader() throws ReaderException, IOException {
		log.info("Starting reader");

		SimpleReadPlan plan = new SimpleReadPlan(antennaList, TagProtocol.GEN2, null, null, 1000);
		reader.paramSet(TMConstants.TMR_PARAM_READ_PLAN, plan);
		reader.addReadExceptionListener(new TagReadExceptionHandler());
		reader.paramSet("/reader/gen2/BLF", Gen2.LinkFrequency.LINK250KHZ);
		reader.paramSet("/reader/gen2/tari", Gen2.Tari.TARI_25US);
		reader.paramSet("/reader/gen2/tagEncoding", Gen2.TagEncoding.M2);
		reader.paramSet("/reader/gen2/session", Gen2.Session.get(config.readerSession));

		// Create and add tag listener
		TagReadHandler handler = new TagReadHandler();
		reader.addReadListener(handler);

		return handler;
	}

	private void connect(String ip) throws ReaderException {
		reader = Reader.create("tmr://" + ip);
		reader.connect();
		log.info("Reader Connected to {}", ip);
	}

	private void shutdownReader() {
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
