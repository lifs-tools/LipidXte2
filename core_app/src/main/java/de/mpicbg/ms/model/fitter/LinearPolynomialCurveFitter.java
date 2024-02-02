package de.mpicbg.ms.model.fitter;

import org.apache.commons.math3.exception.MathInternalError;
import org.apache.commons.math3.fitting.AbstractCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math3.linear.DiagonalMatrix;

import java.util.Collection;

/**
 * Created by moon on 9/1/16.
 */
public class LinearPolynomialCurveFitter extends AbstractCurveFitter
{
	/** Parametric function to be fitted. */
	private static final LinearPolynomialFunction.Parametric FUNCTION = new LinearPolynomialFunction.Parametric();
	/** Initial guess. */
	private final double[] initialGuess;
	/** Maximum number of iterations of the optimization algorithm. */
	private final int maxIter;

	/**
	 * Contructor used by the factory methods.
	 *
	 * @param initialGuess Initial guess.
	 * @param maxIter Maximum number of iterations of the optimization algorithm.
	 * @throws org.apache.commons.math3.exception.MathInternalError if {@code initialGuess} is {@code null}.
	 */
	private LinearPolynomialCurveFitter(double[] initialGuess,
			int maxIter) {
		this.initialGuess = initialGuess;
		this.maxIter = maxIter;
	}

	/**
	 * Creates a default curve fitter.
	 * Zero will be used as initial guess for the coefficients, and the maximum
	 * number of iterations of the optimization algorithm is set to
	 * {@link Integer#MAX_VALUE}.
	 *
	 * @param degree Degree of the polynomial to be fitted.
	 * @return a curve fitter.
	 *
	 * @see #withStartPoint(double[])
	 * @see #withMaxIterations(int)
	 */
	public static LinearPolynomialCurveFitter create(int degree) {
		return new LinearPolynomialCurveFitter(new double[degree + 1], Integer.MAX_VALUE);
	}

	/**
	 * Configure the start point (initial guess).
	 * @param newStart new start point (initial guess)
	 * @return a new instance.
	 */
	public LinearPolynomialCurveFitter withStartPoint(double[] newStart) {
		return new LinearPolynomialCurveFitter(newStart.clone(),
				maxIter);
	}

	/**
	 * Configure the maximum number of iterations.
	 * @param newMaxIter maximum number of iterations
	 * @return a new instance.
	 */
	public LinearPolynomialCurveFitter withMaxIterations(int newMaxIter) {
		return new LinearPolynomialCurveFitter(initialGuess,
				newMaxIter);
	}

	/** {@inheritDoc} */
	@Override
	protected LeastSquaresProblem getProblem(Collection<WeightedObservedPoint> observations) {
		// Prepare least-squares problem.
		final int len = observations.size();
		final double[] target  = new double[len];
		final double[] weights = new double[len];

		int i = 0;
		for (WeightedObservedPoint obs : observations) {
			target[i]  = obs.getY();
			weights[i] = obs.getWeight();
			++i;
		}

		final AbstractCurveFitter.TheoreticalValuesFunction model =
				new AbstractCurveFitter.TheoreticalValuesFunction(FUNCTION, observations);

		if (initialGuess == null) {
			throw new MathInternalError();
		}

		// Return a new least squares problem set up to fit a polynomial curve to the
		// observed points.
		return new LeastSquaresBuilder().
				maxEvaluations(Integer.MAX_VALUE).
				maxIterations(maxIter).
				start(initialGuess).
				target(target).
				weight(new DiagonalMatrix(weights)).
				model(model.getModelFunction(), model.getModelFunctionJacobian()).
				build();

	}

}
