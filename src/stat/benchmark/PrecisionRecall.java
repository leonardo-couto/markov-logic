package stat.benchmark;

import java.util.ArrayList;
import java.util.List;


public class PrecisionRecall {

	private final ROC roc;
	
	public PrecisionRecall(List<InputPoint> points) {
		this.roc = new ROC(points);
	}
	
	public double auc() {
		List<Point> points = this.getCurve();
		return Point.AUC(points);
	}
	
	public List<Point> getCurve() {
		List<UnormalizedROCPoint> rocPoints = this.roc.getUnormalizedPoints();
		int size = rocPoints.size();
		int positive = this.roc.getPositive();
				
		List<Point> curve = new ArrayList<Point>(size);
		curve.add(new Point(0, 1.0));
		
		for (int i = 1; i < size; i++) {
			UnormalizedROCPoint roc = rocPoints.get(i);
			double precision = ((double) roc.tp) / (roc.tp + roc.fp);
			double recall = ((double) roc.tp) / positive;
			Point point = new Point(recall, precision);
			curve.add(point);
		}
		
		return curve;
	}
	
}