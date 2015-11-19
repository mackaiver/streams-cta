package streams.cta.io;


import com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import stream.Data;
import stream.ProcessContext;
import stream.StatefulProcessor;
import stream.annotations.Parameter;
import streams.cta.CTARawDataProcessor;
import streams.cta.CTATelescope;
import streams.cta.CTATelescopeType;
import streams.cta.io.datamodel.nano.CoreMessages;
import streams.cta.io.datamodel.nano.L0;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * ProtoEventPublisher uses ZeroMQ with a Push / Pull pattern to publish telescope events serialized
 * with protocol buffer format according to DAQs data model
 *
 * @author kai
 */
public class DaqMonitor extends CTARawDataProcessor implements StatefulProcessor {

    static Logger log = LoggerFactory.getLogger(DaqMonitor.class);

    private ZMQ.Socket publisher;
    private ZMQ.Socket monitorPublisher;
    private ZMQ.Context context;
    private int itemCounter = 0;

    private Stopwatch stopwatch;


    @Parameter(required = false, description = "The IP of the daq monitor. Asin in tcp://127.0.0.1:1222")
    String monitorAddress = "tcp://127.0.0.1:4849";

    @Parameter(required = false)
    String[] addresses = {"tcp://*:4849"};

    @Override
    public void init(ProcessContext processContext) throws Exception {
        context = ZMQ.context(1);
        publisher = context.socket(ZMQ.PUSH);
        for (String address : addresses) {
            publisher.bind(address);
            log.info("Binding to address: " + address);
        }

        monitorPublisher = context.socket(ZMQ.PUB);
        monitorPublisher.bind(monitorAddress);
        log.info("Binding monitor to address: " + monitorAddress);

        stopwatch = Stopwatch.createStarted();
    }

    private void sendToMonitor(int bytesSend, long ellapsedMicros){
        CoreMessages.ThroughputStats stats = new CoreMessages.ThroughputStats();
        stats.comment = "Bytes Published";
        stats.numBytes = bytesSend;
        stats.elapsedUs = (int) ellapsedMicros;
        byte[] bytesTosend = CoreMessages.ThroughputStats.toByteArray(stats);
        monitorPublisher.send(bytesTosend);
    }

    @Override
    public Data process(Data input, CTATelescope telescope, LocalDateTime timeStamp, short[][] eventData) {
        if (telescope.type != CTATelescopeType.LST) {
            log.debug("Found non LST event");
            return null;
        }

        int roi = eventData[0].length;
        int numPixel = telescope.type.numberOfPixel;

        //create the L0 data
        CoreMessages.AnyArray anyArray = new CoreMessages.AnyArray();
        anyArray.currentComp = CoreMessages.AnyArray.RAW;
        anyArray.type = CoreMessages.AnyArray.S16;
        //save samples into byte array
        int bytesPerSample = 2;
        ByteBuffer buffer = ByteBuffer.allocate(roi*numPixel*bytesPerSample);
        for (int i = 0; i < numPixel; i++) {
            for (int j = 0; j <  roi; j++) {
                buffer.putShort(eventData[i][j]);
            }
        }
        anyArray.data = buffer.array();

        L0.CameraRunHeader header = new L0.CameraRunHeader();
        header.numTraces = roi;
        header.telescopeID = telescope.telescopeId;

        L0.WaveFormData waveFormData = new L0.WaveFormData();
        waveFormData.numSamples = roi;
        waveFormData.samples = anyArray;

        L0.PixelsChannel pixelsChannel = new L0.PixelsChannel();
        pixelsChannel.waveforms = waveFormData;

        L0.CameraEvent cameraEvent = new L0.CameraEvent();
        cameraEvent.telescopeID = telescope.telescopeId;
        cameraEvent.head = header;
        cameraEvent.hiGain = pixelsChannel;

        //in theory the CameraEvent can carry many payloads. In practice however. There is just one payload
        //per package
        byte[] payload = L0.CameraEvent.toByteArray(cameraEvent);
        byte[][] payloadWrap = new byte[1][];
        payloadWrap[0] = payload;
        //create the core message:
        CoreMessages.CTAMessage ctaMessage = new CoreMessages.CTAMessage();
        ctaMessage.payloadType = new int[]{CoreMessages.CAMERA_EVENT};
        ctaMessage.payloadData = payloadWrap;
        if (input.get("@source") != null){
            ctaMessage.sourceName = input.get("@source").toString();
        }

        byte[] bytesTosend = CoreMessages.CTAMessage.toByteArray(ctaMessage);

        //send stuff to the central daq monitor every 5000 events or so
        if(itemCounter == 5000){
            itemCounter = 0;
            long ellapsedMicros = stopwatch.elapsed(TimeUnit.MICROSECONDS);
            stopwatch.reset();
            sendToMonitor(bytesTosend.length, ellapsedMicros);
        }

        input.put("@packetSize", bytesTosend.length);
        publisher.send(bytesTosend, 0);
        return input;
    }

    @Override
    public void resetState() throws Exception {

    }

    @Override
    public void finish() throws Exception {

        System.out.println("Sleeping for 0.2 seconds");
        Thread.sleep(200);


        if (publisher != null) {
            publisher.close();
        }
        if (context != null) {
            context.term();
        }

    }

    public void setAddresses(String[] addresses) {
        this.addresses = addresses;
    }

    public void setMonitorAddress(String monitorAddress) {
        this.monitorAddress = monitorAddress;
    }

}