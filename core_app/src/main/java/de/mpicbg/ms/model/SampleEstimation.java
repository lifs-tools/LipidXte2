package de.mpicbg.ms.model;

import de.mpicbg.ms.db.MasterDatabase;
import de.mpicbg.ms.model.data.BA;
import de.mpicbg.ms.model.data.BARow;
import de.mpicbg.ms.model.data.CO;
import de.mpicbg.ms.model.data.EstSample;
import de.mpicbg.ms.model.data.FA;
import de.mpicbg.ms.model.data.FAAnion;
import de.mpicbg.ms.model.data.Fraction;
import de.mpicbg.ms.model.data.Sample;
import de.mpicbg.ms.model.fitter.ExponentialDecayFunction;
import de.mpicbg.ms.model.fitter.SimpleExponentialFunction;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import org.apache.commons.lang3.Range;
import org.apache.commons.math3.analysis.differentiation.UnivariateDifferentiableFunction;
import org.apache.commons.math3.util.Precision;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static de.mpicbg.ms.model.regression.SimpleRegression.computeRegressionParameters;
import static de.mpicbg.ms.view.pipeline.validation.TxCorrectionTab.tryParseTxFunctionString;

/**
 * Author: HongKee Moon (moon@mpi-cbg.de), Scientific Computing Facility
 * Organization: MPI-CBG Dresden
 * Date: May 2017
 */
@SuppressWarnings( "Duplicates" )
public class SampleEstimation
{
	public static enum TxCorrectionFunc
	{
		ExpDecay,
		SimpleExp
	}

	static TxCorrectionFunc selectedTxCFunction;
	static TreeMap<Float, double[]> txCFunctionParamMap;
	static TreeMap<Float, UnivariateDifferentiableFunction > txCFunctionMap;

	static TreeMap<String, TreeMap< Integer, FAAnion > > masterDBSet;
	static TreeMap<String, Fraction > referenceFAIMap;

	public static TreeMap< String, EstSample > createEstSamples( final MasterDatabase masterDatabase,
			String groupKey, String sampleId, Collection< TreeItem< BARow > > species,
			HashMap< TreeItem< BARow >, BA > baMap, ObservableList< FAAnion > mFaAnionsList, boolean noCorrection )
	{
		final TreeMap< String, Fraction > sampleTreeMap = new TreeMap<>();
		final TreeMap< String, EstSample > estSampleTreeMap = new TreeMap<>();
		final HashMap< String, ArrayList< String > > speciesMap = new HashMap<>();
		final TreeMap< Integer, Fraction > fractionTreeMap = new TreeMap<>();

      // Create reference data
		if ( masterDBSet == null )
		{
			referenceFAIMap = new TreeMap<>();
			masterDBSet = createReferenceFAIMap( masterDatabase, mFaAnionsList, referenceFAIMap );
		}

      // Iterate all the species and split fractions
		for ( TreeItem< BARow > baRowTreeItem : species )
		{
			final String specieName = baRowTreeItem.getValue().getTitle();

			final String clazz = specieName.split( " " )[ 0 ];
			final double priMass = baRowTreeItem.getValue().getMass();

			boolean isSym = baRowTreeItem.getChildren().size() == 1 ? true : false;

			HashSet<String> massSet = new HashSet<>();

			for ( TreeItem< BARow > faTreeItem : baRowTreeItem.getChildren() )
			{
				final String faMz = faTreeItem.getValue().getMassString();
				massSet.add( faMz );
			}

			final String specieId = specieName + " " + massSet.toString();
//			System.out.println( specieId );
			speciesMap.put( specieId, new ArrayList<>() );

			boolean bError = false;
			for ( TreeItem< BARow > faTreeItem : baRowTreeItem.getChildren() )
			{
				final String faMz = faTreeItem.getValue().getMassString();
				final String key = specieName + "-" + faMz;

				final int carbon = faTreeItem.getValue().getCarbon();
				final int db = faTreeItem.getValue().getDb();
				final double mass = Double.parseDouble( faMz );

				final int index = getIndex( mFaAnionsList, mass, carbon, db );

				if(index == 0)
				{
					bError = true;
					continue;
				}

				if ( !sampleTreeMap.containsKey( key ) )
				{
					sampleTreeMap.put( key, new Fraction( clazz, index ) );
				}

				speciesMap.get(specieId).add( key );

				CO co = null;

				if ( faTreeItem.getValue().isCoValid() )
				{
					co = ( ( FA ) baMap.get( faTreeItem ) ).getCO();
				}

				BA fa = baMap.get( faTreeItem );

				Sample sample = fa.getSample( sampleId );

				for ( Float ce : sample.getKeys() )
				{
               // Create sample estimation
					EstSample estSample = new EstSample( sampleId, groupKey, specieName, faMz, ce, getTxCF( priMass, ce ) );

               // Retrieve theoretical intensities with given information
					List<String[]> details = isSym ? (clazz.equals( "PCO" ) || clazz.equals( "PEO" ) ? masterDatabase.getDetails( index, clazz, "0", "1.0", "0" ) : masterDatabase.getDetails( index, clazz, "0", "0", "1.0" )) : masterDatabase.getDetails( index, clazz, "0", "1.0", "0" );

					for ( String[] detailRow : details )
					{
						if ( ce == Float.parseFloat( detailRow[ 0 ] ) )
						{
							Float cInt = sample.get( ce );

							sampleTreeMap.get( key ).put( ce, "Sample", cInt );

							Float cCoi = null == co ? 0f : co.getSample( sampleId ).get( ce );

							if ( cCoi != 0f )
							{
								sampleTreeMap.get( key ).put( ce, "Sample:CO2", cCoi );
							}

                     // Apply TX CF or not
                     estSample.setcFAI( cInt );
                     estSample.addCFRange( Float.parseFloat( detailRow[ 2 ] ) );
                     estSample.setSecondaryCF( estSample.getCF() );
							estSample.setcCOI( cCoi );

                     // Calculate co2 ratio
							estSample.setFaCoRatio( cInt == 0 ? 0 : cCoi / cInt  );

							break;
						}
					}

					estSampleTreeMap.put( key + ce, estSample );
				}

				if(bError) continue;

//            if(key.startsWith( "PI" )) continue;

				final FAAnion faAnion = masterDBSet.get( clazz ).get( index );
            // Get multiple faanions for having different isomer values
				List< FAAnion > faAnions = getMassIndexes( masterDBSet.get( clazz ).values(), faAnion );

            // Initial isomer estimation
            estimateIsomer( masterDatabase, clazz, sample.getKeys(), fractionTreeMap, estSampleTreeMap, faAnions, isSym, key );

            // Initial correction factor
            updateCorrectionFactor( key, masterDatabase, sampleTreeMap, estSampleTreeMap, fractionTreeMap, faAnions, isSym );

            // Complement check and update correction factors for both SN1 and SN2
            if ( speciesMap.get( specieId ).size() > 1 )
            {
               String sn2Key = speciesMap.get( specieId ).get( 1 );

               // When the key is the secondary key of the fragments, we assume sn1 is already built
               if (sn2Key.equals( key )) {
                  updateCorrectionFactorsWithPosition(specieId, masterDatabase, speciesMap, sampleTreeMap, estSampleTreeMap);
               }

//               if(!refPRIMap.containsKey( specieName ))
               estimate2ndPosition( referenceFAIMap, clazz, sample.getKeys(), speciesMap.get( specieId ), estSampleTreeMap );
               estimate3rdPosition( masterDatabase, masterDBSet, referenceFAIMap, clazz, sample.getKeys(), speciesMap.get( specieId ),
                       sampleTreeMap, estSampleTreeMap );
            }
            else
            {
               // for symmetric cases
               for( Float ce : sample.getKeys() )
               {
                  Range<Float> pos = Range.between( 0.5f, 0.5f );
                  estSampleTreeMap.get( key + ce ).setRel_FAI( pos );
                  estSampleTreeMap.get( key + ce ).setSecondaryPosition( pos );
                  estSampleTreeMap.get( key + ce ).setSecondaryRel_FAI( pos );
                  estSampleTreeMap.get( key + ce ).setThirdPosition( pos );
               }
            }
			}
		}

		return estSampleTreeMap;
	}

   public static void updateCorrectionFactorsWithPosition(String specieId, MasterDatabase masterDatabase,
           HashMap< String, ArrayList< String > > speciesMap, TreeMap< String, Fraction > sampleTreeMap,
           TreeMap< String, EstSample > estSampleTreeMap)
   {
      String sn1Key = speciesMap.get( specieId ).get( 0 );
      String sn2Key = speciesMap.get( specieId ).get( 1 );

      // Update SN1 Fraction position
      Fraction fraction = sampleTreeMap.get( sn1Key );

      for ( Float ce : fraction.keySet() )
      {
         Range<Float> range = estSampleTreeMap.get( sn2Key + ce ).getPosition();
         estSampleTreeMap.get( sn1Key + ce ).setPosition( Range.between( 1f - range.getMaximum(), 1f - range.getMinimum() ) );
      }

      // Update SN1 and SN2 correction factors according to the updated position
      for (String snKey : speciesMap.get( specieId )) {
         Fraction snFraction = sampleTreeMap.get( snKey );
         String clazz = snFraction.getClazz();
         int refIndex = snFraction.getIndex();

         final FAAnion faAnion = masterDBSet.get( clazz ).get( refIndex );
         // Get multiple faanions for having different isomer values
         List< FAAnion > faAnions = getMassIndexes( masterDBSet.get( clazz ).values(), faAnion );
         final int FA_db = faAnions.stream().findFirst().get().getFADoubleBonds();

         // TODO: Ask if this logic is correct
         // If sn1 has multiple isomers, the CF needs to be recalculated
         if( FA_db > 0 )
         {
            Float isoMin = null, isoMax = null;

            for ( Float ce : fraction.keySet() )
            {
               Range< Float > isoRange = estSampleTreeMap.get( snKey + ce ).getIsomer();

               if ( null == isoMin )
               {
                  isoMin = isoRange.getMinimum();
                  isoMax = isoRange.getMaximum();
               }

               isoMin = Float.min( isoMin, isoRange.getMinimum() );
               isoMax = Float.max( isoMax, isoRange.getMaximum() );
            }

            for ( Float ce : fraction.keySet() )
            {
               // 3rd method
               TreeMap< Float, TreeMap< Float, Float > > cfMap = new TreeMap<>();
               cfMap.put(0f, new TreeMap<>());
               cfMap.put(0.5f, new TreeMap<>());
               cfMap.put(1f, new TreeMap<>());

               String[][] scopes = new String[][] { { "1.0", "0", "0" }, { "0.5", "0.5", "0" },
                       { "0", "1.0", "0" } };

               for ( FAAnion faItem : faAnions )
               {
                  for ( String[] scope : scopes )
                  {
                     String[] detailRow = masterDatabase.getDetail( ce, faItem.getIndex(), fraction.getClazz(), scope[ 0 ], scope[ 1 ], scope[ 2 ] );

                     float sn2 = Float.parseFloat( scope[ 1 ] );
                     float isomer = faItem.getFAIsomer();
                     float ret = Float.parseFloat( detailRow[ 2 ] );

                     cfMap.get( sn2 ).put( isomer, ret );
                  }
               }

               if( isoMin.equals( isoMax ) ) {
                  // One iso value
                  String[] sn1Row = masterDatabase.getDetail( ce, refIndex, fraction.getClazz(), "1.0", "0", "0" );
                  String[] sn2Row = masterDatabase.getDetail( ce, refIndex, fraction.getClazz(), "0", "1.0", "0" );

                  float cf1 = Float.parseFloat( sn1Row[ 2 ] );
                  float cf2 = Float.parseFloat( sn2Row[ 2 ] );

                  float a = cf2 - cf1;
                  float b = cf1;

                  Range< Float > posRange = estSampleTreeMap.get( snKey + ce ).getPosition();

                  float min = a * posRange.getMinimum() + b;
                  float max = a * posRange.getMaximum() + b;

                  Range< Float > range = Range.between( min, max );
                  //                  System.out.println( "2nd CF:" + nextCfRange + " at " + ce );
                  estSampleTreeMap.get( snKey + ce ).setCF( range );
               }
               else
               {
                  TreeMap< Float, Float > minCF = new TreeMap<>();
                  TreeMap< Float, Float > maxCF = new TreeMap<>();

                  Range< Float > isoRange = Range.between( isoMin, isoMax );

                  for ( float i = 0f; i <= 1; i += 0.5f )
                  {
                     Range< Float > range = getCFValue( cfMap.get( i ), isoRange );
                     minCF.put( i, range.getMinimum() );
                     maxCF.put( i, range.getMaximum() );
                  }

                  Range< Float > posRange = estSampleTreeMap.get( snKey + ce ).getPosition();

                  Range< Float > range = Range.between( getCFValue( minCF, posRange.getMinimum() ), getCFValue( maxCF, posRange.getMaximum() ) );

                  estSampleTreeMap.get( snKey + ce ).setCF( range );
               }
            }
         }
         else
         {
            for ( Float ce : fraction.keySet() )
            {
               String[] sn1Row = masterDatabase.getDetail( ce, refIndex, fraction.getClazz(), "1.0", "0", "0" );
               String[] sn2Row = masterDatabase.getDetail( ce, refIndex, fraction.getClazz(), "0", "1.0", "0" );

               float cf1 = Float.parseFloat( sn1Row[ 2 ] );
               float cf2 = Float.parseFloat( sn2Row[ 2 ] );

               float a = cf2 - cf1;
               float b = cf1;

               Range< Float > posRange = estSampleTreeMap.get( snKey + ce ).getPosition();

               float min = a * posRange.getMinimum() + b;
               float max = a * posRange.getMaximum() + b;

               Range< Float > cfRange = Range.between( min, max );

               System.out.println( "1st CF:" + cfRange + " at " + ce );

               estSampleTreeMap.get( snKey + ce ).setCF( cfRange );
            }
         }
      }
   }

	public static TreeMap<String, TreeMap< Integer, FAAnion > > createReferenceFAIMap( MasterDatabase masterDatabase, ObservableList< FAAnion > mFaAnionsList, TreeMap< String, Fraction > referenceFAIMap )
	{
		TreeMap<String, TreeMap< Integer, FAAnion > > masterDBSet = masterDatabase.getMasterDB();

		for ( String clazz : masterDBSet.keySet() )
		{
			for ( FAAnion faAnion : mFaAnionsList )
			{
				if( masterDBSet.get( clazz ).containsKey( faAnion.getIndex() ) )
				{
					FAAnion dbFaAnion = masterDBSet.get( clazz ).get( faAnion.getIndex() );
					dbFaAnion.setFACarbon( faAnion.getFACarbon() );
					dbFaAnion.setFADoubleBonds( faAnion.getFADoubleBonds() );
               dbFaAnion.setFAIsomer( faAnion.getFAIsomer() );
				}
			}

			int refIndex = getFA_ID( mFaAnionsList, 255.23d );

			referenceFAIMap.put( clazz, new Fraction( clazz, refIndex ) );

			String[][] scopes = new String[][] { { "1.0", "0", "0" }, { "0", "1.0", "0" } };

			for ( String[] scope : scopes )
			{
				for ( String[] detailRow : masterDatabase.getDetails( refIndex, clazz, scope[ 0 ], scope[ 1 ], scope[ 2 ] ) )
				{
					Float ce = Float.parseFloat( detailRow[ 0 ] );
					Float intensity = Float.parseFloat( detailRow[ 1 ] );

					referenceFAIMap.get( clazz ).put( ce, scope[ 1 ], intensity );
				}
			}

			for ( Float ce : referenceFAIMap.get( clazz ).keySet() )
			{
				if ( referenceFAIMap.get( clazz ).containsKeys( ce, "0" ) && referenceFAIMap.get( clazz ).containsKeys( ce, "1.0" ) )
				{
					Float sum = referenceFAIMap.get( clazz ).get( ce, "0" ) + referenceFAIMap.get( clazz ).get( ce, "1.0" );

					referenceFAIMap.get( clazz ).put( ce, "rel0",
							referenceFAIMap.get( clazz ).get( ce, "0" ) / sum );

					referenceFAIMap.get( clazz ).put( ce, "rel1",
							referenceFAIMap.get( clazz ).get( ce, "1.0" ) / sum );
				}
			}
		}

		return masterDBSet;
	}

	public static void processEstSample(String sampleId, ObservableList< TreeItem< BARow > > species,
			HashMap< TreeItem< BARow >, BA > baMap, Map<String, Float> refPRIMap, ObservableList< FAAnion > mFaAnionsList,
			boolean applyTXCF, boolean noCorrection, TreeMap< String, EstSample > estSampleTreeMap, ChartControl control)
	{
		// Compute reference PRI first
		TreeMap<String, Integer> priReferenceCountMap = new TreeMap<>(  );
      TreeMap<String, TreeMap< Float, Float >> priReferenceMap = new TreeMap<>();
      TreeMap<String, TreeMap< Float, Float >> faiReferenceMap = new TreeMap<>();
      TreeMap<String, TreeMap< Float, Float >> priSumMap = new TreeMap<>();

      if (refPRIMap.isEmpty()) {
         String maxBAItem = null;
         Float maxSum = 0f;
         for ( TreeItem< BARow > baRowTreeItem : species )
         {
            BA specie = baMap.get( baRowTreeItem );
            Sample sample = specie.getSample( sampleId );
            Float sum = 0f;
            for ( Float ce : sample.getKeys() )
            {
               sum += sample.get(ce);
            }

            if (sum > maxSum) {
               maxSum = sum;
               maxBAItem = baRowTreeItem.getValue().getTitle();
            }
         }

         refPRIMap.put( maxBAItem, 100f );
      }

		for ( TreeItem< BARow > baRowTreeItem : species )
		{
			final String specieName = baRowTreeItem.getValue().getTitle();
			final String clazz = specieName.split( " ", 2 )[ 0 ];

         if(!priReferenceMap.containsKey( clazz )) priReferenceMap.put( clazz, new TreeMap<>() );

			BA specie = baMap.get( baRowTreeItem );
			Sample sample = specie.getSample( sampleId );

			if(!priSumMap.containsKey( clazz )) priSumMap.put(clazz, new TreeMap<>(  ));

			// Sum up the PRI
			for ( Float ce : sample.getKeys() )
			{
				if ( !priSumMap.get(clazz).containsKey( ce ) )
				{
					priSumMap.get(clazz).put( ce, 0f );
				}

				priSumMap.get(clazz).put( ce, priSumMap.get(clazz).get( ce ) + sample.get( ce ) );
			}

			for ( TreeItem< BARow > faTreeItem : baRowTreeItem.getChildren() )
			{
				final String faMz = faTreeItem.getValue().getMassString();
				final String key = specieName + "-" + faMz;

				for ( Float ce : sample.getKeys() )
				{
					estSampleTreeMap.get( key + ce ).setcFAI( estSampleTreeMap.get( key + ce ).getCorrectedFAI() );
					estSampleTreeMap.get( key + ce ).setcCOI( estSampleTreeMap.get( key + ce ).getCorrectedCOI() );
				}
			}

			// Sum up only for the references
			if ( refPRIMap.containsKey( baRowTreeItem.getValue().getTitle() ) )
			{
				if(! priReferenceCountMap.containsKey( clazz ) ) priReferenceCountMap.put(clazz, 0);
				priReferenceCountMap.put(clazz, priReferenceCountMap.get(clazz) + 1);

				for ( Float ce : sample.getKeys() )
				{
					if ( !priReferenceMap.get(clazz).containsKey( ce ) )
					{
						priReferenceMap.get(clazz).put( ce, 0f );
					}

					priReferenceMap.get(clazz).put( ce, priReferenceMap.get(clazz).get( ce ) + sample.get( ce ) );
				}

				for ( TreeItem< BARow > faTreeItem : baRowTreeItem.getChildren() )
				{
					final String faMz = faTreeItem.getValue().getMassString();
					final String key = specieName + "-" + faMz;

					for ( Float ce : sample.getKeys() )
					{
                  if ( !faiReferenceMap.containsKey( clazz ) ) faiReferenceMap.put( clazz, new TreeMap<>() );

						if ( !faiReferenceMap.get(clazz).containsKey( ce ) )
						{
							faiReferenceMap.get(clazz).put( ce, 0f );
						}

						// Range or not?
						//						Range< Float > range = estSampleTreeMap.get( key + ce ).getSecondDbCorrectedFAI();
						//						Float avg = ( range.getMaximum() + range.getMinimum() ) / 2f;
						//						faiReferenceMap.put( ce, faiReferenceMap.get( ce ) + avg );

                  if(noCorrection)
                  {
                     Float fai = applyTXCF ? estSampleTreeMap.get( key + ce ).getTxCorrectedFAI() : estSampleTreeMap.get( key + ce ).getCorrectedFAI();
                     faiReferenceMap.get(clazz).put( ce, faiReferenceMap.get(clazz).get( ce ) + fai );
                  }
                  else
                  {
//                     Range< Float > range = applyTXCF ? estSampleTreeMap.get( key + ce ).get2ndCFTxCorrectedFAI() : estSampleTreeMap.get( key + ce ).getSecondCFCorrectedFAI();
                     Range< Float > range = applyTXCF ? estSampleTreeMap.get( key + ce ).getCFTxCorrectedFAI() : estSampleTreeMap.get( key + ce ).getCFCorrectedFAI();

                     Float avg = ( range.getMaximum() + range.getMinimum() ) / 2f;
                     faiReferenceMap.get(clazz).put( ce, faiReferenceMap.get(clazz).get( ce ) + avg );
                  }
					}
				}
			}
		}

		// Transmission correction
		TreeMap<String, TreeMap< Double, Fragment >> computedPriFragmentMap = new TreeMap<>();
		LinkedHashMap< FASample, FASample[] > faSampleMap = new LinkedHashMap<>();

		for ( TreeItem< BARow > baRowTreeItem : species )
		{
			BA specie = baMap.get( baRowTreeItem );
			Sample sample = specie.getSample( sampleId );

			// Calculate n_PRI_x
			final String specieName = baRowTreeItem.getValue().getTitle();
			final int pric = baRowTreeItem.getValue().getCarbon();
			final int pridb = baRowTreeItem.getValue().getDb();

			final String clazz = specieName.split( " ", 2 )[ 0 ];
			Double mass = specie.getMass();

			if(!computedPriFragmentMap.containsKey( clazz )) computedPriFragmentMap.put(clazz, new TreeMap<>(  ));

			if(!computedPriFragmentMap.get(clazz).containsKey( mass ))
				computedPriFragmentMap.get(clazz).put( mass, new Fragment( mass ) );

			for ( Float ce : sample.getKeys() )
			{
				Float nPriX = ( sample.get( ce ) / priReferenceMap.get(clazz).get( ce ) ) * priReferenceCountMap.get(clazz);
				computedPriFragmentMap.get(clazz).get( mass ).put( ce, nPriX );
			}

			TreeMap< Float, Range<Float> > faiSum = new TreeMap<>();
			ArrayList< String > masses = new ArrayList<>();

			for ( TreeItem< BARow > faTreeItem : baRowTreeItem.getChildren() )
			{
				final String faMz = faTreeItem.getValue().getMassString();
				final String key = specieName + "-" + faMz;
				masses.add( faMz );

				for ( Float ce : sample.getKeys() )
				{
					if ( !faiSum.containsKey( ce ) )
					{
						faiSum.put( ce, Range.between( 0f, 0f ) );
					}

					Range<Float> sum = faiSum.get( ce );

               if(noCorrection)
               {
                  Float fai = applyTXCF ? estSampleTreeMap.get( key + ce ).getTxCorrectedFAI() : estSampleTreeMap.get( key + ce ).getCorrectedFAI();
                  sum = Range.between( sum.getMinimum() + fai, sum.getMaximum() + fai);
                  faiSum.put( ce, sum );
               }
               else
               {
//                  Range<Float> fai = applyTXCF ? estSampleTreeMap.get( key + ce ).get2ndCFTxCorrectedFAI() : estSampleTreeMap.get( key + ce ).getSecondCFCorrectedFAI();
                  Range<Float> fai = applyTXCF ? estSampleTreeMap.get( key + ce ).getCFTxCorrectedFAI() : estSampleTreeMap.get( key + ce ).getCFCorrectedFAI();
                  sum = Range.between( sum.getMinimum() + fai.getMinimum(), sum.getMaximum() + fai.getMaximum() );
                  faiSum.put( ce, sum );
               }
				}
			}

//			System.out.println( mass + ":" );
			for ( Float ce : faiSum.keySet() )
			{
				Range< Float > range = faiSum.get(ce);
				Float avg = ( range.getMaximum() + range.getMinimum() ) / 2f;

				Float nFaiX = ( avg / faiReferenceMap.get(clazz).get( ce ) ) * priReferenceCountMap.get(clazz);
				Float nPriX = computedPriFragmentMap.get(clazz).get( mass ).get( ce );

//				System.out.println( ce + ", " + nPriX + " / " + nFaiX);

				if ( !nFaiX.equals( 0f ) )
				{
//					System.out.println(computedPriFragmentMap.get( mass ).getCF( ce ));
					if(computedPriFragmentMap.get(clazz).get( mass ).getCF( ce ).equals( 0f )) {
						computedPriFragmentMap.get(clazz).get( mass ).putCF( ce, nPriX / nFaiX );
					}
					else
					{
						float t =  nPriX / computedPriFragmentMap.get(clazz).get( mass ).getCF( ce ) + nFaiX;
//						System.out.println(t);
						computedPriFragmentMap.get(clazz).get( mass ).putCF( ce, nPriX / t );
					}
				}
			}

			// Normalization
			TreeMap< Float, Range< Float > > sumMap = new TreeMap<>();
			TreeMap< Float, Float > noCorrectedSumMap = new TreeMap<>();

			// Split check
			int co2lossCnt = 0;

			for ( Float ce : sample.getKeys() )
			{
				Float min = 0f, max = 0f, sum = 0f;

				for ( TreeItem< BARow > faTreeItem : baRowTreeItem.getChildren() )
				{
					final String faMz = faTreeItem.getValue().getMassString();
					final String key = specieName + "-" + faMz;

               if(noCorrection)
               {
                  Float fa = applyTXCF ? estSampleTreeMap.get( key + ce ).getTxCorrectedFAI() : estSampleTreeMap.get( key + ce ).getCorrectedFAI();
                  min += fa;
                  max += fa;
               }
               else
               {
//                  Range< Float > faC = applyTXCF ? estSampleTreeMap.get( key + ce ).get2ndCFTxCorrectedFAI() : estSampleTreeMap.get( key + ce ).getSecondCFCorrectedFAI();
                  Range< Float > faC = applyTXCF ? estSampleTreeMap.get( key + ce ).getCFTxCorrectedFAI() : estSampleTreeMap.get( key + ce ).getCFCorrectedFAI();

                  min += faC.getMinimum();
                  max += faC.getMaximum();
               }
					sum += applyTXCF ? estSampleTreeMap.get( key + ce ).getTxCorrectedFAI() : estSampleTreeMap.get( key + ce ).getCorrectedFAI();

					if( faTreeItem.getValue().isCoValid() &&
							!estSampleTreeMap.get( key + ce ).getCorrectedCOI().equals( 0f ) ) {
//                  System.out.println(key);
                  co2lossCnt++;
               }

				}

				sumMap.put( ce, Range.between( min, max ) );
				noCorrectedSumMap.put(ce, sum);
			}


			// Create fragment strings
			String priKey = specieName;

			if(priKey.startsWith( "PCO" )) {
				priKey = priKey.replace( "PCO", "PC O-" );
			} else if(priKey.startsWith( "PEO" )) {
				priKey = priKey.replace( "PEO", "PE O-" );
			}
			System.out.println( priKey + " -> " );

			FASample priSample = new FASample( priKey );
			priSample.setSecondKey( masses.toString() );
			priSample.setNCE( sample.getKeys() );

			// Check if isomer split is necessary or not
//         FASample[] samples = getSamples( co2lossCnt > 0, clazz, specieName, priSample.getNCE(), baRowTreeItem, baRowTreeItem.getChildren(), estSampleTreeMap, mFaAnionsList, sumMap, noCorrectedSumMap, noCorrection );
			FASample[] samples = getSamples( false, clazz, specieName, priSample.getNCE(), baRowTreeItem, baRowTreeItem.getChildren(), estSampleTreeMap, mFaAnionsList, sumMap, noCorrectedSumMap, noCorrection );

			final boolean isSym = baRowTreeItem.getChildren().size() == 1;

			faSampleMap.put( priSample, samples );


			// Sum up the PRI
			Float ce = sample.getFirstKey();
			priSample.setMass( mass );
			priSample.setIntensity( sample.get( ce ) );

//			System.out.println("isSym: " + isSym + " Len: " + samples.length);

			// Generate the fraction string
			if ( isSym && samples.length == 1 )
			{
				String sn0Key = String.format( "%s %s/%s", clazz, samples[ 0 ].getKey(), samples[ 0 ].getKey() );

				if(clazz.equals( "PCO" )) {
					BARow r = baRowTreeItem.getChildren().get( 0 ).getValue();
					sn0Key = String.format( "%s %d:%d/%s", "PC O-", pric - r.getCarbon(), pridb - r.getDb(), samples[ 0 ].getKey() );
				} else if(clazz.equals( "PEO" )) {
					BARow r = baRowTreeItem.getChildren().get( 0 ).getValue();
					sn0Key = String.format( "%s %d:%d/%s", "PE O-", pric - r.getCarbon(), pridb - r.getDb(), samples[ 0 ].getKey() );
				}

				System.out.println( "\t" + sn0Key );

				samples[ 0 ].setKey( sn0Key );
			}
			else if( !isSym && samples.length == 2 )
			{
				String sn1Key = String.format( "%s %s/%s", clazz, samples[ 0 ].getKey(), samples[ 1 ].getKey() );
				String sn0Key = String.format( "%s %s/%s", clazz, samples[ 1 ].getKey(), samples[ 0 ].getKey() );

				if(!noCorrection)
				{
					if(clazz.equals( "PCO" )) {
						BARow r = baRowTreeItem.getChildren().get( 0 ).getValue();
						sn0Key = String.format( "%s %d:%d/%s", "PC O-", pric - r.getCarbon(), pridb - r.getDb(), samples[ 0 ].getKey() );
						samples[ 0 ].setKey( sn0Key );

						faSampleMap.put( priSample, new FASample[]{samples[0]} );
					}
					else if(clazz.equals( "PEO" )) {
						BARow r = baRowTreeItem.getChildren().get( 0 ).getValue();
						sn0Key = String.format( "%s %d:%d/%s", "PE O-", pric - r.getCarbon(), pridb - r.getDb(), samples[ 0 ].getKey() );
						samples[ 0 ].setKey( sn0Key );

						faSampleMap.put( priSample, new FASample[]{samples[0]} );
					}
					else if(clazz.equals( "PI" ))
					{
						// Merge the information
						String snKey;

						if( samples[ 0 ].getKey().compareTo( samples[ 1 ].getKey() ) < 0 )
						{
							snKey = String.format( "%s %s_%s", clazz, samples[ 0 ].getKey(), samples[ 1 ].getKey() );
						}
						else
						{
							snKey = String.format( "%s %s_%s", clazz, samples[ 1 ].getKey(), samples[ 0 ].getKey() );
						}

						System.out.println( "\t" + snKey);

						FASample newSample = new FASample(samples[0]);
						newSample.setKey( snKey );

						newSample.addIntensityData( sample.getKeys(), samples[0], samples[1] );
						faSampleMap.put( priSample, new FASample[] { newSample } );
					}
					else
					{
						System.out.println(
								String.format( "\t%s %s -> %s", sn1Key, samples[ 1 ].get( ce ).getPos(), samples[ 1 ].get( ce ).getRatio() )
						);

						System.out.println(
								String.format( "\t%s %s -> %s", sn0Key, samples[ 0 ].get( ce ).getPos(), samples[ 0 ].get( ce ).getRatio() )
						);

						samples[ 0 ].setKey( sn0Key );
						samples[ 1 ].setKey( sn1Key );

						samples[ 0 ].setComplement( sn1Key );
						samples[ 1 ].setComplement( sn0Key );
					}
				}
				else
				{
					// Merge the information
					String snKey;

					if( samples[ 0 ].getKey().compareTo( samples[ 1 ].getKey() ) < 0 )
					{
						snKey = String.format( "%s %s-%s", clazz, samples[ 0 ].getKey(), samples[ 1 ].getKey() );
					}
					else
					{
						snKey = String.format( "%s %s-%s", clazz, samples[ 1 ].getKey(), samples[ 0 ].getKey() );
					}

               System.out.println( "\t" + snKey);

					FASample newSample = new FASample(samples[0]);
					newSample.setKey( snKey );

					newSample.addIntensityData( sample.getKeys(), samples[0], samples[1] );
					faSampleMap.put( priSample, new FASample[] { newSample } );
				}
			}
		}

		computeNorm( noCorrection, faSampleMap );

		// merge for the same specie
		LinkedHashMap< FASample, FASample[] > newFaSampleMap = new LinkedHashMap<>();
		HashMap< String, FASample > priMap = new HashMap<>(  );

		for( FASample priSample : faSampleMap.keySet() )
		{
			final String specie = priSample.getKey();

			if( priMap.containsKey( specie ) )
			{
				FASample pri = priMap.get( specie );
				ArrayList<FASample> list = new ArrayList<>(  );
				TreeMap< Float, Range< Float > > sumMap = new TreeMap<>();

				FASample newSample = newFaSampleMap.get( pri )[0];
				FASample oldSample = faSampleMap.get( priSample )[0];

				for ( Float ce : pri.getNCE() )
				{
					sumMap.put( ce, Range.between (
									newSample.get(ce).getSum().getMinimum() + oldSample.get(ce).getSum().getMinimum(),
									newSample.get(ce).getSum().getMaximum() + oldSample.get(ce).getSum().getMaximum() )
					);
				}

				for( FASample sample : newFaSampleMap.get( pri ) )
					if( null != sample )
					{
						list.add( sample );
					}

				for( FASample sample : faSampleMap.get( priSample ) )
					if( null != sample )
					{
						list.add( sample );
					}

				for( FASample sample : list )
				{
					for ( Float ce : pri.getNCE() )
					{
						sample.get(ce).setSum( sumMap.get( ce ) );
					}
				}

				newFaSampleMap.put( pri, list.toArray( new FASample[]{} ) );
			}
			else
			{
				priMap.put( specie, priSample );
				newFaSampleMap.put( priSample, faSampleMap.get( priSample ) );
			}
		}

		control.setup( priReferenceMap, computedPriFragmentMap, newFaSampleMap, estSampleTreeMap );
	}

	public interface ChartControl
	{
		void setup( TreeMap<String, TreeMap< Float, Float >> priReferenceMap, TreeMap<String, TreeMap< Double, Fragment >> computedPriFragmentMap, LinkedHashMap< FASample, FASample[] > faSampleMap, TreeMap< String, EstSample > estSampleTreeMap );
	}

   public static void estimate2ndPosition( TreeMap<String, Fraction > referenceFAIMap, String clazz, Set<Float> ceSet, ArrayList<String> keyList, TreeMap< String, EstSample > estSampleTreeMap )
   {
      for( Float ce : ceSet )
      {
         Float sumMin = null, sumMax = null;
         for( String key : keyList )
         {
            Range<Float> faiRange = estSampleTreeMap.get( key + ce ).getCFCorrectedFAI();

            if( null == sumMin )
            {
               sumMin = faiRange.getMinimum();
               sumMax = faiRange.getMaximum();
            }
            else
            {
               sumMin += faiRange.getMinimum();
               sumMax += faiRange.getMaximum();
            }
         }

         for( String key : keyList )
         {
            Range<Float> val = estSampleTreeMap.get( key + ce ).getCFCorrectedFAI();

//            Range< Float > pos = Range.between( val.getMinimum() / sumMin, val.getMaximum() / sumMax );
            float posAvg = val.getMinimum() / sumMin;

            Range<Float> pos = posAvg > 0.5f ? Range.between( val.getMinimum() / sumMin, val.getMaximum() / sumMax ) :
                    Range.between( val.getMaximum() / sumMax, val.getMinimum() / sumMin );

            if( sumMin != 0 && sumMax != 0) {
               estSampleTreeMap.get( key + ce ).setRel_FAI( pos );
            }
         }
      }

//      System.out.println(clazz + ":");
      for( String key : keyList )
      {
//         if(key.startsWith( "PG 32:0" ))
//            System.out.println(key);

         Float posMin = null, posMax = null;

         for( Float ce : ceSet )
         {
            Range<Float> pos = estSampleTreeMap.get( key + ce ).getRel_FAI();

            if( null == posMin )
            {
               posMin = pos.getMinimum();
               posMax = pos.getMaximum();
            }

            posMin = Float.min( posMin, pos.getMinimum() );
            posMax = Float.max( posMax, pos.getMaximum() );
         }

         if(key.startsWith( "PI" ))
            System.out.println( "[" + posMin + "..." + posMax + "]");

         Float estPosMin = null, estPosMax = null;

         for( Float ce : ceSet )
         {
            Range<Float> pos = estSampleTreeMap.get( key + ce ).getRel_FAI();

            // Get the reference line from 255.23
            Float position0 = referenceFAIMap.get( clazz ).get( ce, "rel0" );
            Float position1 = referenceFAIMap.get( clazz ).get( ce, "rel1" );

//            if(key.startsWith( "PI" ))
//               System.out.println( "SN2 0.0 : " + position0 + ", SN2 1.0 : " + position1 );

            if(position0 == null) break;

            float a = position1 - position0;
            float b = position0;

            float min = (pos.getMinimum() - b) / a;
            float max = (pos.getMaximum() - b) / a;

            if( null == estPosMin )
            {
               estPosMin = min;
               estPosMax = max;
            }

            estPosMin = Float.min( estPosMin, min );
            estPosMax = Float.max( estPosMax, max );
         }

         estPosMin = estPosMin == null ? posMin : estPosMin;
         estPosMax = estPosMax == null ? posMax : estPosMax;

         if(estPosMin < 0)
            estPosMin = 0f;
         else if(estPosMin > 1)
            estPosMin = 1f;

         if(estPosMax < 0)
            estPosMax = 0f;
         else if(estPosMax > 1)
            estPosMax = 1f;

//         if(key.startsWith( "PI" ))
//            System.out.println( "[" + estPosMin + "..." + estPosMax + "]");

         for( Float ce : ceSet )
         {
            estSampleTreeMap.get( key + ce ).setSecondaryPosition( Range.between( estPosMin, estPosMax ));
         }
      }
   }

   public static void estimate3rdPosition( MasterDatabase masterDatabase, TreeMap<String, TreeMap< Integer, FAAnion > > masterDBSet,
           TreeMap<String, Fraction > referenceFAIMap, String clazz, Set<Float> ceSet, ArrayList<String> keyList,
           TreeMap< String, Fraction > sampleTreeMap, TreeMap< String, EstSample > estSampleTreeMap )
   {
      // 1. Compute the secondary Double Bond Correction Factor (2nd iteration)
      for ( String key : keyList )
      {
         Fraction fraction = sampleTreeMap.get( key );

         FAAnion faAnion = masterDBSet.get( clazz ).get( fraction.getIndex() );

         if (faAnion.getMass().equals( 255.23d )) {
            for ( Float ce : fraction.keySet() )
            {
               estSampleTreeMap.get( key + ce ).setSecondaryCF( Range.between( 1f, 1f ) );
            }
         } else {
            List<FAAnion> faAnions = getMassIndexes( masterDBSet.get( clazz ).values(), faAnion );
            final int FA_db = faAnions.stream().findFirst().get().getFADoubleBonds();

            if( FA_db > 0 )
            {
               Float isoMin = null, isoMax = null;
               Float posMin = null, posMax = null;

               for ( Float ce : fraction.keySet() )
               {
                  Range< Float > isoRange = estSampleTreeMap.get( key + ce ).getIsomer();
                  Range< Float > posRange = estSampleTreeMap.get( key + ce ).getSecondaryPosition();

                  if ( null == isoMin )
                  {
                     isoMin = isoRange.getMinimum();
                     isoMax = isoRange.getMaximum();

                     posMin = posRange.getMinimum();
                     posMax = posRange.getMaximum();
                  }

                  isoMin = Float.min( isoMin, isoRange.getMinimum() );
                  isoMax = Float.max( isoMax, isoRange.getMaximum() );

                  posMin = Float.min( posMin, posRange.getMinimum() );
                  posMax = Float.max( posMax, posRange.getMaximum() );
               }

               for ( Float ce : fraction.keySet() )
               {
                  // 3rd method
                  TreeMap< Float, TreeMap< Float, Float > > cfMap = new TreeMap<>();
                  cfMap.put(0f, new TreeMap<>());
                  cfMap.put(0.5f, new TreeMap<>());
                  cfMap.put(1f, new TreeMap<>());

                  String[][] scopes = new String[][] { { "1.0", "0", "0" }, { "0.5", "0.5", "0" },
                          { "0", "1.0", "0" } };

                  for ( FAAnion faItem : faAnions )
                  {
                     for ( String[] scope : scopes )
                     {
                        String[] detailRow = masterDatabase.getDetail( ce, faItem.getIndex(), fraction.getClazz(), scope[ 0 ], scope[ 1 ], scope[ 2 ] );

                        float sn2 = Float.parseFloat( scope[ 1 ] );
                        float isomer = faItem.getFAIsomer();
                        float ret = Float.parseFloat( detailRow[ 2 ] );

                        cfMap.get( sn2 ).put( isomer, ret );
                     }
                  }

                  if( isoMin.equals( isoMax ) ) {
                     // One iso value
//                  System.out.println( "1st CF used for 2nd CF due to only one isomer" );
                     Range< Float > cfRange = estSampleTreeMap.get( key + ce ).getCF();

                     float a = cfRange.getMaximum() - cfRange.getMinimum();
                     float b = cfRange.getMinimum();

                     Range< Float > posRange = estSampleTreeMap.get( key + ce ).getSecondaryPosition();

                     float min = a * posRange.getMinimum() + b;
                     float max = a * posRange.getMaximum() + b;

                     Range< Float > nextCfRange = Range.between( min, max );
//                  System.out.println( "2nd CF:" + nextCfRange + " at " + ce );
                     estSampleTreeMap.get( key + ce ).setSecondaryCF( nextCfRange );
                  }
                  else
                  {

                     TreeMap< Float, Float > minCF = new TreeMap<>();
                     TreeMap< Float, Float > maxCF = new TreeMap<>();

                     Range< Float > isoRange = Range.between( isoMin, isoMax );

                     for ( float i = 0f; i <= 1; i += 0.5f )
                     {
                        Range< Float > range = getCFValue( cfMap.get( i ), isoRange );
                        minCF.put( i, range.getMinimum() );
                        maxCF.put( i, range.getMaximum() );
                     }

                     Range< Float > posRange = estSampleTreeMap.get( key + ce ).getSecondaryPosition();

                     Range< Float > range = Range.between( getCFValue( minCF, posRange.getMinimum() ), getCFValue( maxCF, posRange.getMaximum() ) );

                     //                  System.out.println( "Second CF:" + range + " at " + ce );
                     estSampleTreeMap.get( key + ce ).setSecondaryCF( range );
                  }
               }
            }
            else
            {
               // There is no isomer specific values, estimate CF based on the position
               for ( Float ce : fraction.keySet() )
               {
                  Range< Float > cfRange = estSampleTreeMap.get( key + ce ).getCF();

                  float a = cfRange.getMaximum() - cfRange.getMinimum();
                  float b = cfRange.getMinimum();

                  Range< Float > posRange = estSampleTreeMap.get( key + ce ).getSecondaryPosition();

                  float min = a * posRange.getMinimum() + b;
                  float max = a * posRange.getMaximum() + b;

                  Range< Float > nextCfRange = Range.between( min, max );
                  estSampleTreeMap.get( key + ce ).setSecondaryCF( nextCfRange );
               }
            }
         }
      }

      // 2. Position estimation by using the first corrected FAI
      for( Float ce : ceSet )
      {
         Float sumMin = null, sumMax = null;
         for ( String key : keyList )
         {
            Range< Float > faiRange = estSampleTreeMap.get( key + ce ).getSecondCFCorrectedFAI();

            if ( null == sumMin )
            {
               sumMin = faiRange.getMinimum();
               sumMax = faiRange.getMaximum();
            }
            else
            {
               sumMin += faiRange.getMinimum();
               sumMax += faiRange.getMaximum();
            }
         }

         for ( String key : keyList )
         {
            Range< Float > val = estSampleTreeMap.get( key + ce ).getSecondCFCorrectedFAI();

//            Range< Float > pos = Range.between( val.getMinimum() / sumMin, val.getMaximum() / sumMax );
            float posAvg = val.getMinimum() / sumMin;

            Range<Float> pos = posAvg > 0.5f ? Range.between( val.getMinimum() / sumMin, val.getMaximum() / sumMax ) :
                    Range.between( val.getMaximum() / sumMax, val.getMinimum() / sumMin );

            if( sumMin != 0 && sumMax != 0)
               estSampleTreeMap.get( key + ce ).setSecondaryRel_FAI( pos );
         }
      }

      for( String key : keyList )
      {
         Float posMin = null, posMax = null;

         for( Float ce : ceSet )
         {
            Range<Float> pos = estSampleTreeMap.get( key + ce ).getSecondaryRel_FAI();

            if( null == posMin )
            {
               posMin = pos.getMinimum();
               posMax = pos.getMaximum();
            }

            posMin = Float.min( posMin, pos.getMinimum() );
            posMax = Float.max( posMax, pos.getMaximum() );
         }

         Float estPosMin = null, estPosMax = null;

         for( Float ce : ceSet )
         {
            Range<Float> pos = estSampleTreeMap.get( key + ce ).getRel_FAI();

            // Get the reference line from 255.23
            Float position0 = referenceFAIMap.get( clazz ).get( ce, "rel0" );
            Float position1 = referenceFAIMap.get( clazz ).get( ce, "rel1" );

            if(position0 == null) break;

            float a = position1 - position0;
            float b = position0;

            float min = (pos.getMinimum() - b) / a;
            float max = (pos.getMaximum() - b) / a;

            if( null == estPosMin )
            {
               estPosMin = min;
               estPosMax = max;
            }

            estPosMin = Float.min( estPosMin, min );
            estPosMax = Float.max( estPosMax, max );
         }

         estPosMin = estPosMin == null ? posMin : estPosMin;
         estPosMax = estPosMax == null ? posMax : estPosMax;

         if ( estPosMin < 0 )
            estPosMin = 0f;
         else if ( estPosMin > 1 )
            estPosMin = 1f;

         if ( estPosMax < 0 )
            estPosMax = 0f;
         else if ( estPosMax > 1 )
            estPosMax = 1f;

         for( Float ce : ceSet )
            estSampleTreeMap.get( key + ce ).setThirdPosition( Range.between( estPosMin, estPosMax ) );
      }
   }

   public static void updateCorrectionFactor( String key, MasterDatabase masterDatabase, TreeMap< String, Fraction > sampleTreeMap,
           TreeMap< String, EstSample > estSampleTreeMap, TreeMap< Integer, Fraction > fractionTreeMap, List< FAAnion > faAnions, boolean isSym )
   {
      //		final float faIndex = faAnions.stream().findFirst().get().getCq();
      final FAAnion faAnion = faAnions.stream().findFirst().get();

      // Initial Position Estimation
      final int FA_db = faAnion.getFADoubleBonds();

//      if(key.startsWith( "PG 36:4" ))
//         System.out.println(key);

      if( FA_db > 0 )
      {
         Fraction fraction = sampleTreeMap.get(key);
         fraction.getMax( "Sample" );

         if( isSym )
         {
            Float isoMin = null, isoMax = null;

            float avgPosition = 0.5f;

            for( Float ce : fraction.keySet() )
            {
               estSampleTreeMap.get( key + ce ).setPosition( Range.between( avgPosition, avgPosition ) );

               if( estSampleTreeMap.get( key + ce ).getCorrectedCOI().equals( 0f ) )
                  continue;

               Range< Float > isoRange = estSampleTreeMap.get( key + ce ).getIsomer();

               if( null == isoMin )
               {
                  isoMin = isoRange.getMinimum();
                  isoMax = isoRange.getMaximum();
               }

               isoMin = Float.min( isoMin, isoRange.getMinimum() );
               isoMax = Float.max( isoMax, isoRange.getMaximum() );
            }

            if( isoMin == null )
            {
               isoMax = getMaxIsomer( faAnions );
               isoMin = getMinIsomer( faAnions );
            }

            for( Float ce : fraction.keySet() )
            {
               if( estSampleTreeMap.get( key + ce ).getCorrectedCOI().equals( 0f ) )
               {
                  estSampleTreeMap.get( key + ce ).setIsomer( Range.between( isoMin, isoMax ) );
               }
            }
         }
         else
         {
//            System.out.println("Max CE:" + fraction.getMaxCE());
            for( Float ce : fraction.keySet() )
            {
               // There is an issue for get MAX CE from the sample
               if(ce.equals( fraction.getMaxCE() )) continue;

               List<Float> estPosition = new ArrayList<>(  );

               faAnions.forEach( c ->
               {
                  TreeMap<Float, Float> data =  fractionTreeMap.get( c.getIndex() ).getFAData( ce );
                  float sampleRatio = sampleTreeMap.get( key ).getNormalizedValue( ce, "Sample" );
                  float val = getSN2Value( data, sampleRatio );
                  estPosition.add( val );
               });

               Range positionRange = Range.between( Collections.min( estPosition ), Collections.max( estPosition ) );
               estSampleTreeMap.get( key + ce ).setPosition( positionRange );
            }

            Float posMin = null, posMax = null;
            for( Float ce : fraction.keySet() )
            {
               if(ce.equals( fraction.getMaxCE() )) continue;

               Range<Float> pos = estSampleTreeMap.get( key + ce ).getPosition();

               if( null == posMin )
               {
                  posMin = pos.getMinimum();
                  posMax = pos.getMaximum();
               }

               posMin = Float.min( posMin, pos.getMinimum() );
               posMax = Float.max( posMax, pos.getMaximum() );
            }

//            System.out.println("Position: " + Range.between( posMin, posMax ));

            if(posMin == null) return;

            //				if(fraction.getMaxCE() != null && estSampleTreeMap.get( key + fraction.getMaxCE() ) != null && posMax != null && posMin != null)
            if( fraction != null && !fraction.getMaxCE().equals( 0f ))
               estSampleTreeMap.get( key + fraction.getMaxCE() ).setPosition( Range.between( posMin, posMax ) );
//               estSampleTreeMap.get( key + 25f ).setPosition( Range.between( posMin, posMax ) );

//  				for( Float ce : fraction.keySet() )
//                 estSampleTreeMap.get( key + ce ).setPosition( Range.between( posMin, posMax ) );

            for ( Float ce : fraction.keySet() )
            {
               TreeMap< Float, TreeMap< Float, Float > > cfMap = new TreeMap<>();
               cfMap.put( 0f, new TreeMap<>() );
               cfMap.put( 0.5f, new TreeMap<>() );
               cfMap.put( 1f, new TreeMap<>() );

               String[][] scopes = new String[][] { { "1.0", "0", "0" }, { "0.5", "0.5", "0" },
                       { "0", "1.0", "0" } };

               for ( FAAnion faItem : faAnions )
               {
                  for ( String[] scope : scopes )
                  {
                     String[] row = masterDatabase.getDetail( ce, faItem.getIndex(), fraction.getClazz(), scope[ 0 ], scope[ 1 ], scope[ 2 ] );

                     float sn2 = Float.parseFloat( scope[ 1 ] );
                     float isomer = faItem.getFAIsomer();
                     float ret = Float.parseFloat( row[ 2 ] );

                     cfMap.get( sn2 ).put( isomer, ret );
                  }
               }

               TreeMap< Float, Float > minCF = new TreeMap<>();
               TreeMap< Float, Float > maxCF = new TreeMap<>();

               Range< Float > isoRange = estSampleTreeMap.get( key + ce ).getIsomer();

               for ( float i = 0f; i <= 1; i += 0.5f )
               {
                  Range< Float > range = getCFValue( cfMap.get( i ), isoRange );
                  minCF.put( i, range.getMinimum() );
                  maxCF.put( i, range.getMaximum() );
               }

               Range< Float > range = Range.between( getCFValue( minCF, posMin ), getCFValue( maxCF, posMax ) );

               System.out.println( "1st CF:" + range + " at " + ce );

               estSampleTreeMap.get( key + ce ).setCF( range );
            }
         }
      }
      else {
         if( isSym )
         {
            Fraction fraction = sampleTreeMap.get(key);

            for ( Float ce : fraction.keySet() )
            {
               estSampleTreeMap.get( key + ce ).setPosition( Range.between( 0.5f, 0.5f ) );
            }
         }
      }
   }

	// The 1st isomer estimation
	public static void estimateIsomer( final MasterDatabase masterDatabase, final String clazz, Set<Float> ceSet,
           final TreeMap< Integer, Fraction > fractionTreeMap, TreeMap< String, EstSample > estSampleTreeMap,
           final List<FAAnion> faAnions, final boolean isSym, final String key )
	{
      for(Float ce : ceSet) {
         // Decide if the given fragment is symmetric or asymmetric
         String[][] scopes = isSym ? (
                 clazz.equals( "PCO" ) || clazz.equals( "PEO" ) ? new String[][] { {"0", "1.0", "0"} } : new String[][] { {"0", "0", "1.0"} }) :
                 new String[][] { {"1.0", "0", "0"}, {"0.5", "0.5", "0"}, {"0", "1.0", "0"} };
         List<Float> estIsomer = new ArrayList<>(  );

         for(String[] scope : scopes)
         {
            TreeMap<Float, Float> isomer = new TreeMap<>(  );

            for( FAAnion faAnion : faAnions )
            {
               if( !fractionTreeMap.containsKey( faAnion.getIndex() ) )
                  fractionTreeMap.put( faAnion.getIndex(), new Fraction(clazz, faAnion.getIndex()) );

               for ( String[] detailRow : masterDatabase.getDetails( faAnion.getIndex(), clazz, scope[ 0 ], scope[ 1 ], scope[2] ) )
               {
                  if ( ce == Float.parseFloat( detailRow[ 0 ] ) )
                  {
                     final float ratio = detailRow[ 3 ].isEmpty() || detailRow[ 1 ].isEmpty() ? 0f : Float.parseFloat( detailRow[ 3 ] ) / Float.parseFloat( detailRow[ 1 ] );

                     // Add FAI
                     fractionTreeMap.get( faAnion.getIndex() ).put( ce, "SN2-" + Float.parseFloat( scope[1] ), Float.parseFloat( detailRow[1] ) );

                     if( ratio > 0 )
                     {
                        // We assume there is CO2Loss
                        // Add COI
                        fractionTreeMap.get( faAnion.getIndex() ).setContainsCo2Loss( true );

                        fractionTreeMap.get( faAnion.getIndex() ).put( ce, "CO2:SN2-" + Float.parseFloat( scope[1] ), Float.parseFloat( detailRow[3] ) );
                     }

                     isomer.put( ratio, faAnion.getFAIsomer() );

                     break;
                  }
               }
            }

            // Suggest isomer value for the sample with ce
            if(isomer.size() == 1)
            {
               estIsomer.add( isomer.get( isomer.firstKey() ) );
            }
            else
            {
               if( null == estSampleTreeMap.get( key + ce ).getFaCoRatio() || estSampleTreeMap.get( key + ce ).getFaCoRatio().equals( 0f ) )
                  estIsomer.addAll( isomer.values() );
               else {
                  estIsomer.add( getIsomerValue( isomer, estSampleTreeMap.get( key + ce ).getFaCoRatio() ) );
               }
            }
         }

         Range isomerRange = Range.between( Collections.min( estIsomer ), Collections.max( estIsomer ) );
         //		System.out.println( key + ce + " : " + isomerRange );

         if( estSampleTreeMap.containsKey( key + ce ) )
            estSampleTreeMap.get( key + ce ).setIsomer( isomerRange );
      }
	}

   public static float getSN2Value( TreeMap< Float, Float > data, float ratio )
	{
		if( data.size() < 3 )
		{
//			System.err.println( "(SampleEstimation) SN2: Data set does not have enough items to extract a function." );
			return 0f;
		}
		else
		{
			double[] params = computeRegressionParameters( false, data );

			DoubleSummaryStatistics stat = data.values().stream().collect( Collectors.summarizingDouble( Float::doubleValue ) );

			Range<Double> range = Range.between( stat.getMin(), stat.getMax() );

			if(params.length == 3)
			{
				double result = Precision.round( params[2] * ratio * ratio + params[1] * ratio + params[0], 1 );

				if(range.isBefore( result ))
				{
					result = range.getMaximum();
				}
				else if(range.isAfter( result ))
				{
					result = range.getMinimum();
				}

				double pct = (result - stat.getMin()) / (stat.getMax() - stat.getMin());

//				pct = Percent.toFivePercentUnit( pct ) * 100;
//
//				String out = String.format( ">> FAI_SN2 = %.1f, %d %% (SN2) and %d %% (SN1)",
//						result, (int) pct, (int) (100 - pct) );
//				System.out.println( out );

				return (float) result;
			}
			else
			{
				System.err.println( "Error" );
				return 0f;
			}
		}
	}

   public static Range<Float> getCFValue( TreeMap< Float, Float > cf, Range<Float> ratio )
   {
      if( cf.size() < 2 )
      {
         System.err.println( "CF: Data set does not have enough items to extract a function." );
         return Range.between( 1f, 1f );
      }
      else
      {
         double[] params = computeRegressionParameters( false, cf );

         if( params.length  == 2)
         {
            double resultMin = Precision.round( params[1] * ratio.getMinimum() + params[0], 5 );

            double resultMax = Precision.round( params[1] * ratio.getMaximum() + params[0], 5 );

            return Range.between( (float) resultMin, (float) resultMax);
         }
         else if(params.length == 3)
         {
            double resultMin = Precision.round( params[2] * ratio.getMinimum() * ratio.getMinimum() + params[1] * ratio.getMinimum() + params[0], 5 );

            double resultMax = Precision.round( params[2] * ratio.getMaximum() * ratio.getMaximum() + params[1] * ratio.getMaximum() + params[0], 5 );

            return Range.between( (float) resultMin, (float) resultMax);
         }
         else
         {
            System.err.println( "Error" );
            return Range.between( 1f, 1f );
         }
      }
   }

   private static Float getCFValue( TreeMap< Float, Float > cfMap, Float ratio )
   {
      if( cfMap.size() < 2 )
      {
         System.err.println( "CF: Data set does not have enough items to extract a function." );
         return 1f;
      }
      else
      {
         double[] params = computeRegressionParameters( false, cfMap );

         if( params.length  == 2)
         {
            return (float) Precision.round( params[1] * ratio + params[0], 3 );
         }
         else if(params.length == 3)
         {
            return (float) Precision.round( params[2] * ratio * ratio + params[1] * ratio + params[0], 3 );
         }
         else
         {
            System.err.println( "Error" );
            return 1f;
         }
      }
   }

	public static float getIsomerValue( TreeMap< Float, Float > isomer, float ratio )
	{
		if( isomer.size() < 2 )
		{
			System.err.println( "ISO: Data set does not have enough items to extract a function." );
			return 0f;
		}
		else
		{
			double[] params = computeRegressionParameters( false, isomer );

			DoubleSummaryStatistics stat = isomer.values().stream().collect( Collectors.summarizingDouble( Float::doubleValue ) );

			Range<Double> range = Range.between( stat.getMin(), stat.getMax() );

			if( params.length  == 2){
				double result = Precision.round( params[1] * ratio + params[0], 1 );

				if(range.isBefore( result ))
				{
					result = range.getMaximum();
				}
				else if(range.isAfter( result ))
				{
					result = range.getMinimum();
				}

//            double pct = (((int) result) - stat.getMin()) / (stat.getMax() - stat.getMin());
//
//            pct = Percent.toFivePercentUnit( pct ) * 100;
//
//            String out = String.format( ">> FAI_iso = %.1f %d %% (%dz) and %d %% (%dz)",
//                    result, (int) pct, (int) stat.getMax(), (int) (100 - pct), (int) stat.getMin());
//            System.out.println( out );

				return (float) result;
			}
			else if(params.length == 3)
			{
				double result = Precision.round( params[2] * ratio * ratio + params[1] * ratio + params[0], 1 );

				if(range.isBefore( result ))
				{
					result = range.getMaximum();
				}
				else if(range.isAfter( result ))
				{
					result = range.getMinimum();
				}

//            double pct = (((int) result) - stat.getMin()) / (stat.getMax() - stat.getMin());
//
//            pct = Percent.toFivePercentUnit( pct ) * 100;
//
//            String out = String.format( ">> FAI_iso = %.1f, %d %% (%dz) and %d %% (%dz)",
//                    result, (int) pct, (int) stat.getMax(), (int) (100 - pct), (int) stat.getMin());
//            System.out.println( out );

				return (float) result;
			}
			else
			{
				System.err.println( "Error" );
				return 0f;
			}
		}
	}

	public static int getIndex( ObservableList< FAAnion > mFaAnionsList, double mass, int carbon, int db )
	{
		int found = 0;

		Optional<FAAnion> faAnion = mFaAnionsList.stream().filter( c ->
				c.getMass().equals( mass ) &&
						c.getFACarbon().equals( carbon ) &&
						c.getFADoubleBonds().equals( db ) ).findFirst();

		if(faAnion.isPresent())
			found = faAnion.get().getIndex();
		else
			System.err.println( mass + ":carbon - " + carbon + ":db - " + db + " => Not found in FAAnion List!" );

		return found;
	}

	public static List< FAAnion > getMassIndexes( Collection<FAAnion> collection, Double mass )
	{
		return collection.stream().filter( c -> c.getMass().equals( mass ) ).collect( Collectors.toList() );
	}

	public static List< FAAnion > getMassIndexes( Collection<FAAnion> collection, FAAnion faAnion )
	{
		return collection.stream().filter( c -> c.getMass().equals( faAnion.getMass() ) ).collect( Collectors.toList() );
	}

	public static int getFA_ID( Collection<FAAnion> collection, Double mass )
	{
		return collection.stream().filter( c -> c.getMass().equals( mass ) ).findFirst().get().getIndex();
	}

	private static List< FAAnion > getMassIndexes( ObservableList<FAAnion> mFaAnionsList, String clazz, double mass, int carbon, int db )
	{
		if( masterDBSet != null )
		{
			final int index = getIndex( mFaAnionsList, mass, carbon, db );

			final FAAnion faAnion = masterDBSet.get( clazz ).get( index );
			return getMassIndexes( masterDBSet.get( clazz ).values(), faAnion );
		}
		else
			return null;
	}

	public static TxCorrectionFunc getSelectedTxCFunction()
	{
		return selectedTxCFunction;
	}

	public static void setSelectedTxCFunction( TxCorrectionFunc selectedTxCFunction )
	{
		SampleEstimation.selectedTxCFunction = selectedTxCFunction;
		updateTxCFunctionMap();
	}

	public static TreeMap< Float, double[] > getTxCFunctionParamMap()
	{
		return txCFunctionParamMap;
	}

	public static void setTxCFunctionParamMap( TreeMap< Float, double[] > txCFunctionParamMap )
	{
		SampleEstimation.txCFunctionParamMap = txCFunctionParamMap;
		updateTxCFunctionMap();
	}

	public static void setTxCFunctionMapForSimpleExp( String input )
	{
		TreeMap< Float, double[] > txCFunctionParamMap = tryParseTxFunctionString( input );
		SampleEstimation.txCFunctionParamMap = txCFunctionParamMap;
		selectedTxCFunction = TxCorrectionFunc.SimpleExp;
		updateTxCFunctionMap();
	}

	static void updateTxCFunctionMap()
	{
		txCFunctionMap = new TreeMap<>();

		if( selectedTxCFunction == TxCorrectionFunc.ExpDecay )
		{
			txCFunctionParamMap.forEach(
					(ce, param)
							-> txCFunctionMap.put( ce, new ExponentialDecayFunction( param ) ) );
		}
		else if( selectedTxCFunction == TxCorrectionFunc.SimpleExp )
		{
			txCFunctionParamMap.forEach(
					(ce, param)
							-> txCFunctionMap.put( ce, new SimpleExponentialFunction( param ) ) );
		}
	}

	public static Float getTxCF( Double priMass, Float ce )
	{
		return (float) txCFunctionMap.get(ce).value( priMass );
	}

	public static TxCorrectionFunc getTxCorrectionFunc() {
		return selectedTxCFunction;
	}

	public static String getTxFunctionName(Float ce)
	{
		String className = txCFunctionMap.get(ce).getClass().toGenericString();
		return className.substring( className.lastIndexOf( "." ) + 1 );
	}

	public static FASample[] getSamples( boolean bValidCo2LossPresent, String clazz, String specieName, Set< Float > ceSet, TreeItem< BARow > parent, ObservableList< TreeItem< BARow > > specieTreeItemList, TreeMap< String, EstSample > estSampleTreeMap, ObservableList< FAAnion > mFaAnionsList, TreeMap< Float, Range< Float > > sumMap, TreeMap< Float, Float > noCorrectedSumMap, boolean noCorrection )
	{
		FASample[] samples;
		boolean bNeedIsomerSplit = false;
		boolean bNoSpecified = !bValidCo2LossPresent;
		final int pric = parent.getValue().getCarbon();
		final int pridb = parent.getValue().getDb();

		// Isomer check first

		for ( TreeItem< BARow > faTreeItem : specieTreeItemList )
		{
			final String faMz = faTreeItem.getValue().getMassString();
			final String key = specieName + "-" + faMz;

			Float avg = 0f;
			for ( Float ce : ceSet )
			{
				if( estSampleTreeMap.get( key + ce ).getCorrectedFAI().equals( 0f ) )
					bNoSpecified = true;


				Float isomer = ( estSampleTreeMap.get( key + ce ).getIsomer().getMinimum() + estSampleTreeMap.get( key + ce ).getIsomer().getMaximum() ) / 2;

				avg += isomer;
			}

			avg /= ceSet.size();

			if( bNoSpecified )
			{
				List< FAAnion > faAnions = getFAanions( clazz, Double.parseDouble( faMz ), faTreeItem.getValue(), mFaAnionsList );

				Float isoMax = getMaxIsomer( faAnions );
				Float isoMin = getMinIsomer( faAnions );

				if( isoMax != 0f && isoMin != 0f )
				{
					faTreeItem.getValue().setIsomer( isoMin );

					if( !isoMax.equals( isoMin ))
						faTreeItem.getValue().setUnspecifiedIsomer( isoMax );
				}
			}
			else
			{
				faTreeItem.getValue().setIsomer( avg );
			}

			if ( noCorrection )
				faTreeItem.getValue().setIsomer( 0f );

			if ( !noCorrection && avg != 0f && checkSplitIsomer( clazz, faMz, faTreeItem.getValue(), mFaAnionsList ) )
			{
				bNeedIsomerSplit = true;
			}
		}


		if( bNoSpecified )
			bNeedIsomerSplit = false;

		final boolean isSym = specieTreeItemList.size() == 1;

		{
			if( isSym ) samples = new FASample[1];
			else samples = new FASample[2];

			for ( TreeItem< BARow > faTreeItem : specieTreeItemList )
			{
				final String faMz = faTreeItem.getValue().getMassString();
				final String key = specieName + "-" + faMz;

				final FASample faSample = new FASample( key );
				faSample.setMass( faTreeItem.getValue().getMass() );

				Range< Float > sn2 = null;

				for ( Float ce : ceSet )
				{
					sn2 = estSampleTreeMap.get( key + ce ).getPosition();

					Range< Float > fai = isSym ?
							sumMap.get( ce ) :
							estSampleTreeMap.get( key + ce ).getNormFAI( sumMap.get( ce ) );

					Float noCorrectedFai = isSym ?
							noCorrectedSumMap.get( ce ) :
							estSampleTreeMap.get( key + ce ).getCorrectedFAI();

               TreeMap< Float, Float > cfMap = estSampleTreeMap.get( key + ce ).getCfMap();

					Range< Float > originalFai = estSampleTreeMap.get( key + ce ).getPosition();

					faSample.add( ce, sn2, fai, sumMap.get( ce ), noCorrectedFai, noCorrectedSumMap.get(ce), cfMap, originalFai );
				}

				BARow baRow = faTreeItem.getValue();
//				System.out.println( "Key: " + baRow.toString() );
				faSample.setKey( baRow.toString() );

				if ( sn2.getMinimum() <= 0.5f )
				{
					if( samples[ 0 ] == null )
						samples[ 0 ] = faSample;
					else
						samples[ 1 ] = faSample;
				}
				else if ( sn2.getMinimum() > 0.5f )
				{
					if( samples[ 1 ] == null )
						samples[ 1 ] = faSample;
					else
						samples[ 0 ] = faSample;
				}
			}
		}

		if( bNeedIsomerSplit )
		{
			System.out.print( "Isomer Split is needed for " );
			// create appropriate samples

			if( isSym )
			{
				FASample sample0 = samples[0];

				System.out.println( String.format( "%s %s", clazz, sample0.getKey() ) );

				// Checking the intensity
//				for ( Float ce : ceSet )
//				{
//					System.out.println( ce + " : " + sample0.get( ce ).getFai() );
//				}
			}
			else
			{
				FASample sample0 = samples[ 0 ];
				FASample sample1 = samples[ 1 ];

				System.out.print( String.format( "%s %s/%s and ", clazz, sample1.getKey(), sample0.getKey() ) );

				// Checking the intensity
//				for ( Float ce : ceSet )
//				{
//					System.out.println( ce + " : " + sample0.get( ce ).getFai() );
//				}

				System.out.println( String.format( "%s %s/%s", clazz, sample0.getKey(), sample1.getKey() ) );

//				for ( Float ce : ceSet )
//				{
//					System.out.println( ce + " : " + sample1.get( ce ).getFai() );
//				}
			}

			// Actual Isomer Split logic
			{
				TreeMap< Double, List< FAAnion > > candidates = new TreeMap<>();
				HashMap< Double, Range< Float > > minMaxMap = new HashMap<>();

				for ( TreeItem< BARow > faTreeItem : specieTreeItemList )
				{
					final Double faMz = faTreeItem.getValue().getMass();
					final String key = specieName + "-" + faMz;

					List< FAAnion > faAnions = getFAanions( clazz, faMz, faTreeItem.getValue(), mFaAnionsList );

					if ( faAnions.size() > 1 )
					{
						// Sum up isomer values
						Float min = Float.MAX_VALUE, max = Float.MIN_VALUE;

						for ( Float ce : ceSet )
						{
							Range< Float > isomer = estSampleTreeMap.get( key + ce ).getIsomer();
							min = Float.min( min, isomer.getMinimum() );
							max = Float.max( max, isomer.getMaximum() );
						}

						Float isoMax = getMaxIsomer( faAnions );
						Float isoMin = getMinIsomer( faAnions );

						Float a = -1 / ( isoMax - isoMin );
						Float b = isoMax / ( isoMax - isoMin );

						min = a * min + b;
						max = a * max + b;

						System.out.println( "Estimated Isomer Ratio : " + Range.between( min, max ) );

						if( min < 0f || min > 1f || max < 0f || max > 1f )
						{
							System.err.println( "Estimated Isomer limited by [0, 1]" );
							if( min < 0f )
								min = 0f;
							else if( min > 1f )
								min = 1f;

							if( max > 1f )
								max = 1f;
							else if( max < 0f )
								max = 0f;

							System.out.println( "New Estimated Isomer Ratio : " + Range.between( min, max ) );
						}

						minMaxMap.put( faMz, Range.between( min, max ) );
					}

					candidates.put( faMz, faAnions );
				}

				if( isSym )
				{
					// Estimate new size of new sample array
					int newSampleSize = 0;

					for( Double subject : candidates.keySet() )
						for( FAAnion sFaAnion : candidates.get( subject ) )
							newSampleSize++;


					FASample[] newSamples = new FASample[newSampleSize];
					newSampleSize = 0;

					// Split logic
					for( Double subject : candidates.keySet() )
					{
						// Checking if there are multiple isomers
						final boolean bSubjectMinMax = minMaxMap.containsKey( subject );

						Range<Float> subjectRatio = null;
						Float subjectIsoMin = null;

						if( bSubjectMinMax )
						{
							subjectRatio = minMaxMap.get( subject );
							subjectIsoMin = getMinIsomer( candidates.get( subject ) );
						}

						for( FAAnion sFaAnion : candidates.get( subject ) )
						{
							// PCO and PEO treat always symmetric positions
							// Therefore, this check exists in only symmetric case
							String newKey = clazz + " " + sFaAnion.getKey() + "/" + sFaAnion.getKey();
							if(clazz.equals( "PCO" )) {
								BARow r = specieTreeItemList.get( 0 ).getValue();
								newKey = String.format( "%s %d:%d/%s", "PC O-", pric - r.getCarbon(), pridb - r.getDb(), sFaAnion.getKey() );
							} else if(clazz.equals( "PEO" )) {
								BARow r = specieTreeItemList.get( 0 ).getValue();
								newKey = String.format( "%s %d:%d/%s", "PE O-", pric - r.getCarbon(), pridb - r.getDb(), sFaAnion.getKey() );
							}

							System.out.println( newKey );
							FASample newSample = new FASample( newKey );
							newSample.setMass( subject );

							FASample subjectSample = findSample( subject, samples );


							// Apply the factor
							for ( Float ce : ceSet )
							{
								Range< Float > fai = subjectSample.get( ce ).getDBCorrectedFAI( sFaAnion.getFAIsomer() );
//								System.out.println( "[Sum] " + ce + ":" + fai );

								Float noCorrectedFAI = subjectSample.get( ce ).getNoCorrectedFAI();
								Float noCorrectedSum = subjectSample.get( ce ).getNoCorrectedSum();
                        TreeMap<Float, Float> map = subjectSample.get( ce ).getDbcfMap();
								Range< Float > orgFAI = subjectSample.get( ce ).getOrgFAI();


								if( bSubjectMinMax )
								{
									Range< Float > subjectFactor = subjectRatio;

									if ( !sFaAnion.getFAIsomer().equals( subjectIsoMin ) )
									{
										subjectFactor = Range.between( 1f - subjectRatio.getMaximum(), 1f - subjectRatio.getMinimum() );
										fai = Range.between(
												fai.getMinimum() * subjectFactor.getMinimum(),
												fai.getMaximum() * subjectFactor.getMaximum() );

										noCorrectedFAI = noCorrectedFAI * subjectFactor.getMaximum();
										noCorrectedSum = noCorrectedSum * subjectFactor.getMaximum();

										orgFAI = Range.between(
												orgFAI.getMinimum() * subjectFactor.getMinimum(),
												orgFAI.getMaximum() * subjectFactor.getMaximum() );
									}
									else
									{
										fai = Range.between(
												fai.getMinimum() * subjectFactor.getMaximum(),
												fai.getMaximum() * subjectFactor.getMinimum() );

										noCorrectedFAI = noCorrectedFAI * subjectFactor.getMaximum();
										noCorrectedSum = noCorrectedSum * subjectFactor.getMaximum();

										orgFAI = Range.between(
												orgFAI.getMinimum() * subjectFactor.getMaximum(),
												orgFAI.getMaximum() * subjectFactor.getMinimum() );
									}
								}

								newSample.add( ce, subjectSample.get( ce ).getPos(), fai, sumMap.get( ce ), noCorrectedFAI, noCorrectedSum, map, orgFAI );
							}

							newSamples[newSampleSize] = newSample;
							newSampleSize++;
						}
					}

					samples = newSamples;
				}
				else
				{
					// Estimate new size of new sample array
					int newSampleSize = 0;

					for( Double subject : candidates.keySet() )
						for( FAAnion sFaAnion : candidates.get( subject ) )
							for ( Double target : candidates.keySet() )
								if ( !subject.equals( target ) )
									for ( FAAnion tFaAnion : candidates.get( target ) )
										newSampleSize++;


					FASample[] newSamples = new FASample[newSampleSize];
					newSampleSize = 0;

					// Split logic
					for( Double subject : candidates.keySet() )
					{
						// Checking if there are multiple isomers
						final boolean bSubjectMinMax = minMaxMap.containsKey( subject );

						Range<Float> subjectRatio = null;
						Float subjectIsoMin = null;

						if( bSubjectMinMax )
						{
							subjectRatio = minMaxMap.get( subject );
							System.out.println(subject + "(sbj):" + subjectRatio);
							subjectIsoMin = getMinIsomer( candidates.get( subject ) );
						}

						for( FAAnion sFaAnion : candidates.get( subject ) )
						{
							for ( Double target : candidates.keySet() )
							{
								if ( !subject.equals( target ) )
								{
									// Checking if there are multiple isomers
									final boolean bTargetMinMax = minMaxMap.containsKey( target );

									Range<Float> targetRatio = null;
									Float targetIsoMin = null;

									if( bTargetMinMax )
									{
										targetRatio = minMaxMap.get( target );
										System.out.println(target + "(tag):" + targetRatio);
										targetIsoMin = getMinIsomer( candidates.get( target ) );
									}

									for ( FAAnion tFaAnion : candidates.get( target ) )
									{
										System.out.println( clazz + " " + tFaAnion.getKey() + "/" + sFaAnion.getKey() );
										FASample newSample = new FASample( clazz + " " + tFaAnion.getKey() + "/" + sFaAnion.getKey() );
										newSample.setMass( subject );
										newSample.setComplement( clazz + " " + sFaAnion.getKey() + "/" + tFaAnion.getKey()  );

										FASample subjectSample = findSample( subject, samples );


										// Apply the factor
										for ( Float ce : ceSet )
										{
											Range< Float > fai = subjectSample.get( ce ).getDBCorrectedFAI( sFaAnion.getFAIsomer() );
											Float noCorrectedFAI = subjectSample.get( ce ).getNoCorrectedFAI();
											Float noCorrectedSum = subjectSample.get( ce ).getNoCorrectedSum();
                                 TreeMap<Float, Float> map = subjectSample.get( ce ).getDbcfMap();
											Range< Float > orgFAI = subjectSample.get( ce ).getOrgFAI();

											System.out.println( "[Sum] " + ce + ":" + fai );

											if( bSubjectMinMax )
											{
												Range< Float > subjectFactor = subjectRatio;
												if ( !sFaAnion.getFAIsomer().equals( subjectIsoMin ) )
												{
													subjectFactor = Range.between( 1f - subjectRatio.getMaximum(), 1f - subjectRatio.getMinimum() );
													fai = Range.between(
															fai.getMinimum() * subjectFactor.getMinimum(),
															fai.getMaximum() * subjectFactor.getMaximum() );

													noCorrectedFAI = noCorrectedFAI * subjectFactor.getMaximum();
													noCorrectedSum = noCorrectedSum * subjectFactor.getMaximum();

													orgFAI = Range.between(
															orgFAI.getMinimum() * subjectFactor.getMinimum(),
															orgFAI.getMaximum() * subjectFactor.getMaximum() );
												}
												else
												{
													fai = Range.between(
															fai.getMinimum() * subjectFactor.getMaximum(),
															fai.getMaximum() * subjectFactor.getMinimum() );

													noCorrectedFAI = noCorrectedFAI * subjectFactor.getMaximum();
													noCorrectedSum = noCorrectedSum * subjectFactor.getMaximum();

													orgFAI = Range.between(
															orgFAI.getMinimum() * subjectFactor.getMaximum(),
															orgFAI.getMaximum() * subjectFactor.getMinimum() );
												}
											}

											if( bTargetMinMax )
											{
												Range< Float > targetFactor = targetRatio;
												if ( !tFaAnion.getFAIsomer().equals( targetIsoMin ) )
												{
													targetFactor = Range.between( 1f - targetRatio.getMaximum(), 1f - targetRatio.getMinimum() );
													fai = Range.between(
															fai.getMinimum() * targetFactor.getMinimum(),
															fai.getMaximum() * targetFactor.getMaximum() );

													noCorrectedFAI = noCorrectedFAI * targetFactor.getMaximum();
													noCorrectedSum = noCorrectedSum * targetFactor.getMaximum();

													orgFAI = Range.between(
															orgFAI.getMinimum() * targetFactor.getMinimum(),
															orgFAI.getMaximum() * targetFactor.getMaximum() );
												}
												else
												{
													fai = Range.between(
															fai.getMinimum() * targetFactor.getMaximum(),
															fai.getMaximum() * targetFactor.getMinimum() );

													noCorrectedFAI = noCorrectedFAI * targetFactor.getMaximum();
													noCorrectedSum = noCorrectedSum * targetFactor.getMaximum();

													orgFAI = Range.between(
															orgFAI.getMinimum() * targetFactor.getMaximum(),
															orgFAI.getMaximum() * targetFactor.getMinimum() );
												}
											}

//											System.out.println( "[Split] " + ce + ":" + fai );

											newSample.add( ce, subjectSample.get( ce ).getPos(), fai, sumMap.get( ce ), noCorrectedFAI, noCorrectedSum, map, orgFAI );
										}

										newSamples[newSampleSize] = newSample;
										newSampleSize++;
									}
								}
							}
						}
					}

					samples = newSamples;
				}
			}
		}

		return samples;
	}

	public static FASample findSample( String key, FASample[] samples )
	{
		for( FASample sample : samples )
		{
			if(sample.getKey().equals( key ))
				return sample;
		}

		return null;
	}

	public static FASample findSample( Double faMz, FASample[] samples )
	{
		for( FASample sample : samples )
		{
			if(sample.getMass().equals( faMz ))
				return sample;
		}

		return null;
	}

	public static Float getMinIsomer(List< FAAnion > faAnions)
	{
		Float result = Float.MAX_VALUE;

		for( FAAnion faAnion : faAnions )
			result = Float.min( result, faAnion.getFAIsomer() );

		return result;
	}

	public static Float getMaxIsomer(List< FAAnion > faAnions)
	{
		Float result = Float.MIN_VALUE;

		for( FAAnion faAnion : faAnions )
			result = Float.max( result, faAnion.getFAIsomer() );

		return result;
	}

	public static List< FAAnion > getFAanions( String clazz, Double mass,
			BARow row, ObservableList< FAAnion > mFaAnionsList )
	{
		final int carbon = row.getCarbon();
		final int db = row.getDb();

		final int index = getIndex( mFaAnionsList, mass, carbon, db );
		if(index == 0) return null;

		final FAAnion faAnion = masterDBSet.get( clazz ).get( index );

		return getMassIndexes( masterDBSet.get( clazz ).values(), faAnion );
	}

	public static boolean checkSplitIsomer( String clazz, String faMz,
			BARow row, ObservableList< FAAnion > mFaAnionsList )
	{
		final double mass = Double.parseDouble( faMz );

		List< FAAnion > faAnions = getFAanions( clazz, mass, row, mFaAnionsList );

		return !faAnions.stream().anyMatch( c -> c.getMass().equals( mass ) && row.getIsomer().equals( c.getFAIsomer() ) );
	}

	public static void computeNorm( boolean noCorrection, LinkedHashMap< FASample, FASample[] > faSampleMap )
	{
		// In order to calculate MSpecie profile, make the sum values each NCE
		HashMap< String, TreeMap<Float, Range<Float> > > lSpecieSumMap = new HashMap<>(  );

		for( FASample priSample : faSampleMap.keySet() )
		{
			FASample sample = faSampleMap.get( priSample )[0];

			if( null != sample )
			{
				//String key = sample.getKey();
				String key = sample.getKey().split( " ", 2 )[ 0 ];

				if( !lSpecieSumMap.containsKey( key ) )
				{
					lSpecieSumMap.put( key, new TreeMap<>() );

					for( Float ce : priSample.getNCE() )
						lSpecieSumMap.get(key).put( ce, sample.get(ce).getSum() );
				}
				else
				{
					for( Float ce : priSample.getNCE() )
					{
						Range<Float> sum = lSpecieSumMap.get(key).get(ce);
						Range<Float> fai = sample.get(ce).getSum();

						lSpecieSumMap.get(key).put(ce, Range.between( sum.getMinimum() + fai.getMinimum(), sum.getMaximum() + fai.getMaximum() ));
					}
				}
			}
		}

		for( FASample priSample : faSampleMap.keySet() )
		{
			FASample[] samples = faSampleMap.get( priSample );

			for( FASample sample : samples )
			{
				if( null != sample )
				{
					//String key = sample.getKey();
					String key = sample.getKey().split( " ", 2 )[ 0 ];

					for( Float ce : priSample.getNCE() )
					{
						Range<Float> sum = lSpecieSumMap.get(key).get(ce);

						Range<Float> fai = sample.get(ce).getFai(noCorrection);

						Float min = sum.getMinimum() == 0f ? 0f : fai.getMinimum() / sum.getMinimum() * 100;
						Float max = sum.getMaximum() == 0f ? 0f : fai.getMaximum() / sum.getMaximum() * 100;

						sample.get( ce ).setNormedFAI( Range.between( min, max ) );
					}
				}
			}
		}
	}

	public static void createGroupData( TreeMap< String, ArrayList< String > > groupMap,
			TreeMap< String, LinkedHashMap< FASample, FASample[] > > sampleData,
			final boolean bGroupOnly, final boolean bMergeIsomerGlobal,
			boolean bNoCorrection, ObservableList< FAAnion > mFaAnionsList )
	{
//		if( bMergeIsomerLocal )
//		{
//			mergeUnspecifiedIsomer( groupMap, sampleData, mFaAnionsList );
//		}

		if( bMergeIsomerGlobal )
		{
			mergeIsomerGlobalHomogeneous( groupMap, sampleData, mFaAnionsList );
		}


		for( String groupKey : groupMap.keySet() )
		{
			LinkedHashMap< String, LinkedHashSet<FASample> > faSampleMap = new LinkedHashMap<>(  );

			for( String sampleId : groupMap.get(groupKey) )
			{
				LinkedHashMap< FASample, FASample[] > sampleMap = sampleData.get( sampleId );

				for ( FASample priSample : sampleMap.keySet() )
				{
					final String specie = priSample.getKey();

					if( ! faSampleMap.containsKey( specie ) )
						faSampleMap.put( specie, new LinkedHashSet<>(  ) );

					for( FASample faSample : sampleMap.get( priSample ) )
						faSampleMap.get( specie ).add( faSample );
				}
			}

			LinkedHashMap< FASample, FASample[] > newGroupMap = new LinkedHashMap<>(  );
			HashMap< String, FASample > groupPriMap = new HashMap<>(  );

			for( String sampleId : groupMap.get(groupKey) )
			{
				LinkedHashMap< FASample, FASample[] > sampleMap = sampleData.get( sampleId );

				for( FASample priSample : sampleMap.keySet() )
				{
					final String specie = priSample.getKey();
					FASample[] faSamples = faSampleMap.get( specie ).toArray( new FASample[]{} );

					if( !groupPriMap.containsKey( specie ) )
					{
						FASample groupPri = new FASample( priSample, priSample.getNCE() );
						groupPriMap.put( specie, groupPri );

						FASample[] groupFASamples = new FASample[faSamples.length];
						for( int i = 0; i < faSamples.length; i++ )
						{
							groupFASamples[i] = new FASample( faSamples[i] );
						}
						newGroupMap.put( groupPri, groupFASamples );
					}

					// Add data into the existing data structure
					FASample groupPri = groupPriMap.get( specie );
					FASample[] groupFASamples = newGroupMap.get( groupPri );

					groupPri.addIntensityData( priSample.getIntensity() );

					for( int i = 0; i < groupFASamples.length; i++ )
					{
						for( FASample faSample : faSamples )
							if(groupFASamples[i].getKey().equals( faSample.getKey() ))
								groupFASamples[i].addData( groupPri.getNCE(), faSample );
					}
				}
			}

			// Make the sum values each NCE and set NormedFAI
			// In order to calculate MSpecie profile, make the sum values each NCE
			computeNorm( bNoCorrection, newGroupMap );

			sampleData.put( groupKey, newGroupMap );
		}


//		if( bGroupOnly )
//		{
//			Set<String> groupKeySet = groupMap.keySet();
//			sampleData.keySet().retainAll( groupKeySet );
//		}
	}

//	private static void mergeUnspecifiedIsomer( TreeMap< String, ArrayList< String > > groupMap,
//			TreeMap< String, LinkedHashMap< FASample, FASample[] > > sampleData,
//			ObservableList< FAAnion > mFaAnionsList )
//	{
//		// Check all the FASamples in each PRI sample if there is inconsistent isomer splits
//		for ( String groupKey : groupMap.keySet() )
//		{
//			LinkedHashMap< String, LinkedHashMap< String, Float > > faSampleNameMap = new LinkedHashMap<>();
//
//			for ( String sampleId : groupMap.get( groupKey ) )
//			{
//				LinkedHashMap< FASample, FASample[] > sampleMap = sampleData.get( sampleId );
//
//				for ( FASample priSample : sampleMap.keySet() )
//				{
//					final String specie = priSample.getKey();
//
//					if ( !faSampleNameMap.containsKey( specie ) )
//						faSampleNameMap.put( specie, new LinkedHashMap<>() );
//
//					for ( FASample faSample : sampleMap.get( priSample ) )
//					{
//						if ( !faSampleNameMap.get( specie ).containsKey( faSample.getKey() ) )
//							faSampleNameMap.get( specie ).put( faSample.getKey(), 0f );
//
//						faSampleNameMap.get( specie ).put( faSample.getKey(), faSampleNameMap.get( specie ).get( faSample.getKey() ) + 1 );
//					}
//				}
//			}
//
//			for ( String sampleId : groupMap.get( groupKey ) )
//			{
//				LinkedHashMap< FASample, FASample[] > sampleMap = sampleData.get( sampleId );
//				float size = groupMap.get( groupKey ).size();
//
//				for ( FASample priSample : sampleMap.keySet() )
//				{
//					final String specie = priSample.getKey();
//					final String clazz = specie.split( " " )[ 0 ];
//
//					LinkedHashMap< String, Float > map = faSampleNameMap.get( specie );
//					boolean bMerge = false;
//					LinkedHashSet< String > mergeFASamples = null;
//
//					LinkedHashMap< String, FASample > mergedMap = new LinkedHashMap<>(  );
//
//					for ( FASample faSample : sampleMap.get( priSample ) )
//					{
//						if ( map.get( faSample.getKey() ) / size < 0.5f )
//						{
//							if ( !bMerge )
//							{
//								bMerge = true;
//								mergeFASamples = new LinkedHashSet<>();
//							}
//							mergeFASamples.add( faSample.getKey() );
//							List< FAAnion > faAnions = getMassIndexes( mFaAnionsList, faSample.getMass() );
//
//							Float isoMax = getMaxIsomer( faAnions );
//							Float isoMin = getMinIsomer( faAnions );
//
//							int carbon = faAnions.get( 0 ).getFACarbon();
//							int db = faAnions.get( 0 ).getFADoubleBonds();
//
//							String key;
//
//							if ( isoMin.equals( 0f ) || isoMax.equals( 0f ) )
//								key = String.format( "%2d:%d", carbon, db );
//							else
//							{
//								if ( isoMax.equals( isoMin ) )
//								{
//									key = String.format( "%2d:%d (%.0fz)", carbon, db, isoMin );
//								}
//								else
//								{
//									key = String.format( "%2d:%d (%.0fz-%.0fz)", carbon, db, isoMin, isoMax );
//								}
//							}
//
//							if( !mergedMap.containsKey( key ) )
//							{
//								mergedMap.put( key, new FASample( key ) );
//							}
//
//							faSample.setSecondKey( key );
//
//							mergedMap.get( key ).addIntensityData( priSample.getNCE(), faSample );
//						}
//						else
//							mergedMap.put( faSample.getKey(), faSample );
//					}
//
//					if ( bMerge )
//					{
//						FASample[] samples = sampleMap.get( priSample );
//						HashSet< String > visited = new HashSet<>();
//
//						for ( String faSampleKey : mergeFASamples )
//						{
//							if ( visited.contains( faSampleKey ) )
//								continue;
//
//							FASample subject = findSample( faSampleKey, samples );
//							FASample target = findSample( subject.getComplement(), samples );
//							visited.add( subject.getComplement() );
//
//							// Generate the fraction string
//							if ( target == null )
//							{
//								String sn0Key = String.format( "%s %s", clazz, subject.getSecondKey() );
//
//								subject.setKey( sn0Key );
//								mergedMap.get( subject.getSecondKey() ).setKey( sn0Key );
//							}
//							else
//							{
//								String sn1Key = String.format( "%s %s/%s", clazz, subject.getSecondKey(), target.getSecondKey() );
//								String sn0Key = String.format( "%s %s/%s", clazz, target.getSecondKey(), subject.getSecondKey() );
//
//								subject.setKey( sn0Key );
//								target.setKey( sn1Key );
//								mergedMap.get( subject.getSecondKey() ).setKey( sn0Key );
//								mergedMap.get( target.getSecondKey() ).setKey( sn1Key );
//								mergedMap.get( subject.getSecondKey() ).setComplement( sn1Key );
//								mergedMap.get( target.getSecondKey() ).setComplement( sn0Key );
//							}
//						}
//
//						sampleMap.put( priSample, mergedMap.values().toArray( new FASample[]{} ) );
//					}
//				}
//			}
//		}
//	}

	private static void mergeIsomerGlobalHomogeneous( TreeMap< String, ArrayList< String > > groupMap,
			TreeMap< String, LinkedHashMap< FASample, FASample[] > > sampleData,
			ObservableList< FAAnion > mFaAnionsList )
	{
		for ( String groupKey : groupMap.keySet() )
		{
			for ( String sampleId : groupMap.get( groupKey ) )
			{
				LinkedHashMap< FASample, FASample[] > sampleMap = sampleData.get( sampleId );

				for ( FASample priSample : sampleMap.keySet() )
				{
					final String specie = priSample.getKey();
					final String clazz = specie.split( " " )[ 0 ];

					LinkedHashSet< String > mergeFASamples = new LinkedHashSet<>();
					LinkedHashMap< String, FASample > mergedMap = new LinkedHashMap<>();

					for ( FASample faSample : sampleMap.get( priSample ) )
					{
						mergeFASamples.add( faSample.getKey() );
						List< FAAnion > faAnions = getMassIndexes( mFaAnionsList, faSample.getMass() );

						Float isoMax = getMaxIsomer( faAnions );
						Float isoMin = getMinIsomer( faAnions );

						int carbon = faAnions.get( 0 ).getFACarbon();
						int db = faAnions.get( 0 ).getFADoubleBonds();

						String key;

						if ( isoMin.equals( 0f ) || isoMax.equals( 0f ) )
							key = String.format( "%2d:%d", carbon, db );
						else
						{
							if ( isoMax.equals( isoMin ) )
							{
								key = String.format( "%2d:%d (%.0fz)", carbon, db, isoMin );
							}
							else
							{
								key = String.format( "%2d:%d (%.0fz-%.0fz)", carbon, db, isoMin, isoMax );
							}
						}

						if( !mergedMap.containsKey( key ) )
						{
							mergedMap.put( key, new FASample( key ) );
						}

						faSample.setSecondKey( key );

						mergedMap.get( key ).addIntensityData( priSample.getNCE(), faSample );
					}

					FASample[] samples = sampleMap.get( priSample );
					HashSet< String > visited = new HashSet<>();

					for ( String faSampleKey : mergeFASamples )
					{
						if ( visited.contains( faSampleKey ) )
							continue;

						FASample subject = findSample( faSampleKey, samples );
						FASample target = findSample( subject.getComplement(), samples );
						visited.add( subject.getComplement() );

						// Generate the fraction string
						if ( target == null )
						{
							String sn0Key = String.format( "%s %s", clazz, subject.getSecondKey() );

							subject.setKey( sn0Key );
							mergedMap.get( subject.getSecondKey() ).setKey( sn0Key );
						}
						else
						{
							String sn1Key = String.format( "%s %s/%s", clazz, subject.getSecondKey(), target.getSecondKey() );
							String sn0Key = String.format( "%s %s/%s", clazz, target.getSecondKey(), subject.getSecondKey() );

							subject.setKey( sn0Key );
							target.setKey( sn1Key );
							mergedMap.get( subject.getSecondKey() ).setKey( sn0Key );
							mergedMap.get( target.getSecondKey() ).setKey( sn1Key );
							mergedMap.get( subject.getSecondKey() ).setComplement( sn1Key );
							mergedMap.get( target.getSecondKey() ).setComplement( sn0Key );
						}
					}

					sampleMap.put( priSample, mergedMap.values().toArray( new FASample[]{} ) );
				}
			}
		}
	}


	public static class FASample
	{
		String key;
		Float intensity;
		TreeMap<Float, FASampleData> data;
		private Set< Float > NCE;
		private String secondKey;
		private Double mass;
		private String complement;
		Range<Float> intensityRange;

		public FASample( String key )
		{
			this.key = key;
		}

		public FASample( FASample faSample )
		{
			key = faSample.getKey();
			intensity = faSample.getIntensity();
			secondKey = faSample.getSecondKey();
			mass = faSample.getMass();
			complement = faSample.getComplement();
		}

		public FASample( FASample faSample, Set<Float> NCE )
		{
			key = faSample.getKey();
			intensity = 0f;
			secondKey = faSample.getSecondKey();
			mass = faSample.getMass();
			this.NCE = new TreeSet<>( NCE );
		}

		public String getKey()
		{
			return key;
		}

		public String getFullKey()
		{
			return key + " " + secondKey;
		}

		public void setKey( String key )
		{
			this.key = key;
		}

		public void add( Float ce, Range< Float > pos, Range< Float > fai, Range< Float > sum,
				Float noCorrectedFAI, Float noCorrectedSum, TreeMap< Float, Float > dbcfMap, Range<Float> orgFAI )
		{
			if( null == data )
				data = new TreeMap<>(  );

			data.put( ce, new FASampleData( pos, fai, sum, noCorrectedFAI, noCorrectedSum, dbcfMap, orgFAI ) );
		}

		public void addData( Set<Float> NCE, FASample faSample )
		{
			if( null == data )
				data = new TreeMap<>(  );

			NCE.forEach( ce ->
			{
				if ( !data.containsKey( ce ) )
					data.put( ce, new FASampleData( faSample.get(ce).getPos(), faSample.get(ce).getFai(), faSample.get(ce).getSum(), faSample.get(ce).getNoCorrectedFAI(), faSample.get(ce).getNoCorrectedSum(), faSample.get(ce).getDbcfMap(), faSample.get(ce).getOrgFAI() ) );
				else
				{
					data.get( ce ).mergePos( faSample.get(ce).getPos() );
					data.get( ce ).mergeFai( faSample.get(ce).getFai() );
					data.get( ce ).mergeSum( faSample.get(ce).getSum() );
					data.get( ce ).mergeOrgFai( faSample.get(ce).getOrgFAI() );
				}
			});
		}

		public void addIntensityData( Set<Float> NCE, FASample faSample )
		{
			if( null == data )
				data = new TreeMap<>(  );

			NCE.forEach( ce ->
			{
				if ( !data.containsKey( ce ) )
					data.put( ce, new FASampleData( faSample.get(ce).getPos(), faSample.get(ce).getFai(), faSample.get(ce).getSum(), faSample.get(ce).getNoCorrectedFAI(), faSample.get(ce).getNoCorrectedSum(), faSample.get(ce).getDbcfMap(), faSample.get(ce).getOrgFAI() ) );
				else
				{
					data.get( ce ).mergePos( faSample.get( ce ).getPos() );
					data.get( ce ).addFai( faSample.get( ce ).getFai() );
					data.get( ce ).addSum( faSample.get( ce ).getSum() );
					data.get( ce ).addOrgFai( faSample.get( ce ).getOrgFAI() );
				}
			});
		}

		public void addIntensityData( Set<Float> NCE, FASample faSample, FASample faSample2 )
		{
			if( null == data )
				data = new TreeMap<>(  );

			NCE.forEach( ce ->
			{
				data.put( ce, new FASampleData(
                  merge( faSample.get(ce).getPos(), faSample2.get(ce).getPos() ),
                  faSample.get(ce).getSum(),
						faSample.get(ce).getSum(),
                  faSample.get(ce).getNoCorrectedSum(),
						faSample.get(ce).getNoCorrectedSum(),
                  faSample.get(ce).getDbcfMap(),
                  merge( faSample.get(ce).getOrgFAI(), faSample2.get(ce).getOrgFAI() ) ) );
			});
		}

		public Range<Float> merge(Range<Float> a, Range<Float> b)
		{
			return Range.between( Float.min( a.getMinimum(), b.getMinimum()), Float.max( a.getMaximum(), b.getMaximum() ) );
		}

		public Range<Float> add(Range<Float> a, Range<Float> b)
		{
			return Range.between( a.getMinimum() + b.getMinimum(), a.getMaximum() + b.getMaximum() );
		}

		public Set<Float> keySet()
		{
			return data.keySet();
		}

		public FASampleData get( Float ce )
		{
			return data.get( ce );
		}

		public boolean contains( Float ce )
		{
			return data.containsKey( ce );
		}

		public Float getIntensity()
		{
			return intensity;
		}

		public void setIntensity( Float intensity )
		{
			this.intensity = intensity;
		}

		public void addIntensity( Float intensity )
		{
			this.intensity += intensity;
		}

		public void addIntensityData( Float intensity )
		{
			if( intensityRange == null )
				intensityRange = Range.between( intensity, intensity );

			if( intensityRange.isAfter( intensity ) )
				intensityRange = Range.between( intensity, intensityRange.getMaximum() );
			else if( intensityRange.isBefore( intensity ) )
				intensityRange = Range.between( intensityRange.getMinimum(), intensity );
		}

		public String getIntensityDataString()
		{
			if( null != intensityRange )
				return intensityRange.toString();
			else return intensity.toString();
		}

		public Float getNormIntensity( Float basis, Float pct )
		{
			return intensity / basis * pct;
		}

		public Range<Float> getIntensityRange()
		{
			return intensityRange;
		}

		public String getNormIntensityDataString( Float basis, Range<Float> basisRange, Float pct )
		{
			if( null != intensityRange )
				return Range.between( intensityRange.getMinimum() / basisRange.getMinimum() * pct, intensityRange.getMaximum() / basisRange.getMaximum() * pct ).toString();
			else
				return Float.toString( intensity / basis * pct );
		}

		public void setNCE( Set<Float> NCE )
		{
			this.NCE = NCE;
		}

		public Set< Float > getNCE()
		{
			return NCE;
		}

		public void setSecondKey( String secondKey )
		{
			this.secondKey = secondKey;
		}

		public String getSecondKey()
		{
			return secondKey;
		}

		public Double getMass()
		{
			return mass;
		}

		public void setMass( Double mass )
		{
			this.mass = mass;
		}

		public void setNormedFAI( Float ce, Range<Float> normedFAI )
		{
			data.get(ce).setNormedFAI( normedFAI );
		}

		public String getComplement()
		{
			return complement;
		}

		public void setComplement( String complement )
		{
			this.complement = complement;
		}
	}

	public static class FASampleData
	{
		Range<Float> pos;
		Range<Float> fai;
		Range<Float> sum;
		Range<Float> normedFAI;
		Range<Float> orgFAI;
		Range<Float> viewFAI;

		Float noCorrectedFAI;
		Float noCorrectedSum;

      TreeMap< Float, Float > dbcfMap;

		public FASampleData( Range< Float > pos, Range< Float > fai, Range< Float > sum, Float noCorrectedFAI, Float noCorrectedSum, TreeMap< Float, Float > dbcfMap, Range< Float > orgFAI )
		{
			this.pos = pos;
			this.fai = fai;
			this.sum = sum;
			this.noCorrectedFAI = noCorrectedFAI;
			this.noCorrectedSum = noCorrectedSum;

         if(null != dbcfMap)
            this.dbcfMap = dbcfMap;

         this.orgFAI = orgFAI;
		}

		public Range< Float > getPos()
		{
			return pos;
		}

		public void mergePos( Range< Float > pos )
		{
			this.pos = Range.between( Float.min( this.pos.getMinimum(), pos.getMinimum() ), Float.max( this.pos.getMaximum(), pos.getMaximum() ) );
		}

		public Range< Float > getFai()
		{
			return fai;
		}

		public Range< Float > getFai(boolean noCorrection)
		{
			if(noCorrection)
				return Range.between( noCorrectedFAI, noCorrectedFAI );
			else
				return fai;
		}

		public void setFai( Range< Float > fai )
		{
			this.fai = fai;
		}

		public void mergeFai( Range< Float > fai )
		{
			this.fai = Range.between( Float.min( this.fai.getMinimum(), fai.getMinimum()), Float.max( this.fai.getMaximum(), fai.getMaximum() ) );
		}

		public Range< Float > getNormedFAI(boolean noCorrection)
		{
			return normedFAI;
		}

		public void setNormedFAI( Range<Float> normedFAI )
		{
			this.normedFAI = normedFAI;
		}

		public Range< Float > getSum()
		{
			return sum;
		}

		public void setSum( Range< Float > sum )
		{
			this.sum = sum;
		}

		public void mergeSum( Range< Float > sum )
		{
			this.sum = Range.between( Float.min( this.sum.getMinimum(), sum.getMinimum()), Float.max( this.sum.getMaximum(), sum.getMaximum() ) );
		}

		public Range< Float > getRatio()
		{
			return Range.between( fai.getMinimum() / sum.getMinimum(), fai.getMaximum() / sum.getMaximum() );
		}

		public Range< Float > getRatio(boolean noCorrection, Range<Float> sum)
		{
			if(noCorrection)
				return Range.between( noCorrectedFAI / sum.getMaximum(), noCorrectedFAI / sum.getMaximum() );
			else
				return Range.between( fai.getMinimum() / sum.getMinimum(), fai.getMaximum() / sum.getMaximum() );
		}

//		public String getNormQuantityDataString( Range<Float> basisRange, Float pct )
//		{
//			Range<Float> fai = this.getOrgFAI();
//
//			return Range.between( fai.getMinimum() / basisRange.getMinimum() * pct, fai.getMaximum() / basisRange.getMaximum() * pct ).toString();
//		}

		public Float getNoCorrectedFAI()
		{
			return noCorrectedFAI;
		}

		public void setNoCorrectedFAI( Float noCorrectedFAI )
		{
			this.noCorrectedFAI = noCorrectedFAI;
		}

		public Float getNoCorrectedSum()
		{
			return noCorrectedSum;
		}

		public void setNoCorrectedSum( Float noCorrectedSum )
		{
			this.noCorrectedSum = noCorrectedSum;
		}

      public TreeMap< Float, Float > getDbcfMap()
      {
         return dbcfMap;
      }

      public Range<Float> getDBCorrectedFAI( Float faIsomer )
      {
         if( dbcfMap != null )
            return Range.between( noCorrectedFAI * dbcfMap.get( faIsomer ), noCorrectedFAI * dbcfMap.get( faIsomer ) );
         else
            return fai;
      }

      public void addFai( Range< Float > fai )
		{
			this.fai = Range.between( this.fai.getMinimum() + fai.getMinimum(), this.fai.getMaximum() + fai.getMaximum() );
		}

		public void addSum( Range< Float > sum )
		{
			this.sum = Range.between( this.sum.getMinimum() + sum.getMinimum(), this.sum.getMaximum() + sum.getMaximum() );
		}

		public void addOrgFai( Range< Float > orgFai )
		{
			this.orgFAI = Range.between( this.orgFAI.getMinimum() + orgFai.getMinimum(), this.orgFAI.getMaximum() + orgFai.getMaximum() );
		}

		public Range< Float > getOrgFAI()
		{
			return orgFAI;
		}

		public void mergeOrgFai( Range< Float > orgFai )
		{
			this.orgFAI = Range.between( Float.min( this.orgFAI.getMinimum(), orgFai.getMinimum()), Float.max( this.orgFAI.getMaximum(), orgFai.getMaximum() ) );
		}

		public Range< Float > getViewFAI()
		{
			return viewFAI;
		}

		public void setViewFAI( Range< Float > viewFAI )
		{
			this.viewFAI = viewFAI;
		}
	}
}
