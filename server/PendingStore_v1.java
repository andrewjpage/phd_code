/* Thomas Keane
Purpose: To store all of the current pending units.

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

import java.util.*;
import java.io.*;
import java.util.logging.*;

public class PendingStore_v1 extends FastDiskDataStore_v1
{
	public PendingStore_v1( File root, Logger error )
	{
		super( root, error );
	}

	//add a data unit to the store
	public synchronized void addUnit( Long unitID, Long algorithmID, Long timeLimit, byte[] DataUnit, Integer exceptions, Integer expired, Integer cpu, Integer mem )
	{
		//record the time the unit was entered into the store
		long timeNow = System.currentTimeMillis();
		
		//check if there is a directory for the problem
		File probDir = new File( parentDirectory, algorithmID.toString() );
		if( ! probDir.isDirectory() )
		{
			//this is the first unit from this problem - create directory
			probDir.mkdir();
		}

		//create the file and write the data unit bytes to the file
		try
		{
			File unit = new File( probDir, unitID + "_" + timeNow + "_" + timeLimit + "_" + exceptions + "_" + expired + "_" + cpu + "_" + mem ); //see NOTE above
			unit.createNewFile();
			FileOutputStream fos = new FileOutputStream( unit );
			fos.write( DataUnit );
			fos.close();
			
			if( ! unit.exists() || unit.length() <= 0 )
			{
				errorLog.warning( "Unit " + unitID + " file is empty. AlgorithmID " + algorithmID );
			}
			
			size ++;
		}
		catch( IOException e )
		{
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			PrintStream ps = new PrintStream( bos );
			e.printStackTrace( ps );
			errorLog.severe( "Failed to write unit to disk: " + bos.toString() );
		}
	}
	
	//check if any units have expired and return a list of files that correspond to the expired units
	public synchronized Vector getExpired()
	{
		//a vector to hold the names of the files of expired units
		Vector expired = new Vector();
		
		//get the current time to compare with
		long timeNow = System.currentTimeMillis();
		
		//must go through EVERY unit on the disk
		File[] probs = parentDirectory.listFiles();
		if( probs != null )
		{
			for( int i = 0; i < probs.length; i ++ )
			{
				if( probs[ i ].getName().endsWith( "old" ) )
				{
					continue; //these units are being removed - algorithm no longer exists
				}
				File[] units = probs[ i ].listFiles();
				
				//if there was an io error or empty directory - goto next directory
				if( units == null )
				{
					continue;
				}
				
				//go thru the units and check if expired
				for( int j = 0; j < units.length; j ++ )
				{
					//see if unit is expired
					StringTokenizer stk = new StringTokenizer( units[ j ].getName(), "_" );
					try
					{
						int uid = Integer.parseInt( stk.nextToken() );
						long timeEntered = Long.parseLong( stk.nextToken() );
						long timeLimit = Long.parseLong( stk.nextToken() );
						if( timeEntered + timeLimit < timeNow ) //if unit is expired
						{
							//unit is expired - add to string of expired units
							expired.add( units[ j ] );
						}
					}
					catch( NumberFormatException nfe )
					{
						errorLog.warning( "Failed to check if unit is expired" );
						continue;
					}
				}
			}
			size -= expired.size();
		}
		return expired;
	}
	
	//record a time extension for a unit
	public synchronized Long extendTime( Long unitID, Long algorithmID )
	{
		//find the file
		File probDir = new File( parentDirectory, algorithmID.toString() );
		if( probDir.isDirectory() )
		{
			File[] units = probDir.listFiles();
			String unit = unitID.toString();
			for( int i = 0; i < units.length; i ++ )
			{
				StringTokenizer stk = new StringTokenizer( units[ i ].getName(), "_" );
				if( stk.nextToken().equalsIgnoreCase( unit ) )
				{
					//rename the unit file with extension
					stk = new StringTokenizer( units[ i ].getName(), "_" );
					String newName = stk.nextToken() + "_" + System.currentTimeMillis();
					stk.nextToken(); //old time entered
					Long timeLimit = null;
					try
					{
						timeLimit = new Long( stk.nextToken() ); //get the time limit for the unit
						newName = newName + "_" + timeLimit + "_" + stk.nextToken() + "_" + stk.nextToken() + "_" + stk.nextToken() + "_" + stk.nextToken();
					}
					catch( NumberFormatException nfe )
					{
						errorLog.warning( "Failed to convert time limit to Long" );
						return new Long( 0 );
					}

					File extended = new File( probDir, newName );
					
					//rename the file and do various checks to make sure the file was renamed correctly
					if( ! units[ i ].renameTo( extended ) ) //rename the file/unit with extension
					{
						errorLog.warning( "Failed to extend unit. Problem " + algorithmID + " Unit " + unitID );
						return new Long( 0 );
					}
					else if( ! extended.exists() )
					{
						errorLog.warning( "Failed to extend unit. Problem " + algorithmID + " Unit " + unitID + ". New extended file doesnt exist" );
						return new Long( 0 );
					}
					else if( units[ i ].exists() )
					{
						errorLog.warning( "Old unit file still exists after renaming file with extension. Problem " + algorithmID + " Unit " + unitID + ". New extended file doesnt exist" );
						return new Long( 0 );
					}
					else
					{
						return timeLimit;
					}
				}
			}
			errorLog.warning( "Failed to find unit: " + unit );
			return new Long( 0 );
		}
		
		errorLog.warning( "Failed to locate problem directory: " + probDir.getName() );
		return new Long( 0 );
	}
	
	//adding a unit that is in the expired store - and the client that was originally processing it has requested an extension
	//before the unit was handed out to another client to be processed
	public synchronized Long addExtendedExpiredUnit( Long algorithmID, File unitData )
	{
		StringTokenizer stk = new StringTokenizer( unitData.getName(), "_" );
		String newName = stk.nextToken() + "_" + System.currentTimeMillis();
		stk.nextToken();
		try
		{
			Long timeLimit = new Long( stk.nextToken() );
			newName = newName + "_" + timeLimit + "_" + stk.nextToken() + "_" + stk.nextToken() + "_" + stk.nextToken() + "_" + stk.nextToken();
			
			//find the directory to put the unit into
			File[] algorithms = parentDirectory.listFiles();
			for( int i = 0; i < algorithms.length; i ++ )
			{
				if( algorithms[ i ].getName().equals( algorithmID.toString() ) ) //if found the directory
				{
					File newUnit = new File( algorithms[ i ], newName );
					if( ! unitData.renameTo( newUnit ) ) //move the file from the expired list to the pending list
					{
						errorLog.warning( "Failed to move extended unit from expired list to pending list" );
						return new Long( 0 );
					}
					else if( unitData.exists() )
					{
						errorLog.warning( "Unit still exists in expired store after moving to pending list. Problem " + algorithmID + " Unit " + unitData.getName() );
					}
					else if( ! newUnit.exists() )
					{
						errorLog.warning( "Unit doesnt exist in pending store after moving from expired store. Problem " + algorithmID + " Unit " + unitData.getName() );
					}
					else
					{
						//unit entered into pending list ok - return the time limit
						size ++;
						return timeLimit;
					}
				}
			}
		}
		catch( NumberFormatException nfe )
		{
			errorLog.warning( "Failed to extract unit information" );
			return new Long( 0 );
		}
		return new Long( 0 );
	}
}
