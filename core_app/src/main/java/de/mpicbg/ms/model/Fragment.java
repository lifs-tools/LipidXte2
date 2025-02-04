package de.mpicbg.ms.model;

import de.mpicbg.ms.model.data.Pos;
import de.mpicbg.ms.model.fitter.ExponentialFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.apache.commons.math3.util.Precision;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

/**
 * Created by moon on 5/24/16.
 */
public class Fragment
{
   Double mz;
   TreeMap< Float, Float > map;
   TreeMap< Float, Float > cfMap;

   Float isomer;
   Integer carbon;
   Integer doubleBond;
   Integer faIndex;

   Pos position;
   Boolean co2loss = false;

   public Fragment()
   {
      this.map = new TreeMap<>();
   }

   public Fragment( Fragment fragment )
   {
      this();

      this.mz = fragment.getMz();
      this.isomer = fragment.getIsomer();
      this.carbon = fragment.getCarbon();
      this.doubleBond = fragment.getDoubleBond();
      this.position = fragment.getPosition();
      this.co2loss = fragment.isCo2loss();

      for ( Float ce : fragment.keys() )
         put( ce, fragment.get( ce ) );
   }

   public Fragment( Double mz, List< Float > collisionList )
   {
      this( mz );
      for ( Float f : collisionList )
         map.put( f, 0f );
   }

   public Fragment( Double mz )
   {
      this();
      this.mz = mz;
   }

   public Fragment( Double mz, Pos position )
   {
      this( mz );
      this.position = position;
   }

   public Fragment( Double mz, Pos position, boolean isCo2Loss )
   {
      this( mz, position );
      this.co2loss = isCo2Loss;
   }

   public void put( Float collisionEnergy, Float intensity )
   {
      map.put( collisionEnergy, intensity );
   }

   public Float get( Float collisionEnergy )
   {
      return map.get( collisionEnergy );
   }

   public void putCF( Float collisionEnergy, Float correctionFactor )
   {
      if ( null == cfMap )
         cfMap = new TreeMap<>();
      cfMap.put( collisionEnergy, correctionFactor );
   }

   public Float getCF( Float collisionEnergy )
   {
      if ( null == cfMap || !cfMap.containsKey( collisionEnergy ) )
         return 0f;
      else
         return cfMap.get( collisionEnergy );
   }

   public void addIntensity( Float collisionEnergy, Float intensity )
   {
      //			System.out.println(mz + ":" + collisionEnergy + "-" + intensity);
      if ( !map.containsKey( collisionEnergy ) )
         map.put( collisionEnergy, 0f );
      map.put( collisionEnergy, get( collisionEnergy ) + intensity );
   }

   public Double getTotalIntensity()
   {
      return map.values().stream().mapToDouble( c -> c.doubleValue() ).sum();
   }

   public Double getMaxMz()
   {
      return map.values().stream().mapToDouble( c -> c.doubleValue() ).max().getAsDouble();
   }

   public Set< Float > keys()
   {
      return map.keySet();
   }

   public Collection< Float > values()
   {
      return map.values();
   }

   public void clear()
   {
      map.clear();
   }

   public Double getMz()
   {
      return mz;
   }

   public void setMz( Double mz )
   {
      this.mz = mz;
   }

   public boolean contains( Float ce )
   {
      return map.containsKey( ce );
   }

   public void remove( Float ce )
   {
      map.remove( ce );
   }

   public void extend()
   {
      Float[] ce = keys().toArray( new Float[] {} );
      Float[] rInt = values().toArray( new Float[] {} );

      // Extrapolate the values when the last intensity is above 5% by using exponential fit
      int noZeroIndex = 0;
      for ( int idx = 1; idx <= rInt.length; idx++ )
      {
         if ( rInt[ rInt.length - idx ] > 0 )
         {
            noZeroIndex = idx;
            break;
         }
      }

      Float rightIntensity = ( noZeroIndex == 0 ) ? 0f : rInt[ rInt.length - noZeroIndex ];
      //System.out.println(getMz() + " : " + rightIntensity);

      if ( rightIntensity > 5f )
      {
         final WeightedObservedPoints obs = new WeightedObservedPoints();

         for ( int idx = noZeroIndex + 5; idx >= noZeroIndex; idx-- )
         {
            obs.add( ce[ rInt.length - idx ], rInt[ rInt.length - idx ] );
         }

         Float lastCE = ce[ rInt.length - noZeroIndex ];
         Float unit = ce[ rInt.length - noZeroIndex ] - ce[ rInt.length - noZeroIndex - 1 ];
         ExponentialFitter right = new ExponentialFitter( obs.toList() );

         for ( int idx = 1; idx <= 5; idx++ )
         {
            Float newCE = lastCE + unit * idx;
            map.put( newCE, ( float ) right.value( newCE ) );
         }
      }
      //
      //		ce = keys().toArray(new Float[]{});
      //		rInt = values().toArray( new Float[] { } );
      //
      //		for(int idx = 0; idx < rInt.length; idx++)
      //		{
      //			if(rInt[idx] == 0f)
      //			{
      //				map.remove( ce[idx] );
      //			}
      //		}
   }

   public ArrayList< Float[] > interpolate()
   {
      extend();
      // Compute the interpolation
      ArrayList< Float[] > arrayList = new ArrayList<>();

      int max = 0;

      Float[] ce = keys().toArray( new Float[] {} );
      Float[] rInt = values().toArray( new Float[] {} );
      Float maxMz = 0f;

      for ( int i = 0; i < ce.length; i++ )
      {
         if ( get( ce[ i ] ) > maxMz )
         {
            max = i;
            maxMz = get( ce[ i ] );
         }
         rInt[ i ] = get( ce[ i ] );
      }

      int size = ce.length;

      float x1 = 0, x2 = 0;
      float y1 = 0, y2, grad, idx = 0;

      for ( float p = 1; p <= 100; p += 1f )
      {
         float perr = 100000f;

         float iInt = 0f, point = 0f, diff;

         for ( int i = 0; i <= max; i++ )
         {
            x2 = ce[ i ];
            y2 = rInt[ i ];

            grad = ( y2 - y1 ) / ( x2 - x1 );

            for ( float w = x1; w <= x2; w += 0.001 )
            {
               float a = grad * ( w - x1 ) + y1;
               float b = 100 * ( a ) / maxMz;
               diff = Math.abs( p - b );

               if ( diff < perr )
               {
                  iInt = a;
                  point = b;
                  idx = w;
                  perr = diff;
               }
            }

            x1 = ce[ i ];
            y1 = rInt[ i ];
         }

         //			System.out.println(idx + "," + iInt + "," + Precision.round( point, 0 ));

         arrayList.add( new Float[] { idx, iInt, Precision.round( point, 0 ) } );
      }

      for ( float p = 99; p > 0; p -= 1.0f )
      {
         float perr = 100000f;

         float iInt = 0f, point = 0f, diff;

         for ( int i = max; i < size; i++ )
         {
            x2 = ce[ i ];
            y2 = rInt[ i ];

            grad = ( y2 - y1 ) / ( x2 - x1 );

            for ( float w = x1; w <= x2; w += 0.001 )
            {
               float a = grad * ( w - x1 ) + y1;
               float b = 100 * ( a ) / maxMz;
               diff = Math.abs( p - b );

               if ( diff < perr )
               {
                  iInt = a;
                  point = b;
                  idx = w;
                  perr = diff;
               }
            }
            x1 = ce[ i ];
            y1 = rInt[ i ];
         }

         //			System.out.println(idx + "," + iInt + "," + Precision.round(point, 0));

         arrayList.add( new Float[] { idx, iInt, Precision.round( point, 0 ) } );
      }

      return arrayList;
   }

   public Float getIsomer()
   {
      return isomer;
   }

   public void setIsomer( Float isomer )
   {
      this.isomer = isomer;
   }

   public Integer getCarbon()
   {
      return carbon;
   }

   public void setCarbon( Integer carbon )
   {
      this.carbon = carbon;
   }

   public Integer getDoubleBond()
   {
      return doubleBond;
   }

   public void setDoubleBond( Integer doubleBond )
   {
      this.doubleBond = doubleBond;
   }

   public Integer getFaIndex()
   {
      return faIndex;
   }

   public void setFaIndex( Integer faIndex )
   {
      this.faIndex = faIndex;
   }

   public TreeMap< Float, Float > getMap()
   {
      return map;
   }

   public void setMap( TreeMap< Float, Float > map )
   {
      this.map = map;
   }

   public Pos getPosition()
   {
      return position;
   }

   public void setPosition( Pos position )
   {
      this.position = position;
   }

   public Boolean isCo2loss()
   {
      return co2loss;
   }

   public void setCo2loss( Boolean co2loss )
   {
      this.co2loss = co2loss;
   }
}
