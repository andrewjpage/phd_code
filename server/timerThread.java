/*
Thomas Keane
Purpose: A thread that will be used by the server to periodically tell it to perform certain actions
e.g. check the expired tasks

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

public class timerThread extends Thread{

	private long waitTime; //amount of minutes to wait
	private Server owner;  //pointer to owner
	private Logger systemLog; //current log

	
	//default constructor
	public timerThread( long numMinutes, Server own, Logger sLog ){
		waitTime = numMinutes;
		owner = own;
		systemLog = sLog;
	
		systemLog.info( "Timer initialisation complete" );
	}

	public void run(){
		try{
			while( true ){
				//wait for X minutes
				long time = waitTime * 60 * 1000;
				
				systemLog.info( "Timer sleeping for " + time + " milliseconds" );
				sleep( time );
				
				//call a timeout at the server
				owner.serverTimeout();				
			}

		//will be interrrupted when the server wants to kill the thread
		}catch( InterruptedException e ){
			return;
		}
	}	
}
