package fol.database;

import java.util.HashMap;

import fol.Atom;

public class RealLocalDB extends RealDatabase {

	private final RealDatabase db;
	private final HashMap<Atom, Double> local;
	
	public RealLocalDB(RealDatabase db) {
		this.db = db; 
		this.local = new HashMap<Atom, Double>();
	}

	@Override
	public double valueOf(Atom key) {
		if (this.local.containsKey(key)) {
			return this.local.get(key).doubleValue();
		}
		return this.db.valueOf(key);
	}
	
	@Override
	public void set(Atom key, double value) {
		if (Atom.TRUE == key) return;
		
		Double v;
		if (value == 0.0d) v = RealDB.FALSE;
		else if (value == 1.0d) v = RealDB.TRUE;
		else v = Double.valueOf(value);
		this.local.put(key, v);
	}
	
	@Override
	public RealDB getLocalCopy() {
		return new RealLocalDB(this);
	}
	
}
