package tech.dronescan;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thingmagic.ReadListener;
import com.thingmagic.Reader;
import com.thingmagic.TagReadData;

public class TagReadHandler implements ReadListener {

	private static final Logger log = LoggerFactory.getLogger(TagReadHandler.class);

	private static final String DATA_DIR = "data";

	private BlockingQueue<TagReadData> queue = new LinkedBlockingQueue<>();

	public TagReadHandler() throws IOException {

		// start new thread to handle printing tags to file
		TagPrinter printer = new TagPrinter(queue, Paths.get(DATA_DIR));
		new Thread(printer).start();
	}

	@Override
	public void tagRead(Reader r, TagReadData t) {

		try {
			queue.put(t);
		} catch (Exception e) {
			log.error("Error added tag data to queue", e);
		}
	}

}
