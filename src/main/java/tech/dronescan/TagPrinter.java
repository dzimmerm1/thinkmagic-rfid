package tech.dronescan;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thingmagic.TagReadData;

public class TagPrinter implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(TagPrinter.class);
	private static final String COLUMN_HEADERS = "epc,time,rssi,phase,antenna";

	private BlockingQueue<TagReadData> queue;
	private Path dataDir;
	private Path transferDir;
	private FileWriter writer = null;
	private DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS");

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

		// add shutdown hook
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				log.info("Shutting down TagPrinter");
				if (writer != null)
					try {
						writer.close();
					} catch (Exception e) {
					}
			}
		});

	}

	/**
	 * Retrieves an TagReadData from the head of the queue and prints to a file
	 */
	public void run() {
		TagReadData tag = null;
		try {
			while (true) {
				tag = queue.take();
				writeTag(tag);
			}

		} catch (Throwable e) {
			log.error("Error writing tag " + tag, e);
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

		// TODO: check if file is ready for transfer
	}

	private FileWriter createNewFile() throws IOException {
		Path dataFile = dataDir.resolve("tags.csv");
		FileWriter writer = new FileWriter(dataFile.toFile(), true);
		writer.write(COLUMN_HEADERS + "\n");
		return writer;
	}
}