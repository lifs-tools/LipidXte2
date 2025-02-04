package de.mpicbg.ms.model.data;

import java.util.HashMap;
import java.util.TreeMap;

/**
 * Author: HongKee Moon (moon@mpi-cbg.de), Scientific Computing Facility
 * Organization: MPI-CBG Dresden
 * Date: May 2017
 */
// Sample, SN2-0, SN2-0.5, SN2-1.0
public class Fraction extends TreeMap< Float, TreeMap< String, Float > >
{
   HashMap< String, Float > max = new HashMap<>();
   Float maxCE = 0f, maxInt = 0f;
   boolean containsCo2Loss = false;

   String clazz;
   int index;

   public Fraction( String clazz, int index )
   {
      super();
      this.clazz = clazz;
      this.index = index;
   }

   public String getClazz()
   {
      return clazz;
   }

   public int getIndex()
   {
      return index;
   }

   public void put( Float ce, String key, Float val )
   {
      if ( !containsKey( ce ) )
         put( ce, new TreeMap< String, Float >() );

      get( ce ).put( key, val );
   }

   public Float get( Float ce, String key )
   {
      return get( ce ).get( key );
   }

   public Float getNormalizedValue( Float ce, String key )
   {
      //if( getMax( key ) != null)
      float max = getMax( key );

      return get( ce ).get( key ) / max;
      //else
      //	return get( ce ).get( key );
   }

   public Float getMax( String key )
   {
      {
         forEach( ( k, v ) ->
                 v.forEach( ( s, f ) ->
                 {
                    if ( maxInt < f )
                    {
                       maxCE = k;
                       maxInt = f;
                    }
                 } )
         );

         TreeMap< String, Float > col = get( maxCE );

         if ( col != null )
            col.forEach( ( k, v ) -> max.put( k, v ) );
      }

      if ( max.containsKey( key ) )
         return max.get( key );
      else
         return 1f;
   }

   public Double getAverage( String key )
   {
      return this.values().stream().filter( c -> c.containsKey( key ) ).mapToDouble( c -> c.get( key ) ).average().getAsDouble();
   }

   public TreeMap< Float, Float > getFAData( Float ce )
   {
      TreeMap< Float, Float > data = new TreeMap<>();

      for ( Float f = 0f; f <= 1f; f += 0.5f )
      {
         float v = getNormalizedValue( ce, "SN2-" + f );
         data.put( v, f );
      }

      return data;
   }

   public TreeMap< Float, Float > getCOData( Float ce )
   {
      TreeMap< Float, Float > data = new TreeMap<>();

      for ( Float f = 0f; f <= 1f; f += 0.5f )
      {
         data.put( getNormalizedValue( ce, "CO2:SN2-" + f ), f );
      }

      return data;
   }

   public boolean isContainsCo2Loss()
   {
      return containsCo2Loss;
   }

   public void setContainsCo2Loss( boolean containsCo2Loss )
   {
      this.containsCo2Loss = containsCo2Loss;
   }

   public boolean containsKeys( Float ce, String s )
   {
      if ( containsKey( ce ) )
      {
         return get( ce ).containsKey( s );
      }
      else
         return false;
   }

   public Float getMaxCE()
   {
      return maxCE;
   }
}
