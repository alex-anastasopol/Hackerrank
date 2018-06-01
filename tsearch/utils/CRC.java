package ro.cst.tsearch.utils;

import java.util.zip.* ;

/** A class that makes it easier to create CRC codes. <p>

    This class inherits java.util.zip.CRC32 - so the real power is there.  This class replaces update(int)
    because the CRC32.update(int) considers only the first byte.  This class considers all four bytes of the int.<p>

    Further, this class adds update(String), update(byte), update(char), update(long),
    update(double), update(float) and update(boolean) for convenience.<p>

    This class also adds getInt31() which returns a 31 bit unsigned value converted from
    CRC32.getValue().<p>

    The original CRC32.update(byte[]), CRC32.getValue() and CRC32.reset() methods are still available.<p>

    Example:

    <pre>

*       class Employee
*       {
*
*           private String name ;
*           private String ssn ;
*           private int salary ;
*
*           public int hashCode()
*           {
*               CRC c = new CRC();
*               c.update( name );
*               c.update( ssn );
*               c.update( salary );
*               return c.getInt31();
*           }
*
*       }

    </pre>

    
    - - - - - - - - - - - - - - - - - <p>
    
    Copyright (c) 1998-2000 Paul Wheaton <p>
    
    You are welcome to do whatever you want to with this source file provided
    that you maintain this comment fragment (between the dashed lines).
    Modify it, change the package name, change the class name ... personal or business 
    use ...  sell it, share it ... add a copyright for the portions you add ... <p>
    
    My goal in giving this away and maintaining the copyright is to hopefully direct 
    developers back to JavaRanch. <p>
    
    The original source can be found at <a href=http://www.javaranch.com>JavaRanch</a> <p>
    
    - - - - - - - - - - - - - - - - - <p>

    @author Paul Wheaton
*/
public class CRC extends CRC32
{

    public CRC()
    {
        super();
    }

    /** work an int into the existing CRC code.<p>

        @param i an integer to work into the CRC code.  All four bytes are worked in.<p>
    */
    public void update( int i )
    {
        byte[] buffy = new byte[4];
        buffy[0] = (byte) ( i >>> 24 ) ;
        buffy[1] = (byte) ( i >>> 16 ) ;
        buffy[2] = (byte) ( i >>> 8 ) ;
        buffy[3] = (byte) ( i ) ;
        update( buffy );
    }

    /** work a byte into the existing CRC code.<p>

        @param b a byte to work into the CRC code.  All eight bits are worked in.<p>
    */
    public void update( byte b )
    {
        // I would call super.update( b ) except that if b is negative, I would be concerned that
        // the negation might get lost in the translation.
        byte[] buffy = new byte[1];
        buffy[0] = b ;
        update( buffy );
    }

    /** work a char into the existing CRC code.<p>

        @param c a char to work into the CRC code.  Both bytes are worked in.<p>
    */
    public void update( char c )
    {
        byte[] buffy = new byte[2];
        buffy[0] = (byte) ( c >>> 8 ) ;
        buffy[1] = (byte) ( c ) ;
        update( buffy );
    }

    /** work a long into the existing CRC code.<p>

        @param i a long integer to work into the CRC code.  All eight bytes are worked in.<p>
    */
    public void update( long i )
    {
        byte[] buffy = new byte[8];
        buffy[0] = (byte) ( i >>> 56 ) ;
        buffy[1] = (byte) ( i >>> 48 ) ;
        buffy[2] = (byte) ( i >>> 40 ) ;
        buffy[3] = (byte) ( i >>> 32 ) ;
        buffy[4] = (byte) ( i >>> 24 ) ;
        buffy[5] = (byte) ( i >>> 16 ) ;
        buffy[6] = (byte) ( i >>> 8 ) ;
        buffy[7] = (byte) ( i ) ;
        update( buffy );
    }

    /** work a double into the existing CRC code.<p>

        @param d a double precision float to work into the CRC code.  All eight bytes are worked in.<p>
    */
    public void update( double d )
    {
        // convert all the bits of d to fit into a long, then have update( long ) do the hard work
        update( Double.doubleToLongBits( d ) );
    }

    /** work a float into the existing CRC code.<p>

        @param f a floating point value to work into the CRC code.  All four bytes are worked in.<p>
    */
    public void update( float f )
    {
        // convert all the bits of f to fit into an int, then have update( int ) do the hard work
        update( Float.floatToIntBits( f ) );
    }

    /** work a boolean into the existing CRC code.<p>

        @param b a boolean to work into the CRC code.  true is worked in as a byte of value 1 and
        false is worked in as a byte of value 0.<p>
    */
    public void update( boolean b )
    {
        byte byteVal = 0 ;
        if ( b )
        {
            byteVal = 1 ;

        }
        byte[] buffy = new byte[1];
        buffy[0]= byteVal ;
        update( buffy );
    }

    /** work a string into the existing CRC code.<p>

        @param s a string to work into the CRC code.  Two bytes for every character is worked in.
        The length is not directly worked in.<p>
    */
    public void update( String s )
    {
        // copy all of the chars in s to a char array and then copy
        // both bytes of every char to a byte array.  Then work all
        // of those bytes into the CRC
        int len = s.length();
        char[] chars = new char[ len ];
        s.getChars( 0 , len , chars , 0 );
        byte[] buffy = new byte[ len * 2 ];
        int i ;
        for( i = 0 ; i < len ; i++ )
        {
            int buffyIndex = i * 2 ;
            char c = chars[ i ];
            buffy[ buffyIndex ] = (byte) ( c >>> 8 ) ;
            buffy[ buffyIndex + 1 ] = (byte) ( c ) ;
        }
        update( buffy );
    }

    /** get the resulting CRC value as an unsigned 31 bit value.<p>

        Use getValue() to get the real CRC value.<p>

        This is useful because it avoids the problems some code might have with a negative value.
        Plus, it makes for a clean and consistant conversion to an int as used for hashCode().<p>

        @return an unsigned 31 bit CRC value (a 32 bit CRC with the sign bit chopped off).<p>
    */
    public int getInt31()
    {
        return (int) ( getValue() & Integer.MAX_VALUE ) ;
    }
    
    /** Get the CRC of a string. <p>
    
        @return an unsigned 31 bit CRC value (a 32 bit CRC with the sign bit chopped off).<p>
    */
    public static int quick( String s )
    {
        CRC crc = new CRC();
        crc.update( s );   
        return crc.getInt31();
    }
}


