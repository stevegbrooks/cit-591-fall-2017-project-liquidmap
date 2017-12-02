import java.util.ArrayList;

/**
 * Converts between world space (degrees) and screen space (pixels).
 * Can be initialized in two different ways (i.e., with two different constructors):
 * 1. With two locations, the positions of which are known in both degrees and pixels.
 * 2. With the upper-left and lower-left corners of a bounding box in degrees, plus an image width.
 * Once the converter is initialized, it can make conversions repeatedly without re-initialization.
 * @author Brian Edwards, Matt Surka
 */
public class Converter {
	private double degLonXPixConvFactor; // deg/pix
	private double degLatYPixConvFactor; // deg/pix
	private double lonAt0X; // deg
	private double latAt0Y; // deg
	
	/**
	 * Creates a PixelPointConv object using two reference Pixel-Point pairs.
	 *
	 * In other words, the conversion is based on two points on a raster image
	 * where you can identify the pixel position and the latitude/longitude. This is
	 * most useful for input images where you can identify two features on the map
	 * and locate them on Google Maps to identify their locations.
	 *
	 * Does not operate well around the poles or on the international dateline.
	 *
	 * @param pt1 First reference Point (lat, lon)
	 * @param px1 First reference Pixel (iX, iY)
	 * @param pt2 Second reference Point (lat, lon)
	 * @param px2 Second reference Pixel (iX, iY)
	 */
	public Converter(Point pt1, Pixel px1, Point pt2, Pixel px2) {
		double deltaLon = (pt2.getLon() - pt1.getLon());
		double deltaX = (px2.getPixelX() - px1.getPixelX());
		double deltaLat = (pt2.getLat() - pt1.getLat());
		double deltaY = (px2.getPixelY() - px1.getPixelY());
		degLonXPixConvFactor = deltaLon / deltaX;
		degLatYPixConvFactor = deltaLat / deltaY;
		lonAt0X = pt1.getLon() - degLonXPixConvFactor * px1.getPixelX();
		latAt0Y = pt1.getLat() - degLatYPixConvFactor * px1.getPixelY();

	}

	/**
	 * Creates a PixelPointConv object using a bounding box on a map defined by
	 * two Points and a width of the image in pixels.
	 *
	 * This will be most useful for defining output images where you can
	 * identify the region you want covered and you know the width of the image.
	 *
	 * Does not operate well around the poles or on the international dateline.
	 *
	 * @param ptUpperLeft Point in the upper-left corner.
	 * @param ptLowerRight Point in the lower-right corner.
	 * @param width The image width.
	 */
	public Converter(Point ptUpperLeft, Point ptLowerRight, int width) {

		double deltaLat = ptUpperLeft.getLat() - ptUpperLeft.getLat();
		double deltaLon = ptUpperLeft.getLon() - ptUpperLeft.getLon();
		degLonXPixConvFactor = deltaLon / width;

		Double meanLat = (ptUpperLeft.getLat() + ptUpperLeft.getLat()) / 2;
		double sizeRatio = Math.cos(Math.toRadians(meanLat));

		int height = new Double(sizeRatio * width * deltaLon / deltaLat)
				.intValue();
		height = Math.abs(height);

		degLatYPixConvFactor = deltaLat / height;
		degLonXPixConvFactor = deltaLon / width;
		latAt0Y = ptUpperLeft.getLat();
		lonAt0X = ptUpperLeft.getLon();
	}
	
	/**
	 * Converts a position in one space (i.e., world or screen) to its corresponding position in the other space.
	 * @param dblLat The latitude in degrees/pixels.
	 * @param dblLon The longitude in degrees/pixels.
	 * @param enumTypeSpaceTo The space to convert to (i.e., world or screen).
	 * @return A pair of doubles (as a double[]) with converted latitude at index [0] and converted longitude at index [1].
	 */
	public double[] convert(double dblLat, double dblLon, Trip.typeSpace enumTypeSpaceTo) {
		double dblLatConverted = 0;
		double dblLonConverted = 0;
		
		switch (enumTypeSpaceTo) {
		case WORLD:
			dblLonConverted = degLonXPixConvFactor * dblLat + lonAt0X;
			dblLatConverted = degLatYPixConvFactor * dblLon + latAt0Y;
		case SCREEN:
			dblLatConverted = (dblLon - lonAt0X) / degLonXPixConvFactor;
			dblLonConverted = (dblLat - latAt0Y) / degLatYPixConvFactor;
			break;
		}
		
		return new double[] {dblLatConverted, dblLonConverted};
	}

	public Pixel getPixelFromPoint(Point point) {
		double[] arrDblLatLon = convert(point.getLat(), point.getLon(), Trip.typeSpace.SCREEN);
		return new Pixel(Mathf.roundToInt(arrDblLatLon[0]), Mathf.roundToInt(arrDblLatLon[1]));
	}

	public Point getPointFromPixel(Pixel pixel) {
		double[] arrDblLatLon = convert(pixel.getPixelX(), pixel.getPixelY(), Trip.typeSpace.WORLD);
		return new Point(arrDblLatLon[0], arrDblLatLon[1]);
	}
	
	public Point getPointFromPoint(Point point, Trip.typeSpace enumTypeSpaceTo) {
		double[] arrDblLatLon = convert(point.getLat(), point.getLon(), enumTypeSpaceTo);
		return new Point(arrDblLatLon[0], arrDblLatLon[1]);
	}
	
	public Trip getTripFromTrip(Trip trip, Trip.typeSpace enumTypeSpaceTo) {
		Trip tripConverted = trip.clone();
		ArrayList<Point> listPoints = tripConverted.getPoints();
		
		for (int i = 0; i < listPoints.size(); i++) {
			listPoints.set(i, getPointFromPoint(listPoints.get(i), enumTypeSpaceTo));
		}
		
		tripConverted.setEnumTypeSpace(enumTypeSpaceTo);
		return tripConverted;
	}
}
