package fol.database;

import java.util.Iterator;
import java.util.List;

import markovLogic.weightLearner.wpll.Count;


import fol.Atom;
import fol.Formula;

public interface Database {
	
	public boolean valueOf(Atom a);
	
	/**
	 * Invert the value of Atom a in the database.
	 * 
	 * @param a Atom that will be flip.
	 * @return The new value of a.
	 */
	public boolean flip(Atom a);
	public void set(Atom a, boolean value);
	/**
	 * Allows one to set values without modifying the original Database.
	 * 
	 * Assumes that the original database does not changes while the local
	 * copy is active. Changing the value of the same Atom in both the
	 * local and original copy will generate inconsistencies.
	 */
	public Database getLocalCopy();
	public Iterator<Atom> groundingIterator(Atom filter);
	public Iterator<Atom> groundingIterator(Atom filter, boolean value);
	public int groundingCount(Atom filter);
	public int groundingCount(Atom filter, boolean value);
	
	public List<Count> getCounts(Formula formula, int sampleSize);
	
}
