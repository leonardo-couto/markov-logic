package fol;

/**
 * @author Leonardo Castilho Couto
 *
 */
public abstract class Operator implements Comparable<Operator> {
  // TODO: SUBCLASSES SERAO SINGLETON, TODOS OS ATRIBUTOS FINAL!
  
  protected final int arity;
  protected final String operator;
  public abstract double value(double ... values);
  public abstract newFormula getFormula(newFormula ... formulas);
  
  protected Operator(int arity, String operator) {
    this.arity = arity;
    this.operator = operator;
  }
  
  public int getArity() {
    return this.arity;
  }
  
  public String getOperator() {
    return operator;
  }
  
  public abstract String toString(String ... formulas);

}
