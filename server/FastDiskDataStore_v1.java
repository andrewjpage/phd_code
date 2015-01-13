/* Thomas Keane, Mon Nov 01 15:32:20 GMT 2004 @689 /Internet Time/
Purpose: To store a set of units that are out being processed by the system. This is done by creating writing them to disk in a problem ID based scheme (for fast access)
Abstract: subclasses add more methods and use the functionality of this class

NOTE: Unit File Name format: UID_TimeEntered_TimeLimit_NumExceptions_NumExpired_CPU_Mem

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

public abstract class FastDiskDataStore_v1
{
	protected File parentDirectory; //top level directory for the store
	protected Logger errorLog; //recording any errors that occur when accessing units on disk
	
	protected int size;

	//default constructor - use the directory that is specified by the constructor
	public FastDiskDataStore_v1( File root, Logger errorL)
	{
		//clear or create the directory
		if( root.isDirectory() )
		{
			recursiveDelete( root ); //delete any old files in the directory
		}
		else
		{
			root.mkdir();
		}
		parentDirectory = root;
		errorLog = errorL;
		size = 0;
	}
	
	//only called when the results of the unit have been received
	//or moving unit to other datastore
	public synchronized void removeUnit( Long unitID, Long algorithmID )
	{
		//find the file
		File probDir = new File( parentDirectory, algorithmID.toString() );
		if( probDir.isDirectory() )
		{
			File[] files = probDir.listFiles();
			String unit = unitID.toString();
			for( int i = 0; i < files.length; i ++ )
			{
				StringTokenizer stk = new StringTokenizer( files[ i ].getName(), "_" );
				if( stk.nextToken().equalsIgnoreCase( unit ) )
				{
					if( ! files[ i ].delete() )
					{
						errorLog.warning( "Failed to delete problem " + algorithmID + " unit file from store: " + unitID );
						return;
					}
					else if( files[ i ].exists() )
					{
						errorLog.warning( "Failed to delete problem " + algorithmID + " unit file from store: " + unitID + ". Unit file still exists after delete" );
					}
					else
					{
						size --;
						return;
					}
				}
			}
		}
	}
	
	//check if a unit exists in the store
	public synchronized boolean unitExist( Long unitID, Long algorithmID )
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
					return true;
				}
			}
		}
		return false;
	}
	
	//increase the memory requirement of a particular unit as an out of memory error was reported for the unit
	public synchronized File increaseMemoryRequirement( Long unitID, Long algorithmID, int maxClientMemoryAvailable )
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
					//increase the unit memory requirements by renaming the file
					String newName = unit + "_" + stk.nextToken() + "_" + stk.nextToken() + "_" + stk.nextToken() + "_" + stk.nextToken() + "_" + stk.nextToken();
					try
					{
						int currentMem = Integer.parseInt( stk.nextToken() );
						if( currentMem > maxClientMemoryAvailable )
						{
							return units[ i ]; //do nothing
						}
						else if( (currentMem * 2) < maxClientMemoryAvailable ) //double the memory if it is still lower than the max available
						{
							int newMem = currentMem * 2;
							newName = newName + "_" + newMem;
						}
						else
						{
							//set the memory req to just below the max available
							int newMem = maxClientMemoryAvailable - 1;
							newName = newName + "_" + newMem;
						}
						
						//rename the unit file
						File newUnit = new File( probDir, newName );
						if( ! units[ i ].renameTo( newUnit ) )
						{
							return units[ i ];
						}
						return newUnit;
					}
					catch( Exception e )
					{
						return units[ i ];
					}
				}
			}
		}
		return null;
	}
	
	//increase the CPU requirement of a particular unit as it ran out of processing time on a particular CPU
	public synchronized File increaseCPURequirement( Long unitID, Long algorithmID, int maxClientCPUAvailable )
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
					//increase the unit CPU requirements by renaming the file
					String newName = unit + "_" + stk.nextToken() + "_" + stk.nextToken() + "_" + stk.nextToken() + "_" + stk.nextToken();
					try
					{
						int currentCPU = Integer.parseInt( stk.nextToken() );
						if( currentCPU > maxClientCPUAvailable )
						{
							return units[ i ]; //do nothing
						}
						else if( (currentCPU * 2) < maxClientCPUAvailable ) //double the memory if it is still lower than the max available
						{
							int newCPU = currentCPU * 2;
							newName = newName + "_" + newCPU + "_" + stk.nextToken();
						}
						else
						{
							//set the CPU req to just below the max available
							int newCPU = maxClientCPUAvailable - 1;
							newName = newName + "_" + newCPU + "_" + stk.nextToken();
						}
						
						//rename the unit file
						File newUnit = new File( probDir, newName );
						if( ! units[ i ].renameTo( newUnit ) )
						{
							return units[ i ];
						}
						return newUnit;
					}
					catch( Exception e )
					{
						return units[ i ];
					}
				}
			}
		}
		return null;
	}
	
	//remove all of the units from a specified problem (problem being removed from system)
	public synchronized void removeUnits( Long algorithmID )
	{
		//find the file
		File probDir = new File( parentDirectory, algorithmID.toString() );
		if( probDir.isDirectory() )
		{
			//delete the entire contents of the directory and delete the directory
			File f = new File( parentDirectory, algorithmID.toString() + "old" );
			if( ! probDir.renameTo( f ) )
			{
				errorLog.warning( "Failed to rename directory" );
			}
			
			String[] files = f.list();
			size -= files.length; //subtract the units in this directory
			files = null;
			
			//start a thread to delete the contents in the background
			Deleter d = new Deleter( f );
			d.setPriority( Thread.MIN_PRIORITY );
			d.start();
		}
	}
	
	public boolean isEmpty()
	{
		if( size == 0 )
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	public int getSize()
	{
		return size;
	}
	
	//method that takes a directory and deletes all the contents & subfolders
	private void recursiveDelete( File f )
	{
		//recusively delete the contents of this directory
		File[] files = f.listFiles();

		if( files != null )
		{
			for( int i = 0; i < files.length; i ++ )
			{
				if( files[ i ].isFile() )
				{
					files[ i ].delete();
				}
				else if( files[ i ].isDirectory() )
				{
					//delete subdirectory
					recursiveDelete( files[ i ] );
					files[ i ].delete();
				}
			}
		}
	}
	
	//an exception has occurred in a unit on a client - record that it has happened
	public synchronized void incrementExceptions( Long unitID, Long algorithmID )
	{
		File probDir = new File( parentDirectory, algorithmID.toString() );
		if( probDir.isDirectory() )
		{
			File[] units = probDir.listFiles();
			String unit = unitID.toString();
			for( int i = 0; i < units.length; i ++ )
			{
				StringTokenizer stk = new StringTokenizer( units[ i ].getName(), "_" );
				if( stk.nextToken().equalsIgnoreCase( unit ) ) //find the unit file
				{
					stk = new StringTokenizer( units[ i ].getName(), "_" );
					String newName = stk.nextToken() + "_" + stk.nextToken() + "_" + stk.nextToken();
					try
					{
						int exceptions = Integer.parseInt( stk.nextToken() ) + 1;
						newName = newName + "_" + exceptions + "_" + stk.nextToken() + "_" + stk.nextToken() + "_" + stk.nextToken();
					}
					catch( NumberFormatException nfe )
					{
						errorLog.warning( "Failed to convert num of exceptions" );
						return;
					}
					
					File newUnit = new File( probDir, newName );
					if( ! units[ i ].renameTo( newUnit ) )
					{
						errorLog.warning( "Failed to increment exceptions (rename file)" );
						return;
					}
					else if( units[ i ].exists() )
					{
						errorLog.warning( "Original unit file still exists after incrementing exceptions" );
					}
					else if( ! newUnit.exists() )
					{
						errorLog.warning( "New unit file doesnt exist after incrementing exceptions" );
					}
					else
					{
						return;
					}
				}
			}
		}
	}
	
	//return the number of exceptions that have occurred in a particular data unit
	public synchronized int getExceptionTimes( Long unitID, Long algorithmID )
	{
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
					stk = new StringTokenizer( units[ i ].getName(), "_" );
					stk.nextToken();
					stk.nextToken();
					stk.nextToken();
					try
					{
						return Integer.parseInt( stk.nextToken() );
					}
					catch( NumberFormatException nfe )
					{
						errorLog.warning( "Failed to get number of extensions from unit" );
						return -1;
					}
				}
			}
		}
		return -1;
	}
	
	//check that there are no leftover data unit files (from problems that have been removed from the system) in the store
	//and delete any empty directories (they will be recreated as needed by the system)
	public synchronized void checkConsistent( Long[] ids )
	{
		File[] directories = parentDirectory.listFiles();
		for( int i = 0; i < directories.length; i ++ )
		{
			boolean exists = false;
			for( int j = 0; j < ids.length; j ++ )
			{
				if( directories[ i ].getName().equals( ids[ j ].toString() ) )
				{
					exists = true;
					break;
				}
			}
			
			//if there is nothing in the directory - delete it
			if( directories[ i ].list().length == 0 )
			{
				if( ! directories[ i ].delete() )
				{
					errorLog.warning( "Failed to delete empty directory" );
				}
			}
			else if( ! exists )
			{
				//there is no problem running in the system corresponding to the directory
				Deleter d = new Deleter( directories[ i ] );
				d.setPriority( Thread.MIN_PRIORITY );
				d.start();
			}
		}
		return;
	}
	
	//a method to synchronise the size variable to the number of units on disk (sometimes due to I/O exceptions size variable becomes incorrect)
	public synchronized void synchronizeSize()
	{
		int count = 0;
		File[] directories = parentDirectory.listFiles();
		if( directories != null )
		{
			for( int i = 0; i < directories.length; i ++ )
			{
				if( directories[ i ].isDirectory() )
				{
					File[] files = directories[ i ].listFiles();
					if( files != null )
					{
						count += files.length;
					}
				}
			}
			size = count;
		}
	}
	
	public File getParentDirectory()
	{
		return parentDirectory;
	}
	
	public void decrementSize()
	{
		size --;
	}
	
	public void incrementSize()
	{
		size ++;
	}
}
