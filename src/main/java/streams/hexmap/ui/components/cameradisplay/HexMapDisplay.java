/**
 * 
 */
package streams.hexmap.ui.components.cameradisplay;

import com.google.common.eventbus.Subscribe;
import streams.hexmap.CameraPixel;
import streams.hexmap.FactCameraPixel;
import streams.hexmap.FactHexPixelMapping;
import streams.hexmap.HexPixelMapping;
import streams.hexmap.ui.Bus;
import streams.hexmap.ui.EventObserver;
import streams.hexmap.ui.SliceObserver;
import streams.hexmap.ui.colormaps.ColorMapping;
import streams.hexmap.ui.colormaps.GrayScaleColorMapping;
import streams.hexmap.ui.events.ItemChangedEvent;
import streams.hexmap.ui.events.SliceChangedEvent;
import streams.hexmap.ui.overlays.CameraMapOverlay;
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stream.Data;
import streams.hexmap.ui.plotting.OverlayPlotData;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.text.DecimalFormat;
import java.util.*;

/**
 * This implements a PixelMap to draw a grid of hexagons as seen in the camera
 * of the fact telescope The hexagons are equally spaced and sized. Orientated
 * with one edge on the bottom. Also has a colorbar next to it.
 * 
 */
public class HexMapDisplay extends JPanel implements PixelMapDisplay, SliceObserver, MouseListener {

	static Logger log = LoggerFactory.getLogger(HexMapDisplay.class);

	//camera specific informations
	private final double PIXEL_RADIUS = 25; //millimeter flat to flat
    final private HexPixelMapping pixelMapping;

    HexTile tiles[];

	int canvasWidth;
	int canvasHeight;

	public double[][] sliceValues;

    int currentSlice = 0;


	// store the smallest and largest value in the data. We need this to map
	// values to colors in the display
	private double minValueInData;
	private double maxValueInData;

	// the colormap of this display and the scale next to the map
	private ColorMapping colormap = new GrayScaleColorMapping();


	private ArrayList<OverlayPlotData> overlays = new ArrayList<>();
//	private Set<Pair<String, Color>> overlayKeys = new HashSet<>();
	Set<CameraPixel> selectedPixels = new LinkedHashSet<>();

	// formater to display doubles nicely
	DecimalFormat fmt = new DecimalFormat("#.##");

//	private boolean patchSelectionMode;

	private boolean drawScaleNumbers = true;

	private boolean includeScale = true;

	private int offsetX = 0;
	private int offsetY = 0;
//
    private final double scale;

	/**
	 * A Hexagon in this case is defined by the passed radius. The radius of the
	 * circle that fits into the hexagon can be calculated by sqrt(3)/2 * (outter radius)
	 * 
	 * @param scale A scaling factor to change the size of the drawn pixels.
	 *
	 */
	public HexMapDisplay(double scale, int canvasWidth, int canvasHeight, HexPixelMapping mapping,
						 boolean mouseAction) {

		Bus.eventBus.register(this);

        this.pixelMapping = mapping;
        this.scale = scale;
		this.canvasHeight = canvasHeight;
		this.canvasWidth = canvasWidth;

		tiles = new HexTile[pixelMapping.getNumberOfPixel()];
		for (int i = 0; i < tiles.length; i++) {
			HexTile t = new HexTile(pixelMapping.getPixelFromId(i), PIXEL_RADIUS, this.scale);
			tiles[i] = t;
		}

		if (mouseAction) {
			// add the mouse listener so we can react to mouse clicks on the hexmap
			this.addMouseListener(this);
		}
	}

	public HexMapDisplay(double scale, int canvasWidth, int canvasHeight, HexPixelMapping mapping) {
		this(scale, canvasWidth, canvasHeight, mapping, true);
	}

	@Override
	public Tile[] getTiles() {
		return tiles;
	}

	@Override
	@Subscribe
	public void handleSliceChangeEvent(SliceChangedEvent ev) {
		// log.debug("Hexmap Selecting slice: {}", ev.currentSlice);
		this.currentSlice = ev.currentSlice;
		this.repaint();
	}

	public void updateOverlays(Set<OverlayPlotData> overlaySet) {

		ArrayList<OverlayPlotData> overlays = new ArrayList<>(overlaySet);

        //TODO Remove baaks dirty hack in here. Meh
		class CustomComparator implements Comparator<OverlayPlotData> {
		    public int compare(OverlayPlotData object1, OverlayPlotData object2) {
		        return object1.getOverlay().getDrawRank() - object2.getOverlay().getDrawRank();
		    }
		}
		// Sortierung in der richtigen Reihenfolge
		// um ueberdeckungen zu vermeiden 
		// von niedrig nach hoch
		Collections.sort(overlays, new CustomComparator());
		this.overlays = overlays;
        this.repaint();
	}

    public void updateImage(int numberOfPixel, int roi, short[][] eventData){
        sliceValues = new double[numberOfPixel][roi];

        minValueInData = Double.MAX_VALUE;
        maxValueInData = Double.MIN_VALUE;
        for (int pixel = 0; pixel < numberOfPixel; pixel++) {
            for (int i = 0; i < roi; i++) {
                short value = eventData[pixel][i];
                sliceValues[pixel][i] = value;
                minValueInData = minValueInData >  value ?  value : minValueInData;
                maxValueInData = maxValueInData <  value ?  value : maxValueInData ;
            }
        }
        this.repaint();
    }


	@Override
	public void setColorMap(ColorMapping m) {
		this.colormap = m;
		this.repaint();
	}

	/**
	 * @see javax.swing.JComponent#paint(java.awt.Graphics)
	 */
	@Override
	public void paint(Graphics g) {
		paint(g, false);
	}

	// The paint method.
	public void paint(Graphics g, boolean transparentBackground) {
		// super.paint(g);
		g.setColor(this.getBackground());
		if (transparentBackground) {
			g.setColor(new Color(255, 255, 255, 0));
		}
		g.fillRect(0, 0, this.getWidth(), this.getHeight());
		int xOffset = getWidth() / 2 + offsetX;
		int yOffset = getHeight() / 2 + offsetY;
		if (g instanceof Graphics2D && sliceValues != null) {

			Graphics2D g2 = (Graphics2D) g;

			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			// draw a grid with lines every 25 pixel in a dark grey color
//			g2.setStroke(new BasicStroke(1.0f));
//			g2.setColor(Color.DARK_GRAY);
//			drawGrid(g2, 25);


			// now draw the actual camera pixel
			// translate to center of canvas
			g2.translate(xOffset, yOffset);
			// rotate 90 degrees counter clockwise
//			g2.rotate(-Math.PI / 2);
			// and draw tiles
			for (Tile tile : tiles) {
				CameraPixel p = tile.getCameraPixel();
				int slice = currentSlice;
				if (currentSlice >= sliceValues[tile.getCameraPixel().id].length) {
					slice = sliceValues[tile.getCameraPixel().id].length - 1;
				}

				double value = sliceValues[tile.getCameraPixel().id][slice];
				tile.setFillColor(this.colormap.getColorFromValue(value, minValueInData, maxValueInData));

				if (selectedPixels.contains(p)) {
					tile.setBorderColor(Color.RED);
				} else {
					tile.setBorderColor(Color.BLACK);
				}
				tile.paint(g);
			}

			// draw all overlays
			for (OverlayPlotData o : overlays) {
				o.getOverlay().paint(g2, this);
			}
			// g2.setStroke(new BasicStroke(1.0f));
			// g2.setColor(Color.WHITE);
			// // undo the rotation
//			g2.rotate(Math.PI / 2);
			// to draw the grid translate back
			g2.translate(-xOffset, -yOffset);

			// draw cross across screen to indicate center of component
            g2.setColor(Color.WHITE);
            g2.drawString("Work In Progress", 50, 50);
            g2.setStroke(new BasicStroke(1.5f));
            Line2D line = new Line2D.Double(0,0, getWidth(),getHeight());
            g2.draw(line);
            line = new Line2D.Double(getWidth(),0,0,getHeight());
            g2.draw(line);

			if (includeScale) {
				g2.translate(this.canvasWidth - 40, 0);
				paintScale(g2, 40);
				g2.translate(-this.canvasWidth + 40, 0);
			}
		}

	}

	private void paintScale(Graphics2D g2, int width) {
		// draw the gradient according to the values returned by the current
		// colormap
		for (int i = 0; i < this.getHeight(); i++) {
			double range = Math.abs(maxValueInData - minValueInData);
			double value = minValueInData + (((double) i) / this.getHeight())
					* range;
			Color c = this.colormap.getColorFromValue(value, minValueInData,
					maxValueInData);
			g2.setColor(c);
			g2.drawLine(20, this.getHeight() - i, width, this.getHeight() - i);
			// draw a number next to the colorbar each 64 pixel
			if (i > 0 && (i % 70) == 0) {
				g2.setColor(Color.GRAY);
				g2.drawString(fmt.format(value), -25, this.getHeight() - i);
			}
		}
		// now draw some numbers next to it

		if (drawScaleNumbers) {
			g2.setColor(Color.WHITE);
			g2.drawString(fmt.format(minValueInData), -25, this.getHeight() - 5);
			g2.drawString(fmt.format(maxValueInData), -25, 10);
		}
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
		if (arg0.getButton() == MouseEvent.BUTTON1) {
			// since we transformed the geometry while painting the polygons
			// above we now have to transform
			// the coordinates of the mouse pointer.
			Point p = arg0.getPoint();
			p.translate(-getWidth() / 2, -getHeight() / 2);
//			AffineTransform rotateInstance = AffineTransform
//					.getRotateInstance(Math.PI / 2);
//			rotateInstance.transform(p, p);

			// In case we want to select wholes patches at a time we save the id
			// of all selected patches in here
//			Set<Integer> selectedPatches = new HashSet<>();

			for (Tile cell : tiles) {
				if (cell.contains(p)) {
					CameraPixel selectedPixel = cell.getCameraPixel();

					// getting the patch by dividing chid by 9 since there are
					// 1440/9 = 160 patches
//					Integer patch = selectedPixel.chid / 9;

					boolean shiftDown = arg0.isShiftDown();

					// in case shift is being pressed and we clicked a pixel
					// thats already been selected we
					// have to remove it
					if (shiftDown && selectedPixels.contains(selectedPixel)) {
						selectedPixels.remove(selectedPixel);
						// in case we are in patchselection mode we have to
						// unselected the patch belongin to
						// pixel clicked
//						selectedPatches.remove(patch);
//						if (patchSelectionMode) {
//							Iterator<FactCameraPixel> it = selectedPixels
//									.iterator();
//							while (it.hasNext()) {
//								FactCameraPixel pt = it.next();
//								if (pt.chid / 9 == patch) {
//									it.remove();
//								}
//							}
//						}
					} else {
						if (!shiftDown) {
							selectedPixels.clear();
//							selectedPatches.clear();
						}
						selectedPixels.add(selectedPixel);
//						selectedPatches.add(patch);
					}
					break;
				}
			}
			// in patch selectionmode add all the pixels with the right patchid
			// to the selectionset
//			if (patchSelectionMode) {
//				for (Tile cell : tiles) {
//					FactCameraPixel pixel = (FactCameraPixel) cell
//							.getCameraPixel();
//					Integer patch = pixel.chid / 9;
//					if (selectedPatches.contains(patch)) {
//						selectedPixels.add(pixel);
//					}
//				}
//			}
		}
		this.repaint();
		Bus.eventBus.post(selectedPixels);

	}

	@Override
	public void mousePressed(MouseEvent e) {
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	// The update method. to update.
	@Override
	public void update(Graphics g) {
		super.paint(g);
		this.paint(g);
	}

	// --------- swing overrides------------
	/**
	 * @see javax.swing.JComponent#getMinimumSize()
	 */
	@Override
	public Dimension getMinimumSize() {
		return new Dimension(getWidth(), getHeight()); // getHeight(),
	}

	/**
	 * @see javax.swing.JComponent#getHeight()
	 */
	@Override
	public int getHeight() {
		return this.canvasHeight;
	}

	/**
	 * @see javax.swing.JComponent#getWidth()
	 */
	@Override
	public int getWidth() {
		return this.canvasWidth;
	}

	/**
	 * @see javax.swing.JComponent#getMaximumSize()
	 */
	@Override
	public Dimension getMaximumSize() {
		return getMinimumSize();
	}

	/**
	 * @see javax.swing.JComponent#getPreferredSize()
	 */
	@Override
	public Dimension getPreferredSize() {
		return getMinimumSize();
	}

	public double getTileRadiusInPixels() {
		return PIXEL_RADIUS;
	}

//	public void setPatchSelectionMode(boolean patchSelectionMode) {
//		this.patchSelectionMode = patchSelectionMode;
//	}
}
