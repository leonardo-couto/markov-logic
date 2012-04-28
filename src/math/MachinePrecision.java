package math;

public class MachinePrecision {
	
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
	public static double get() {
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
