package stat.benchmark;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class ROC {
	
	private static final double EPSLON = 1e-8;
	
	private final List<InputPoint> points;
	private final int negative;
	private final int positive;
	
	public ROC(List<InputPoint> points) {
		this.points = new ArrayList<InputPoint>(points);
		Collections.sort(this.points, InputPoint.observedComparator(true));
		this.positive = this.getPositive(this.points);
		this.negative = this.points.size() - this.positive;
	}
	
	public double auc() {
		List<Point> points = this.getCurve();
		return Point.AUC(points);
	}
	
	public List<InputPoint> getPoints() {
		return new ArrayList<InputPoint>(this.points);
	}
	
	public int getNegative() {
		return negative;
	}
	
	public int getPositive() {
		return positive;
	}

	private int getPositive(List<InputPoint> points) {
		int positive = 0;
		for (InputPoint point : points) {
			if (point.expected) positive++;
		}
		return positive;
	}
	
	List<UnormalizedROCPoint> getUnormalizedPoints() {
		int size = this.points.size() + 1;
		List<UnormalizedROCPoint> points = new ArrayList<UnormalizedROCPoint>(size);
		
		int fp = 0;
		int tp = 0;
		double lastObserved = Double.MAX_VALUE;
		
		for (InputPoint point : this.points) {
			if (lastObserved > point.observed + EPSLON) { // new point
				UnormalizedROCPoint roc = new UnormalizedROCPoint(fp, tp);
				points.add(roc);
				lastObserved = point.observed;
			}
			
			if (point.expected) tp++;
			else fp++;
			
		}
		
		// last point
		UnormalizedROCPoint roc = new UnormalizedROCPoint(fp, tp);
		points.add(roc);		
		
		return points;		
	}
	
	public List<Point> getCurve() {
		if (positive == 0) throw new ArithmeticException("No expected positive value.");
		if (negative == 0) throw new ArithmeticException("No expected negative value.");
		
		List<UnormalizedROCPoint> points = this.getUnormalizedPoints();
		List<Point> curve = new ArrayList<Point>(points.size());
		
		for (UnormalizedROCPoint point : points) {
			double fpRate = ((double) point.fp) / negative; 
			double tpRate = ((double) point.tp) / positive; 
			Point roc = new Point(fpRate, tpRate);
			curve.add(roc);
		}
		
		return curve;
	}

}
