package de.mpicbg.ms.model;

import org.apache.commons.lang3.Range;

import java.util.ArrayList;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;

/**
 * Mz/Intensity collection
 */
public class MzIntCollection extends ArrayList< MzInt >
{
   public enum SortingProperty
   {
      MZ, INTENSITY
   }

   public enum SortingDirection
   {
      ASCENDING, DESCENDING
   }

   public MzIntCollection( double[] mzArray, float[] intArray )
   {
      for ( int i = 0; i < mzArray.length; i++ )
      {
         add( new MzInt( mzArray[ i ], intArray[ i ] ) );
      }
   }

   public void sort( SortingProperty sortingProperty, SortingDirection sortingDirection )
   {
      int sign = ( sortingDirection == SortingDirection.ASCENDING ) ? 1 : -1;

      if ( sortingProperty == SortingProperty.MZ )
      {
         Collections.sort( this, ( c1, c2 ) -> sign * c1.getMz().compareTo( c2.getMz() ) );
      }
      else if ( sortingProperty == SortingProperty.INTENSITY )
      {
         Collections.sort( this, ( c1, c2 ) -> sign * c1.getIntensity().compareTo( c2.getIntensity() ) );
      }
   }

   public Range< Double > getMzRange()
   {
      DoubleSummaryStatistics stat = parallelStream().mapToDouble( i -> i.getMz() ).summaryStatistics();
      return Range.between( stat.getMin(), stat.getMax() );
   }

   public Range< Float > getIntRange()
   {
      DoubleSummaryStatistics stat = parallelStream().mapToDouble( i -> i.getIntensity() ).summaryStatistics();
      return Range.between( ( float ) stat.getMin(), ( float ) stat.getMax() );
   }
}
