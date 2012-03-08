package stat.benchmark;

class UnormalizedROCPoint {
	
	private static final String TO_STRING = "(%s, %s)";
	
	/**
	 * False positives (observed value true, expected false)
	 */
	public final int fp;
	
	/**
	 * True positives (observed and expected values both equals true)
	 */
	public final int tp;
	
	public UnormalizedROCPoint(int fp, int tp) {
		this.fp = fp;
		this.tp = tp;
	}
	
	@Override
	public String toString() {
		return String.format(TO_STRING, this.fp, this.tp);
	}

}
