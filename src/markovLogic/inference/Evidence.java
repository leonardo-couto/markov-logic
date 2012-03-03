package markovLogic.inference;

import java.util.HashMap;
import java.util.Map;

import fol.Atom;
import fol.Predicate;
import fol.database.Database;
import fol.database.SimpleDB;

public class Evidence extends SimpleDB {
	
	private final Map<Predicate, Boolean> evidence;
	private final Database database;
	
	public Evidence(Database database) {
		super();
		this.evidence = new HashMap<Predicate, Boolean>();
		this.database = database;
	}
	
	public Database getDatabase() {
		return this.database;
	}
	
	public boolean flip(Predicate predicate) {
		Boolean evidence = this.evidence.get(predicate);
		boolean value = (evidence == null) || !evidence.booleanValue();
		this.evidence.put(predicate, Boolean.valueOf(value));
		return value;		
	}
	
	public void set(Predicate predicate, boolean value) {
		this.evidence.put(predicate, Boolean.valueOf(value));
	}

	@Override
	public boolean valueOf(Atom a) {
		Boolean evidence = this.evidence.get(a.predicate);
		if (evidence != null && evidence.booleanValue()) return true;
		
		return super.valueOf(a);
	}

}
