package de.mpicbg.ms.model;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.commons.math3.util.Precision;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * FragmentCollection holds the collection of all the fragments
 */
public class FragmentCollection
{
   TreeMap< Double, TreeMap< Integer, Fragment > > fragments;
   TreeMap< Integer, TreeMap< Float, Float > > ticMap;
   ArrayList< Float > collisionEnergyList;

   static SimpleDoubleProperty progressProperty;
   static SimpleDoubleProperty sizeProperty;
   private ArrayList< TreeMap< Double, TreeMap< Integer, Fragment > > > subDatasets = new ArrayList<>();

   static
   {
      progressProperty = new SimpleDoubleProperty( 0d );
      sizeProperty = new SimpleDoubleProperty( 0d );
   }

   public FragmentCollection()
   {
      fragments = new TreeMap<>();
      ticMap = new TreeMap<>();
   }

   public void setFragments( TreeMap< Double, TreeMap< Integer, Fragment > > fragments )
   {
      this.fragments = fragments;
   }

   public void setTicMap( TreeMap< Integer, TreeMap< Float, Float > > ticMap )
   {
      this.ticMap = ticMap;
   }

   public ArrayList< TreeMap< Double, TreeMap< Integer, Fragment > > > getSubDatasets()
   {
      return subDatasets;
   }

   public void setSubDatasets( ArrayList< TreeMap< Double, TreeMap< Integer, Fragment > > > subDatasets )
   {
      this.subDatasets = subDatasets;
   }

   public ArrayList< Float > getCollisionEnergyList()
   {
      return collisionEnergyList;
   }

   public void setCollisionEnergyList( ArrayList< Float > collisionEnergyList )
   {
      this.collisionEnergyList = collisionEnergyList;
   }

   public TreeMap< Integer, TreeMap< Float, Float > > getTicMap()
   {
      return ticMap;
   }

   public synchronized void addSubDataset( TreeMap< Double, TreeMap< Integer, Fragment > > l )
   {
      subDatasets.add( l );
   }

   public void clear()
   {
      subDatasets.parallelStream().forEach( c -> c.values().forEach( i -> i.values().forEach( f -> f.clear() ) ) );
      subDatasets.parallelStream().forEach( c -> c.values().forEach( i -> i.values().clear() ) );
      subDatasets.parallelStream().forEach( c -> c.values().clear() );
      subDatasets.clear();

      ticMap.values().clear();
      ticMap.clear();

      fragments.values().forEach( c -> {
         c.values().forEach( i -> i.clear() );
         c.values().clear();
      } );
      fragments.values().clear();

      fragments.clear();
      collisionEnergyList = null;
   }

   public static void setSizeProperty( double sizeProperty )
   {
      FragmentCollection.sizeProperty.set( sizeProperty );
   }

   public static SimpleDoubleProperty progressPropertyProperty()
   {
      return progressProperty;
   }

   public static void setProgressPropertyProperty( double value )
   {
      FragmentCollection.progressProperty.set( value / sizeProperty.doubleValue() );
   }

   public TreeMap< Double, TreeMap< Integer, Fragment > > getFragments()
   {
      return fragments;
   }

   public void addCycles( int cycleCount, CycleCollection cycleCollection, int consecutiveNumber, int mmu )
   {
      LinkedList< CycleCollection.Cycle > cycles = cycleCollection.getCycles();

      setProgressPropertyProperty( cycleCount );

      int digit = 3 - ( int ) Math.log10( mmu );

      if ( collisionEnergyList == null )
      {
         collisionEnergyList = new ArrayList<>( cycleCollection.getRangeSet() );
      }

      for ( CycleCollection.Cycle cycle : cycles )
      {
         for ( Float collisionEnergy : collisionEnergyList )
         {
            double mzValues[];
            float intensityValues[];

            mzValues = MzXMLConverter.extractMzValues( cycle.getScan( collisionEnergy ), null );
            intensityValues = MzXMLConverter.extractIntensityValues( cycle.getScan( collisionEnergy ), null );

            for ( int i = 0; i < mzValues.length; i++ )
            {
               Double mz = Precision.round( mzValues[ i ], digit );

               if ( !fragments.containsKey( mz ) )
               {
                  fragments.put( mz, new TreeMap<>() );
               }

               if ( !fragments.get( mz ).containsKey( cycle.getId() ) )
               {
                  fragments.get( mz ).put( cycle.getId(), new Fragment( mz, collisionEnergyList ) );
               }

               fragments.get( mz ).get( cycle.getId() ).addIntensity( collisionEnergy, intensityValues[ i ] );
            }
         }
      }
      // Here find significant peaks and merge into the highest intensity
      LinkedList< Double > removeList = new LinkedList<>();
      TreeSet< Double > refinedFragments = new TreeSet<>( fragments.keySet() );

      // Find significant peak based on window
      SortedSet< Double > highMzSet = detectHighPeaks( mmu, refinedFragments, removeList );

      // Filtering them by mmu (Milli Mass Tolerance)
      for ( Double mz : highMzSet )
      {
         double min = Precision.round( mz - mmu * 0.001d, digit );
         double max = Precision.round( mz + mmu * 0.001d, digit );
         //			System.out.println("Mz " + mz + ": [" + min + ", " + max + "]");

         removeList.stream().filter( a -> a >= min && a <= max ).forEach( item -> fragments.get( mz ).keySet().stream().filter( cycle -> fragments.get( item ).containsKey( cycle ) ).forEach( cycle -> merge( fragments.get( item ).get( cycle ), fragments.get( mz ).get( cycle ) ) ) );
      }

      //////////////////////////
      // Filtering process
      //////////////////////////
      System.out.println( "Filtering fragments with consecutive value = " + consecutiveNumber );

      for ( Double mz : refinedFragments )
      {
         int consecutiveChecker = 0;

         for ( Integer cycle : fragments.get( mz ).keySet() )
         {
            Fragment fragment = fragments.get( mz ).get( cycle );
            int count = 1;

            for ( Float key : fragment.keys() )
            {
               if ( fragment.get( key ) > 0 )
               {
                  if ( ++count > consecutiveNumber )
                  {
                     consecutiveChecker++;
                     break;
                  }
               }
               else
                  count = 1;
            }
         }

         if ( consecutiveChecker < fragments.get( mz ).size() )
            removeList.add( mz );
      }

      removeList.forEach( refinedFragments::remove );

      // Filtering them by mmu (Milli Mass Tolerance)
      System.out.println( "Merged fragments based on " + mmu + " mmu." );

      // Find significant peak based on window and merge if it is necessary
      detectPeaksAndMerge( mmu, refinedFragments, removeList );

      // Set 0 by using the consecutive check in order to remove noise
      for ( Double mz : refinedFragments )
      {
         for ( Integer cycle : fragments.get( mz ).keySet() )
         {
            Fragment fragment = fragments.get( mz ).get( cycle );
            Float[] keys = fragment.keys().toArray( new Float[] {} );

            int count = 0;
            int index = 0;

            for ( Float key : fragment.keys() )
            {
               if ( fragment.get( key ) > 0 )
               {
                  count++;

                  if ( key.equals( keys[ keys.length - 1 ] ) && count < consecutiveNumber )
                  {
                     fragment.put( key, 0f );
                  }
               }
               else
               {
                  if ( count > 0 && count < consecutiveNumber )
                  {
                     // remove the item until count
                     for ( int i = 0; i <= count; i++ )
                     {
                        fragment.put( keys[ index - i ], 0f );
                     }
                  }
                  count = 0;
               }

               index++;
            }
         }
      }

      System.out.println( "Removed fragments." );
      //		System.out.print( '[' );
      for ( Double mz : removeList )
      {
         //			if(!mz.equals( removeList.getLast() ))
         //				System.out.print( mz + ", " );
         //			else
         //				System.out.print( mz );
         fragments.remove( mz );
      }

      for ( CycleCollection.Cycle cycle : cycles )
      {
         ticMap.put( cycle.getId(), new TreeMap<>() );

         for ( Float collisionEnergy : collisionEnergyList )
         {
            Float sum = 0f;
            for ( Double mz : fragments.keySet() )
            {
               if ( fragments.get( mz ).containsKey( cycle.getId() ) )
                  sum += fragments.get( mz ).get( cycle.getId() ).get( collisionEnergy );
            }

            ticMap.get( cycle.getId() ).put( collisionEnergy, sum );
         }
      }
      //		System.out.println("]");
   }

   private void addTic( TreeMap< Double, TreeMap< Integer, Fragment > > inputFragments )
   {
      TreeMap< Integer, Fragment > ticMap = new TreeMap<>();

      for ( Double mz : inputFragments.keySet() )
      {
         for ( Integer cycle : inputFragments.get( mz ).keySet() )
         {
            if ( !ticMap.containsKey( cycle ) )
               ticMap.put( cycle, new Fragment( 0d, collisionEnergyList ) );

            for ( Float collisionEnergy : collisionEnergyList )
            {
               if ( inputFragments.get( mz ).containsKey( cycle ) )
                  ticMap.get( cycle ).addIntensity( collisionEnergy, inputFragments.get( mz ).get( cycle ).get( collisionEnergy ) );
            }
         }
      }

      inputFragments.put( 0d, ticMap );
   }

   private void addTicOfAverage( TreeMap< Double, Fragment > inputFragments )
   {
      Fragment ticFragment = new Fragment( 0d, collisionEnergyList );

      for ( Float collisionEnergy : collisionEnergyList )
      {
         Float sum = 0f;
         for ( Double mz : inputFragments.keySet() )
         {
            if ( inputFragments.containsKey( mz ) )
               sum += inputFragments.get( mz ).get( collisionEnergy );
         }
         ticFragment.put( collisionEnergy, sum );
      }

      inputFragments.put( 0d, ticFragment );
   }

   private SortedSet< Double > detectHighPeaks( int mmu, SortedSet< Double > refinedFragments, LinkedList< Double > removeList )
   {
      int digit = 3 - ( int ) Math.log10( mmu );

      Double step = 1 / Math.pow( 10, digit );

      SortedSet< Double > sourceFragments = Collections.unmodifiableSortedSet( refinedFragments );
      TreeSet< Double > candidates = new TreeSet<>();
      HashSet< Double > visited = new HashSet<>();
      TreeSet< Double > peaks = new TreeSet<>();

      for ( Double item : sourceFragments )
      {
         // Handle the exceptions
         if ( item == 283.24d || item == 283.26d )
         {
            visited.add( item );
            peaks.add( item );
            continue;
         }

         if ( visited.contains( item ) )
            continue;

         // Otherwise, finding satellite fragments
         Double offset = item;
         while ( sourceFragments.contains( offset ) )
         {
            visited.add( offset );
            offset = Precision.round( offset + step, digit );
         }

         offset = Precision.round( offset - step, digit );

         if ( item < offset )
            candidates.add( item );
      }

      TreeSet< Double > toRemove = new TreeSet<>();

      // Find the highest peak among the satellite fragments
      for ( Double item : candidates )
      {
         TreeMap< Double, Double > maxValues = new TreeMap<>();
         Double offset = item;
         while ( sourceFragments.contains( offset ) )
         {
            Double sum = 0d;
            for ( Integer cycle : fragments.get( offset ).keySet() )
            {
               Fragment fragment = fragments.get( offset ).get( cycle );
               sum += fragment.getTotalIntensity();
            }

            maxValues.put( sum, offset );

            offset = Precision.round( offset + step, digit );
         }

         offset = Precision.round( offset - step, digit );
         if ( item < offset )
         {
            peaks.add( maxValues.get( maxValues.lastKey() ) );
         }

         toRemove.addAll( maxValues.values() );
      }

      // Filtering
      toRemove.removeAll( peaks );

      refinedFragments.removeAll( toRemove );
      removeList.addAll( toRemove );

      return refinedFragments;
   }

   //	private SortedSet< Double > detectHighPeaks( int mmu, SortedSet< Double > refinedFragments, LinkedList< Double > removeList )
   //	{
   //		int digit = 3 - (int) Math.log10( mmu );
   //
   //		Double step = 1 / Math.pow( 10 , digit );
   //
   //		SortedSet< Double > sourceFragments = Collections.unmodifiableSortedSet( refinedFragments );
   //		TreeSet< Double > candidates = new TreeSet<>();
   //		HashSet< Double > visited = new HashSet<>();
   //		TreeSet< Double > peaks = new TreeSet<>();
   //
   //		for( Double item : sourceFragments )
   //		{
   //			// Handle the exceptions
   //			if(item == 283.24d || item == 283.26d)
   //			{
   //				visited.add( item );
   //				peaks.add( item );
   //				continue;
   //			}
   //
   //			if(visited.contains( item )) continue;
   //
   //			Double from = Precision.round( item - mmu / 1000d, digit );
   //			Double to = Precision.round( item + mmu / 1000d, digit );
   //
   //			int count = 0;
   //			for(Double subSetItem : sourceFragments.subSet( from, to ) )
   //			{
   //				visited.add( subSetItem );
   //				count++;
   //			}
   //
   //			if( count > 0 )
   //				candidates.add( item );
   //		}
   //
   //		TreeSet< Double > toRemove = new TreeSet<>();
   //
   //		// Find the highest peak among the satellite fragments
   //		for( Double item : candidates )
   //		{
   //			TreeMap< Double, Double > maxValues = new TreeMap<>();
   //
   //			Double from = item - Precision.round( mmu / 1000d, digit );
   //			Double to = item + Precision.round( mmu / 1000d, digit );
   //
   //			for(Double subSetItem : sourceFragments.subSet( from, to ) )
   //			{
   //				if( peaks.contains(  subSetItem ) ) continue;
   //
   //				Double sum = 0d;
   //				for ( Integer cycle : fragments.get( subSetItem ).keySet() )
   //				{
   //					Fragment fragment = fragments.get( subSetItem ).get( cycle );
   //					sum += fragment.getTotalIntensity();
   //				}
   //
   //				maxValues.put( sum, subSetItem );
   //			}
   //
   //			Double lastKey = maxValues.lastKey();
   //			Double peak = maxValues.get( lastKey );
   //			maxValues.remove( lastKey );
   //
   //			toRemove.addAll( maxValues.values() );
   //			peaks.add( peak  );
   //		}
   //
   //		// Filtering
   //		toRemove.removeAll( peaks );
   //
   //		refinedFragments.removeAll( toRemove );
   //		removeList.addAll( toRemove );
   //		removeList.removeAll( peaks );
   //
   //		return refinedFragments;
   //	}

   private SortedSet< Double > detectPeaksAndMerge( int mmu, SortedSet< Double > refinedFragments, LinkedList< Double > removeList )
   {
      int digit = 3 - ( int ) Math.log10( mmu );

      SortedSet< Double > sourceFragments = Collections.unmodifiableSortedSet( refinedFragments );
      TreeSet< Double > candidates = new TreeSet<>();
      HashSet< Double > visited = new HashSet<>();
      TreeSet< Double > peaks = new TreeSet<>();

      TreeSet< Double > mergingMz = new TreeSet<>();

      for ( Double item : sourceFragments )
      {
         if ( visited.contains( item ) )
            continue;

         Double from = Precision.round( item - mmu / 1000d, digit );
         Double to = Precision.round( item + mmu / 1000d, digit );

         //			System.out.println( "[" + from + ", " + to + "]" );
         int count = 0;
         Double sum = 0d;
         for ( Double subSetItem : sourceFragments.subSet( from, to ) )
         {
            visited.add( subSetItem );
            count++;
            sum += subSetItem;
         }

         if ( count > 0 )
         {
            candidates.add( item );

            // More than 1 mz are found, then we should merge them
            if ( count > 1 )
            {
               for ( Double subSetItem : sourceFragments.subSet( from, to ) )
                  candidates.remove( subSetItem );

               Double newMz = Precision.round( sum / count, digit );
               candidates.add( newMz );
               mergingMz.add( newMz );
            }
         }
      }

      TreeSet< Double > toRemove = new TreeSet<>();

      // Find the highest peak among the satellite fragments
      for ( Double item : candidates )
      {
         TreeMap< Double, Double > maxValues = new TreeMap<>();

         Double from = item - Precision.round( mmu / 1000d, digit );
         Double to = item + Precision.round( mmu / 1000d, digit );

         if ( mergingMz.contains( item ) )
         {
            if ( !fragments.containsKey( item ) )
               fragments.put( item, new TreeMap<>() );

            TreeSet< Double > subSet = new TreeSet<>();
            for ( Double subSetItem : sourceFragments.subSet( from, to ) )
            {
               for ( Integer cycle : fragments.get( subSetItem ).keySet() )
               {
                  Fragment fragment = fragments.get( subSetItem ).get( cycle );

                  if ( !fragments.get( item ).containsKey( cycle ) )
                     fragments.get( item ).put( cycle, new Fragment( item ) );

                  for ( Float intensity : fragment.keys() )
                     fragments.get( item ).get( cycle ).addIntensity( intensity, fragment.get( intensity ) );
               }

               subSet.add( subSetItem );
               toRemove.add( subSetItem );
            }

            peaks.add( item );

            System.out.println( "Merged mz: " + item + " by using : " + subSet );
         }
         else
         {
            for ( Double subSetItem : sourceFragments.subSet( from, to ) )
            {
               if ( peaks.contains( subSetItem ) )
                  continue;

               Double sum = 0d;
               for ( Integer cycle : fragments.get( subSetItem ).keySet() )
               {
                  Fragment fragment = fragments.get( subSetItem ).get( cycle );
                  sum += fragment.getTotalIntensity();
               }

               maxValues.put( sum, subSetItem );
            }

            Double lastKey = maxValues.lastKey();
            Double peak = maxValues.get( lastKey );
            maxValues.remove( lastKey );

            toRemove.addAll( maxValues.values() );
            peaks.add( peak );

            if ( maxValues.values().size() > 0 )
               System.out.println( "Peak mz: " + peak + " among : " + maxValues.values() );
            else
               System.out.println( "Peak mz: " + peak );
         }
      }

      refinedFragments.addAll( peaks );
      refinedFragments.removeAll( toRemove );

      removeList.addAll( toRemove );
      removeList.removeAll( peaks );

      return refinedFragments;
   }

   public TreeMap< Double, TreeMap< Integer, Fragment > > normalize( TreeMap< Double, TreeMap< Integer, Fragment > > data, int firstBasisPoints, int maximaThreshold )
   {
      Float threshold = maximaThreshold / 10f;

      TreeMap< Double, TreeMap< Integer, Fragment > > normalizedTreeMap = new TreeMap<>();

      TreeMap< Integer, Float > avgTics = new TreeMap<>();

      // TIC normalize
      for ( Integer cycle : ticMap.keySet() )
      {
         Float sum = 0f;
         for ( int i = 0; i < firstBasisPoints; i++ )
         {
            sum += ticMap.get( cycle ).get( collisionEnergyList.get( i ) );
         }

         avgTics.put( cycle, sum / firstBasisPoints );
      }

      // Apply TIC normalization
      for ( Double mz : data.keySet() )
      {
         normalizedTreeMap.put( mz, new TreeMap<>() );

         for ( Integer cycle : data.get( mz ).keySet() )
         {
            normalizedTreeMap.get( mz ).put( cycle, new Fragment( mz, collisionEnergyList ) );

            Fragment fragment = normalizedTreeMap.get( mz ).get( cycle );

            for ( Float key : fragment.keys() )
            {
               fragment.put( key, 100 * data.get( mz ).get( cycle ).get( key ) / avgTics.get( cycle ) );
            }
         }
      }

      // Relative Intensity Maxima check
      if ( maximaThreshold > 0 )
      {
         HashSet< Double > removeList = new HashSet<>();

         // Remove all fragments with relative intensity maxima is less than threshold
         for ( Double mz : normalizedTreeMap.keySet() )
         {
            for ( Integer cycle : normalizedTreeMap.get( mz ).keySet() )
            {
               if ( normalizedTreeMap.get( mz ).get( cycle ).getMaxMz() < threshold )
                  removeList.add( mz );
            }
         }

         for ( Double mz : removeList )
         {
            normalizedTreeMap.get( mz ).values().forEach( c -> c.clear() );
            normalizedTreeMap.get( mz ).clear();
            normalizedTreeMap.remove( mz );
         }

         // Calculate TIC again
         TreeMap< Integer, TreeMap< Float, Float > > ticMap = new TreeMap<>();

         int cycleNo = 1;
         for ( Integer cycle : avgTics.keySet() )
         {
            ticMap.put( cycleNo, new TreeMap<>() );

            for ( Float collisionEnergy : collisionEnergyList )
            {
               Float sum = 0f;
               for ( Double mz : normalizedTreeMap.keySet() )
               {
                  if ( data.get( mz ).containsKey( cycleNo ) )
                     sum += data.get( mz ).get( cycleNo ).get( collisionEnergy );
               }

               ticMap.get( cycleNo ).put( collisionEnergy, sum );
            }
            cycleNo++;
         }

         // TIC normalize
         for ( Integer cycle : ticMap.keySet() )
         {
            Float sum = 0f;
            for ( int i = 0; i < firstBasisPoints; i++ )
            {
               sum += ticMap.get( cycle ).get( collisionEnergyList.get( i ) );
            }

            avgTics.put( cycle, sum / firstBasisPoints );
         }

         // Apply TIC normalization again
         for ( Double mz : normalizedTreeMap.keySet() )
         {
            for ( Integer cycle : normalizedTreeMap.get( mz ).keySet() )
            {
               Fragment fragment = normalizedTreeMap.get( mz ).get( cycle );

               for ( Float key : fragment.keys() )
               {
                  fragment.put( key, 100 * data.get( mz ).get( cycle ).get( key ) / avgTics.get( cycle ) );
               }
            }
         }

         addTic( normalizedTreeMap );

         //
         //			currentTicInformation = new TreeMap<>();
         //			for( Integer cycle : ticMap.keySet() )
         //			{
         //				currentTicInformation.put(cycle, new TreeMap<>());
         //
         //				for( Float collisionEnergy : collisionEnergyList)
         //				{
         //					currentTicInformation.get( cycle ).put( collisionEnergy,
         //							100 * ticMap.get(cycle).get( collisionEnergy ) / avgTics.get( cycle ) );
         //				}
         //			}
      }

      return normalizedTreeMap;
   }

   public TreeMap< Double, TreeMap< Integer, Fragment > > averageWithThreePoints( TreeMap< Double, TreeMap< Integer, Fragment > > data )
   {
      TreeMap< Double, TreeMap< Integer, Fragment > > averagedTreeMap = new TreeMap<>();

      for ( Double mz : data.keySet() )
      {
         if ( mz.equals( 0d ) )
            continue;

         averagedTreeMap.put( mz, new TreeMap<>() );

         for ( Integer cycle : data.get( mz ).keySet() )
         {
            averagedTreeMap.get( mz ).put( cycle, new Fragment( mz, collisionEnergyList ) );

            Fragment fragment = averagedTreeMap.get( mz ).get( cycle );

            Float[] ceList = fragment.keys().toArray( new Float[] {} );
            for ( int i = 1; i < ceList.length - 1; i++ )
               fragment.put( ceList[ i ], 0.25f * data.get( mz ).get( cycle ).get( ceList[ i - 1 ] ) + 0.50f * data.get( mz ).get( cycle ).get( ceList[ i ] ) + 0.25f * data.get( mz ).get( cycle ).get( ceList[ i + 1 ] ) );
         }
      }

      addTic( averagedTreeMap );

      //		TreeMap< Integer, TreeMap<Float, Float> > normTic = currentTicInformation;
      //		currentTicInformation = new TreeMap<>();
      //		for( Integer cycle : ticMap.keySet() )
      //		{
      //			currentTicInformation.put(cycle, new TreeMap<>());
      //
      //			Float[] ceList = collisionEnergyList.toArray( new Float[] { } );
      //			for(int i = 1; i < collisionEnergyList.size() - 1; i++)
      //				currentTicInformation.get( cycle ).put( ceList[i],
      //						0.25f * normTic.get(cycle).get( ceList[i-1] )
      //						+ 0.50f * normTic.get(cycle).get( ceList[i] )
      //						+ 0.25f * normTic.get(cycle).get( ceList[i+1] ) );
      //		}

      return averagedTreeMap;
   }

   public TreeMap< Double, TreeMap< Integer, Fragment > > averageWithBoxCar( TreeMap< Double, TreeMap< Integer, Fragment > > data, int size )
   {
      TreeMap< Double, TreeMap< Integer, Fragment > > averagedTreeMap = new TreeMap<>();

      for ( Double mz : data.keySet() )
      {
         if ( mz.equals( 0d ) )
            continue;

         averagedTreeMap.put( mz, new TreeMap<>() );

         for ( Integer cycle : data.get( mz ).keySet() )
         {
            averagedTreeMap.get( mz ).put( cycle, new Fragment( mz, collisionEnergyList ) );

            Fragment fragment = averagedTreeMap.get( mz ).get( cycle );

            Float[] ceList = fragment.keys().toArray( new Float[] {} );

            int cnt = 0, mod;
            float[] lasts = new float[ size ];

            for ( int i = 0; i < ceList.length; i++ )
            {
               mod = cnt % size;
               lasts[ mod ] = data.get( mz ).get( cycle ).get( ceList[ i ] );

               if ( i < size )
               {
                  fragment.put( ceList[ i ], lasts[ mod ] );
               }
               else
               {
                  fragment.put( ceList[ i ], avg( lasts ) );
               }
            }
         }
      }

      return averagedTreeMap;
   }

   private float sum( float[] items )
   {
      float s = 0f;
      for ( float f : items )
      {
         s += f;
      }
      return s;
   }

   private float avg( float[] items )
   {
      float s = 0f;
      for ( float f : items )
      {
         s += f;
      }
      return s / items.length;
   }

   public List< TreeMap > removeOutliers( TreeMap< Double, TreeMap< Integer, Fragment > > data, int range )
   {
      TreeMap< Double, TreeMap< Integer, Fragment > > refinedTreeMap = new TreeMap<>();
      TreeMap< Double, TreeMap< Integer, Fragment > > outliers = new TreeMap<>();

      final TreeSet< Double > dataKeys = new TreeSet<>( data.keySet() );
      dataKeys.remove( 0d );

      for ( Double mz : dataKeys )
      {
         refinedTreeMap.put( mz, new TreeMap<>() );

         HashMap< Integer, Double > maxMap = new HashMap<>();

         for ( Integer cycle : data.get( mz ).keySet() )
         {
            refinedTreeMap.get( mz ).put( cycle, new Fragment( mz, collisionEnergyList ) );

            Fragment fragment = refinedTreeMap.get( mz ).get( cycle );
            for ( Float ce : fragment.keys() )
               fragment.put( ce, data.get( mz ).get( cycle ).get( ce ) );

            Double max = fragment.keys().stream().mapToDouble( c -> fragment.get( c ) ).max().getAsDouble();
            maxMap.put( cycle, max );
         }

         try
         {
            for ( Integer cycle : maxMap.keySet() )
            {
               // Average max value except the current cycle
               Double avg = maxMap.keySet().stream().filter( c -> c.intValue() != cycle ).mapToDouble( c -> maxMap.get( c ) ).average().getAsDouble();
               Double max = maxMap.get( cycle ).doubleValue();

               if ( max < avg * ( 1d - range / 100d ) || max > avg * ( 1d + range / 100d ) )
               {
                  System.out.println( "Outlier is detected. " + mz + " : Cycle-" + cycle );
                  System.out.println( "Max: " + max + " Avg: " + avg );

                  Fragment removedFragment = refinedTreeMap.get( mz ).remove( cycle );

                  // Collect removed outliers
                  if ( !outliers.containsKey( mz ) )
                     outliers.put( mz, new TreeMap<>() );

                  outliers.get( mz ).put( cycle, removedFragment );
               }
            }
         }
         catch ( NoSuchElementException e )
         {

         }
      }

      ArrayList< TreeMap > list = new ArrayList<>();
      list.add( refinedTreeMap );
      list.add( outliers );

      // Refined average calculation
      TreeMap< Double, Fragment > refinedAverage = new TreeMap<>();
      TreeMap< Double, TreeMap< Float, Float > > stddev = new TreeMap<>();
      TreeMap< Double, TreeMap< Float, Float > > stderr = new TreeMap<>();

      for ( Double mz : refinedTreeMap.keySet() )
      {
         if ( refinedTreeMap.get( mz ).values().size() > 0 )
         {
            Fragment fragment = new Fragment( mz, collisionEnergyList );
            refinedAverage.put( mz, fragment );

            stddev.put( mz, new TreeMap<>() );
            stderr.put( mz, new TreeMap<>() );

            for ( Float ce : collisionEnergyList )
            {
               float avg = ( float ) refinedTreeMap.get( mz ).values().stream().mapToDouble( c -> c.get( ce ) ).average().getAsDouble();
               fragment.put( ce, avg );

               double var = refinedTreeMap.get( mz ).values().stream().mapToDouble( c -> c.get( ce ) ).map( c -> ( c - avg ) * ( c - avg ) ).sum();

               float standardDev = ( float ) Math.sqrt( var / refinedTreeMap.get( mz ).values().stream().mapToDouble( c -> c.get( ce ) ).count() );
               stddev.get( mz ).put( ce, standardDev );

               stderr.get( mz ).put( ce, standardDev / ( float ) Math.sqrt( refinedTreeMap.get( mz ).values().size() ) );
            }
         }
      }

      list.add( refinedAverage );
      list.add( stddev );
      list.add( stderr );

      addTic( refinedTreeMap );
      addTic( outliers );
      addTicOfAverage( refinedAverage );

      return list;
   }

   public static void merge( Fragment from, Fragment to )
   {
      for ( Float collisionEnergy : to.keys() )
      {
         to.addIntensity( collisionEnergy, from.get( collisionEnergy ) );
      }
   }
}
