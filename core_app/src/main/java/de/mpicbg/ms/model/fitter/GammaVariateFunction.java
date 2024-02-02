package de.mpicbg.ms.model.fitter;

import org.apache.commons.math3.analysis.ParametricUnivariateFunction;
import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.analysis.differentiation.UnivariateDifferentiableFunction;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.NoDataException;
import org.apache.commons.math3.exception.NullArgumentException;
import org.apache.commons.math3.exception.util.LocalizedFormats;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.MathUtils;

/**
 * GammaVariateFunction
 */
public class GammaVariateFunction implements UnivariateDifferentiableFunction
{
	private final double coefficients[];

	public GammaVariateFunction(double c[])
	{
		super();

		MathUtils.checkNotNull( c );
		int n = c.length;
		if (n != 4) {
			throw new NoDataException( LocalizedFormats.ARGUMENT_OUTSIDE_DOMAIN );
		}

		this.coefficients = new double[4];
		System.arraycopy(c, 0, this.coefficients, 0, 4);
	}

	protected static double evaluate(double[] coefficients, double argument)
			throws NullArgumentException, NoDataException {
		MathUtils.checkNotNull(coefficients);
		int n = coefficients.length;
		if (n == 0) {
			throw new NoDataException(LocalizedFormats.EMPTY_POLYNOMIALS_COEFFICIENTS_ARRAY);
		}

		double a = coefficients[0];
		double b = coefficients[1];
		double c = coefficients[2];
		double d = coefficients[3];

		return b * FastMath.pow( argument - a, c) * FastMath.exp( ( a - argument ) / d );
	}

	@Override public DerivativeStructure value( DerivativeStructure t ) throws DimensionMismatchException
	{
		double a = coefficients[0];
		double b = coefficients[1];
		double c = coefficients[2];
		double d = coefficients[3];

		DerivativeStructure result =
				new DerivativeStructure(t.getFreeParameters(), t.getOrder(), t.getValue());

		DerivativeStructure exp =
				new DerivativeStructure(t.getFreeParameters(), t.getOrder(), t.getValue());

		result = result.subtract( a ).pow( c ).multiply( b );

		result = exp.subtract( a ).signum().divide( d ).exp().multiply( result );

//		t.subtract( a ).pow( c ).multiply( b ).multiply( t.subtract( a ).multiply( -1 ).divide( d ).exp() );

//		double e = FastMath.exp( (a - t.getValue()) / d );
//
//		t.subtract( a ).pow( c - 1 ).multiply( b ).multiply( c ).multiply( e )
//				.subtract( ( b * FastMath.pow( t.getValue() - a, c ) * e ) / d );
		return result;
	}

	@Override public double value( double v )
	{
		return evaluate( coefficients, v );
	}

	public static class Parametric implements ParametricUnivariateFunction
	{
		@Override public double value( double v, double... doubles )
		{
			return GammaVariateFunction.evaluate( doubles, v );
		}

		@Override public double[] gradient( double v, double... doubles )
		{
			final double[] gradient = new double[doubles.length];

			double a = doubles[0];
			double b = doubles[1];
			double c = doubles[2];
			double d = doubles[3];

			double e = FastMath.exp((a - v) / d);
			double pc = FastMath.pow((v - a), c);

			gradient[0] = b * pc * e / d  - b * c * FastMath.pow( (v - a), c - 1 ) * e;
			gradient[1] = pc * e;
			gradient[2] = b * pc * e * FastMath.log( v - a );
			gradient[3] = b * FastMath.pow( ( v - a), c + 1) * e / FastMath.pow( d, 2 );

			return gradient;
		}
	}
}
