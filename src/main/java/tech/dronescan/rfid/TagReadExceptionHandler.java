package tech.dronescan.rfid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thingmagic.ReadExceptionListener;
import com.thingmagic.Reader;
import com.thingmagic.ReaderException;

public class TagReadExceptionHandler implements ReadExceptionListener {

	private static final Logger log = LoggerFactory.getLogger(MercuryScanner.class);
	
	@Override
	public void tagReadException(Reader r, ReaderException re) {
		log.error("Read exception", re);
	}

}
