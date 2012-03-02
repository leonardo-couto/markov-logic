package fol.database;

import java.util.HashMap;

import fol.Atom;

public class LocalDB extends SimpleDB {

	private final SimpleDB db;
	private final HashMap<Atom, Boolean> local;
	
	public LocalDB(SimpleDB db) {
		this.db = db; 
		this.local = new HashMap<Atom, Boolean>();
	}

	@Override
	public boolean valueOf(Atom key) {
		if (this.local.containsKey(key)) {
			return this.local.get(key).booleanValue();
		}
		return this.db.valueOf(key);
	}
	
	@Override
	public boolean flip(Atom key) {
		if (Atom.TRUE == key) return true;
		if (this.local.containsKey(key)) {
			boolean value = !this.local.get(key).booleanValue();
			this.local.remove(key);
			return value;
		}
		boolean value = !this.db.valueOf(key);
		this.local.put(key, Boolean.valueOf(value));
		return value;
	}
	
	@Override
	public void set(Atom key, boolean value) {
		if (Atom.TRUE == key) return;
		if (this.local.containsKey(key)) {
			boolean current = this.local.get(key).booleanValue();
			if (value != current) {
				this.local.remove(key);
			}
		} else {
			boolean current = this.db.valueOf(key);
			if (current != value) {
				this.local.put(key, Boolean.valueOf(value));
			}			
		}
	}
	
	@Override
	public Database getLocalCopy() {
		return new LocalDB(this);
	}
	
}
