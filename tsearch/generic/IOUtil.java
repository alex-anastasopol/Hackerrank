package ro.cst.tsearch.generic;

import java.io.*;

public final class IOUtil
{
    /**
     * Private constructor to prevent instantiation.
     */
    private IOUtil()
    {
    }

    public static void shutdownStream( final OutputStream output )
    {
        if( null == output ) return;

        try { output.close(); }
        catch( final IOException ioe ) {}
    }

    public static void shutdownStream( final InputStream input )
    {
        if( null == input ) return;

        try { input.close(); }
        catch( final IOException ioe ) {}
    }

    public static void shutdownWriter( final Writer writer )
    {
        if( null == writer ) return;

        try { writer.close(); }
        catch( final IOException ioe ) {}
    }



    /**
     * Copy stream-data from source to destination.
     */
    public static void copy( final InputStream source, final OutputStream destination )
        throws IOException
    {
        try
        {
            final BufferedInputStream input = new BufferedInputStream( source );
            final BufferedOutputStream output = new BufferedOutputStream( destination );

            final int BUFFER_SIZE = 1024 * 4;
            final byte[] buffer = new byte[ BUFFER_SIZE ];

            while( true )
            {
                final int count = input.read( buffer, 0, BUFFER_SIZE );
                if( -1 == count ) break;

                // write out those same bytes
                output.write( buffer, 0, count );
            }

            //needed to flush cache
            output.flush();
        }
        finally
        {
            shutdownStream( source );
            shutdownStream( destination );
        }
    }
}
