package stat.benchmark;

import java.util.List;

/**
 * Represents a point in two dimensions
 */
public class Point {
	
	private static final String TO_STRING = "(%s, %s)";
	
	public final double x;
	public final double y;
	
	public Point(double x, double y) {
		this.x = x;
		this.y = y;
	}
	
	@Override
	public String toString() {
		return String.format(TO_STRING, x, y);
	}
	
	/**
	 * Evaluates the area under curve, assume ordered points
	 * @param points
	 * @return
	 */
	static double AUC(List<Point> points) {
		double area = 0;
		Point last = points.get(0);
		
		for (Point point : points) {
			if (Double.compare(last.x, point.x) < 0) {
				double base = point.x - last.x;
				double ymin = Math.min(last.y, point.y);
				double ymax = Math.max(last.y, point.y);
				
				area += base * (ymax+ymin) / 2;
			}
			last = point;
		}
		
		return area;
	}

}
