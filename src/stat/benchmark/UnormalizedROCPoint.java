package stat.benchmark;

class UnormalizedROCPoint {
	
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

}
