/**
 * 
 */
package util;

/**
 * @author leonardo.couto
 *
 */
public class MyException extends RuntimeException {
  
  /**
   * 
   */
  private static final long serialVersionUID = -8229154075778281522L;

  /**
   * 
   */
  public MyException() {
  }
  
  /**
   * @param message
   */
  public MyException(String message) {
    super(message);
  }
  
  /**
   * @param cause
   */
  public MyException(Throwable cause) {
    super(cause);
  }
  
  /**
   * @param message
   * @param cause
   */
  public MyException(String message, Throwable cause) {
    super(message, cause);
  }
  
}
