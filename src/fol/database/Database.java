package fol.database;

import java.util.Iterator;

import fol.Atom;

public interface Database {
	
//	public void close();
	public boolean valueOf(Atom a);
//	public boolean isVariable(Atom a);
	public boolean flip(Atom a);
	public void set(Atom a, boolean value);
//	public void setVariable(Atom a, boolean initialValue);
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
	
}
