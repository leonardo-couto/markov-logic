package stat;

import util.NameID;

/**
 * @author Leonardo Castilho Couto
 *
 */
public interface RandomVariable<T extends RandomVariable<?>> extends NameID {
	
	public Class<? extends Distribution<T>> getDistributionClass();

}
