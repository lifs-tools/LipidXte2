package de.mpicbg.ms.model.fitter;

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.NonMonotonicSequenceException;
import org.apache.commons.math3.exception.NumberIsTooSmallException;
import org.apache.commons.math3.exception.util.LocalizedFormats;
import org.apache.commons.math3.util.MathArrays;

/**
 * Author: HongKee Moon (moon@mpi-cbg.de), Scientific Computing Facility
 * Organization: MPI-CBG Dresden
 * Date: April 2017
 */
public class WeightedSplineInterpolator extends SplineInterpolator
{
	/**
	 * Computes an interpolating function for the data set.
	 * @param x the arguments for the interpolation points
	 * @param y the values for the interpolation points
	 * @param w the weights for the values
	 * @return a function which interpolates the data set
	 * @throws DimensionMismatchException if {@code x} and {@code y}
	 * have different sizes.
	 * @throws NonMonotonicSequenceException if {@code x} is not sorted in
	 * strict increasing order.
	 * @throws NumberIsTooSmallException if the size of {@code x} is smaller
	 * than 3.
	 */
	public PolynomialSplineFunction interpolate(double x[], double y[], double w[])
			throws DimensionMismatchException,
			NumberIsTooSmallException,
			NonMonotonicSequenceException {
		if (x.length != y.length) {
			throw new DimensionMismatchException(x.length, y.length);
		}

		if (w != null && y.length != w.length) {
			throw new DimensionMismatchException(y.length, w.length);
		}

		if (x.length < 3) {
			throw new NumberIsTooSmallException( LocalizedFormats.NUMBER_OF_POINTS,
					x.length, 3, true);
		}

		// Number of intervals.  The number of data points is n + 1.
		final int n = x.length - 1;

		MathArrays.checkOrder(x);

		double[] sigma = new double[x.length];

		for (int i = 0; i < n; i++) {
			if( w == null )
				sigma[i] = 1.0;
			else
			{
				if ( w[ i ] <= 0.0 )
					sigma[ i ] = 1.0e100;
				else
					sigma[ i ] = 1.0 / Math.sqrt( w[ i ] );
			}
		}

		// Differences between knot points
		final double h[] = new double[n];
		for (int i = 0; i < n; i++) {
			h[i] = x[i + 1] - x[i];
		}

		final double mu[] = new double[n];
		final double z[] = new double[n + 1];
		mu[0] = 0d;
		z[0] = 0d;
		double g = 0;
		for (int i = 1; i < n; i++) {
			g = 2d * (x[i+1]  - x[i - 1]) - h[i - 1] * mu[i -1];
			mu[i] = h[i] / g;
			z[i] = (3d * (y[i + 1] * h[i - 1] - y[i] * (x[i + 1] - x[i - 1])+ y[i - 1] * h[i]) /
					(h[i - 1] * h[i]) - h[i - 1] * z[i - 1]) / g;
		}

		// cubic spline coefficients --  b is linear, c quadratic, d is cubic (original y's are constants)
		final double b[] = new double[n];
		final double c[] = new double[n + 1];
		final double d[] = new double[n];

		z[n] = 0d;
		c[n] = 0d;

		for (int j = n -1; j >=0; j--) {
			c[j] = z[j] - mu[j] * c[j + 1] * sigma[j + 1];
			b[j] = (y[j + 1] - y[j]) / h[j] - h[j] * (c[j + 1] + 2d * c[j]) / 3d;
			d[j] = (c[j + 1] - c[j]) / (3d * h[j]);
		}

		final PolynomialFunction polynomials[] = new PolynomialFunction[n];
		final double coefficients[] = new double[4];
		for (int i = 0; i < n; i++) {
			coefficients[0] = y[i];
			coefficients[1] = b[i];
			coefficients[2] = c[i];
			coefficients[3] = d[i];
			polynomials[i] = new PolynomialFunction(coefficients);
		}

		return new PolynomialSplineFunction(x, polynomials);
	}
}
