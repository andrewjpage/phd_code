/* Thomas Keane
A class that is used to compress/decompress objects.

Copyright (C) 2003  Thomas Keane

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
*/

import java.io.*;
import java.util.zip.*;
import java.net.URL;

class Compressor
{
	private static ByteArrayOutputStream baos;
	private static ObjectOutputStream oos;
	private static ProblemObjectInputStream pois;

	//compress the passed in object into a byte[]
	public static byte[] compress( Object o )
	{
		//if a null object passed in - then output is also null
		if( o == null )
		{
			return null;
		}
		
		try
		{
			baos = new ByteArrayOutputStream();
			GZIPOutputStream gzos = new GZIPOutputStream( baos );
        		oos = new ObjectOutputStream( gzos );

			//compress the object
			oos.writeObject( o );
			gzos.finish();
			oos.flush();
			oos.close();
			
		}catch( IOException e )
		{
			return null;
		}

		//return the compressed byte[]	
		return baos.toByteArray();
	}

	//decompress the passed in byte[] 
	public static Object decompress( ClassLoader ccl, byte[] b )
	{
		//if b is null - return null
		if( b == null )
		{
			return null;
		}
		
		try
		{
			pois = new ProblemObjectInputStream( new GZIPInputStream( new ByteArrayInputStream( b ) ), ccl );
			Object o = pois.readObject();
			pois.close();
			
			return o;
		}catch( Exception e )
		{
			e.printStackTrace( System.out );
			return null;
		}
	}
	
	public static void reset()
	{
		baos = null;
		oos = null;
		pois = null;
	}
}