/* Thomas Keane
This is the classloader that will be used by the client to load in classes that are downloaded
from the server as byte[]. It keeps a cache of the algorithms that have already been loaded
into the client.

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
import java.util.*;
import java.net.*;

class AlgorithmLoader extends URLClassLoader
{
	//cache of already loaded classes
	private Hashtable cache;
	private Throwable exceptionDetails;
	private boolean reset;
	
	//default constructor
	public AlgorithmLoader( URL[] urls )
	{
		super( urls, ClassLoader.getSystemClassLoader() ); //use the sytem class loader for delegation
		cache = new Hashtable();
		exceptionDetails = null;
		reset = false;
	}

	//called to see if the class has already been loaded into the client
	public Class findClass( Long algorithmID )
	{
		//get the name of the class
		for( Enumeration e = cache.keys(); e.hasMoreElements(); )
		{
			Long k = (Long) e.nextElement();
			if( k.compareTo( algorithmID ) == 0 ) //find the key corresponding to this algorithm
			{
				StringTokenizer entry = new StringTokenizer( (String) cache.get( k ) );
				try
				{
					//find and get an instance of the class
					return findLoadedClass( entry.nextToken() );
				}
				catch( Throwable t )
				{
					exceptionDetails = t;
					return null;
				}
			}
		}

		//if the class has not already been loaded - return null
		return null;
	}

	//called when findClass returns null pass in the byte[] containing the class
	public Class loadNewAlgorithm( Long algorithmID, byte[] algorithm, byte[] hash )
	{
		Class algo = null;
		try
		{
			//now must define the class and return Class object
			algo = defineClass( null, algorithm, 0, algorithm.length );
		}
		catch( NoClassDefFoundError n )
		{
			exceptionDetails = n;
			return null;
		}
		//catch the case where we have received a duplicate algorithm (different ID)
		catch( LinkageError le )
		{
			String error = le.toString();
			int index = error.lastIndexOf( ':' );
			String className = error.substring( index + 2 );
			if( cache.containsValue( className + " " + new String( hash ) ) ) //if the class has been loade by another problem
			{
				//make sure the algorithm name is in the cache for next time
				if( ! cache.containsKey( algorithmID ) )
				{
					cache.put( algorithmID, className + " " + new String( hash ) );
					return findLoadedClass( className );
				}
			}
			else
			{
				//this is a new version of an existing algorithm - must reset the loader (cant unload classes!)
				reset = true;
				return null;
			}
		}
		catch( Throwable e )
		{
			exceptionDetails = e;
			return null;
		}

		//get the name of the class - not known already
		String name = algo.getName();

		//enter it into our cache
		cache.put( algorithmID, name + " " + new String( hash ) );

		//return the Class object so new instances can be created
		return algo;
	}

	//method for loading the other class defs needed by the algorithm
	//return 1 if success writing jars
	//return -1 if duplicate name jar found (different size of jar files)
	//return 0 if an exception occurred 
	public int loadJarFiles( File classes_dir, Vector jarDefinitions )
	{
		//make sure directory exists first
		if( ! classes_dir.isDirectory() )
		{
			classes_dir.mkdir();
		}
		
		try
		{
			FileOutputStream fos = null;
			URL[] urls = getURLs();
			for( int i = 0; i < jarDefinitions.size(); i = i + 2 )
			{
				boolean existsAlready = false;
				String name = (String) jarDefinitions.get( i );

				for( int j = 0; j < urls.length; j ++ )
				{
					String fileName = urls[ j ].getFile();
					File f = new File( fileName );
					int index = fileName.lastIndexOf( (int) '/' );
					
					if( fileName.substring( index + 1 ).equals( name )  )
					{
						if( f.length() != ( (byte[]) jarDefinitions.get( i + 1 ) ).length )
						{
							return -1; //found a file in the class path with same name but different size - must reset the algorithm loader
						}
						else
						{
							existsAlready = true; //file already in classpath - dont do anything
						}
					}
				}
				
				if( ! existsAlready )
				{
					//check if the jar files already exist - if so overwrite
					File jarF = new File( classes_dir, (String) jarDefinitions.get( i ) );
					fos = new FileOutputStream( jarF );
					fos.write( (byte[]) jarDefinitions.get( i + 1 ) );
					fos.close();
					addURL( jarF.toURL() );
				}
			}
			return 1;
		}
		catch( Throwable e )
		{
			exceptionDetails = e;
			return 0;
		}
	}

	public Throwable getException()
	{
		return exceptionDetails;
	}
	
	public Class locateProblemClass( String name ) throws Throwable
	{
		return loadClass( name );
	}
	
	public boolean getReset()
	{
		return reset;
	}
}