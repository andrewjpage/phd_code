/*Thomas Keane
Purpose: A thread that will be used by the client to keep track of how long each data unit
has been executing for - allowing the client to request time extensions from the server.

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
import java.security.*;

class timerThread extends Thread
{
	private long waitTime; //amount of time in minutes to wait for
	private ClientController owner; //pointer to the client
	private boolean valid;

	//default constructor
	public timerThread( long numMinutes, ClientController own )
	{
		valid = true;
		waitTime = numMinutes;
		owner = own;
	}

	public void run()
	{
		try
		{
			long time = waitTime;

         	       	//wait for X minutes
			sleep( time );

			if( valid )
			{
				synchronized( owner )
				{
					//set the client state to unit timeout
					owner.setCurrentState( ClientController.ALGORITHM_TIMEOUT );

					//tell the client to perform some action according to the state
					owner.notify();
				}
			}
		}				
		//interrrupted if the client wants to kill this thread
		catch( Throwable t )
		{
			return;
		}
	}
	
	//we are finished with this timer thread - dont notify owner
	public void finished()
	{
		valid = false;
	}
	
	//returns whether or not this thread is still valid (i.e. will notify its owner)
	public boolean _isAlive()
	{
		return valid;
	}
}