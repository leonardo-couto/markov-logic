package markovLogic.inference;

import java.util.HashMap;
import java.util.Map;

import fol.Atom;
import fol.Predicate;
import fol.database.BinaryDatabase;
import fol.database.BinaryDB;

public class Evidence {
	
	public static final Evidence NO_EVIDENCE = new Evidence(new BinaryDatabase());
	
	private final Map<Predicate, Boolean> predicates;
	private final Map<Atom, Boolean> atoms;
	
	private BinaryDB database;
	
	public Evidence(BinaryDB database) {
		this.predicates = new HashMap<Predicate, Boolean>();
		this.atoms = new HashMap<Atom, Boolean>();
		this.database = database;
	}
	
	public BinaryDB getDatabase() {
		return this.database;
	}
	
	public void setDatabase(BinaryDB database) {
		this.database = database;
	}
	
	public boolean flip(Predicate predicate) {
		Boolean evidence = this.predicates.get(predicate);
		boolean value = (evidence == null) || !evidence.booleanValue();
		this.predicates.put(predicate, Boolean.valueOf(value));
		return value;		
	}
	
	public boolean flip(Atom atom) {
		Boolean evidence = this.atoms.get(atom);
		boolean value = (evidence == null) || !evidence.booleanValue();
		this.atoms.put(atom, Boolean.valueOf(value));
		return value;		
	}
	
	public void set(Predicate predicate, boolean value) {
		this.predicates.put(predicate, Boolean.valueOf(value));
	}
	
	public void set(Atom atom, boolean value) {
		this.atoms.put(atom, Boolean.valueOf(value));
	}
	
	public boolean isEvidence(Predicate p) {
		Boolean evidence = this.predicates.get(p);
		if (evidence != null && evidence.booleanValue()) return true;
		
		return false;
	}

	public boolean isEvidence(Atom a) {
		Boolean evidence = this.predicates.get(a.predicate);
		if (evidence != null && evidence.booleanValue()) return true;
		
		evidence = this.atoms.get(a);
		if (evidence != null && evidence.booleanValue()) return true;
		
		return false;
	}

}
