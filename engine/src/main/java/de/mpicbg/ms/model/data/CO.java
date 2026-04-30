package de.mpicbg.ms.model.data;

/**
 * Author: HongKee Moon (moon@mpi-cbg.de), Scientific Computing Facility
 * Organization: MPI-CBG Dresden
 * Date: January 2017
 */
public class CO extends BA
{
   public CO( double mass )
   {
      super( mass, 0, 0 );
   }

   public boolean validCO()
   {
      return null != samples;
   }
}
