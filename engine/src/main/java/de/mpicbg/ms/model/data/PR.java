package de.mpicbg.ms.model.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

/**
 * Author: HongKee Moon (moon@mpi-cbg.de), Scientific Computing Facility
 * Organization: MPI-CBG Dresden
 * Date: January 2017
 */
public class PR extends BA
{
   final private String clazz;
   final private String specie;

   private Map< Double, FA > faMap;
   private Map< Double, FAO > faoMap;

   public PR( double mass, String clazz, String specie, int carbon, int db )
   {
      super( mass, carbon, db );
      this.clazz = clazz;
      this.specie = specie;

      faMap = new TreeMap<>();
      faoMap = new TreeMap<>();
   }

   public PR( PR pr )
   {
      this( pr.getMass(), pr.getClazz(), pr.getSpecie(), pr.getCarbon(), pr.getDoubleBonds() );
      setSampleMap( pr.getSampleMap() );
   }

   public String getClazz()
   {
      return clazz;
   }

   public String getSpecie()
   {
      return specie;
   }

   public boolean containFA( Double mz )
   {
      return faMap.containsKey( mz );
   }

   public void addFA( Double mz, FA fa )
   {
      faMap.put( mz, fa );
   }

   public FA getFA( Double mz )
   {
      return faMap.get( mz );
   }

   public Collection< FA > getFAs()
   {
      return faMap.values();
   }

   public void removeFA( Double mz )
   {
      faMap.remove( mz );
   }

   public boolean containFAO( Double mz )
   {
      return faoMap.containsKey( mz );
   }

   public void addFAO( Double mz, FAO fao )
   {
      faoMap.put( mz, fao );
   }

   public FAO getFAO( Double mz )
   {
      return faoMap.get( mz );
   }

   public static ArrayList< PR > splitPR( PR pr )
   {
      int prc = pr.getCarbon();
      int prdb = pr.getDoubleBonds();

      HashMap< Integer, HashSet< FA > > splitMap = new HashMap<>();

      for ( FA fa : pr.getFAs() )
      {
         int fac = fa.getCarbon();
         int fadb = fa.getDoubleBonds();

         if ( fac * 2 == prc && fadb * 2 == prdb )
         {
            int idx = splitMap.size();
            splitMap.put( idx, new HashSet<>() );
            splitMap.get( idx ).add( fa );
         }
         else
         {
            int key = searchComplement( prc, prdb, splitMap, fa );
            splitMap.get( key ).add( fa );
         }
      }

      ArrayList< PR > list = new ArrayList<>();

      for ( Integer key : splitMap.keySet() )
      {
         PR precursor = new PR( pr );

         splitMap.get( key ).forEach( c -> precursor.addFA( c.getMass(), c ) );

         list.add( precursor );
      }

      return list;
   }

   public static Integer searchComplement( int prc, int prdb, HashMap< Integer, HashSet< FA > > splitMap, FA frag )
   {
      final int[] idx = { -1 };

      splitMap.forEach( ( k, v ) ->
      {
         int[] sum = new int[] { 0, 0 };
         v.forEach( c -> {
            sum[ 0 ] += c.getCarbon();
            sum[ 1 ] += c.getDoubleBonds();
         } );

         sum[ 0 ] += frag.getCarbon();
         sum[ 1 ] += frag.getDoubleBonds();

         if ( prc == sum[ 0 ] && prdb == sum[ 1 ] )
            idx[ 0 ] = k;
      } );

      if ( idx[ 0 ] == -1 )
      {
         idx[ 0 ] = splitMap.size();
         splitMap.put( idx[ 0 ], new HashSet<>() );
      }

      return idx[ 0 ];
   }

   @Override public boolean equals( Object o )
   {
      if ( this == o )
         return true;
      if ( o == null || getClass() != o.getClass() )
         return false;
      if ( !super.equals( o ) )
         return false;

      PR pr = ( PR ) o;

      if ( !clazz.equals( pr.clazz ) )
         return false;
      return specie.equals( pr.specie );
   }

   @Override public int hashCode()
   {
      int result = clazz.hashCode();
      result = 31 * result + specie.hashCode();
      return result;
   }

   @Override public String toString()
   {
      return getSpecie();
   }
}
