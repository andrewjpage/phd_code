/*
Thomas Keane, 5:57 PM 2/17/03
takes a socket connection and performs the file transfer: upload/download

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
import java.io.*;
import java.util.StringTokenizer;
import java.util.logging.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class SocketCommunicator extends Thread
{
	private Socket socket;
	private String problemsDir;
	private Logger systemLog;
	private Logger errorLog;
	private String clientAddress;
	private Long algorithmID;
	private boolean killed;

	public SocketCommunicator( ThreadGroup group, Socket s, String pDir, Logger sysLog, Logger errLog )
	{
		super( group, "" + Math.random() );
		socket = s;
		problemsDir = pDir;
		systemLog = sysLog;
		errorLog = errLog;
		clientAddress = socket.getInetAddress().getHostAddress();
		systemLog.info( "Initialised socketCommunicator Thread successful: " + clientAddress );

		//make sure a problems directory exists
		File f = new File( System.getProperty( "user.dir" ), problemsDir );
		if( ! f.isDirectory() )
		{
			f.mkdir();
		}
		algorithmID = null;
		killed = false;
	}

	//implement the communications protocol with the other side
	public void run()
	{
		systemLog.info( clientAddress + " In run method for socketCommunicator" );
		//do some checks on the socket
		DataInputStream ds = null;
		if( socket.isConnected() )
		{
			String info = null;
			try
			{
				//get the inputstream & put through a buffered reader
				ds = new DataInputStream( new GZIPInputStream( socket.getInputStream() ) );
				info = ds.readUTF();
			}
			catch( Exception e )
			{
				errorLog.severe( "Could not set up DataInputStream: " + e );
				closeSocket();
				return;
			}

			StringTokenizer st = new StringTokenizer( info, ":" );
			String connectionType = st.nextToken();
			String ID = st.nextToken();
			String fileName = st.nextToken();

			//decide who is connecting - gui or client
			if( connectionType.equals( "g" ) )
			{
				systemLog.info( clientAddress + " Received GUI connection - File Upload: " + fileName );
				//gui connection -> file upload
				//check the problem dir exists first
				File f = new File( problemsDir, ID );

				if( ! f.exists() )
				{
					f.mkdir();
				}

				//problem exists in system - create file and upload
				File newFile = new File( f, fileName );
				if( newFile.exists() )
				{
					errorLog.warning( clientAddress + "Attempt by GUI to create a duplicate problem file" );
					closeSocket();
					return;
				}

				//set up the streams
				FileOutputStream fos = null;
				int size = 0;
				try
				{
					newFile.createNewFile();
					size = socket.getReceiveBufferSize();
					fos = new FileOutputStream( newFile );
				}
				catch( IOException ioe )
				{
					errorLog.severe( clientAddress + " IOException while creating GUI streams: " + ioe );
					closeSocket();
					return;
				}

				try
				{
					//read the file and write to disk
					int read = 1;
					byte[] buffer = new byte[ size ];
					//stream the data to file
					while( read > 0 )
					{
						read = ds.read( buffer );
						if( read > 0 )
						{
							fos.write( buffer, 0, read );
						}
					}
					fos.close();

					GZIPOutputStream gzipout =  new GZIPOutputStream( socket.getOutputStream() );
					DataOutputStream dos = new DataOutputStream( gzipout );
					dos.writeUTF( "finished" );
					gzipout.finish();
					dos.close();
					closeSocket();
				}
				catch( IOException ioe )
				{
					errorLog.severe( clientAddress + " IOexception getting data from gui: " + ioe );
					closeSocket();
				}
				return;
			}
			else if( connectionType.equals( "c" ) )
			{
				algorithmID = new Long( ID );
				systemLog.info( clientAddress + " Received Client Connection - File Download: " + fileName );
				//client connection - requesting a file
				//check the problem dir exists first
				File f = new File( problemsDir, ID );
				if( f.isDirectory() )
				{
					//problem exists in system - download file to client
					File probFile = locateFile( f, fileName );
					if( probFile != null )
					{
						//stream the file to the other side
						byte[] buffer = null;

						DataOutputStream dos = null;
						GZIPOutputStream gzipout = null;
						FileInputStream fis = null;
						int size = 0;
						try
						{
							size = socket.getSendBufferSize();
							gzipout = new GZIPOutputStream( socket.getOutputStream() );
							dos = new DataOutputStream( gzipout );
							fis = new FileInputStream( probFile );
						}
						catch( Exception e )
						{
							errorLog.severe( clientAddress + " Problem setting up Client streams: " + e );
							closeSocket();
							return;
						}

						if( size < probFile.length() )
						{
							buffer = new byte[ size ];
						}
						else
						{
							buffer = new byte[ (int) probFile.length() ];
						}

						//stream the data to the client side
						int offset = 0, bytesRead = 1;
						try
						{
							while( bytesRead > 0 && ! killed )
							{
								bytesRead = fis.read( buffer );
								offset += bytesRead;
								if( bytesRead > 0 )
								{
									dos.write( buffer, 0, bytesRead );
								}
							}

							gzipout.finish();
							dos.flush();
							fis.close();
							//check the num bytes sent
							if( dos.size() != probFile.length() && ! killed )
							{
								errorLog.warning( clientAddress + " Bytes sent and file size dont match" );
							}
							//dos.close();
						}
						catch( IOException ioe )
						{
							errorLog.severe( clientAddress + " IOException while writing to socket " + ioe );
							closeSocket();
							return;
						}			
					}
					else
					{
						errorLog.info( "Client requesting file that doesnt exist: " + fileName );
					}
					//closeSocket();
				}
				else
				{
					errorLog.info( "Client requesting data from a problem that doesnt exist: " + ID );
					//doesnt exist - close connection
					closeSocket();
					return;
				}
			}
			else if( connectionType.equals( "r" ) )
			{
				//results set being sought - stream it over
				systemLog.info( clientAddress + " Result set being sought by GUI" );

				//check that the result set exists
				File f = new File( System.getProperty( "user.dir" ), "problems" );
				File[] files = f.listFiles();
				File results = null;

				for( int i = 0; i < files.length; i ++ )
				{
					if( files[ i ].isFile() && files[ i ].getName().indexOf( ID ) != -1 )
					{
						results = files[ i ];
						break;
					}
				}

				if( results != null )
				{
					//results exist - stream them back
					//stream the file to the other side
					byte[] buffer = null;

					DataOutputStream dos = null;
					FileInputStream fis = null;
					GZIPOutputStream gzipout = null;
					int size = 0;
					try
					{
						size = socket.getSendBufferSize();
						gzipout = new GZIPOutputStream( socket.getOutputStream() );
						dos = new DataOutputStream( gzipout );
						fis = new FileInputStream( results );
					}
					catch( Exception e )
					{
						errorLog.severe( clientAddress + " Problem setting up Client streams: " + e );
						closeSocket();
						return;
					}

					if( size < results.length() )
					{
						buffer = new byte[ size ];
					}
					else
					{
						buffer = new byte[ (int) results.length() ];
					}

					//stream the data to the client side
					int offset = 0, bytesRead = 1;
					
					try
					{
						//first send the total number of bytes to be sent
						dos.writeInt( (int) results.length() );
						while( bytesRead > 0 )
						{
							bytesRead = fis.read( buffer );
							offset += bytesRead;
							if( bytesRead > 0 )
							{
								dos.write( buffer, 0, bytesRead );
							}
						}

						gzipout.finish();
						dos.flush();
						fis.close();
						//check the num bytes sent
						if( dos.size() != ( (int) results.length() + 4 ) )
						{
							errorLog.warning( clientAddress + " Result Bytes sent and file size dont match" );
						}
						dos.close();
					}
					catch( IOException ioe )
					{
						errorLog.severe( clientAddress + " IOException while writing to socket " + ioe );
						closeSocket();
						return;
					}
					//closeSocket();
				}
				else
				{
					systemLog.warning( clientAddress + " Results set being sought not found. ID: " + fileName );
					closeSocket();
				}
			}
			else if( connectionType.equals( "k" ) )
			{
				systemLog.info( clientAddress + " Received killed Job Download Request from GUI" );
				String endFileName = ID + ".zip";
				File directory = new File( System.getProperty( "user.dir" ), "problems" );
				File[] files = directory.listFiles();

				//locate file and stream it back to gui
				for( int i = 0; i < files.length; i ++ )
				{
					if( files[ i ].getName().endsWith( endFileName ) )
					{
						systemLog.info( clientAddress + " Found file to download to GUI" );

						//stream the file to the other side
						byte[] buffer = null;

						DataOutputStream dos = null;
						FileInputStream fis = null;
						GZIPOutputStream gzipout = null;
						int size = 0;

						try
						{
							size = socket.getSendBufferSize();
							gzipout = new GZIPOutputStream( socket.getOutputStream() );
							dos = new DataOutputStream( gzipout );
							fis = new FileInputStream( files[ i ] );
						}
						catch( Exception e )
						{
							errorLog.severe( clientAddress + " Problem setting up Client streams: " + e );
							closeSocket();
							return;
						}

						if( size < files[ i ].length() )
						{
							buffer = new byte[ size ];
						}
						else
						{
							buffer = new byte[ (int) files[ i ].length() ];
						}

						//stream the data to the client side
						int bytesRead = 1;
						try
						{
							while( bytesRead > 0 )
							{
								bytesRead = fis.read( buffer );
								if( bytesRead > 0 )
								{
									dos.write( buffer, 0, bytesRead );
								}
							}

							gzipout.finish();
							dos.flush();

							//check the num bytes sent
							if( dos.size() != ( (int) files[ i ].length() ) )
							{
								errorLog.warning( clientAddress + " Result Bytes sent and file size dont match" );
							}
							dos.close();
						}
						catch( IOException ioe )
						{
							errorLog.severe( clientAddress + " IOException while writing to socket " + ioe );
							closeSocket();
							return;
						}

						//close socket and exit loop
						closeSocket();
						i = files.length;
					}
				}
			}
		}
	}

	//completely close the socket - exiting thread
	private void closeSocket()
	{
		systemLog.info( clientAddress + " Closing socket" );
		try
		{
			if( ! socket.isInputShutdown() )
			{
				socket.shutdownInput();
			}

			if( ! socket.isOutputShutdown() )
			{
				socket.shutdownOutput();
			}

			if( socket.isConnected() && ! socket.isClosed() )
			{
				socket.close();
			}
		}
		catch( IOException ioe )
		{}
	}
	
	private File locateFile( File directory, String name )
	{
		File[] files = directory.listFiles();

			if( files != null )
			{
				for( int i = 0; i < files.length; i ++ )
				{
					if( files[ i ].isDirectory() )
					{
						File f = locateFile( files[ i ], name );
						if( f != null )
						{
							return f;
						}
					}
					else if( files[ i ].getName().equals( name ) )
					{
						return files[ i ];
					}
				}
			}
		return null;
	}

	//method that takes a directory and deletes all the contents
	private void recursiveDelete( File f )
	{
		systemLog.info( clientAddress + " Call to recursive Delete file: " + f.getName() );
		//recusively delete the contents of this directory
		File[] files = f.listFiles();

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
	
	//called when the problem is removed from the system
	//tells the thread to stop the download
	public void killDownload( Long aID )
	{
		if( algorithmID != null && algorithmID.compareTo( aID ) == 0 )
		{
			killed = true;
		}
	}
}
