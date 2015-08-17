package streams.cta.io.eventio.mcshower;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import streams.cta.io.eventio.EventIOBuffer;
import streams.cta.io.eventio.EventIOHeader;

/**
 * Created by alexey on 30.06.15.
 */
public class ShowerExtraParameters {

    static Logger log = LoggerFactory.getLogger(ShowerExtraParameters.class);

    /**
     * May identify to the user what the parameters should mean.
     */
    long id;

    /**
     * May be reset after writing the parameter block and must thus be set to 1 for each shower for
     * which the extra parameters should get recorded.
     */
    int isSet;

    /**
     * To be used if the weight of a shower may change during processing, e.g. when shower
     * processing can be aborted depending on how quickly the electromagnetic component builds up
     * and the remaining showers may have a larger weight to compensate for that. For backwards
     * compatibility this should be set to 1.0 when no additional weight is needed.
     */
    float weight;

    /**
     * Number of extra integer parameters.
     */
    long niparam;

    /**
     * Space for extra integer parameters, at least of size niparam.
     */
    int[] iparam;

    /**
     * Number of extra floating-point parameters.
     */
    long nfparam;

    /**
     * Space for extra floats, at least of size nfparam.
     */
    float[] fparam;

    /**
     * Reset all the values for the object of type ShowerExtraParameters
     */
    public void clearShowerExtraParameters() {
        id = 0;
        isSet = 0;
        weight = 1.0f;

        if (iparam != null) {
            for (int i = 0; i < niparam; i++) {
                iparam[i] = 0;
            }
        }

        if (fparam != null) {
            for (int i = 0; i < nfparam; i++) {
                fparam[i] = 0;
            }
        }
    }

    public boolean readShowerExtraParameters(EventIOBuffer buffer){

        // TODO: check this implementation after such extra parameters has been found
        log.error("Please check the implementation of readShowerExtraParameters " +
                "method and then remove this output.");

        // read the header of extra parameters
        EventIOHeader header = new EventIOHeader(buffer);

        try {
            if (header.findAndReadNextHeader()) {
                isSet = 0;

                if (header.getVersion() != 1) {
                    log.error("Skipping MCShower because version is not 1, but " + header.getVersion());
                    header.getItemEnd();
                    return false;
                }

                id = header.getIdentification();
                weight = buffer.readFloat();

                // detect number of integer and float parameters dynamically
                long ni = buffer.readCount64();
                long nf = buffer.readCount64();

                // fill the iparam list
                if (ni > 0) {
                    if (ni != niparam) {
                        iparam = new int[(int) ni];
                        for (int i = 0; i < ni; i++) {
                            iparam[i] = buffer.readInt32();
                        }
                    }
                }
                niparam = ni;

                // fill the fparam list
                if (nf > 0) {
                    if (nf != nfparam) {
                        fparam = new float[(int) nf];
                        for (int i = 0; i < nf; i++) {
                            fparam[i] = buffer.readFloat();
                        }
                    }
                }
                nfparam = nf;

                isSet = 1;

                return header.getItemEnd();
            }
        } catch (IOException e) {
            log.error("Something went wrong while reading the header:\n" + e.getMessage());
        }
        return false;
    }
}