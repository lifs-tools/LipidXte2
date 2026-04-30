package de.mpicbg.ms.view.pane;

import org.dockfx.DockNode;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

/**
 * Standard Output and Error capture console
 */
public class StdOutputCaptureConsole extends DockNode
{
   public StdOutputCaptureConsole()
   {
      super( new ConsolePane() );
      setTitle( "Console" );

      ConsolePane consolePane = ( ConsolePane ) getContents();

      try
      {
         System.setOut( new StreamAppender( "INFO", consolePane, System.out ) );
         System.setErr( new StreamAppender( "WARN", consolePane, System.err ) );
      }
      catch ( UnsupportedEncodingException e )
      {
         e.printStackTrace();
      }
   }

   public class StreamAppender extends PrintStream
   {
      final private StringBuilder buffer;
      final private String prefix;
      final private TextAppender textAppender;

      StreamAppender( String prefix, TextAppender consumer, PrintStream old ) throws UnsupportedEncodingException
      {
         super( old, true, "UTF-8" );

         this.prefix = prefix;
         this.buffer = new StringBuilder( 128 );
         buffer.append( "[" ).append( prefix ).append( "] " );
         this.textAppender = consumer;
      }

      @Override
      public void write( byte buf[], int off, int len )
      {
         try
         {
            String string = new String( buf, off, len );
            buffer.append( string );
            if ( string.equals( "\n" ) || string.equals( "\r\n" ) )
            {
               String outString = buffer.toString();
               textAppender.appendText( outString );

               buffer.delete( 0, buffer.length() );
               buffer.append( "[" ).append( prefix ).append( "] " );
            }

            out.write( buf, off, len );
         }
         catch ( IOException e )
         {
            e.printStackTrace();
         }
      }
   }
}
