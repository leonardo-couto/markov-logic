package stat;

import java.util.List;

import util.NameID;

/**
 * @author Leonardo Castilho Couto
 *
 */
public interface RandomVariable<T extends RandomVariable<T>> extends NameID {
	
	//public List<Domain> getDomains();
	
	public double[] getData();
	
	public double[][] getData(T Y, List<T> Z);
	
}
