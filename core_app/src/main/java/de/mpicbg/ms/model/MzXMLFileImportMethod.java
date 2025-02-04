package de.mpicbg.ms.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.tools.mzxml_parser.MzXMLFile;
import uk.ac.ebi.pride.tools.mzxml_parser.mzxml.model.Scan;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This class reads XML-based mass spec data formats (mzData, mzXML, and mzML)
 * using the jmzreader library.
 */
public class MzXMLFileImportMethod
{

   private final Logger logger = LoggerFactory.getLogger( this.getClass() );

   private final File sourceFile;

   private boolean canceled = false;

   private ScanCollection scanCollection;
   private long totalScans = 0, parsedScans;

   /**
    * <p>
    * Constructor for MzXMLFileImportMethod.
    * </p>
    * @param sourceFile a {@link java.io.File} object.
    */
   public MzXMLFileImportMethod( File sourceFile )
   {
      this.sourceFile = sourceFile;
   }

   public ScanCollection executeForMs2() throws ParsingRuntimeException
   {

      logger.info( "Started parsing file " + sourceFile );

      try
      {

         MzXMLFile parser = new MzXMLFile( sourceFile );

         totalScans = parser.getMS2ScanCount();

         // Prepare data structures
         List< Scan > scansList = new ArrayList<>();
         double mzValues[] = new double[ 10000 ];
         float intensityValues[] = new float[ 10000 ];

         // Create the XMLBasedRawDataFile object
         scanCollection = new ScanCollection( sourceFile, parser, scansList );

         // Create the converter from jmzreader data model to our data model
         //			final MzXMLConverter converter = new MzXMLConverter();

         MzXMLFile.MzXMLScanIterator iter = parser.getMS2ScanIterator();
         while ( iter.hasNext() )
         {
            if ( canceled )
               return null;

            Scan scan = iter.next();

            if ( scan != null && scan.getCollisionEnergy() != null )
            {
               scansList.add( scan );
               parsedScans++;
            }
         }

      }
      catch ( Exception e )
      {
         throw new ParsingRuntimeException( e );
      }

      logger.info( "Finished importing " + sourceFile + ", parsed " + parsedScans + " scans" );

      logger.info( "Total " + totalScans + ", parsed " + parsedScans + " scans" );

      return scanCollection;
   }

   public Float getFinishedPercentage()
   {
      return totalScans == 0 ? null : ( float ) parsedScans / totalScans;
   }

   public void cancel()
   {
      this.canceled = true;
   }
}
