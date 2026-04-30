package de.mpicbg.ms.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * File conversion class for RAW and XML files
 */
public class FileConvert
{
   public static File convertRaw( File file )
   {
      String filePath = file.getAbsolutePath();

      String dll = file.getParent() + File.separator + "zlib1.dll";

      try
      {
         if ( Files.notExists( Paths.get( dll ) ) )
         {
            InputStream is = FileConvert.class.getResourceAsStream( "zlib1.dll" );
            OutputStream os = new FileOutputStream( dll );
            byte[] buffer = new byte[ 2048 ];
            int read;
            while ( ( read = is.read( buffer ) ) != -1 )
            {
               os.write( buffer, 0, read );
            }
            is.close();
            os.close();
         }
      }
      catch ( FileNotFoundException e )
      {
         e.printStackTrace();
      }
      catch ( IOException e )
      {
         e.printStackTrace();
      }

      String exec = file.getParent() + File.separator + "ReAdW.exe";

      try
      {
         if ( Files.notExists( Paths.get( exec ) ) )
         {
            InputStream is = FileConvert.class.getResourceAsStream( "ReAdW.exe" );
            OutputStream os = new FileOutputStream( exec );
            byte[] buffer = new byte[ 2048 ];
            int read;
            while ( ( read = is.read( buffer ) ) != -1 )
            {
               os.write( buffer, 0, read );
            }
            is.close();
            os.close();
         }
      }
      catch ( FileNotFoundException e )
      {
         e.printStackTrace();
      }
      catch ( IOException e )
      {
         e.printStackTrace();
      }

      System.out.println( "Run.." + exec );
      Exec.run( exec, "-c", filePath );

      // Generated Xml File name
      String newXmlName = filePath.substring( 0, filePath.lastIndexOf( "." ) ).concat( ".mzXML" );
      File xmlFile = new File( newXmlName );

      if ( xmlFile.exists() )
      {
         System.out.println( xmlFile + " is successfully generated." );
      }
      else
      {
         System.err.println( xmlFile + " is not generated." );
      }

      return xmlFile;
   }

   public static List< File > checkRawFiles( File file )
   {
      ArrayList< File > list = new ArrayList<>();
      if ( file.isDirectory() )
      {
         for ( File child : file.listFiles( ( dir, name ) -> name.toLowerCase().endsWith( ".raw" ) ) )
         {
            list.add( child );
         }
      }
      else if ( file.isFile() && file.getName().toLowerCase().endsWith( ".raw" ) )
      {
         list.add( file );
      }

      return list;
   }

   public static List< File > checkXmlFiles( File file )
   {
      ArrayList< File > list = new ArrayList<>();
      if ( file.isDirectory() )
      {
         for ( File child : file.listFiles( ( dir, name ) -> name.toLowerCase().endsWith( ".mzxml" ) ) )
         {
            list.add( child );
         }
      }
      else if ( file.isFile() && file.getName().toLowerCase().endsWith( ".mzxml" ) )
      {
         list.add( file );
      }

      return list;
   }
}
