package streams.cta.io.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import streams.cta.Constants;
import streams.cta.io.EventIOBuffer;
import streams.cta.io.EventIOHeader;
import streams.cta.io.HTime;

/**
 * Central trigger event data
 *
 * @author alexey
 */
public class CentralEvent {

    static Logger log = LoggerFactory.getLogger(CentralEvent.class);

    /**
     * Global event count.
     */
    int globCount;

    /**
     * CPU time at central trigger station.
     */
    public HTime cpuTime;

    /**
     * GPS time at central trigger station.
     */
    public HTime gpsTime;

    /**
     * Bit pattern of telescopes having sent a trigger signal to the central station. (Historical;
     * only useful for small no. of telescopes.)
     */
    int teltrgPattern;

    /**
     * Bit pattern of telescopes having sent event data that could be merged. (Historical; only
     * useful for small no. of telescopes.)
     */
    int teldataPattern;

    /**
     * How many telescopes triggered.
     */
    public int numTelTriggered;

    /**
     * List of IDs of triggered telescopes.
     */
    short[] teltrgList;

    /**
     * Relative time of trigger signal after correction for nominal delay [ns].
     */
    float[] teltrgTime;

    /**
     * Bit mask which type of trigger fired.
     */
    int[] teltrgTypeMask;

    /**
     * Time of trigger separate for each type.
     */
    float[][] teltrgTimeByType;

    /**
     * Number of telescopes expected to have data.
     */
    int numTelData;

    /**
     * List of IDs of telescopes with data.
     */
    short[] teldataList;

    public CentralEvent() {
        globCount = 0;
        teltrgPattern = 0;
        teldataPattern = 0;
        numTelTriggered = 0;
        numTelData = 0;
    }

    private void initArrays(int numberTriggeredTelescopes){
        teltrgList = new short[numberTriggeredTelescopes];
        teltrgTime = new float[numberTriggeredTelescopes];
        teltrgTypeMask = new int[numberTriggeredTelescopes];
        teltrgTimeByType = new float[numberTriggeredTelescopes][Constants.MAX_TEL_TRIGGERS];
        teldataList = new short[numberTriggeredTelescopes];
    }

    public boolean readCentralEvent(EventIOBuffer buffer) {

        EventIOHeader header = new EventIOHeader(buffer);
        try {
            if (header.findAndReadNextHeader()) {
                if (header.getVersion() > 2) {
                    log.error("Unsupported central event version: " + header.getVersion());
                    header.getItemEnd();
                    return false;
                }
                globCount = (int) header.getIdentification();
                cpuTime.readTime(buffer);
                gpsTime.readTime(buffer);
                teltrgPattern = buffer.readInt32();
                teldataPattern = buffer.readInt32();

                if (header.getVersion() >= 1) {
                    numTelTriggered = buffer.readShort();

                    if (numTelTriggered > Constants.H_MAX_TEL) {
                        log.error("Invalid number of triggered telescopes " + numTelTriggered
                                + " in central trigger block for event " + globCount);
                        numTelTriggered = 0;
                        header.getItemEnd();
                        return false;
                    }

                    teltrgList = buffer.readVectorOfShorts(numTelTriggered);
                    teltrgTime = buffer.readVectorOfFloats(numTelTriggered);
                    numTelData = buffer.readShort();

                    if (numTelData > Constants.H_MAX_TEL) {
                        log.error("Invalid number of telescopes with data " + numTelData
                                + " in central trigger block for event " + globCount);
                        numTelData = 0;
                        header.getItemEnd();
                        return false;
                    }

                    teldataList = buffer.readVectorOfShorts(numTelData);
                } else {
                    numTelTriggered = 0;
                    numTelData = 0;
                }

                // initialize arrays with the needed number of telescopes
                initArrays(numTelTriggered);

                //TODO wtf? versions greater than 2 are not supported so just check for ==2?
                if (header.getVersion() == 2) {
                    for (int i = 0; i < numTelTriggered; i++) {
                        long value = buffer.readCount32();
                        if (value > Integer.MAX_VALUE) {
                            log.error("Telescope triggered type mask was higher value " +
                                    "than Integer.MAX_VALUE.");
                        }
                        teltrgTypeMask[i] = (int) value;
                    }
                    for (int telCount = 0; telCount < numTelTriggered; telCount++) {
                        int ntt = 0;
                        for (int triggers = 0; triggers < Constants.MAX_TEL_TRIGGERS; triggers++) {
                            if ((teltrgTypeMask[telCount] & (1 << triggers)) == 1) {
                                ntt++;
                                teltrgTimeByType[telCount][triggers] = teltrgTime[telCount];
                            } else {
                                teltrgTimeByType[telCount][triggers] = 9999;
                            }
                        }

                        if (ntt > 1) {
                            for (int triggers = 0; triggers < Constants.MAX_TEL_TRIGGERS; triggers++) {
                                if ((teltrgTypeMask[telCount] & (1 << triggers)) == 1) {
                                    teltrgTimeByType[telCount][triggers] = buffer.readFloat();
                                }
                            }
                        }
                    }
                } else {
                    for (int telCount = 0; telCount < numTelTriggered; telCount++) {
                        // older data was always majority trigger
                        teltrgTypeMask[telCount] = 1;
                        teltrgTimeByType[telCount][0] = teltrgTime[telCount];
                        teltrgTimeByType[telCount][1] = 9999;
                        teltrgTimeByType[telCount][2] = 9999;
                    }
                }
            }
            return header.getItemEnd();
        } catch (IOException e) {
            log.error("Something went wrong while reading the header:\n" + e.getMessage());
        }
        return false;
    }
}
