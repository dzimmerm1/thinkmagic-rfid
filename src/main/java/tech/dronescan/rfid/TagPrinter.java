package tech.dronescan.rfid;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.BlockingQueue;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thingmagic.TagReadData;

/**
 * Reads tags from the queue and prints them to file
 *
 */
public class TagPrinter implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(TagPrinter.class);
	private static final String COLUMN_HEADERS = "epc,time,rssi,phase,antenna";
	
	private BlockingQueue<TagReadData> queue;
	private Path dataDir;
	private Path transferDir;
	private Path dataFile;
	private FileWriter writer = null;
	private DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS");
	private int numRead = 0;
	private long lastTransferTime = System.currentTimeMillis();
	private boolean isRunning = true;
	private Configuration config = Configuration.getInstance();


	public TagPrinter(BlockingQueue<TagReadData> queue, Path dataDir) throws IOException {
		this.queue = queue;
		this.dataDir = dataDir;

		if (!Files.exists(dataDir)) {
			Files.createDirectory(dataDir);
			log.info("Created data directory " + dataDir);
		}

		if (!Files.isDirectory(dataDir))
			throw new RuntimeException("The directory " + dataDir + " does not exist");

		transferDir = dataDir.resolve("transfer");
		if (!Files.isDirectory(transferDir)) {
			Files.createDirectory(transferDir);
			log.info("Created transfer directory " + transferDir);
		}

	}

	/**
	 * Retrieves an TagReadData from the head of the queue and prints to a file
	 */
	public void run() {
		TagReadData tag = null;
		try {
			while (isRunning) {
				tag = queue.take();
				writeTag(tag);
			}

		} catch (Throwable e) {
			log.error("Error writing tag " + tag, e);
		}
	}
	
	public void shutdown() {
		log.info("Shutting down TagPrinter");
		isRunning = false;
		if (writer != null)
			try {
				writer.close();
			} catch (Exception e) {
			}
	}

	private void writeTag(TagReadData tag) throws IOException {
		if (writer == null) {
			writer = createNewFile();
		}

		StringBuilder sb = new StringBuilder();
		DateTime dateTime = new DateTime(tag.getTime());
		sb.append(tag.epcString()).append(",");
		sb.append(formatter.print(dateTime)).append(",");
		sb.append(tag.getRssi()).append(",");
		sb.append(tag.getPhase()).append(",");
		sb.append(tag.getAntenna());
		sb.append("\n");

		writer.write(sb.toString());
		numRead++;
		
		if (numRead % config.maxTagsPerFile == 0)
			log.info("Read {} tags", numRead);

		//transfer file if max tags per file is exceeded or if the max time per file is exceeded
		if (numRead > config.maxTagsPerFile || System.currentTimeMillis() - lastTransferTime > config.maxTimePerFile) {
			writer.close();
			//TODO: zip file when moving to transfer directory
			Files.move(dataFile, transferDir.resolve(dataDir.getFileName() + "-" + System.currentTimeMillis()), StandardCopyOption.REPLACE_EXISTING);
			writer = createNewFile();
			numRead = 0;
			lastTransferTime = System.currentTimeMillis();
		}
	}

	private FileWriter createNewFile() throws IOException {
		dataFile = dataDir.resolve("tags.csv");
		boolean addHeaders = true;
		if (Files.exists(dataFile))
			addHeaders = false;
		
		FileWriter writer = new FileWriter(dataFile.toFile(), true);
		
		if (addHeaders)
			writer.write(COLUMN_HEADERS + "\n");
		
		return writer;
	}
}