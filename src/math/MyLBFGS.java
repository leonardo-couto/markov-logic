package math;

import java.util.Arrays;

public class MyLBFGS {
	
	private final int length;
	private final double[] hessian;
	private final double[] x0;
	@SuppressWarnings("unused")
	private final ScalarFunction function;
	private final VectorFunction gradient;
	
	public MyLBFGS(double[] x, ScalarFunction function, VectorFunction gradient) {
		this.x0 = x;
		this.function = function;
		this.gradient = gradient;
		this.length = x.length;
		this.hessian = new double[this.length];
		
		this.initHessian();
	}
	
	private void initHessian() {
		Arrays.fill(this.hessian, 1.0);
	}
	
	@SuppressWarnings("unused")
	public double[] minimize() {
		int iteration = 0;
		
		double[] x = Arrays.copyOf(this.x0, this.length);
		double[] grad = this.gradient.g(x);
		double[] direction = new double[this.length];
		double[] sk = new double[this.length];
		double[] yk = new double[this.length];
		double[] lastGrad;
		
		double sk_dot_yk; // sk . yk
		double sk_H_norm; // transpose(sk) x Hessian x sk
		double sk_H; // Hessian x sk
		double[] sk_norm; // sk x sk
		
		
		for (int i = 0; i < this.length; i++) {
			direction[i] = - grad[i];
		}
		
		double gradNorm = vectorNorm(grad);
		System.out.println("GRADIENT NORM: " + gradNorm);
		
		while(iteration < 20) {
			iteration++;
			
			double alpha = this.lineSearch(x, direction);
			// sk = alpha * x
			// x_i+1 = x_i + sk
			for (int i = 0; i < this.length; i++) {
				sk[i] = x[i] * alpha;
				x[i] += sk[i];
			}
			
			lastGrad = grad;
			grad = this.gradient.g(x);
			gradNorm = vectorNorm(grad);
			// yk = grad - lastGrad;
			for (int i = 0; i < this.length; i++) {
				yk[i] = grad[i] - lastGrad[i];
			}
			
			sk_dot_yk = dotProduct(sk, yk);
			sk_H_norm = vectorMatrixNorm(sk, hessian);
			for (int i = 0; i < this.length; i++) {
				sk_H = sk[i] * this.hessian[i];
				this.hessian[i] += yk[i]*yk[i]/sk_dot_yk - sk_H/sk_H_norm;
				direction[i] = - grad[i] / this.hessian[i];
			}
			
			System.out.println(String.format("Iteration %s: x = %s, |grad| = %s",iteration, x, gradNorm));
						
		}
		
		
		return null;
	}
	
	/**
	 * <p>Finds the step alpha that minimizes the function given
	 * an initial point x, and a direction.</p>
	 * alpha = arg min [ f(x + alpha*direction) ]
	 * 
	 * @param x initial point
	 * @param direction direction of search
	 * @return alpha that minimizes the function f(x + alpha*direction)
	 */
	private double lineSearch(double[] x, double[] direction) {
		return 0;
	}
	
	
	private static double vectorNorm(double[] x) {
		double sum = 0;
		for (double d : x) {
			sum += d*d;
		}
		return Math.sqrt(sum);
	}
	
	/**
	 * @param vector 
	 * @param matrix diagonal matrix
	 * @return transpose(vector) x matrix x vector
	 */
	private static double vectorMatrixNorm(double[] vector, double[] matrix) {
		double sum = 0;
		for (int i = 0; i < vector.length; i++) {
			sum += vector[i] * vector[i] * matrix[i];
		}
		return 0;
	}
	
	private static double dotProduct(double[] x, double[] y) {
		double sum = 0;
		for (int i = 0; i < x.length; i++) {
			sum += x[i] * y[i];
		}
		return sum;
	}
	
}
