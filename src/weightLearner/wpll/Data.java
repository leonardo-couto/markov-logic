package weightLearner.wpll;

public class Data {
	public final double trueCount;
	public final double falseCount;
	public final double value;

	public Data(double trueCount, double falseCount, double value) {
		this.trueCount = trueCount;
		this.falseCount = falseCount;
		this.value = value;
	}
}
