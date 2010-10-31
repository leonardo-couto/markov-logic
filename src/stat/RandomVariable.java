package stat;

import java.util.Iterator;
import java.util.List;

import util.NameID;

/**
 * @author Leonardo Castilho Couto
 *
 */
public interface RandomVariable<T extends RandomVariable<?>> extends NameID {
	
	//public List<Domain> getDomains();
	
	/**
	 * @return This variable marginal data.
	 */
	public double[] getData();
	
  /**
   * I(this, Y | {}), where I is an independence test between
   * this and Y given an empty set.
   * @return true if both RandomVariables are independent in
   * the absence of other variables.
   */
	public boolean isIndependent(T y);
	
	/**
	 * TODO: refazer esse comentário!
	 * Devolve valores de TNodes na ordem Z_0, Z_1, ... , Z_n, X, Y. Onde
	 * X = this. 
	 * TODO: devolver um iterador!!!
	 * TODO: ver o que fazer quando a variavel nao estiver conectada pelo grafo!
	 * @return This variable marginal data.
	 */
  public Iterator<double[]> getDataIterator(List<T> nodes);
  
  public T emptyVariable();

}
