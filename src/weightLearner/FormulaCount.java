package weightLearner;

public class FormulaCount {
	
	public double trueCounts;
	public int totalCounts;
	public int NaNCounts;

	public FormulaCount() {
		init(0.0d, 0, 0);
		
	}
	
	public FormulaCount(double trueCounts, int totalCounts, int NaNCounts) {
		init(trueCounts, totalCounts, NaNCounts);
		
	}
	
	private void init(double trueCounts, int totalCounts, int NaNCounts) {
		this.trueCounts = trueCounts;
		this.totalCounts = totalCounts;
		this.NaNCounts = NaNCounts;
	}
	
	public void addNaNCount() {
		NaNCounts++;
		totalCounts++;
	}
	
	public void addTrueCounts(double d) {
		trueCounts = trueCounts + d;
		totalCounts++;
	}
	
	public void addCounts(FormulaCount fc) {
		trueCounts = trueCounts + fc.trueCounts;
		totalCounts = totalCounts + fc.totalCounts;
		NaNCounts = NaNCounts + fc.NaNCounts;
	}

}
