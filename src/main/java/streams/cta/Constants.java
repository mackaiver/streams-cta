package streams.cta;

/**
 * Created by alexey on 17.06.15.
 */
public class Constants {

    public static final int H_MAX_PROFILE = 10;
    public static final int MAX_IO_ITEM_LEVEL = 20;
    public static final int H_MAX_TEL = 16;
    public static final int H_MAX_PIX = 4095;
    public static final int H_MAX_TRG_PER_SECTOR = 1;
    public static final int H_MAX_SECTORS = H_MAX_PIX * H_MAX_TRG_PER_SECTOR;

    /**< Maximum number of different gains per PM */
    public static final int H_MAX_GAINS = 2;

    /**< Maximum number of time slices handled. */
    public static final int H_MAX_SLICES = 128;
}
