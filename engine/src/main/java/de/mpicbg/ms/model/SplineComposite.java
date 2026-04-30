package de.mpicbg.ms.model;

import de.mpicbg.ms.model.fitter.ExponentialFitter;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

/**
 * SplineComposite consists of three functions. The left and right tails are Exponential functions and
 * the middle function is a polynomial spline function which is interpolated by the actual values
 */
public class SplineComposite implements UnivariateFunction
{
   final ExponentialFitter left, right;
   final PolynomialSplineFunction middle;
   final double leftPart, rightPart;

   public SplineComposite( ExponentialFitter left, ExponentialFitter right, PolynomialSplineFunction middle, double leftPart, double rightPart )
   {
      this.left = left;
      this.right = right;
      this.middle = middle;

      this.leftPart = leftPart;
      this.rightPart = rightPart;
   }

   @Override public double value( double x )
   {
      if ( x < leftPart )
      {
         return left.value( x );
      }
      else if ( x > rightPart )
      {
         return right.value( x );
      }
      else
      {
         return middle.value( x );
      }
   }
}
