package de.mpicbg.ms.model.data;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;

/**
 * Author: HongKee Moon (moon@mpi-cbg.de), Scientific Computing Facility
 * Organization: MPI-CBG Dresden
 * Date: January 2017
 */
public class Sample
{
   private final String id;
   private TreeMap< Float, Float > map;

   public Sample( String id )
   {
      this.id = id;
   }

   public String getId()
   {
      return id;
   }

   public Float getFirstKey()
   {
      return map.firstKey();
   }

   public Set< Float > getKeys()
   {
      return map.keySet();
   }

   public void put( Float collisionEnergy, Float intensity )
   {
      if ( null == map )
         this.map = new TreeMap<>();
      map.put( collisionEnergy, intensity );
   }

   public Float get( Float collisionEnergy )
   {
      if ( null == map )
         return 0f;
      return map.get( collisionEnergy );
   }

   public void setOffset( float offset )
   {
      for ( Float ce : map.keySet().toArray( new Float[] {} ) )
      {
         map.put( ce + offset, map.get( ce ) );
         map.remove( ce );
      }
   }
}
