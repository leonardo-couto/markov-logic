package stat.convergence;

public class ConvergenceTester {

	private final double precision;
	private final double confidenceLevel;
	private final ZValue z;

	public ConvergenceTester(double confidenceLevel, double precision) {
		this.precision = precision;
		this.confidenceLevel = confidenceLevel;
		this.z = ZValue.getZValue(confidenceLevel);
	}

	// see Haas, Peter J. and Swami, Arun N. - Sequential Sampling (1992)
	// Procedures for Query Size Estimation
	// e*mean*n > z_{p,n}*(variance*n)^1/2
	//
	// z = distribuicao normal (n -> inf) or t-student
	// s = somatorio de f(x, y, ...). (valor desconhecido, a ser estimado)
	// Y = estimativa de s com base nas amostras coletadas
	// epsilon = precisao de Y
	// p = grau de confianca da estimativa
	// n = numero de elementos amostrados (populacao)
	// u = media estimada
	//
	// Queremos:
	//
	// P(|Y-s| <= epsilon*n*u) = p
	public boolean hasConverged(double variance, double mean, int n) {
		variance = Math.max(variance, 0.5 / n);
		double z = this.z.valueOf(n);
		double precision = mean > 1 ? this.precision*mean : this.precision;
		double a = precision*n;
		double b = z*Math.sqrt(variance*n);
		return (Double.compare(a, b) > 0);
	}

	public double getPrecision() {
		return this.precision;
	}

	public double getConfidenceLevel() {
		return this.confidenceLevel;
	}	

}
