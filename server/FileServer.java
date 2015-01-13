/*
Thomas Keane, 16:33 14/05/2004
acts as a server for streaming data files to/from client and gui
implemented using sockets because they allow streaming of large amounts of data

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

import java.net.*;
import java.util.logging.*;
import java.io.*;

public class FileServer extends Thread
{
	private ServerSocket serverSocket; //listens for connections
	private String probsDir; //directory where problems are
	private ThreadGroup current_threads; //the current group of active threads
	private Logger systemLog;
	private Logger errorLog;
	private int maxSockets;

	//constructor
	public FileServer( int port, String pDir, Logger sysLog, Logger errLog, int max )
	{
		probsDir = pDir;
		systemLog = sysLog;
		errorLog = errLog;
		try
		{
			serverSocket = new ServerSocket( port );
			systemLog.info( "Created FileServer on port: " + port );
		}
		catch( Exception e )
		{
			errorLog.severe( "Couldn't set up FileServer: " + e );
		}
		
		//create a thread group for all threads spawned
		current_threads = new ThreadGroup( this.getThreadGroup(), "socketThreads" );
		maxSockets = max;
	}

	//continuously loop waiting for connections
	public void run()
	{
		//wait for connection and spawn new thread to handle it
		while( true )
		{
			try
			{
				Socket s = serverSocket.accept();

				//must place a limit on the number of simultaneous active threads
				if( current_threads.activeCount() < maxSockets )
				{
					//create a new socketCommunicator to handle the communication
					SocketCommunicator so = new SocketCommunicator( current_threads, s, probsDir, systemLog, errorLog );
					so.setPriority( Thread.MIN_PRIORITY );
					so.start();
				}
				else
				{
					errorLog.info( "Reached maximum number of SocketCommunicator threads - denying thread request" );
					s.close();
				}
			}
			catch( IOException ioe )
			{
				errorLog.severe( "I/O Problem accepting Connection: " + ioe );
			}
			catch( Exception e )
			{
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				PrintStream ps = new PrintStream( bos );
				e.printStackTrace( ps );
				errorLog.severe( "Problem accepting connection: " + bos.toString() );
			}
		}
	}
	
	public void stopDownloads( Long algorithmID )
	{
		//go through each active socket and tell them to stop if they belong to the problem
		Thread[] threads = new Thread[ current_threads.activeCount() ];
		current_threads.enumerate( threads );
		systemLog.info( "Found " + threads.length + " active download threads for problem - killing" );
		
		for( int i = 0; i < threads.length; i ++ )
		{
			if( threads[ i ].isAlive() )
			{
				( (SocketCommunicator) threads[ i ] ).killDownload( algorithmID );
			}
		}
	}
}
