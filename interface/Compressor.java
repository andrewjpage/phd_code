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

class Compressor
{
	private static ByteArrayOutputStream baos;
	private static ObjectOutputStream oos;
	private static ObjectInputStream ois;

	//compress the passed in object into a byte[]
	public static byte[] compress( Object o )
	{
		try
		{
			baos = new ByteArrayOutputStream();
        		oos = new ObjectOutputStream(new GZIPOutputStream(baos));
      		
			//compress the object
			oos.writeObject(o);
			oos.flush();
			oos.close();
			oos = null;

		}catch( Exception e )
		{
			System.err.println( "Exception in compressor: " );
			e.printStackTrace( System.err );
			return null;
		}

		//return the compressed byte[]	
		return baos.toByteArray();
	}

	//decompress the passed in byte[] 
	public static Object decompress( byte[] b )
	{
		try
		{
			ois = new ObjectInputStream( new GZIPInputStream( new ByteArrayInputStream( b ) ) );
			Object o = ois.readObject();
			ois.close();
			ois = null;
			
			return o;

		}catch( Exception e )
		{
			System.err.println( "Exception in compressor: " );
			e.printStackTrace( System.err );
			return null;
		}
	}
	
	public static void reset()
	{
		baos = null;
		oos = null;
		ois = null;
	}
}