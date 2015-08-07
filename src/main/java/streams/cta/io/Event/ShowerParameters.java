package streams.cta.io.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import streams.cta.io.EventIOBuffer;
import streams.cta.io.EventIOHeader;

/**
 * Reconstructed shower parameters.
 *
 * @author alexey
 */
public class ShowerParameters {

    static Logger log = LoggerFactory.getLogger(ShowerParameters.class);

    public int known;
    public int numTrg;      ///< Number of telescopes contributing to central trigger.
    int numRead;     ///< Number of telescopes read out.
    int numImg;      ///< Number of images used for shower parameters.
    int imgPattern;  ///< Bit pattern of which telescopes were used (for small no. of telescopes only).
    int[] imgList; ///< With more than 16 or 32 telescopes, we can only use the list.

    /**
     * Bit pattern of what results are available: Bits 0 + 1: direction + errors Bits 2 + 3: core
     * position + errors Bits 4 + 5: mean scaled image shape + errors Bits 6 + 7: energy + error
     * Bits 8 + 9: shower maximum + error
     */
    long resultBits;

    /**
     * Azimuth angle [radians from N->E]
     */
    float azimuthAngle;
    float altitude;       ///< Altitude [radians]
    float errDirX;  ///< Error estimate in nominal plane X direction (|| Alt) [rad]
    float errDirY;  ///< Error estimate in nominal plane Y direction (|| Az) [rad]
    float errDir3;  ///< ?
    float corePosX;        ///< X core position [m]
    float corePosY;        ///< Y core position [m]
    float errCoreX; ///< Error estimate in X coordinate [m]
    float errCoreY; ///< Error estimate in Y coordinate [m]
    float errCore3; ///< ?
    float meanScaledLength;      ///< Mean scaled image length [gammas ~1 (HEGRA-style) or ~0 (HESS-style)].
    float errMeanScaledLength;
    float meanScaledWidth;      ///< Mean scaled image width [gammas ~1 (HEGRA-style) or ~0 (HESS-style)].
    float errMeanScaledWidth;
    float energy;    ///< Primary energy [TeV], assuming a gamma.
    float errEnergy;
    float xmax;      ///< Atmospheric depth of shower maximum [g/cm^2].
    float errXmax;

    public boolean readShower(EventIOBuffer buffer) {
        EventIOHeader header = new EventIOHeader(buffer);
        try {
            if (header.findAndReadNextHeader()) {
                known = 0;

                if (header.getVersion() > 2) {
                    log.error("Unsupported reconstructed shower version: " + header.getVersion());
                    header.getItemEnd();
                    return false;
                }

                resultBits = header.getIdentification();

                numTrg = buffer.readShort();
                numRead = buffer.readShort();
                numImg = buffer.readShort();

                if (header.getVersion() >= 1) {
                    imgPattern = buffer.readInt32();
                } else {
                    imgPattern = 0;
                }

                if (header.getVersion() >= 2) {
                    imgList = buffer.readVectorOfShorts(numImg);
                }

                if ((resultBits & 0x01) != 0) {
                    azimuthAngle = buffer.readFloat();
                    altitude = buffer.readFloat();
                } else {
                    azimuthAngle = 0.f;
                    altitude = 0.f;
                }

                if ((resultBits & 0x02) != 0) {
                    errDirX = buffer.readFloat();
                    errDirY = buffer.readFloat();
                    errDir3 = buffer.readFloat();
                } else {
                    errDirX = 0.f;
                    errDirY = 0.f;
                    errDir3 = 0.f;
                }

                if ((resultBits & 0x04) != 0) {
                    corePosX = buffer.readFloat();
                    corePosY = buffer.readFloat();
                } else {
                    corePosX = 0.f;
                    corePosY = 0.f;
                }

                if ((resultBits & 0x08) != 0) {
                    errCoreX = buffer.readFloat();
                    errCoreY = buffer.readFloat();
                    errCore3 = buffer.readFloat();
                } else {
                    errCoreX = 0.f;
                    errCoreY = 0.f;
                    errCore3 = 0.f;
                }

                if ((resultBits & 0x10) != 0) {
                    meanScaledLength = buffer.readFloat();
                    meanScaledWidth = buffer.readFloat();
                } else {
                    meanScaledLength = -1.f;
                    meanScaledWidth = -1.f;
                }

                if ((resultBits & 0x20) != 0) {
                    errMeanScaledLength = buffer.readFloat();
                    errMeanScaledWidth = buffer.readFloat();
                } else {
                    errMeanScaledLength = 0.f;
                    errMeanScaledWidth = 0.f;
                }

                if ((resultBits & 0x40) != 0) {
                    energy = buffer.readFloat();
                } else {
                    energy = -1.f;
                }

                if ((resultBits & 0x80) != 0) {
                    errEnergy = buffer.readFloat();
                } else {
                    errEnergy = 0.f;
                }

                xmax = 0.f;

                if ((resultBits & 0x0100) != 0) {
                    xmax = buffer.readFloat();
                }

                errXmax = 0.f;

                if ((resultBits & 0x0200) != 0) {
                    errXmax = buffer.readFloat();
                }

                known = 1;

                return header.getItemEnd();
            }
        } catch (IOException e) {
            log.error("Something went wrong while reading the header:\n" + e.getMessage());
        }
        return false;
    }
}
