package de.mpicbg.ms.model.data;

import de.mpicbg.ms.model.Fragment;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.math3.util.Precision;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Fatty Acid Data Model class
 */
public class FAAnion implements Comparable
{
   Integer index;
   Double mass;
   Integer FACarbon;
   Integer FADoubleBonds;
   Float FAIsomer;

   CO2 co2mass;
   String name;

   boolean sn1;
   boolean sn2;

   Fragment fragment;
   Fragment correctionFactor;

   public FAAnion()
   {

   }

   public FAAnion( int index, double mass, float isomer )
   {
      this.name = "" + index;
      this.index = index;
      this.mass = mass;
      this.FAIsomer = isomer;
   }

   public FAAnion( int index, double mass, int carbon, int doubleBonds, float isomer, double co2 )
   {
      this.name = "" + index;
      this.index = index;
      this.mass = mass;
      this.FACarbon = carbon;
      this.FADoubleBonds = doubleBonds;
      this.FAIsomer = isomer;
      this.co2mass = new CO2( co2 );
   }

   public FAAnion( int index, FAAnion faAnion )
   {
      this.name = "" + index;
      this.index = index;
      this.mass = faAnion.getMass();
      this.FACarbon = faAnion.getFACarbon();
      this.FADoubleBonds = faAnion.getFADoubleBonds();
      this.FAIsomer = faAnion.getFAIsomer();
      this.co2mass = faAnion.getCo2mass();
   }

   public FAAnion( String name )
   {
      this.index = 0;
      this.name = name;
   }

   @Override public boolean equals( Object obj )
   {
      return index.equals( obj );
   }

   @Override public int hashCode()
   {
      return index.hashCode();
   }

   @Override public String toString()
   {
      return getIndex() + ":" + getMass().toString();
   }

   public Integer getIndex()
   {
      return index;
   }

   public void setIndex( Integer index )
   {
      this.index = index;
   }

   public Double getMass()
   {
      return mass;
   }

   public void setMass( Double mass )
   {
      this.mass = mass;
   }

   public Integer getFACarbon()
   {
      return FACarbon;
   }

   public void setFACarbon( Integer faCarbon )
   {
      this.FACarbon = faCarbon;
   }

   public Integer getFADoubleBonds()
   {
      return FADoubleBonds;
   }

   public void setFADoubleBonds( Integer faDoubleBonds )
   {
      this.FADoubleBonds = faDoubleBonds;
   }

   public Float getFAIsomer()
   {
      return FAIsomer;
   }

   public void setFAIsomer( Float faIsomer )
   {
      this.FAIsomer = faIsomer;
   }

   public CO2 getCo2mass()
   {
      return co2mass;
   }

   public void setCo2mass( CO2 co2mass )
   {
      this.co2mass = co2mass;
   }

   public String getName()
   {
      return name;
   }

   public void setName( String name )
   {
      this.name = name;
   }

   public Fragment getFragment()
   {
      return fragment;
   }

   public void setFragment( Fragment fragment )
   {
      this.fragment = fragment;
   }

   public Fragment getCorrectionFactor()
   {
      return correctionFactor;
   }

   public void setCorrectionFactor( Fragment correctionFactor )
   {
      this.correctionFactor = correctionFactor;
   }

   public boolean isSn1()
   {
      return sn1;
   }

   public void setSn1( boolean sn1 )
   {
      this.sn1 = sn1;
   }

   public boolean isSn2()
   {
      return sn2;
   }

   public void setSn2( boolean sn2 )
   {
      this.sn2 = sn2;
   }

   public static List< FAAnion > tryParseAnalyteInput( String contents, final int mmu )
   {
      final int digit = 3 - ( int ) Math.log10( mmu );
      final ArrayList< FAAnion > arrayList = new ArrayList<>();
      StringReader reader = new StringReader( contents );
      CSVParser parser = null;

      try
      {
         parser = CSVFormat.TDF.withHeader().withNullString( "" ).parse( reader );
      }
      catch ( IOException e )
      {
         e.printStackTrace();
      }

      if ( parser != null )
      {
         parser.forEach( c ->
         {
            //			System.out.println(  + "-" + c.get("mz") + "-" + c.get("FA_C") + c.get("FA_DB") + c.get("FA_iso") );

            Double fragment = Double.parseDouble( c.get( "mz" ) );
            Double roundedValue = Precision.round( fragment, digit );
            Double co2 = Precision.round( roundedValue - Precision.round( 43.99, digit ), digit );

            arrayList.add( new FAAnion( Integer.parseInt( c.get( "FA_anion" ) ),
                    roundedValue,
                    Integer.parseInt( c.get( "FA_C" ) ),
                    Integer.parseInt( c.get( "FA_DB" ) ),
                    Float.parseFloat( c.get( "FA_iso" ) ),
                    co2
            ) );
         } );
      }

      return arrayList;
   }

   public static String update( String contents, final List< FAAnion > list )
   {
      StringReader reader = new StringReader( contents );
      CSVParser parser = null;

      StringWriter writer = new StringWriter();
      CSVPrinter printer = null;

      try
      {
         printer = new CSVPrinter( writer, CSVFormat.TDF );
         printer.printRecord( "FA_anion", "mz", "FA_C", "FA_DB", "FA_iso", "FA_index" );

         parser = CSVFormat.TDF.withHeader().withNullString( "" ).parse( reader );
      }
      catch ( IOException e )
      {
         e.printStackTrace();
      }

      if ( parser != null )
      {
         HashMap< Integer, FAAnion > map = new HashMap<>();
         list.stream().forEach( c -> map.put( c.getIndex(), c ) );

         final CSVPrinter finalPrinter = printer;
         parser.forEach( c ->
         {

            //			System.out.println(  + "-" + c.get("mz") + "-" + c.get("FA_C") + c.get("FA_DB") + c.get("FA_iso") );

            Integer id = Integer.parseInt( c.get( "FA_anion" ) );
            Double fragment = Double.parseDouble( c.get( "mz" ) );
            Integer faC = Integer.parseInt( c.get( "FA_C" ) );
            Integer faDb = Integer.parseInt( c.get( "FA_DB" ) );
            Integer faIso = Integer.parseInt( c.get( "FA_iso" ) );

            try
            {
               finalPrinter.printRecord( id, fragment, faC, faDb, faIso );
            }
            catch ( IOException e )
            {
               e.printStackTrace();
            }

         } );
      }

      return writer.toString();
   }

   public boolean containsCo2mz( Set< Double > refinedMz )
   {
      //System.out.println( co2mass );

      return refinedMz.contains( co2mass.getMass() );
   }

   @Override public int compareTo( Object o )
   {
      return index.compareTo( ( ( FAAnion ) o ).getIndex() );
   }

   public String getKey()
   {
      if ( getFAIsomer().equals( 0f ) )
         return String.format( "%2d:%d", getFACarbon(), getFADoubleBonds() );
      else if ( getFAIsomer() == getFAIsomer().intValue() )
         return String.format( "%2d:%d (%.0fz)", getFACarbon(), getFADoubleBonds(), getFAIsomer() );
      else
         return String.format( "%2d:%d (%.1fz)", getFACarbon(), getFADoubleBonds(), getFAIsomer() );
   }
}
