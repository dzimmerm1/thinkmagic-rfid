package tech.dronescan;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thingmagic.ReadListener;
import com.thingmagic.Reader;
import com.thingmagic.TagReadData;

public class TagReadHandler implements ReadListener {

	private static final Logger log = LoggerFactory.getLogger(TagReadHandler.class);

	private BlockingQueue<TagReadData> queue = new LinkedBlockingQueue<>();
	

	@Override
	public void tagRead(Reader r, TagReadData t) {

		try {
			queue.put(t);
		}
		catch (Exception e) {
			log.error("Error added tag data to queue", e);
		}
		
//		if (t != null) {
//			DateTime dateTime = new DateTime(t.getTime());
//
//			String s = String.format("EPC:%s ant:%d count:%d time:%s", (t == null) ? "none" : t.epcString(),
//					t.getAntenna(), t.getReadCount(), formatter.print(dateTime));
//			System.out.println(s);
//		}
	}
	

}
