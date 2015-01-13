/*
Thomas Keane, 29/10/04
a stub file that reads in the client jar and starts the client
allows client jar file to be reloaded
completely independant of where server is located - this info is stored in client only
 
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
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;
import java.security.*;
import java.lang.reflect.*;

class ClientStub extends Thread {
		public static void main( String args[] ) {
			Properties clientProperties = new Properties();

			//check that the donor is running at least jvm 1.4
			String version = System.getProperty( "java.version" );
			int major = 0, minor = 0, minor1 = 0;
			try {
					major = Integer.parseInt( "" + version.charAt( 0 ) );
					minor = Integer.parseInt( "" + version.charAt( 2 ) );
					minor1 = Integer.parseInt( "" + version.charAt( 4 ) );

					if( major < 1 || ( major == 1 && minor < 4 ) ) {
							System.out.println( "The client requires JVM 1.4 or greater" );
							System.exit( 1 );
						}
				} catch( NumberFormatException nfe ) {
					System.out.println( "Error reading jvm version: " + nfe );
					System.exit( 1 );
				}


args[0] = "149.157.247.202";
args[1] = "15000";
args[2] = "15001";
args[3] = "2";



			try {
					Integer socket = new Integer( args[ 1 ] );
					Integer socket1 = new Integer( args[ 2 ] );
					Integer time = new Integer( args[ 3 ] );
					Integer mf = new Integer(args[4]);
				} catch( NumberFormatException nfe ) {
					System.out.println( "Usage: java -jar client.jar <server IP> <RMI port> <socket Port> <client timeout>" );
					System.exit( 1 );
				}

			System.out.println( "Starting client......" );
			System.out.println( "Check stdout.out file to confirm client started successfully........" );

			//infinite loop so that when a client file update occurs - client restarts again
			while( true ) {
					//extract the security policy file
					File policy = new File( System.getProperty( "user.dir" ), "security.pol" );
					if( ! policy.exists() ) {
							try {
									//extract the policy file from the jar file and place on disk
									JarFile jf = new JarFile( new File( System.getProperty( "user.dir" ), "client.jar" ) );
									ZipEntry entry = jf.getEntry( "security.pol" );
									InputStream is = jf.getInputStream( entry );
									byte[] polFile = new byte[ (int) entry.getSize() ];

									int offset = 0;
									int i = 1;
									while( i > 0 ) {
											i = is.read( polFile, offset, polFile.length - offset );
											offset += i;
										}
									is.close();

									//write it to disk so it can be used as the policy file for the client
									FileOutputStream fos = new FileOutputStream( policy );
									fos.write( polFile );
									fos.close();

									polFile = null;
									jf = null;
									entry = null;
									fos = null;

									//find out how much available memory the donor has for the jvm
									long availableMemory = Runtime.getRuntime().maxMemory();

									if( major == 1 && minor == 4 && minor1 < 2 ) //jvm versions 1.4.0 & 1.4.1
										{
											//bug in certain jvm's that causes memory to be over stated by 64MB (bug ID 4686462)
											availableMemory = ( ( availableMemory - ( 64 * 1024 * 1024 ) ) / ( 1024 * 1024 ) ) + 1;

											//restart the client and it should not enter this if statement next time
											Process restarted = Runtime.getRuntime().exec( "java -Xrs -Xmx" + availableMemory + "m -Djava.security.policy==security.pol -jar client.jar " + args[ 0 ] + " " + args[ 1 ] + " " + args[ 2 ] + " " + args[ 3 ] );
											System.exit( 1 );
										} else {
											//restart the client and it should not enter this if statement next time
											Process restarted = Runtime.getRuntime().exec( "java -Xrs -Xmx" + availableMemory + " -Djava.security.policy==security.pol -jar client.jar " + args[ 0 ] + " " + args[ 1 ] + " " + args[ 2 ] + " " + args[ 3 ] );
											System.exit( 1 );
										}
								} catch( Throwable t ) {
									//get some detailed info from the exception - stack trace
									ByteArrayOutputStream bos = new ByteArrayOutputStream();
									PrintStream ps = new PrintStream( bos );
									t.printStackTrace( ps );
									System.err.println( bos.toString() );
									System.exit( 1 );
								}
						}

						/*
					SecurityManager sm = System.getSecurityManager();
					if( sm == null ) {
							//install the security manager
							System.setSecurityManager( new SecurityManager() );
						} else {
							//update the security policy to latest one
							Policy.getPolicy().refresh();
						}
					policy.delete(); //delete the policy file
					*/

					//read in the client properties
					clientProperties.setProperty( "server.ip", args[ 0 ] );
					clientProperties.setProperty( "server.port", args[ 1 ] );
					clientProperties.setProperty( "socket.port", args[ 2 ] );
					clientProperties.setProperty( "client.timeout", args[ 3 ] );
           			clientProperties.setProperty( "client.mf", args[ 4 ] );
					try {
							ClientController c = new ClientController( clientProperties );
							c.run();
							c = null;
						} catch( Throwable e ) {
							//get some detailed info from the exception - stack trace
							ByteArrayOutputStream bos = new ByteArrayOutputStream();
							PrintStream ps = new PrintStream( bos );
							e.printStackTrace( ps );

							System.err.println( bos.toString() );
							try {
									sleep( 20 * 60 * 1000);
								} catch( Exception ex ) {}}
					System.out.println( "restarting" );
				}
		}
	}
