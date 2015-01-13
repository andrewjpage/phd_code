/* Thomas Keane, Mon Nov 01 10:12:46 GMT 2004 @467 /Internet Time/
Purpose: To store all of the current expired units.
Has some specialised methods for the expired store only

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

public class ExpiredStore_v1 extends FastDiskDataStore_v1
{
	public ExpiredStore_v1( File root, Logger error )
	{
		super( root, error );
	}
	
	//add a unit to this expired list
	//move the unit files from the pending store to the expired store directory
	public synchronized void addExpiredUnits( Vector expired )
	{		
		for( int i = 0; i < expired.size(); i ++ )
		{
			//increment the number of times the unit has expired
			File unit = (File) expired.get( i );
			StringTokenizer stk = new StringTokenizer( unit.getName(), "_" );
			String newName = stk.nextToken() + "_" + stk.nextToken() + "_" + stk.nextToken() + "_" + stk.nextToken();
			try
			{
				int expiredTimes = Integer.parseInt( stk.nextToken() ) + 1;
				newName = newName + "_" + expiredTimes + "_" + stk.nextToken() + "_" + stk.nextToken();
			}
			catch( NumberFormatException nfe )
			{
				errorLog.warning( "Failed to create new expired unit file name" );
				continue;
			}
			
			//move the file to the expired store
			File pdir = new File( parentDirectory, unit.getParentFile().getName() );
			if( ! pdir.isDirectory() ) //if there isnt a directory corresponding to this problem
			{
				pdir.mkdir();
			}
			
			//now move the unit from the pending store to the expired store
			File expiredUnit = new File( pdir, newName );
			if( ! unit.renameTo( expiredUnit ) )
			{
				errorLog.warning( "Failed to move an expired unit from the pending store to the expired store. Expired Unit: " + expiredUnit.getAbsolutePath() );
				continue;
			}
			else if( ! expiredUnit.exists() )
			{
				errorLog.warning( "Failed to move expired unit from pending to expired store. Expired Unit: " + expiredUnit.getAbsolutePath() );
			}
			size ++;
		}
	}
	
	//return a suitable unit from the expired list
	public synchronized File returnUnit( int clientCPU, int clientMemory )
	{
		File[] problems = parentDirectory.listFiles();
		
		//io error - return null
		if( problems == null )
		{
			errorLog.warning( "Failed to access expired directory or directory empty" );
			return null;
		}
		
		//find a unit
		for( int i = 0; i < problems.length; i ++ )
		{
			if( problems[ i ].getName().endsWith( "old" ) )
			{
				continue; //the problem was removed recently - ignore its units
			}

			File[] units = problems[ i ].listFiles();
			if( units.length > 0 )
			{
				StringTokenizer stk = new StringTokenizer( units[ 0 ].getName(), "_" );
				for( int j = 0; j < 5; j ++ )
				{
					stk.nextToken();
				}
				try
				{
					int cpu = Integer.parseInt( stk.nextToken() );
					int mem = Integer.parseInt( stk.nextToken() );
					if( cpu > clientCPU || mem > clientMemory ) //see if the unit is suitable for the donor machine
					{
						continue; //not suitable - look for another unit
					}
					else
					{
						size --;
						return units[ 0 ];
					}
				}
				catch( NumberFormatException nfe )
				{
					errorLog.warning( "Failed to get CPU and Memory information from expired unit" );
					continue;
				}
			}
			else
			{
				//delete the directory as it is empty
				if( ! problems[ i ].delete() )
				{
					errorLog.warning( "Failed to delete empty directory in expired list: " + problems[ i ].getName() );
				}
			}
		}
		
		//no suitable unit - return null
		return null;
	}
	
	//return a particular unit File
	public synchronized File returnUnit( Long algorithmID, Long unitID )
	{
		//find the data unit file
		File[] algorithms = parentDirectory.listFiles();
		for( int i = 0; i < algorithms.length; i ++ )
		{
			if( algorithms[ i ].getName().equals( algorithmID.toString() ) && ! algorithms[ i ].getName().endsWith( "old" ) )
			{
				File[] units = algorithms[ i ].listFiles();
				String unit = unitID.toString();
				for( int j = 0; j < units.length; j ++ )
				{
					StringTokenizer stk = new StringTokenizer( units[ i ].getName(), "_" );
					if( stk.nextToken().equalsIgnoreCase( unit ) )
					{
						size --;
						return units[ j ];
					}
				}
			}
		}
		return null;
	}
}
