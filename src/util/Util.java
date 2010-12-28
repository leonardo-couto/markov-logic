package util;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Leonardo Castilho Couto
 *
 */
public class Util {

	// Remove white spaces, tabs, and returns.
	public static String strip(String string) {
		return Util.join(string.split("\\s"));
	}		
	
	// joins a array of strings into a single string.
	public static String join(String[] strings) {
	    StringBuffer sb = new StringBuffer();
	    for (int i=0; i < strings.length; i++) {
	  	    sb.append(strings[i]);
	  	}
	  	return sb.toString();
	}	
	
	// joins a array of strings into a single string.
	public static String join(Object[] objects) {
	    StringBuffer sb = new StringBuffer();
	    for (int i=0; i < objects.length; i++) {
	  	    sb.append(objects[i].toString());
	  	}
	  	return sb.toString();
	}	
	
	// joins a array of strings into a single string
	// with a separator between then.
	public static String join(String[] strings, String separator) {
	    StringBuffer sb = new StringBuffer();
	    for (int i=0; i < strings.length; i++) {
	        if (i != 0) sb.append(separator);
	  	    sb.append(strings[i]);
	  	}
	  	return sb.toString();
	}
	
	// joins a array of strings into a single string
	// with a separator between then.
	public static String join(Object[] objects, String separator) {
	    StringBuffer sb = new StringBuffer();
	    for (int i=0; i < objects.length; i++) {
	        if (i != 0) sb.append(separator);
	  	    sb.append(objects[i].toString());
	  	}
	  	return sb.toString();
	}
	
	// Converts a Set<T extends NameID> into a Map of <String, T>
	public static <T extends NameID> Map<String, T> setToMap(Set<T> tSet) {
		Map<String, T> m = new HashMap<String, T>();
		for(T t : tSet) {
			m.put(t.getName(), t);
		}
		return m;
	}
	
	// put Element T in the List's first position without deleting and adding elements.
	public static <T> void reorder(List<T> tList, T element) {
		int idx = tList.indexOf(element);
		if(idx > 0) {
			T aux = tList.get(0);
			tList.set(0, element);
			T next;
			for(int i = 0; i < idx; i++) {
				next = tList.get(i+1);
				tList.set(i+1, aux);
				aux = next;				
			}
		}
	}
	
	public static double geometricMean(Collection<Integer> values) {
		long n = 1;
		for (Integer v : values) {
			n = n * v.longValue();
		}
		return Math.pow(n, (1/values.size()));
	}
	
	public static double geometricMean(int[] values) {
		long n = 1;
		for (int v : values) {
			n = n * (long) v;
		}
		return Math.pow(n, (1/values.length));
	}
	
	/**
	 * This method computes the machine precision number as the smallest 
	 * floating point number such that 1 + number differs from 1.
	 * 
	 * <p>This method is based on the subroutine machar described in 
	 * 
	 * <p>W. J. Cody, 
	 * MACHAR: A subroutine to dynamically determine machine parameters, 
	 * ACM Transactions on Mathematical Software, 14, 1988, pages 303-311.
	 * 
	 * <p>MINPACK-2 Project. February 1991. 
	 * Argonne National Laboratory and University of Minnesota. 
	 * Brett M. Averick.
	 * 
	 * @return smallest floating point number such that 
	 *  1 + number differs from 1;
	 */
	public static double machinePrecision() {
	    long ibeta,irnd,it,itemp,negep;
	    double a,b,beta,betain,betah,temp,tempa,temp1,
	           zero=0.0,one=1.0,two=2.0;
	    double ddpmeps;
	 
	    //determine ibeta, beta ala malcolm.

	    a = one;
	    b = one;
	    
	    do {
		    a = a + a;
		    temp = a + one;
		    temp1 = temp - a;
	    } while (Double.compare(temp1 - one, zero) == 0);
	    
	    do {
			b = b + b;
			temp = a + b;
			itemp = (int) (temp - a);
		} while (itemp == 0);

	    ibeta = itemp;
	    beta = (double) itemp;
	    
	    //determine it, irnd.
	    
	    it = 0;
	    b = one;
	    
	    do {
			it++;
			b = b*beta;
			temp = b + one;
			temp1 = temp -b;
		} while (Double.compare(temp1 - one, zero) == 0);
	    
	    irnd = 0;
	    betah = beta/two;
	    temp = a + betah;
	    if (Double.compare(temp -a, zero) != 0) {
	    	irnd = 1;
	    }
	    tempa = a + beta;
	    temp = tempa + betah;
	    if ((irnd == 0) && (Double.compare(temp -tempa, zero) != 0)) {
	    	irnd = 2;
	    }
	    
	    //determine ddpmeps.
    
	    negep = it + 3;
	    betain = one/beta;
	    a = one;
	    for (int i = 1; i <= negep; i++) {
		      a = a*betain;
	    }
	    
	    while (true) {
	    	temp = one + a;
	    	if (Double.compare(temp -one, zero) != 0) {
	    		break;
	    	}
	    	a = a*beta;
	    }
	    
	    ddpmeps = a;
	    if ((ibeta == 2) || (irnd == 0)) {
	    	return ddpmeps;
	    }
	    a = (a*(one + a))/two;
	    temp = one + a;
	    if (Double.compare(temp -one, zero) != 0) { 
	    	ddpmeps = a;
	    }
	    
	    return ddpmeps;
	}

}