/*
Thomas Keane, 15:55 22/07/2003
classloader for loading files from a problem into the system
N.B. doesnt use system class loader as parent (so old problem classes can be unloaded when problem is removed from system)

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

import java.util.logging.*;
import java.util.*;
import java.io.*;
import java.util.zip.*;
import java.util.jar.*;
import java.net.*;
import java.lang.reflect.*;

public class ProblemLoader extends URLClassLoader
{
	private Logger systemLog;
	private Logger errorLog;
	private Long problemID;
	private File problemDirectory;

	private String dmName;
	private String algorithmName;
	
	private Object dataManager; //the user defined datamanager
	private Method processResults;
	private Method adjustGranularity;
	private Method closeResources;
	private Method generateWorkUnit;
	private Method getStatus;
	
	public ProblemLoader( Long probID, Logger syst, Logger err, URL[] urls, File problemDir )
	{
		super( urls );
		dmName = null;
		algorithmName = null;
		systemLog = syst;
		errorLog = err;
		problemID = probID;
		problemDirectory = problemDir;
	}
	
	public String loadDM( byte[] user_dataManager_bytes )
	{
		try
		{
			//set the PROBLEMDIRECTORY field of the dataManager
			Class parentDM = findClass( "DataManager" );
			Field probDir = parentDM.getField( "PROBLEMDIRECTORY" );
			probDir.set( null, problemDirectory ); //set the problem directory for the datamanager to work within

			//get the user-defined 
			Class dm = defineClass( null, user_dataManager_bytes, 0, user_dataManager_bytes.length );

			if( dm.getSuperclass().getName().compareTo( "DataManager" ) != 0 )
			{				
				errorLog.warning( "Attempt to Load Invalid DataManager - SuperClass is not DataManager: " + dm.getSuperclass().getName() );
				return "Attempt to Load Invalid DataManager - SuperClass is not DataManager: " + dm.getSuperclass().getName();
			}
			
			Method[] dm_methods = dm.getMethods();
			for( int i = 0; i < dm_methods.length; i ++ )
			{
				if( dm_methods[ i ].getName().compareTo( "processResults" ) == 0 )
				{
					processResults = dm_methods[ i ];
				}
				else if( dm_methods[ i ].getName().compareTo( "generateWorkUnit" ) == 0 )
				{
					generateWorkUnit = dm_methods[ i ];
				}
				else if( dm_methods[ i ].getName().compareTo( "getStatus" ) == 0 )
				{
					getStatus = dm_methods[ i ];
				}
				else if( dm_methods[ i ].getName().compareTo( "adjustGranularity" ) == 0 )
				{
					adjustGranularity = dm_methods[ i ];
				}
				else if( dm_methods[ i ].getName().compareTo( "closeResources" ) == 0 )
				{
					closeResources = dm_methods[ i ];
				}
			}

			//check that there is a default constructor
			Constructor[] cons = dm.getConstructors();
			if( cons.length == 0 )
			{
				errorLog.warning( "Attempt to Load Invalid DataManager - no default constructor defined" );
				return "Attempt to Load Invalid DataManager - no default constructor defined";
			}
			else if( cons[ 0 ].getParameterTypes().length != 0 )
			{
				errorLog.warning( "Attempt to Load Invalid DataManager - no default constructor defined" );
				return "Attempt to Load Invalid DataManager - no default constructor defined";
			}
			
			systemLog.info( "Successfully Loaded Datamanager" );
			dmName = dm.getName();
			dataManager = dm.newInstance();
		}
		catch( NoClassDefFoundError e )
		{
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			PrintStream ps = new PrintStream( bos );
			e.printStackTrace( ps );
			errorLog.warning( "Couldnt find required Class Definition: " + bos.toString() );
			return "Couldnt find required Class Definition for DataManager: " + e;
		}
		catch( LinkageError le )
		{
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			PrintStream ps = new PrintStream( bos );
			le.printStackTrace( ps );
			errorLog.info( "LinkageError - Attempt to load a DataManager class that is same as the Algorithm: " + bos.toString() );
			return "Attempt to load Algorithm as DataManager";
		}
		catch( Throwable e )
		{
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			PrintStream ps = new PrintStream( bos );
			e.printStackTrace( ps );
			errorLog.warning( "Failed to load DataManager: " + bos.toString() );
			return "Failed to load DataManager: " + e;
		}
		return null;
	}
	
	public Vector getDataUnit() throws Throwable
	{
		return (Vector) generateWorkUnit.invoke( dataManager, null );
	}
	
	public boolean processResults( Long unitID, Vector results ) throws Throwable
	{
		Object[] args = { unitID, results };
		return ( (Boolean) processResults.invoke( dataManager, args ) ).booleanValue();
	}
	
	public void adjustGranularity( int percent ) throws Throwable
	{
		Object[] args = { new Integer( percent ) };
		adjustGranularity.invoke( dataManager, args );
	}
	
	public String getStatus() throws Throwable
	{
		return (String) getStatus.invoke( dataManager, null );
	}
	
	public void closeResources() throws Throwable
	{
		closeResources.invoke( dataManager, null );
	}
	
	public String loadAlgorithm( byte[] algorithmBytes )
	{
		try
		{
			//first load the algorithm parent class
			//Class algorithmParent = defineClass( "Algorithm", algorithmParentBytes, 0, algorithmParentBytes.length );
			//resolveClass( algorithmParent );

			//check it is a valid class file
			Class al = defineClass( null, algorithmBytes, 0, algorithmBytes.length );

			if( ! al.getSuperclass().getName().equals( "Algorithm" ) )
			{
				errorLog.warning( "Attempt to Load Invalid Algorithm - SuperClass is not Algorithm: " + al.getSuperclass().getName() );
				return "Attempt to Load Invalid Algorithm - SuperClass is not Algorithm: " + al.getSuperclass().getName();
			}
			
			//check that there is a default constructor
			Constructor[] cons = al.getConstructors();
			if( cons.length > 0 && cons[ 0 ].getParameterTypes().length != 0 )
			{
				errorLog.warning( "Attempt to Load Invalid Algorithm - only allowed define a default constructor" );
				return "Attempt to Load Invalid Algorithm - only allowed define a default constructor";
			}
			
			systemLog.info( "Successfully Checked Algorithm" );
			algorithmName = al.getName();
		}
		catch( NoClassDefFoundError e )
		{
			errorLog.warning( "Couldnt find required Class Definition: " + e );
			return "Couldnt find required Class Definition for Algorithm: " + e;
		}
		catch( LinkageError le )
		{
			errorLog.info( "LinkageError - class already in system: " + le );
		}
		catch( Throwable e )
		{
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			PrintStream ps = new PrintStream( bos );
			e.printStackTrace( ps );
			errorLog.warning( "Failed to load Algorithm: " + bos.toString() );
			return "Failed to load Algorithm: " + e;
		}
		return null;
	}

	public String getDMName()
	{
		return dmName;
	}
	
	public String getAlgorithmName()
	{
		return algorithmName;
	}
	
	public Class locateProblemClass( String name ) throws Throwable
	{
		return loadClass( name );
	}
}
