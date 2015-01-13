/*
Thomas Keane
Class Server: Acts as the main server class in the system
 
Copyright (C) 2003  Thomas Keane
Updated by Andrew Page June 2005
 
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
import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.util.logging.*;
import java.io.*;
import java.util.jar.*;
import java.util.zip.ZipEntry;
import java.security.MessageDigest;


/** Class Server: Acts as the main server class in the system
* @author Thomas Keane
*/
public class Server implements ClientCommunications {
		private static final String DELIMITER = "^&*$";

		//server data structures
		private PendingStore_v1 pending; //list of data units out being processed
		private ExpiredStore_v1 expired; //list of expired data units
		private timerThread timer; //used to perform regular tasks
		private Hashtable algorithms; //hold copy of current executing algorithms
		private long timeout;

		//network connection info
		private int port; //server listens on this port
		private FileServer fs; //the socket based "server" used to upload/download problem data
		public int socketPort; //port used to transfer problem data - socket based streaming

		//log files for system
		private Logger systemLog;
		private Logger errorLog;
		private Logger timingLog;

		//info about the current version of the client being used
		private byte[] clientJar;
		private Integer clientVersion;
		private String clientJarIPRange;

		//RMI object to access the scheduler
		private Scheduler scheduler;

		//cache that holds the ip of each unique client that has connected - recording purposes
		private Vector clientIPs;
		private Vector totalConnected;

		//maximum amount of memory available on a donor machine
		private int maxClientMemory;
		private int maxClientCPU;
		public String schedName;

		/** server starts here with initialisation file */
		public Server() throws RemoteException {
				System.out.println( "start problems: " + ((new File( "problems" )).listFiles()).length );
				String admin = null;
				String systemFile = "server.ini";
				float optimalTolerance = 0.0f;
				int optimalUnitTime = 0, systemLogSize = 0, errorLogSize = 0, maxSockets = 0;

				try {
						File dir = new File( System.getProperty( "user.dir" ), "logs" );
						//redirect the standard error to a file
						File stderr = new File( dir, "stderror.log" );
						System.setErr( new PrintStream( new FileOutputStream( stderr ), true ) );

						//redirect the standard out to a file (shouldnt be any output but ya never know!)
						File stdout = new File( dir, "stdout.log" );
						System.setOut( new PrintStream( new FileOutputStream( stdout ), true ) );

						//get system info from inputted file
						Properties systemInfo = new Properties();

						BufferedReader br = new BufferedReader( new FileReader( "server.ini" ) );
						String line = br.readLine();

						while( line != null ) {
								// remove trailing/leading whitespace...
								line = line.trim();

								// skip comments...
								if( ( line.length() > 0 )&&( line.charAt( 0 ) != '#' ) ) {
										String name, value;
										StringTokenizer lineBreaker = new StringTokenizer( line , "=" );

										try {
												name = lineBreaker.nextToken();
												name = name.trim();
												name = name.toLowerCase();
												value = lineBreaker.nextToken();
												value = value.trim();
												systemInfo.setProperty( name,value );
											} catch( NoSuchElementException e ) {
												System.out.println( "Exception in Server setup -> Malformed initialisation file: " + e.toString() );
												System.exit(1);
											}
									}
								line = br.readLine();
							}
						br.close();
						String property = null;

						property = systemInfo.getProperty( "server.timeout" );
						if( property == null ) {
								System.out.println( "Couldnt read server.timeout property from server.ini" );
								System.exit( 1 );
							}
						timeout = Long.parseLong( property );

						property = systemInfo.getProperty( "rmi.port" );
						if( property == null ) {
								System.out.println( "Couldnt read rmi.port property from server.ini" );
								System.exit( 1 );
							}
						port = Integer.parseInt( property );

						property = systemInfo.getProperty( "socket.port" );
						if( property == null ) {
								System.out.println( "Couldnt socket.port property from server.ini" );
								System.exit( 1 );
							}
						socketPort = Integer.parseInt( property );

						property = systemInfo.getProperty( "admin.password" );
						if( property == null ) {
								System.out.println( "Couldnt read admin.password property from server.ini" );
								System.exit( 1 );
							}
						admin = property;

						property = systemInfo.getProperty( "unit.optimal" );
						if( property == null ) {
								System.out.println( "Couldnt read unit.optimal property from server.ini" );
								System.exit( 1 );
							}
						optimalUnitTime = Integer.parseInt( property );

						property = systemInfo.getProperty( "unit.tolerance" );
						if( property == null ) {
								System.out.println( "Couldnt read unit.tolerance property from server.ini" );
								System.exit( 1 );
							}
						optimalTolerance = Float.parseFloat( property );

						property = systemInfo.getProperty( "errorlog.size" );
						if( property == null ) {
								System.out.println( "Couldnt read errorLog.size property from server.ini" );
								System.exit( 1 );
							}
						errorLogSize = Integer.parseInt( property );

						property = systemInfo.getProperty( "systemlog.size" );
						if( property == null ) {
								System.out.println( "Couldnt read systemLog.size property from server.ini" );
								System.exit( 1 );
							}
						systemLogSize = Integer.parseInt( property );

						property = systemInfo.getProperty( "server.ip" );
						if( property == null ) {
								System.out.println( "Could not get server host IP address from server.ini" );
								System.exit( 1 );
							}
						System.setProperty( "java.rmi.server.hostname", property );

						property = systemInfo.getProperty( "sched.name" );
						if( property == null ) {
								System.out.println( "Could not get server scheduler server.ini" );
								System.exit( 1 );
							}
						schedName = property;

						property = systemInfo.getProperty( "socket.maximum" );
						if( property == null ) {
								System.out.println( "Couldnt read socket.maximum property from server.ini" );
								System.exit( 1 );
							}
						maxSockets = Integer.parseInt( property );

					} catch( IOException e ) {
						System.out.println( "I/OException while reading from file: " + systemFile + " -> "+ e.toString() );
						System.exit( 1 );
					}
				catch( Exception e ) {
						System.out.println( "Unknown Exception while reading from file " + systemFile + " -> " + e.toString() );
						System.exit( 1 );
					}

				systemLog = Logger.getAnonymousLogger();
				errorLog = Logger.getAnonymousLogger();
				timingLog = Logger.getAnonymousLogger();

				FileHandler shandler = null;
				FileHandler ehandler = null;
				FileHandler thandler = null;
				try {
						//create file handlers for logs - cycle through 10 different log files - autodelete
						shandler = new FileHandler( "logs/system%g.log", (int) ((systemLogSize * 1000000)/ 20), 20, false );
						ehandler = new FileHandler( "logs/error%g.log", (int) ((errorLogSize * 1000000)/ 5), 5, false );
						thandler = new FileHandler( "logs/timing%g.log", (int) ((errorLogSize * 1000000)/ 5), 5, false );
					} catch( IOException ioe ) {
						System.out.println( "I/O Exception while setting up log files: " + ioe.toString() );
						System.exit( 1 );
					}

				//associate loggers with files
				SimpleFormatter simple = new SimpleFormatter();
				shandler.setFormatter( simple );
				ehandler.setFormatter( simple );
				thandler.setFormatter( new emptyFormatter() );

				//stop printing to screen
				systemLog.setUseParentHandlers( false );
				systemLog.addHandler( shandler );

				//stop printing to screen
				errorLog.setUseParentHandlers( false );
				errorLog.addHandler( ehandler );

				//stop printing to screen
				timingLog.setUseParentHandlers( false );
				timingLog.addHandler( thandler );

				timingLog.info( "CurrentTimeMillis IP MFLOPS Memory uptime clientProcTime numExtensions numConnectFailed Latency AlgorithmID unitid networkTime processingTime OSName OSVersion Arch\n" );

				systemLog.info( "Log files successfully created" );

				systemLog.info( "Read in server initialisation file" );

				pending = new PendingStore_v1( new File( "pending" ), errorLog );
				expired = new ExpiredStore_v1( new File( "expired" ), errorLog );

				//create the algorithm cache
				algorithms = new Hashtable();

				systemLog.info( "Created pending and expired list and algorithm Cache");

				//create the timer and start it going
				timer = new timerThread( timeout, this, systemLog );
				systemLog.info( "Created Timer thread according to init file" );
				timer.setPriority( Thread.MIN_PRIORITY ); //low priority thread
				timer.start();

				//set the default for newer client versions
				clientJar = null;
				clientVersion = new Integer( 0 ); //default to 0 -> not a newer version
				clientJarIPRange = null;

				clientIPs = new Vector();
				totalConnected = new Vector();
				maxClientCPU = 0;
				maxClientMemory = 0;
				//set up the socket based data transfer thread
				fs = new FileServer( socketPort, System.getProperty( "user.dir" ) + "/problems", systemLog, errorLog, maxSockets );
				fs.setPriority( Thread.MIN_PRIORITY );
				fs.start();
				systemLog.info( "File Server created successfully" );

				try {
						//read in the bytes of DataManager parent class
						JarFile jf = new JarFile( new File( "server.jar" ) );
						ZipEntry entry = jf.getEntry( "DataManager.class" );
						InputStream is = jf.getInputStream( entry );
						byte[] dataMBytes = new byte[ (int) entry.getSize() ];

						int offset = 0;
						int i = 1;
						while( i > 0 ) {
								i = is.read( dataMBytes, offset, dataMBytes.length - offset );
								offset += i;
							}
						is.close();

						entry = jf.getEntry( "Algorithm.class" );
						is = jf.getInputStream( entry );
						byte[] algorithmBytes = new byte[ (int) entry.getSize() ];

						offset = 0;
						i = 1;
						while( i > 0 ) {
								i = is.read( algorithmBytes, offset, algorithmBytes.length - offset );
								offset += i;
							}
						is.close();

						systemLog.info( "Creating Scheduler" );
						scheduler = new Scheduler( this, systemLog, errorLog, admin, optimalUnitTime, optimalTolerance, dataMBytes, algorithmBytes );
						systemLog.info( "Scheduler Created successfully" );

						GUICommunications stub = ( GUICommunications ) UnicastRemoteObject.exportObject( scheduler );
						Registry registry = LocateRegistry.createRegistry( port );
						systemLog.info( "Created RMI registry on port: " + port );

						systemLog.info( "Binding scheduler to registry" );
						registry.bind( "scheduler", scheduler );
						systemLog.info( "Scheduler Bound to registry" );
					} catch( Exception abe ) {
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						PrintStream ps = new PrintStream( bos );
						abe.printStackTrace( ps );
						systemLog.severe( "Cannot bind scheduler to RMI registry: " + bos.toString() );
						System.exit( 1 );
					}

				//set up the socket based data transfer thread
				//fs = new FileServer( socketPort, System.getProperty( "user.dir" ) + "/problems", systemLog, errorLog, maxSockets );
				//fs.setPriority( Thread.MIN_PRIORITY );
				//fs.start();
				//systemLog.info( "File Server created successfully" );

				systemLog.info( "******Completed Server initialisation******" );
				System.out.println( "Server started successfully");
				System.out.println( "Check log files ('logs' directory) for more information on server status and progress" );
			}

		//*********************start client comms interface****************************
		//a client has requested a data unit via the communicator

		/** test latency*/
		public synchronized void ping(  ) throws RemoteException {}

		/** Called by client via RMI to request a new data unit to process*/
		public synchronized Vector getDataUnit( Vector systemInformation , Vector dynamicInfo) throws RemoteException {

				Vector clientDetails = new Vector();
				//get client system information - in order to match with a suitable unit
				int clientCPU = ( (Integer) systemInformation.get( 0 ) ).intValue();
				int clientMemory = ( (Integer) systemInformation.get( 1 ) ).intValue();

				try {
						if( clientMemory > maxClientMemory ) {
								maxClientMemory = clientMemory;
							}

						if( clientCPU > maxClientCPU ) {
								maxClientCPU = clientCPU;
							}
						systemLog.info( "Client Data Request IP: " + getClientIP() + " CPU (MFLOPS): " + clientCPU + " Memory: " + clientMemory + " OS Name: " + ( (String) systemInformation.get( 2 ) ) + " OS Version: " + ( (String) systemInformation.get( 4 ) ) + " Architecture: " + ( (String) systemInformation.get( 3 ) ) + " Pending Size: " + pending.getSize() + " Expired size: " + expired.getSize() );

						//  AP send more info to scheduler
						clientDetails.add(0,getClientIP());
						clientDetails.add(1, (Integer) systemInformation.get( 0 ));// cpu speed in mflops
						clientDetails.add(2, (Integer) systemInformation.get( 1 )); // memory in mb
						clientDetails.add(3, (Long) dynamicInfo.get( 0 )); //uptime
						clientDetails.add(4, (Long) dynamicInfo.get( 1 )); // total processing time
						clientDetails.add(5, (Long) dynamicInfo.get( 2 )); // latency

					}
				catch(Exception e) {
						errorLog.severe("Error taking in client resource information in getDataUnit -> "+ e);
						e.printStackTrace();
					}

				//check the expired unit list first
				if( expired.getSize() > 0 ) {
						systemLog.info( "Getting unit from expired list......." );
						//there are some expired units
						File ex = expired.returnUnit( clientCPU, clientMemory );

						if( ex != null ) {
								systemLog.info( "Found suitable unit for client......" );

								//add the unit to the pending list
								//first get the unit information
								StringTokenizer stk = new StringTokenizer( ex.getName(), "_" );
								try {
										Long unitID = new Long( stk.nextToken() );
										Long algorithmID = new Long( ex.getParentFile().getName() );

										stk.nextToken(); //time entered - throw away
										Long timeLimit = new Long( stk.nextToken() );
										Integer exceptions = new Integer( stk.nextToken() );
										Integer expireds = new Integer( stk.nextToken() );
										Integer cpu = new Integer( stk.nextToken() );
										Integer mem = new Integer( stk.nextToken() );

										//get the unit data
										byte[] dataFile = new byte[ (int) ex.length() ];
										FileInputStream fis = new FileInputStream( ex );
										int offset = 0;
										int i = 1;
										while( i > 0 ) {
												i = fis.read( dataFile, offset, dataFile.length - offset );
												offset += i;
											}
										fis.close();

										//delete the unit file
										if( ! ex.delete() ) {
												errorLog.warning( "Failed to delete expired unit after reading in to re-send out again" );
											}

										//add the unit to the pending list
										pending.addUnit( unitID, algorithmID, timeLimit, dataFile, exceptions, expireds, cpu, mem );

										//create the vector to return to the client
										Vector unit = new Vector( 6, 1 );
										unit.add( 0, clientVersion );
										unit.add( 1, unitID );
										unit.add( 2, algorithmID );
										unit.add( 3, timeLimit );
										unit.add( 4, dataFile );

										systemLog.info( "Got data unit from expired List. Unit ID : " + unitID.longValue() + ". Algorithm ID: " + algorithmID + ". Pending Size: " + pending.getSize() + "Expired List Size: " + expired.getSize() );
										return unit;
									} catch( Exception e ) {
										ByteArrayOutputStream bos = new ByteArrayOutputStream();
										PrintStream ps = new PrintStream( bos );
										e.printStackTrace( ps );
										errorLog.severe( "Failed to prepare and send expired unit to client: " + bos.toString() );

										//send a null unit
										systemLog.info( "Exception occurred when attempting to issue expired unit - issuing null unit" );
										Vector unit = new Vector( 1, 1 );
										unit.add( 0, clientVersion );
										return unit;
									}
							}
						systemLog.info( "No suitable unit found in expired list - attempting to generate new unit" );
					}

				//generate a new data unit - there are no expired units
				//vector: unitID, algorithmID, Long timeLimit, byte[] data
				Vector unit = new Vector();
				try {
						systemLog.info( "Requesting new Data unit from Scheduler" );
						unit = scheduler.generateDataUnit( clientCPU, clientMemory , clientDetails);
					} catch(Exception e) {
						errorLog.severe("Error generating unit in getDataUnit in Server -> "+ e);
						e.printStackTrace();
					}

				if( unit != null ) {
						//add the unit to the pending unit list
						Long unitID = (Long) unit.elementAt( 0 );
						Long alID = (Long) unit.elementAt( 1 );
						Long timeL = (Long) unit.elementAt( 2 ); //in milliseconds
						byte[] d = (byte[]) unit.elementAt( 3 );
						Integer cpu = (Integer) unit.elementAt( 4 );
						Integer mem = (Integer) unit.elementAt( 5 );
						pending.addUnit( unitID, alID, timeL, d, new Integer( 0 ), new Integer( 0 ), cpu, mem );

						systemLog.info( "Added Algorithm " + alID + " data unit " + unitID + " to pending list. Size: " + pending.getSize() + " TimeLimit: " + timeL );

						//remove the cpu and memory requirements
						unit.remove( 4 );
						unit.remove( 4 );

						//add the system info to the unit
						unit.add( 0, clientVersion );

						return unit;
					} else {
						//if sending a null data unit - add one element - client version
						systemLog.info( "Scheduler issued null unit - no new data units to issue to client at this time or no suitable units for client" );
						unit = new Vector( 1, 1 );
						unit.add( 0, clientVersion );
						return unit;
					}
			}

		/**a client requesting a time extension on its data unit*/
		public synchronized Long getExtension( Long unitID, Long algorithmID ) throws RemoteException {
				systemLog.info( getClientIP() + "       Received extension request: UnitID " + unitID.longValue() + " AlgorithmID " + algorithmID.longValue() );

				boolean contains = scheduler.jobRunning( algorithmID );

				//if the problem has finished - then deny the request
				if( ! contains ) {
						systemLog.info( "Cannot locate problem in system - denying extension" );
						return new Long( 0 );
					}

				Long extension = new Long( 0 );
				//check the lists to see if the unit exists
				if( pending.unitExist( unitID, algorithmID ) ) {
						systemLog.info( "Found unit in pending list" );

						//set the new time extension for the unit
						extension = pending.extendTime( unitID, algorithmID );

						if( extension.longValue() == 0 ) {
								systemLog.info( "Did not grant extension" );
							}
						return extension;
					} else if( expired.unitExist( unitID, algorithmID ) ) {
						systemLog.info( "Found unit in expired list" );

						//first get the unit information
						File unit = expired.returnUnit( algorithmID, unitID );
						if( unit != null ) {
								extension = pending.addExtendedExpiredUnit( algorithmID, unit );

								if( extension.longValue() == 0 ) {
										systemLog.info( "Did not grant extension" );
									}
							}
						return extension;
					}

				errorLog.info( "Cannot locate unit in lists - extension denied. AlgorithmID: " + algorithmID + " UnitID " + unitID );
				return extension;
			}

		/** client is requesting a certain algorithm to process a data unit */
		public Vector getAlgorithm( Long algorithmID ) throws RemoteException {
				systemLog.info( getClientIP() + "       Received Algorithm request from client. AlgorithmID: " + algorithmID.longValue() );

				//locate the correct entry in the hashtable
				Enumeration e = algorithms.keys();
				while( e.hasMoreElements() ) {
						String key = (String) e.nextElement();
						GlobalStringTokenizer gstk = null;
						try { gstk = new GlobalStringTokenizer( key, DELIMITER ); } catch( Exception ex ) {}
						gstk.nextToken(); //hash vale
						while( gstk.hasMoreTokens() ) {
								if( gstk.nextToken().compareTo( algorithmID.toString() ) == 0 ) {
										return (Vector) algorithms.get( key );
									}
							}
					}

				return null; //didnt find an entry
			}

		/**a client is reporting an exception in an algorithm  - send error logs. Adjust memory requirements*/
		public synchronized void sendAlgorithmError( Long unitID, Long algorithmID, String e ) throws RemoteException {
				int allowedExceptions = scheduler.getExceptions( algorithmID );

				if( allowedExceptions == -1 ) {
						errorLog.warning( getClientIP() + "     Error Occurred in Old Algorithm " + algorithmID.longValue() + " at client with unitID " + unitID.longValue() + " -> " + e );
						return;
					} else {
						errorLog.warning( getClientIP() + "     Error Occurred in Algorithm " + algorithmID.longValue() + " at client with unitID " + unitID.longValue() + " -> " + e );
					}

				//if unit in pending list
				if( pending.unitExist( unitID, algorithmID ) ) {
						int times = pending.getExceptionTimes( unitID, algorithmID ) + 1;

						if( times > allowedExceptions ) {
								scheduler.logAlgorithmError( unitID, algorithmID, e );

								//stop problem - unit causing exceptions multiple times
								errorLog.severe( "Stopping Algorithm : " + unitID.longValue() + " unit caused multiple algorithm exceptions at clients" );
								scheduler.killJob( algorithmID ); //scheduler - remove problem here
							}
						else {
								//increment the number of exceptions that have occurred with this unit
								pending.incrementExceptions( unitID, algorithmID );

								//if the error was caused by an out of memory error - increase the memory requirements
								if( e.matches( ".*OutOfMemoryError.*" ) ) {
										File unit = pending.increaseMemoryRequirement( unitID, algorithmID, maxClientMemory );

										if( unit != null ) {
												//move to the expired list - check a directory exists already
												File probDir = new File( expired.getParentDirectory(), algorithmID.toString() );
												if( ! probDir.isDirectory() ) {
														probDir.mkdir();
													}

												File unitExpired = new File( probDir, unit.getName() );
												if( unit.renameTo( unitExpired ) ) //move the unit to the expired list
													{
														pending.decrementSize();
														expired.incrementSize();
													}
											}
									} else if( e.matches( ".*Unit running for.*" ) ) {
										//error was caused by the unit having too slow a CPU - increase min CPU
										File unit = pending.increaseCPURequirement( unitID, algorithmID, maxClientCPU );

										if( unit != null ) {
												//move unit to expired list
												File probDir = new File( expired.getParentDirectory(), algorithmID.toString() );
												if( ! probDir.isDirectory() ) {
														probDir.mkdir();
													}

												File unitExpired = new File( probDir, unit.getName() );
												if( unit.renameTo( unitExpired ) ) //move the unit to the expired list
													{
														pending.decrementSize();
														expired.incrementSize();
													}
											}
									}
							}
					} else if( expired.unitExist( unitID, algorithmID ) ) {
						int times = expired.getExceptionTimes( unitID, algorithmID ) + 1;

						if( times > allowedExceptions ) {
								scheduler.logAlgorithmError( unitID, algorithmID, e );

								//stop problem - unit causing exceptions multiple times
								errorLog.severe( "Stopping Algorithm: " + unitID.longValue() + " unit caused multiple algorithm exceptions at clients" );
								scheduler.killJob( algorithmID ); //scheduler - remove problem here
							}
						else {
								expired.incrementExceptions( unitID, algorithmID );

								//if the error was caused by an out of memory error - increase the memory requirements
								if( e.endsWith( "java.lang.OutOfMemoryError" ) ) {
										expired.increaseMemoryRequirement( unitID, algorithmID, maxClientMemory );
									} else if( e.matches( ".*Unit running for 480000.*" ) ) {
										expired.increaseCPURequirement( unitID, algorithmID, maxClientCPU );
									}
							}
					} else {
						errorLog.warning( "Unlisted unit returned and Exception. Algorithm ID " + algorithmID.toString() + " Unit ID " + unitID.toString() );
					}
			}

		/**client is sending a result set back to the server - remove unit from lists of out data units */
		public synchronized void sendResults( Long unitID, Long algorithmID, Vector results, Vector systemInformation, Vector dynamicInformation ) throws RemoteException {
				String ip = getClientIP();
				systemLog.info( ip + "  Results set received. UnitID: " + unitID.longValue() + " AlgorithmID " + algorithmID.longValue() );


				//make sure no problem with the results set
				if( results == null ) {
						systemLog.warning( "Null Results set received" );
					} else if( results.size() == 0 ) {
						//results vector is size 0 - record this but dont discard results
						systemLog.warning( "Results Vector size: 0" );
					}

				//first check that the unit exists
				if( pending.unitExist( unitID, algorithmID ) ) {
						Long[] timingInfo = (Long[]) results.elementAt( 0 );
						if( ! scheduler.handleResults( unitID, algorithmID, (byte[]) results.elementAt( 1 ), timingInfo ,systemInformation, dynamicInformation ,ip ) ) {
								return;
							}
						pending.removeUnit( unitID, algorithmID );
						timingLog.info( System.currentTimeMillis() + " " + ip + " " + systemInformation.get( 0 ) + " " + systemInformation.get( 1 ) + " " + dynamicInformation.get( 0 ) + " " + dynamicInformation.get( 1 ) + " " + dynamicInformation.get( 2 ) + " " + dynamicInformation.get( 3 ) + " "+dynamicInformation.get( 4 )  + " " + algorithmID.toString() + " " + unitID.toString() + " " + timingInfo[ 0 ].toString() + " " + timingInfo[ 1 ].toString() + " " + systemInformation.get( 2 ) + " " + systemInformation.get( 4 ) + " " + systemInformation.get( 3 ) + "\n" );

						//unit could exist in both lists - although it shouldnt! log to error if it happens
						if( expired.unitExist( unitID, algorithmID ) ) {
								errorLog.warning( "Found unit in both pending and expired lists: AlgorithmID " + algorithmID.toString() + " UnitID " + unitID.toString() );
								expired.removeUnit( unitID, algorithmID );
							}
					} else if( expired.unitExist( unitID, algorithmID ) ) {
						Long[] timingInfo = (Long[]) results.elementAt( 0 );
						if( ! scheduler.handleResults( unitID, algorithmID, (byte[]) results.elementAt( 1 ), (Long[]) results.elementAt( 0 )  ,systemInformation, dynamicInformation ,ip) ) {
								return;
							}
						expired.removeUnit( unitID, algorithmID );
						timingLog.info( System.currentTimeMillis() + " " + ip + " " + systemInformation.get( 0 ) + " " + systemInformation.get( 1 ) + " " + dynamicInformation.get( 0 ) + " " + dynamicInformation.get( 1 ) + " " + dynamicInformation.get( 2 ) + " " + dynamicInformation.get( 3 ) + " "+dynamicInformation.get( 4 )  + " " + algorithmID.toString() + " " + unitID.toString() + " " + timingInfo[ 0 ].toString() + " " + timingInfo[ 1 ].toString() + " " + systemInformation.get( 2 ) + " " + systemInformation.get( 4 ) + " " + systemInformation.get( 3 ) + "\n" );
					} else {
						errorLog.warning( "Unlisted unit returned: unitID " + unitID.longValue() + " algorithmID " + algorithmID.longValue() );
					}

				systemLog.info( "Units being Processed: " + pending.getSize() + ". Expired List Size: " + expired.getSize() );
			}

		/**client stubs checking to see if there is a newer version of the client software*/
		public byte[] getNewestVersion() throws RemoteException {
				if( clientJar == null ) {
						return null;
					}

				//check if the client is in the range of ips that should get the new client
				if( clientJarIPRange != null ) {
						//if this client is not within the ip range for clients that should receive this version of the client
						if( ! getClientIP().startsWith( clientJarIPRange ) ) {
								systemLog.info( "Client IP doesnt match with this client version IP range - no download" );
								return null;
							} else {
								systemLog.info( "Client IP matches this client version IP range - download" );
								synchronized( clientJar ) {
										return clientJar;
									}
							}
					} else {
						systemLog.info( "No Client IP range for this client version - download" );
						synchronized( clientJar ) {
								return clientJar;
							}
					}
			}

		/**client reporting exception - sends to server error logs*/
		public void reportException( String ex ) throws RemoteException {
				errorLog.severe( getClientIP() + "      Exception at client - " + ex );
			}
		//*********************************end client comms interface************************

		//*********************************start scheduler interface*************************
		/**scheduler is adding a new algorithm to the set of current algorithms */
		public void addAlgorithm( Long algorithmID, byte[] algorithm, Vector classDefinitions, Vector jarDefinitions ) {
			Vector defs = new Vector();
			defs.add( 0, algorithm );
			defs.add( 1, classDefinitions );
			defs.add( 2, jarDefinitions );

			//compute a hash value for the algorithm and libraries
			byte[] hash = computeHash( algorithm, classDefinitions, jarDefinitions );
			defs.add( hash );

			Enumeration keys = algorithms.keys();
			String nhash = new String( hash );
			while( keys.hasMoreElements() ) {
					String key = (String) keys.nextElement();
					GlobalStringTokenizer stkey = null;
					try {stkey = new GlobalStringTokenizer( key, DELIMITER );} catch( Exception e ) {}
					String hash_ = stkey.nextToken();
					if( hash_.compareTo( nhash ) == 0 ) //if found an identical algorithm definition
						{
							String newKey = key + DELIMITER + algorithmID.toString();
							synchronized( algorithms ) //update the entry in the hash table to include this algorithmID
								{
									Object entry = algorithms.remove( key );
									algorithms.put( newKey, entry );
								}

							systemLog.info( "New algorithm (duplicate definition) added to server: AlgorithmID " + algorithmID.toString() );
							return;
						}
				}

			//no match found - then create a new entry
			String newKey = new String( hash );
			newKey = newKey + DELIMITER + algorithmID.toString();
			synchronized( algorithms ) {
					systemLog.info( "New algorithm with no existing definition added to server: AlgorithmID " + algorithmID.toString() );
					algorithms.put( newKey, defs );
				}
		}

		/**scheduler is removing an algorithm from the set of current algorithms*/
		public void removeAlgorithm( Long algorithmID ) {
			systemLog.info( "Algorithm being removed from server: AlgorithmID " + algorithmID.longValue() );

			Enumeration keys = algorithms.keys();
			while( keys.hasMoreElements() ) {
					String key = (String) keys.nextElement();
					GlobalStringTokenizer stkey = null;
					try { stkey = new GlobalStringTokenizer( key, DELIMITER ); } catch( Exception e ) {}
					String restOfKey = stkey.nextToken(); //hash part of key
					String nextID = stkey.nextToken();
					if( nextID.compareTo( algorithmID.toString() ) == 0 && ! stkey.hasMoreTokens() ) {
							//delete the entry in the hash table
							synchronized( algorithms ) {
									algorithms.remove( key );
									systemLog.info( "Found algorithm (unique): " + algorithmID );
									break;
								}
						} else {
							while( stkey.hasMoreTokens() ) //search for the ID in the rest of the key
								{
									if( nextID.compareTo( algorithmID.toString() ) == 0 ) {
											//update the entry in the hashtable
											while( stkey.hasMoreTokens() ) {
													restOfKey = restOfKey + DELIMITER + stkey.nextToken();
												}

											synchronized( algorithms ) //remove the old entry and add new entry without the algorithmID being removed
												{
													Object entry = algorithms.remove( key );
													algorithms.put( restOfKey, entry );
												}
											systemLog.info( "Found algorithm (in duplicate): " + algorithmID );
											break;
										} else {
											restOfKey = restOfKey + DELIMITER + nextID;
										}
									nextID = stkey.nextToken();
								}
						}
				}

			//tell the FileServer to stop any dowloads relating to this problem also
			fs.stopDownloads( algorithmID );

			//remove any leftover data units from the stores
			systemLog.info( "Removing problem DataUnits" );
			pending.removeUnits( algorithmID );
			expired.removeUnits( algorithmID );

			systemLog.info( "Algorithm Fully removed from Server" );
		}

		/**called by the scheduler to set the current version of the client jar file*/
		public void setClientJar( Integer version, byte[] clientJ, String IPRange ) {
			systemLog.info( "Updating Client on Server" );

			if( IPRange != null) {
					clientJarIPRange = IPRange;
				} else {
					//if its a universal update then there is no range
					clientJarIPRange = null;
				}

			clientVersion = null;
			clientVersion = version;
			if( clientJar != null ) {
					synchronized( clientJar ) {
							clientJar = clientJ;
						}
				} else {
					clientJar = clientJ;
				}
			systemLog.info( "Client updated successfully - version: " + clientVersion.toString() + " Only issuing to IP prefixes: " + clientJarIPRange );
		}

		/**method that returns some statistics on server*/
		public Vector getServerStats() {
			systemLog.info( "Scheduler requested server statistics - retrieving" );
			Vector stats = new Vector();

			//pending units info
			String ex = "Number of pending Units: " + pending.getSize();
			stats.add( ex );
			ex = "Number of expired Units: " + expired.getSize();
			stats.add( ex );
			ex = "Number of Clients Connected since startup: " + totalConnected.size();
			stats.add( ex );
			ex = "Number of Clients connected since timeout: " + clientIPs.size();
			stats.add( ex );

			ex = "Server Timeout: " + timeout + " minutes";
			stats.add( ex );
			if( clientVersion != null ) {
					if( clientVersion.intValue() != 0 ) {
							if( clientJarIPRange != null ) {
									ex = "Current Client Version: " + clientVersion.toString() + ". Issuing to Client IP's: " + clientJarIPRange;
									stats.add( ex );
								} else {
									ex = "Current Client Version: " + clientVersion.toString() + ". Issuing to all clients";
									stats.add( ex );
								}
						} else {
							ex = "No client update loaded into system";
							stats.add( ex );
						}
				}
			systemLog.info( "Got server statistics successfully" );
			return stats;
		}

		public void updateTimeout( Long time ) {
			//cause a timeout
			serverTimeout();

			//kill the timerthread
			timer.stop();

			//restart timer with new timeout
			timer = new timerThread( time.longValue(), this, systemLog );
			timer.setPriority( Thread.MIN_PRIORITY );
			timer.start();
			timeout = time.longValue();
		}
		//*******************************end scheduler interface***********************************

		//*******************************start server methods********************************************
		/**called periodically by the timerThread to check for expired units*/
		public void serverTimeout() {
			//do a check on the unit lists to make sure consistency with the list of running problems
			Long[] ids = scheduler.getCurrentAlgorithmIDs();
			pending.checkConsistent( ids );
			expired.checkConsistent( ids );

			//check the pending list for expired units
			boolean finished = false;
			Vector expiredUnits = null;

			while( ! finished ) {
					expiredUnits = pending.getExpired();

					if( expiredUnits.size() == 0 ) //if there are no expired units then finished
						{
							finished = true;
						} else {
							//check that no unit has expired more than the maximum number of allowed times
							for( int i = 0; i < expiredUnits.size(); i ++ ) {
									File unit = (File) expiredUnits.get( i );
									StringTokenizer stk = new StringTokenizer( unit.getName(), "_" );
									String uid = stk.nextToken();
									stk.nextToken();
									stk.nextToken();
									try {
											int expiredTimes = Integer.parseInt( stk.nextToken() ) + 1;
											Long aid = new Long( unit.getParentFile().getName() );
											int expireAllowed = scheduler.getExpired( aid );
											if( expiredTimes > expireAllowed && expireAllowed != -1 ) {
													errorLog.info( "Removing problem from system - unit expired " + expiredTimes + " times. " + "Algorithm ID: " + aid + ". UnitID: " + uid );
													try {
															scheduler.killJob( aid );
														} catch( Exception e ) {}

													break; //go around again and reset the list of expired units
												}
										}
									catch( NumberFormatException nfe ) {
											errorLog.warning( "Failed to extract expired times info from unit" );
											continue;
										}

									//if the for loop got to the end - then we are finished
									if( i == expiredUnits.size() - 1 ) {
											finished = true;
										}
								}
						}
				}

			systemLog.info( "Server timeout occurred: " + expiredUnits.size() + " data units moved to expired list" );
			if( expiredUnits.size() > 0 ) {
					expired.addExpiredUnits( expiredUnits );
				}

			systemLog.info( "Units being processed: " + pending.getSize() );
			systemLog.info( clientIPs.size() + " unique clients have connected to the server since last timeout" );

			synchronized( clientIPs ) {
					for( int i = 0; i < clientIPs.size(); i ++ ) {
							String ip = (String) clientIPs.get( i );
							if( ! totalConnected.contains( ip ) ) {
									totalConnected.add( ip );
								}
						}
					clientIPs.clear();
				}

			//check the problem directory to make sure there are no partially uploaded problem files left over - if so delete
			scheduler.verifyProblemsDirectory();

			//sychronize the size variable in the pending and expired lists
			pending.synchronizeSize();
			expired.synchronizeSize();

			//delete any completed problem files that are greater than 10 days old
			File[] files = ( new File( System.getProperty( "user.dir" ), "problems" ) ).listFiles();
			long currentTime = System.currentTimeMillis();
			for( int i = 0; i < files.length; i ++ ) {
					if( files[ i ].isFile() && files[ i ].getName().endsWith( ".zip" ) ) {
							if( currentTime - files[ i ].lastModified() > 864000000 ) {
									files[ i ].delete();
								}
						}
				}

			//reset the max client memory (to keep up to date with recently connected set of clients)
			maxClientMemory = 0;
			maxClientCPU = 0;

			systemLog.info( totalConnected.size() + " unique clients have connected to server since first timeout" );
		}

		/**method to retrieve client info - only called inside an rmi method */
		private String getClientIP() {
			try {
					String clientIP = RemoteServer.getClientHost();

					synchronized( clientIPs ) {
							if( ! clientIPs.contains( clientIP ) ) {
									clientIPs.add( clientIP );
								}
						}

					return clientIP;
				} catch( Exception e ) {
					systemLog.warning( "Server - getClientIP -> could not retrieve clientIP: " + e);
				}

			return null;
		}

		public int getPort() {
			return port;
		}

		/**method that takes a directory and deletes all the contents*/
		private void recursiveDelete( File f ) {
			//recusively delete the contents of this directory
			File[] files = f.listFiles();

			if( files != null ) {
					for( int i = 0; i < files.length; i ++ ) {
							if( files[ i ].isFile() ) {
									files[ i ].delete();
								} else if( files[ i ].isDirectory() ) {
									//delete subdirectory
									recursiveDelete( files[ i ] );
									files[ i ].delete();
								}
						}
				}
		}

		/** Caculates the MD5 hash of the problem algorithm*/
		private byte[] computeHash( byte[] algorithm, Vector classDefs, Vector jarDefs ) {
			try {
					MessageDigest md5 = MessageDigest.getInstance( "MD5" );
					md5.update( algorithm );
					for( int i = 1; i < classDefs.size(); i = i + 2 ) {
							md5.update( (byte[]) classDefs.get( i ) );
						}

					for( int i = 1; i < jarDefs.size(); i = i + 2 ) {
							md5.update( (byte[]) jarDefs.get( i ) );
						}

					return md5.digest();
				} catch( Exception e ) {
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					PrintStream ps = new PrintStream( bos );
					e.printStackTrace( ps );
					errorLog.warning( "Failed to compute hash of problem algorithm: " + bos.toString() );
				}
			return null;
		}
		//*************************************end server methods*****************************************

		public static void main( String args[] ) {
			try {
					//check if there is a logs directory
					File dir = new File( System.getProperty( "user.dir" ), "logs" );
					if( ! dir.isDirectory() ) {
							dir.mkdir();
						} else {
							File[] files = dir.listFiles();
							if( files != null && files.length > 0 ) {
									System.out.println( "There may be old log files you may wish to keep - delete or rename the 'logs' directory and try again" );
									System.exit( 1 );
								}
						}

					File problems = new File( System.getProperty( "user.dir" ), "problems" );

					if( ! problems.isDirectory() ) {
							problems.mkdir();
						}

					System.out.println( "The server is starting up. Check the 'system0.log' file (in the 'logs' directory) for the status of the server........" );

					File policy = new File( System.getProperty( "user.dir" ), "security.pol" );
					if( ! policy.exists() ) {
							//extract the policy file from the jar file and place on disk
							JarFile jf = new JarFile( new File( "server.jar" ) );
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
							File securityPol = new File( System.getProperty( "user.dir" ), "security.pol" );
							FileOutputStream fos = new FileOutputStream( securityPol );
							fos.write( polFile );
							fos.close();

							polFile = null;
							jf = null;
							entry = null;
							fos = null;

							//figure out how much memory the server is allocated
							long availableMemory = Runtime.getRuntime().maxMemory();

							String version = System.getProperty( "java.version" );
							int major = 0, minor = 0, minor1 = 0;
							try {
									major = Integer.parseInt( "" + version.charAt( 0 ) );
									minor = Integer.parseInt( "" + version.charAt( 2 ) );
									minor1 = Integer.parseInt( "" + version.charAt( 4 ) );
								} catch( Exception e ) {}

							if( major == 1 && minor == 4 && minor1 < 2 ) //jvm versions 1.4.0 & 1.4.1
								{
									//bug in certain jvm's that causes memory to be over stated by 64MB (bug ID 4686462)
									availableMemory = ( ( availableMemory - ( 64 * 1024 * 1024 ) ) / ( 1024 * 1024 ) ) + 1;

									//restart the server and it should go into the else below
									//Process restarted = Runtime.getRuntime().exec( "java -Xrs -Xmx" + availableMemory + "m -Djava.security.policy==security.pol -classpath /usr/local/javaProfilers/ymp-1.0.2-build115/lib/ympagent.jar:./server.jar -Xrunympagent -Dymp.agent.port=10000 Server" );
									Process restarted = Runtime.getRuntime().exec( "java -Xrs -Xmx" + availableMemory + "m -Djava.security.policy==security.pol -jar server.jar" );
								} else {
									//restart the server and it should go into the else below
									//Process restarted = Runtime.getRuntime().exec( "java -Xrs -Xmx" + availableMemory + " -Djava.security.policy==security.pol -classpath /usr/local/javaProfilers/ymp-1.0.2-build115/lib/ympagent.jar:./server.jar -Xrunympagent -Dymp.agent.port=10000 Server" );
									Process restarted = Runtime.getRuntime().exec( "java -Xrs -Xmx" + availableMemory + " -Djava.security.policy==security.pol -jar server.jar" );
									//Process restarted = Runtime.getRuntime().exec( "/home/andrew/Downloads/jre1.5.0_05/bin/java -javaagent:/home/andrew/system/testsys/server/DistributedInstrumenter.jar -Xrs -Xmx" + availableMemory + " -Djava.security.policy==security.pol -jar server.jar" );
								}
						}
					else {
							//start the security manager
							System.setSecurityManager( new SecurityManager() );

							//delete the policy file
							policy.delete();

							//create the server
							Server server = new Server();
							ClientCommunications stub = ( ClientCommunications ) UnicastRemoteObject.exportObject( server );

							int port = server.getPort();
							Registry registry = LocateRegistry.getRegistry( port );
							registry.bind( "server", stub );
						}
				} catch( Exception e ) {
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					PrintStream ps = new PrintStream( bos );
					e.printStackTrace( ps );

					System.out.println( "Exception in server - main -> " + bos.toString() );
				}
		}
	}
