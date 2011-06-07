package parse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import util.Util;
import fol.Atom;
import fol.Constant;
import fol.Domain;
import fol.Predicate;
import fol.Term;

/**
 * @author Leonardo Castilho Couto
 *
 */
public class ParseDataSet {
	
	private Map<String, Predicate> predicateMap;
	private Map<String, Constant> constantMap;
	private Set<Atom> groundings;
	private Set<Constant> constants;
	private File dbFile;

	/**
	 * 
	 */
	public ParseDataSet(File dbFile, Set<Predicate> predicates) {
		this.dbFile = dbFile;
		this.predicateMap = Util.setToMap(predicates);
		this.constants = getConstants(predicates);
		this.constantMap = Util.setToMap(constants);		
		this.groundings = new HashSet<Atom>();
	}
	
	// Get all constants already defined in the domain, if any.
	private Set<Constant> getConstants(Set<Predicate> pSet) {
		Set<Constant> cSet = new HashSet<Constant>();
		Set<Domain> domains = new HashSet<Domain>();
		for(Predicate p : pSet) {
			for(Domain d : p.getDomains()) {
				domains.add(d);
			}
		}
		for(Domain d: domains) {
			cSet.addAll(d);
		}
		return cSet;
	}

	// TODO: Maybe this should be called in the constructor?
	// TODO: Check file/lines format?
	public void parse() throws IOException, FileNotFoundException {
		int lineNumber = 0;
		BufferedReader bf = new BufferedReader(new FileReader(dbFile));
		String line = bf.readLine();
		while(line != null) {
			lineNumber++;
			parseLine(line, lineNumber);
			line = bf.readLine();
		}
		bf.close();		
	}
	
	// TODO: Check line format?
	// line format: [!?]PredicateName(Constant0[,Constant1,...,ConstantN])[ floatValue]
	private void parseLine(String line, int lineNumber) {
		double value = 1.0d;
		boolean negated = false;
		boolean unknown = false;
		Constant c;
		Predicate p;
		List<Domain> domains;
		String mline = line;
		List<Constant> constantList = new ArrayList<Constant>();
		
		// check the first character of line
		switch (line.charAt(0)) {
		case '!':
			negated = true;
			mline = line.substring(1);
			value = 0.0d;
			break;
		case '?':
			unknown = true;
			mline = line.substring(1);
			value = Double.NaN;
			break;
		}
		
		// break into tokens
		String[] tokens = Parse.predicateTokenizer(mline);
		int length = tokens.length;
		
		// get the predicate
		String predicateName = tokens[0];
		p = predicateMap.get(predicateName);
		
		// Check if this predicate has been declared in domain.
		try {
			domains = p.getDomains();
		} catch (NullPointerException e) {
			throw new RuntimeException("Error in file " + dbFile.getName() + 
					" line " + lineNumber + ": " + line + "\nPredicate \"" + 
					predicateName + "\" not declared in domain.");
		}
		
		// check if the last token is a float
		try {
			double d = Double.parseDouble(tokens[length-1]);
			// make sure the value lies between 0 and 1;
			if (Double.compare(d, 0) < 0 || Double.compare(d, 1) > 0) {
				throw new RuntimeException("Format error in file " + dbFile.getName() + 
					" line " + lineNumber + ": " + line + "\nProbability outside range [0,1]");
			}
			if(negated) {
				value = 1.0d - d;
			} else if (!unknown) {
				value = d;
			}
			length = length -1;
		} catch (NumberFormatException e) {
			// last value is not an number
		}

		// Constants loop
		for (int i = 1; i < length; i++) {
			
			// if this constant already exists:
			if (constantMap.containsKey(tokens[i])) {
				// add to constantList and check domain.
				c = constantMap.get(tokens[i]);
				constantList.add(c);
				if (!Domain.in(c, domains.get(i-1))) {
					// TODO: Warning, two domains for the same constant.
					c.getDomain().add(domains.get(i-1));
				}
				
			// else create new constant
			} else {
				c = new Constant(tokens[i], domains.get(i-1));
				constantList.add(c);
				constantMap.put(tokens[i], c);
				constants.add(c);				
			}
			
		}
		
		Term[] terms = constantList.toArray(new Term[constantList.size()]);
		// Test whether this Atom is already defined or not.
		if(!p.hasGrounding(terms)) {
			Atom at = new Atom(p, value, terms);
			this.groundings.add(at);
			p.addGrounding(at);
		} else {
			throw new RuntimeException("Error in file " + dbFile.getName() + 
					" line " + lineNumber + ": " + line + "\nDuplicated entry \"" + 
					p.getGrounding(terms).toString() + "\".");
		}
	}
	
	/**
	 * @return the groundings
	 */
	public Set<Atom> getGroundings() {
		return groundings;
	}

	/**
	 * @return the constants
	 */
	public Set<Constant> getConstants() {
		return constants;
	}

}
