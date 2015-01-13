/*
Thomas Keane, 11:28 AM 2/4/03
class that is used to get user to add a problem to the server
only accessible in admin mode
 
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
import java.net.Socket;
import java.util.*;
import java.rmi.RemoteException;
import java.util.zip.*;
import java.util.jar.*;

public class cmdAdd {
		private RemoteInterface owner; //overall gui class
		private GUICommunications scheduler; //rmi stub

		//files chosen
		private File algorithmF;
		private File datamanagerF;
		private File[] problemDataF;
		private File[] classDefsF;
		private File parentDirectory;

		public cmdAdd(RemoteInterface owner, GUICommunications scheduler) {
			super();
			this.scheduler  = scheduler;
			this.owner = owner;
		}

		public void addSmallProblem( File algorithmF, File datamanagerF, String description) {
			//read the selected files in
			byte[] al = getFile( algorithmF );
			byte[] dm = getFile( datamanagerF );
			Long probID = new Long( Math.abs( ( new Random() ).nextLong() % 10000000 ) );

			boolean b = false;


			System.out.println("test");
			try {
					b = scheduler.addProblem( probID, al, dm, 10, description +
					                          "$%^&" + description, 5, 5, null, null, 20, 60 );
				} catch( RemoteException e ) {
					System.out.println(e);
					return;
				}


		}


		public void addProblem( File algorithmF, File datamanagerF, String description,  File[]
		                        classDefsF, File[] problemDataF ) {

			//read the selected files in
			byte[] al = getFile( algorithmF );
			byte[] dm = getFile( datamanagerF );

			//if there are any class defs - then get them
			Vector classDefinitions = new Vector();
			Vector jarDefinitions = new Vector();
			try {
					if( classDefsF != null ) {
							for( int i = 0; i < classDefsF.length; i ++ ) {
									//seperate the jar files from the class files
									if( classDefsF[ i ].getName().endsWith( ".jar" ) ) {
											//get the bytes for the file
											byte[] file = new byte[ (int) classDefsF[ i ].length() ];
											FileInputStream fis = new FileInputStream( classDefsF[ i ] );
											int offset = 0, read = 1;
											while( read > 0 ) {
													read = fis.read( file, offset, file.length - offset );
													offset += read;
												}
											fis.close();
											jarDefinitions.add( classDefsF[ i ].getName() );
											jarDefinitions.add( file );
										} else if( classDefsF[ i ].getName().endsWith( ".class" ) ) {
											//get the bytes for the file
											byte[] file = new byte[ (int) classDefsF[ i ].length() ];
											FileInputStream fis = new FileInputStream( classDefsF[ i ] );
											int offset = 0, read = 1;
											while( read > 0 ) {
													read = fis.read( file, offset, file.length - offset );
													offset += read;
												}
											fis.close();
											classDefinitions.add( classDefsF[ i ].getName() );
											classDefinitions.add( file );
										}
								}
						}
				} catch( IOException e ) {
					return;
				}


			Long probID = new Long( Math.abs( ( new Random() ).nextLong() % 10000000 ) );

			try {
					if( problemDataF != null ) {
							System.out.println("test");
							Vector v = owner.getSocketInfo();
							//upload the files to the server via socket
							for( int i = 0; i < problemDataF.length; i ++ ) {
									int retries = 0;
									Exception exception = null;
									while( retries < 5 ) {
											try {

													String ip = (String) v.get( 0 );
													int port = ( (Integer) v.get( 1 ) ).intValue();

													//open socket and tell server about to upload
													Socket socket = new Socket( ip, port );
													GZIPOutputStream gzipout = new GZIPOutputStream( socket.getOutputStream() );
													DataOutputStream ds = new DataOutputStream( gzipout );
													ds.writeUTF( "g:" + probID + ":" + problemDataF[ i ].getName() );

													ds.flush();

													FileInputStream fis = new FileInputStream( problemDataF[ i ] );
													byte[] buffer = new byte[ socket.getSendBufferSize() ];
													int numRead = 1;

													//stream data to server
													while( numRead > 0 ) {
															numRead = fis.read( buffer );
															if( numRead > 0 ) {
																	ds.write( buffer, 0, numRead );
																}
														}
													gzipout.finish();
													ds.flush();

													//wait for server to confirm all file received
													DataInputStream dis = new DataInputStream( new GZIPInputStream( socket.getInputStream() ) );
													String s = dis.readUTF();

													dis.close();
													ds.close();
													break;
												} catch( Exception e ) {
													//						e.traceStack();
													System.out.println(e);
													exception = e;
													retries ++;
												}
										}

									if( retries >= 5 ) {

											return;
										}
								}
						}

				} catch(Exception f) {
					System.out.println(f);
				}
			//add the job to the server
			boolean b = false;



			try {
					b = scheduler.addProblem( probID, al, dm, 10, description +
					                          "$%^&" + description, 5, 5, classDefinitions,
					                          jarDefinitions, 20, 60 );
				} catch( RemoteException e ) {

					return;
				}

		}

		private byte[] getFile( File f ) {
			//read in the bytes of the file
			try {
					FileInputStream fi = new FileInputStream( f );
					byte[] bytes = new byte[ (int) f.length() ];

					int offset = 0;
					int numRead = 0;

					while( offset < bytes.length && ( numRead = fi.read( bytes, offset, bytes.length - offset ) ) > 0 ) {
							offset += numRead;
						}

					fi.close();
					return bytes;
				} catch( Exception e ) {
					return null;
				}
		}
	}
