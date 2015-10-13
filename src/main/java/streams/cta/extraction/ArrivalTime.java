package streams.cta.extraction;

import java.time.LocalDateTime;

import stream.Data;
import stream.ProcessContext;
import stream.StatefulProcessor;
import streams.cta.CTARawDataProcessor;
import streams.cta.CTATelescope;

/**
 * Created by kai on 11.08.15.
 */
public class ArrivalTime extends CTARawDataProcessor implements StatefulProcessor {

    long nonLSTCounter = 0;

    @Override
    public Data process(Data input, CTATelescope telescope, LocalDateTime timeStamp, short[][] eventData) {

        if (eventData.length != 1855) {
            nonLSTCounter++;
            return input;
        }

        double[] arrivalTimes = new double[telescope.type.numberOfPixel];
        for (int pixel = 0; pixel < telescope.type.numberOfPixel; pixel++) {
            short max = 0;
            double arrivalTime = 0;
            for (int slice = 5; slice < 15; slice++) {
                short value = eventData[pixel][slice];
                if (value > max) {
                    arrivalTime = slice;
                    max = value;
                }
            }
            arrivalTimes[pixel] = arrivalTime;
        }
        input.put("arrivalTimes", arrivalTimes);
        return input;
    }

    @Override
    public void init(ProcessContext processContext) throws Exception {

    }

    @Override
    public void resetState() throws Exception {
        nonLSTCounter = 0;
    }

    @Override
    public void finish() throws Exception {
        System.out.println("Non LST telescope events: " + nonLSTCounter);
    }
}
