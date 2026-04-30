package de.mpicbg.ms.test;

/**
 * Author: HongKee Moon (moon@mpi-cbg.de), Scientific Computing Facility
 * Organization: MPI-CBG Dresden
 * Date: April 2017
 */
public class Bspline
{
   // bspline degree = 2
   // quadratic
   // knot vector { 0, 0, 0, 0.2, 0.4, 0.6, 0.8, 1, 1, 1 }
   //	private
   //	function interpolate(double t, int degree, double[] points, double[] knots, double[] weights, double[] result) {
   //
   //		var i,j,s,l;              // function-scoped iteration variables
   //		var n = points.length;    // points count
   //		var d = points[0].length; // point dimensionality
   //
   //		if(degree < 1) throw new Error('degree must be at least 1 (linear)');
   //		if(degree > (n-1)) throw new Error('degree must be less than or equal to point count - 1');
   //
   //		if(!weights) {
   //			// build weight vector of length [n]
   //			weights = [];
   //			for(i=0; i<n; i++) {
   //				weights[i] = 1;
   //			}
   //		}
   //
   //		if(!knots) {
   //			// build knot vector of length [n + degree + 1]
   //			var knots = [];
   //			for(i=0; i<n+degree+1; i++) {
   //				knots[i] = i;
   //			}
   //		} else {
   //			if(knots.length !== n+degree+1) throw new Error('bad knot vector length');
   //		}
   //
   //		var domain = [
   //		degree,
   //				knots.length-1 - degree
   //  ];
   //
   //		// remap t to the domain where the spline is defined
   //		var low  = knots[domain[0]];
   //		var high = knots[domain[1]];
   //		t = t * (high - low) + low;
   //
   //		if(t < low || t > high) throw new Error('out of bounds');
   //
   //		// find s (the spline segment) for the [t] value provided
   //		for(s=domain[0]; s<domain[1]; s++) {
   //			if(t >= knots[s] && t <= knots[s+1]) {
   //				break;
   //			}
   //		}
   //
   //		// convert points to homogeneous coordinates
   //		var v = [];
   //		for(i=0; i<n; i++) {
   //			v[i] = [];
   //			for(j=0; j<d; j++) {
   //				v[i][j] = points[i][j] * weights[i];
   //			}
   //			v[i][d] = weights[i];
   //		}
   //
   //		// l (level) goes from 1 to the curve degree + 1
   //		var alpha;
   //		for(l=1; l<=degree+1; l++) {
   //			// build level l of the pyramid
   //			for(i=s; i>s-degree-1+l; i--) {
   //				alpha = (t - knots[i]) / (knots[i+degree+1-l] - knots[i]);
   //
   //				// interpolate each component
   //				for(j=0; j<d+1; j++) {
   //					v[i][j] = (1 - alpha) * v[i-1][j] + alpha * v[i][j];
   //				}
   //			}
   //		}
   //
   //		// convert back to cartesian and return
   //		var result = result || [];
   //		for(i=0; i<d; i++) {
   //			result[i] = v[s][i] / v[s][d];
   //		}
   //
   //		return result;
   //	}

   private static final int SEGMENTS = 16;
   private static final double INTERVAL = 1d / ( SEGMENTS - 1 );
   //	private static final int GRADIENT_DEPTH = 20;
   //	private static final int ALPHA_DEPTH = 20;
   //	private static final Color[][] GRADIENTS_ALPHA = new Color[ALPHA_DEPTH][GRADIENT_DEPTH];

   private static final double[] B0 = new double[ SEGMENTS ];
   private static final double[] B1 = new double[ SEGMENTS ];
   private static final double[] B2 = new double[ SEGMENTS ];
   private static final double[] B3 = new double[ SEGMENTS ];

   static
   {
      // Setup gradients green --> red
      //		float gradChange = 1f / GRADIENT_DEPTH;
      //		float a = 0;
      //		float alphaChange = (1f - a) / ALPHA_DEPTH;
      //		for (int i = 0; i < ALPHA_DEPTH; i++) {
      //			float r = 0;
      //			float g = 1;
      //			for (int j = 0; j < GRADIENT_DEPTH; j++) {
      //				GRADIENTS_ALPHA[i][j] = new Color(r, g, 0, a);
      //				r += gradChange;
      //				g -= gradChange;
      //			}
      //			a += alphaChange;
      //		}

      // Precalculate the coefficients
      double t = 0;
      for ( int i = 0; i < SEGMENTS; i++ )
      {
         double t1 = 1 - t;
         double t12 = t1 * t1;
         double t2 = t * t;
         B0[ i ] = t1 * t12;
         B1[ i ] = 3 * t * t12;
         B2[ i ] = 3 * t2 * t1;
         B3[ i ] = t * t2;
         t += INTERVAL;
      }
   }

   private final double[] pX;
   private final double[] pY;
   private final double[] dx;
   private final double[] dy;
   private final int n;
   private int[] txPts;
   private int[] tyPts;

   public Bspline( double[] xval, double[] yval )
   {
      pX = xval;
      pY = yval;

      n = pX.length;
      dx = new double[ n ];
      dy = new double[ n ];
   }

   public double getStartPointX()
   {
      return pX[ 0 ];
   }

   public double getStartPointY()
   {
      return pY[ 0 ];
   }

   public double getEndPointX()
   {
      return pX[ pX.length - 1 ];
   }

   public double getEndPointY()
   {
      return pY[ pY.length - 1 ];
   }

   private void straightenControlPoints( double[] pointsX, double[] pointsY )
   {
      int length = pointsX.length;
      int length1 = length - 1;
      for ( int i = 1; i < length1; i++ )
      {
         double beta = 0;
         double iOverL1 = ( double ) i / length1;
         pointsX[ i ] = ( beta * pointsX[ i ] + ( 1 - beta )
                 * ( pointsX[ 0 ] + iOverL1 * ( pointsX[ length1 ] - pointsX[ 0 ] ) ) );
         pointsY[ i ] = ( beta * pointsY[ i ] + ( 1 - beta )
                 * ( pointsY[ 0 ] + iOverL1 * ( pointsY[ length1 ] - pointsY[ 0 ] ) ) );
      }
   }

   public void findControlPoints( double[] xPts, double[] yPts )
   {
      double[] aX = new double[ n ];
      double[] aY = new double[ n ];
      double[] bI = new double[ n ];

      bI[ 1 ] = -0.25;
      aX[ 1 ] = ( xPts[ 2 ] - xPts[ 0 ] - dx[ 0 ] ) / 4;
      aY[ 1 ] = ( yPts[ 2 ] - yPts[ 0 ] - dy[ 0 ] ) / 4;
      for ( int i = 2; i < n - 1; i++ )
      {
         bI[ i ] = -1 / ( 4 + bI[ i - 1 ] );
         aX[ i ] = -( xPts[ i + 1 ] - xPts[ i - 1 ] - aX[ i - 1 ] ) * bI[ i ];
         aY[ i ] = -( yPts[ i + 1 ] - yPts[ i - 1 ] - aY[ i - 1 ] ) * bI[ i ];
      }
      for ( int i = n - 2; i > 0; i-- )
      {
         dx[ i ] = aX[ i ] + dx[ i + 1 ] * bI[ i ];
         dy[ i ] = aY[ i ] + dy[ i + 1 ] * bI[ i ];
      }
   }

   //	public void draw(Graphics g, Color color) {
   //		g.setColor(color);
   //		int counter = 0;
   //		for (int i = 0; i < n - 1; i++) {
   //			for (int k = 0; k < SEGMENTS; k++) {
   //				counter++;
   //				g.drawLine(txPts[counter - 1], tyPts[counter - 1],
   //						txPts[counter], tyPts[counter]);
   //			}
   //		}
   //	}
   //
   //	public void draw(Graphics g, float alpha) {
   //		if (alpha < 0 || alpha > 1)
   //			throw new IllegalArgumentException(Messages.get("alpha.exception"));
   //		if (n < 2)
   //			return;
   //
   //		int[] pX = Arrays.copyOf(this.pX, this.pX.length);
   //		int[] pY = Arrays.copyOf(this.pY, this.pY.length);
   //		straightenControlPoints(pX, pY);
   //
   //		findControlPoints(pX, pY);
   //
   //		int[] gIndexes = getDistanceProportionalGradientIndexes();
   //
   //		int total = (n - 1) * SEGMENTS;
   //		txPts = new int[total + 1];
   //		tyPts = new int[total + 1];
   //		txPts[0] = pX[0];
   //		tyPts[0] = pY[0];
   //
   //		int counter = 0;
   //		int previousGradIndex = 0;
   //		for (int i = 0; i < n - 1; i++) {
   //			for (int k = 0; k < SEGMENTS; k++) {
   //				counter++;
   //
   //				txPts[counter] = (int) (pX[i] * B0[k] + (pX[i] + dx[i]) * B1[k]
   //						+ (pX[i + 1] - dx[i + 1]) * B2[k] + pX[i + 1] * B3[k]);
   //				tyPts[counter] = (int) (pY[i] * B0[k] + (pY[i] + dy[i]) * B1[k]
   //						+ (pY[i + 1] - dy[i + 1]) * B2[k] + pY[i + 1] * B3[k]);
   //
   //				// Check the distance between two points, and if it is less than
   //				// 2 make the points equal so curve is smooth.
   //				if (Point.distance(txPts[counter - 1], tyPts[counter - 1],
   //						txPts[counter], tyPts[counter]) < 2) {
   //					txPts[counter] = txPts[counter - 1];
   //					tyPts[counter] = tyPts[counter - 1];
   //					continue;
   //				}
   //
   //				int gIndex = (int) ((double) k / SEGMENTS * gIndexes[i]);
   //				int aIndex = (int) (alpha * (ALPHA_DEPTH - 1));
   //				g.setColor(GRADIENTS_ALPHA[aIndex][previousGradIndex + gIndex]);
   //				g.drawLine(txPts[counter - 1], tyPts[counter - 1],
   //						txPts[counter], tyPts[counter]);
   //			}
   //			previousGradIndex += gIndexes[i];
   //		}
   //	}
   //
   //	private int[] getDistanceProportionalGradientIndexes() {
   //		double totalDistance = 0;
   //		double[] distances = new double[n - 1];
   //
   //		for (int i = 0; i < n - 1; i++) {
   //			double d = Point.distance(pX[i], pY[i], pX[i + 1], pY[i + 1]);
   //			totalDistance += d;
   //			distances[i] = d;
   //		}
   //
   //		int total = 0;
   //		int[] gIndexes = new int[n - 1];
   //		for (int i = 0; i < n - 1; i++) {
   //			gIndexes[i] = (int) (distances[i] / totalDistance * GRADIENT_DEPTH);
   //			total += gIndexes[i];
   //		}
   //		gIndexes[gIndexes.length - 1] += GRADIENT_DEPTH - total;
   //
   //		return gIndexes;
   //	}
   //
   //	public double distanceFrom(Point point) {
   //		// txPts are null when spline was not drawn on the screen.
   //		if (txPts == null)
   //			return Double.MAX_VALUE;
   //		return distanceFrom(this, point);
   //	}
   //
   //	public static double distanceFrom(Bspline s, Point point) {
   //		if (s.n < 2) {
   //			return point.distance(s.pX[0], s.pY[0]);
   //		}
   //
   //		double min = Double.MAX_VALUE;
   //		for (int i = 0; i < s.txPts.length - 1; i++) {
   //			double dist = Line2D.ptSegDist(s.txPts[i], s.tyPts[i],
   //					s.txPts[i + 1], s.tyPts[i + 1], point.x, point.y);
   //			min = Math.min(min, dist);
   //		}
   //		return min;
   //	}
}
