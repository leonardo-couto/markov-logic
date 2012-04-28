package fol.database;

import fol.Atom;

public interface RealDB {
	
	static final Double TRUE = Double.valueOf(1.0d);
	static final Double FALSE = Double.valueOf(0.0d);
	
	public double valueOf(Atom a);
	
	public void set(Atom a, double value);
	/**
	 * Allows one to set values without modifying the original Database.
	 * 
	 * Assumes that the original database does not changes while the local
	 * copy is active. Changing the value of the same Atom in both the
	 * local and original copy will generate inconsistencies.
	 */
	public RealDB getLocalCopy();
	
}
