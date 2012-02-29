package formulaLearner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import fol.Atom;
import fol.Predicate;
import fol.Variable;

public class EqualsFactory {
	
	private final List<Map<Integer,Atom>> equals;
	private final Map<Variable, Integer> variables;
	private int varIndex;
	private final Object lock;
	private static final EqualsFactory eqFactory = new EqualsFactory();
	
	private EqualsFactory() {
		this.equals = new ArrayList<Map<Integer,Atom>>();
		this.variables = new HashMap<Variable, Integer>();
		this.varIndex = 0;
		this.lock = new Object();
	}
	
	public List<Atom> getEquals(Set<Variable> vars) {
		if (vars.isEmpty()) {
			return new LinkedList<Atom>();
		}
		List<Atom> found = new LinkedList<Atom>();
		Integer[] indexes = new Integer[vars.size()];
		Iterator<Variable> it = vars.iterator();
		Variable v = it.next();
		indexes[0] = this.variables.containsKey(v) ? this.variables.get(v) : this.addVar(v);
		for (int i = 1; i < vars.size(); i++) {
			v = it.next();
			if (!this.variables.containsKey(v)) {
				this.addVar(v);
			}
			Integer vi = this.variables.containsKey(v) ? this.variables.get(v) : this.addVar(v);
			indexes[i] = vi;
			for (int j = i-1; j > -1; j--) {
				Atom a;
				if (vi.compareTo(indexes[j]) < 0) {
					a = this.equals.get(vi).get(indexes[j]);
				} else {
					a = this.equals.get(indexes[j]).get(vi);
				}
				if (a != null) { 
					found.add(a);
				}
			}
		}
		return found;
	}
	
	private Integer addVar(Variable v) {
		Integer ret;
		synchronized (this.lock) {
			if (!this.variables.containsKey(v)) {
				ret = this.varIndex;
				this.varIndex++;
				this.variables.put(v, ret);
				this.equals.add(new HashMap<Integer, Atom>());
				for (Entry<Variable, Integer> e : this.variables.entrySet()) {
					if (e.getKey().getDomain().equals(v.getDomain())) {
						this.equals.get(e.getValue()).put(
								ret, new Atom(Predicate.EQUALS, e.getKey(), v));
					}
				}
			} else {
				ret = this.variables.get(v);
			}
		}
		return ret;
	}
	
	public static EqualsFactory get() {
		return EqualsFactory.eqFactory;
	}
	
}
