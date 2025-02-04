package de.mpicbg.ms.model.data;

/**
 * Author: HongKee Moon (moon@mpi-cbg.de), Scientific Computing Facility
 * Organization: MPI-CBG Dresden
 * Date: January 2017
 */
public class FA extends BA
{
   CO co;

   public FA( double mass, int carbon, int db )
   {
      super( mass, carbon, db );
   }

   public CO getCO()
   {
      return co;
   }

   public void setCO( CO co )
   {
      this.co = co;
   }

   public boolean validCO()
   {
      return co.validCO();
   }

   @Override
   public void setOffset( float offset )
   {
      super.setOffset( offset );
      if ( null != co && co.validCO() )
         co.setOffset( offset );
   }
}
