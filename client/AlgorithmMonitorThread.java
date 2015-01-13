/* Thomas Keane
This is the main client class. It initialises & manages the overall client and the running
of the data units on the downloaded algorithms.

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

class AlgorithmMonitorThread extends Thread
{
	private ClientController owner;
	private Vector dataUnit;
	private Vector results;
	private Class algorithm;
	private boolean alive;
	private Long unitID;
	private Long algorithmID;
	
	public AlgorithmMonitorThread( ClientController own, Vector data, Class al, Long uID, Long aID )
	{
		dataUnit = data;
		algorithm = al;
		owner = own;
		alive = false;
		results = null;
		unitID = new Long( uID.longValue() );
		algorithmID = new Long( aID.longValue() );
	}
	
	public void run()
	{
		alive = true;
		try
		{
			Algorithm a = (Algorithm) algorithm.newInstance();
			Vector _results = a.processUnit( dataUnit ); //process the unit
			results = _results;
			
			//confirm the unit is valid
			if( owner.checkCurrentUnit( unitID, algorithmID ) )
			{
				synchronized( owner )
				{
					owner.setCurrentState( ClientController.ALGORITHM_FINISHED );
					owner.notify();
				}
			}
			else
			{
				return;
			}
			alive = false;
		}
		catch( Throwable t )
		{
			alive = false;
			
			//confirm the unit is the current unit being processed (may fail if multiple versions of this thread exist)
			if( ! owner.checkCurrentUnit( unitID, algorithmID ) )
			{
				return;
			}
			
			//check to see if its a filenot found exception - if so tell clientController to stream download the file from the server
			String details = t.toString();

			if( details.startsWith( "java.io.FileNotFoundException:" ) )
			{
				//get name of the file being sought
				StringTokenizer st = new StringTokenizer( details );
				st.nextToken();
				//get file name
				String fileName = st.nextToken();

				//replace all \ slashes with / slashes - in case its windows
				char[] fname = fileName.toCharArray();
				for( int i = 0; i < fname.length; i ++ )
				{
					if( fname[i ] == '\\' )
					{
						fname[ i ] = '/';
					}
				}

				//get the actual file name - less the absolute path on the machine
				st = new StringTokenizer( new String( fname ), "/" );
				int tokens = st.countTokens();
				//get the final token
				for( int i = 0; i < tokens - 1; i ++ )
				{
					st.nextToken();
				}

				//actual file name
				fileName = st.nextToken();

				synchronized( owner )
				{
					owner.sendFileName( fileName );
					owner.setCurrentState( ClientController.GET_PROBLEM_DATA );
					owner.notify();
				}
			}
			else
			{
				synchronized( owner )
				{
					owner.setCurrentState( ClientController.ALGORITHM_EXCEPTION );
					owner.sendException( t );
					owner.notify();
				}
			}
		}
	}
	
	public Vector getResults()
	{
		return results;
	}
	
	public boolean isAlgorithmAlive()
	{
		return alive;
	}
}
