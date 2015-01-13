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
import java.util.*;

public class Compressor
{
	private ByteArrayOutputStream baos;
	private ObjectOutputStream oos;
	private ProblemObjectInputStream pois;
	private ClassLoader ccl;

	public Compressor( ClassLoader cl )
	{
		baos = null;
		oos = null;
		pois = null;
		ccl = cl;
	}
	
	//compress the passed in object into a byte[]
	public synchronized byte[] compress( Object o )
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
		}catch( Exception e )
		{
			System.err.println( "Exception in compressor: " );
			e.printStackTrace( System.err );
			return null;
		}

		//return the compressed byte[]
		byte[] b = baos.toByteArray();
		
		//set everything to null
		baos = null;
		oos = null;
		
		return b;
	}

	//decompress the passed in byte[]
	public synchronized Object decompress( byte[] b )
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
			
			pois = null; 
			
			return o;

		}catch( Exception e )
		{
			System.err.println( "Exception in compressor: " );
			e.printStackTrace( System.err );
			return null;
		}
	}
}
