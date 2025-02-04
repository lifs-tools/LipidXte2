package de.mpicbg.ms.model.data;

import org.apache.commons.math3.util.Precision;

import java.util.Collection;
import java.util.Set;
import java.util.TreeMap;

/**
 * Author: HongKee Moon (moon@mpi-cbg.de), Scientific Computing Facility
 * Organization: MPI-CBG Dresden
 * Date: January 2017
 */
public abstract class BA
{
   protected Double mass;
   final protected Integer carbon;
   final protected Integer doubleBonds;

   protected TreeMap< String, Sample > samples;

   protected BA( double mass, int carbon, int db )
   {
      this.mass = mass;
      this.carbon = carbon;
      this.doubleBonds = db;
   }

   public Double getMass()
   {
      return Precision.round( mass, 2 );
   }

   public Double getActualMass()
   {
      return mass;
   }

   public void setMass( double mass )
   {
      this.mass = mass;
   }

   public Integer getCarbon()
   {
      return carbon;
   }

   public Integer getDoubleBonds()
   {
      return doubleBonds;
   }

   public Collection< Sample > getSamples()
   {
      return samples.values();
   }

   public void addSample( String id, Sample sample )
   {
      if ( null == samples )
         samples = new TreeMap<>();

      samples.put( id, sample );
   }

   public void putSampleIntensity( String id, Float ce, Float intensity )
   {
      if ( null == samples )
         samples = new TreeMap<>();

      if ( !samples.containsKey( id ) )
         samples.put( id, new Sample( id ) );

      samples.get( id ).put( ce, intensity );
   }

   public Sample getSample( String id )
   {
      if ( null == samples )
         return null;
      return samples.get( id );
   }

   public TreeMap< String, Sample > getSampleMap()
   {
      return this.samples;
   }

   public void setSampleMap( TreeMap< String, Sample > samples )
   {
      this.samples = samples;
   }

   public Set< Float > getKeys()
   {
      if ( samples.size() > 0 )
         return samples.get( samples.firstKey() ).getKeys();
      else
         return null;
   }

   public void setOffset( float offset )
   {
      samples.values().stream().forEach( c -> c.setOffset( offset ) );
   }

   @Override public boolean equals( Object o )
   {
      if ( this == o )
         return true;
      if ( o == null || getClass() != o.getClass() )
         return false;

      BA ba = ( BA ) o;

      if ( !mass.equals( ba.mass ) )
         return false;
      if ( !carbon.equals( ba.carbon ) )
         return false;
      return doubleBonds.equals( ba.doubleBonds );
   }

   @Override public int hashCode()
   {
      int result = mass.hashCode();
      result = 31 * result + carbon.hashCode();
      result = 31 * result + doubleBonds.hashCode();
      return result;
   }
}
