package parse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import util.Util;

import fol.Domain;
import fol.Predicate;

/**
 * @author Leonardo Castilho Couto
 *
 */
public class ParseDomain {
	// TODO: implement domain parent, to use the same predicate.
	
	private Map<String, Domain> domainMap;
	private Set<Predicate> predicateSet;
	private Set<Domain> domainSet;
	private File domainFile;

	/**
	 * 
	 */
	public ParseDomain() {
		this.predicateSet = new HashSet<Predicate>();
		this.domainMap = new HashMap<String, Domain>();
		this.domainSet = new HashSet<Domain>();
	}

	// If there is already a Set of Predicates and/or Domains, this make sure
	// there will be no duplicate Predicates/Domains.
	public ParseDomain(Set<Predicate> predicates, Set<Domain> domains) {
		this.predicateSet = predicates;
		this.domainSet = domains;
		this.domainMap = Util.toMap(domains);
	}

	// TODO: Check file/lines format?
	public void parse(File DomainFile) throws IOException, FileNotFoundException {
		BufferedReader bf = new BufferedReader(new FileReader(domainFile));
		String line = bf.readLine();
		while(line != null) {
			parseLine(line);
			line = bf.readLine();
		}
		bf.close();		
	}
	
	// TODO: Check line format?
	private void parseLine(String line) {
		String[] tokens = Parse.predicateTokenizer(line);
		String predicateName = tokens[0];
		List<Domain> domainList = new ArrayList<Domain>();
		Domain d;
		// Domains loop
		for (int i = 1; i < tokens.length; i++) {
			if (domainMap.containsKey(tokens[i])) {
				domainList.add(domainMap.get(tokens[i]));
			} else {
				d = new Domain(tokens[i]);
				domainList.add(d);
				domainMap.put(tokens[i], d);
				domainSet.add(d);
			}
		}
		predicateSet.add(new Predicate(predicateName, domainList.toArray(new Domain[domainList.size()])));
	}
	
	/**
	 * @return the predicates Set
	 */
	public Set<Predicate> getPredicates() {
		return predicateSet;
	}

	/**
	 * @return the domains Set
	 */
	public Set<Domain> getDomains() {
		return domainSet;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO: Remove
		File f = new File("/home/leonardo/opt/alchemy/exdata/univ-empty.mln");
		ParseDomain pd = new ParseDomain();
		try {
			pd.parse(f);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("*************");
		for(Predicate p : pd.getPredicates()) {
			System.out.println(p.toString());
		}
		System.out.println("*************");		
		for(Domain d : pd.getDomains()) {
			System.out.println(d.toString());
		}

	}

}
