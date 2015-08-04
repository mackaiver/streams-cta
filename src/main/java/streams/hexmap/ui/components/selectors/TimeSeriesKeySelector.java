package streams.hexmap.ui.components.selectors;

import streams.Utils;
import streams.hexmap.ui.Bus;
import streams.hexmap.ui.events.TimeSeriesSelectionChangedEvent;
import stream.Data;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;

/**
 * This selector can display all arrays which are 1440*B with B > 1 length. The Selector will be displayed
 * in the plotting window.
 *
 * Created by kaibrugge on 02.06.14.
 */
public class TimeSeriesKeySelector extends KeySelector {


    @Override
    public void selectionUpdate() {

        Bus.eventBus.post(new TimeSeriesSelectionChangedEvent(getSelectedItemPairs()));

    }

    @Override
    public Set<SeriesKeySelectorItem> filterItems(Data item) {
        Set<SeriesKeySelectorItem> newItems = new HashSet<>();
        for (String key : item.keySet()) {
            double[] series = Utils.toDoubleArray(item.get(key));
//                double[] series = (double[]) item.get(key);
            if (series !=  null &&(series.length > 1440) && (series.length % 1440 == 0)){
                SeriesKeySelectorItem newSelectorItem = new SeriesKeySelectorItem(key, Color.GRAY, this);
                //newSelectorItem.setSelector(this);
                newItems.add(newSelectorItem);
            }
        }
        return newItems;
    }
}
