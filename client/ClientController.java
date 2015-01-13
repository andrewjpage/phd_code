/* Thomas Keane
This is the main client class. It initialises & manages the overall client and the running
of the data units on the downloaded algorithms.
 
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
import java.net.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;
import java.io.*;
import java.security.*;
import java.rmi.*;
import java.rmi.registry.*; //needed to look up the communications stub

//implements the state interface because when the data units are executing the client
//can be in one of a number of states
public class ClientController extends Thread {
		//9001 - added code to send back lots of control/system information with each result set
		public static final int CLIENTVERSION = 9105;

		//states the program can be in at any time
		public static final int IDLE = 0;
		public static final int ALGORITHM_EXECUTING = 1;
		public static final int GET_PROBLEM_DATA = 2;
		public static final int ALGORITHM_EXCEPTION = 3;
		public static final int ALGORITHM_FINISHED = 4;
		public static final int ALGORITHM_TIMEOUT = 5;

		//static variables - define system infomation
		private String SERVERIP;
		private int SERVERPORT;
		private int SOCKETPORT;
		private final int MAX_PROCESSING_TIME = 480 * 60000; //max time client will keep processing one single unit before killing the unit
		private File INCOMPLETE_UNIT;
		private File CLASSES_DIR;
		private File TEMP_DIRECTORY;

		//info about the machine the client is running on
		private int AVAILABLE_MEMORY;
		private int CPU_SPEED_MFLOPS;
		private String OS_NAME;
		private String OS_VERSION;
		private String OS_ARCH;
		
		private int MFLOPS;

		//#milliseconds the client will sleep for when cant connect
		public long SLEEPTIME;

		private ClientCommunications server;  //used to comm with server

		//some variables needed
		protected int currentState; //current state of client
		protected String fileName; //name of a problem data file needed from server
		protected Throwable except; //details of an exception that occurred in a running Algorithm
		private AlgorithmLoader aLoader; //loads new algorithms

		//used to keep track + report back communication over head
		private long startTime;
		private long finishTime;
		private long globalTotalProcessingTime; //amount of time client has spent processing since it started up
		private Vector systemInformation;
		private long clientStartedTime;

		//variable used to say whether or not there has been a second attempt to run a unit (when a problem occured with first attempt)
		private boolean retried;

		//information on the current unit
		private Long unitID;
		private Long algorithmID;

		private long latency;

		//only constructor available - pass in the parent classloader
		public ClientController( Properties systemInfo ) {
			INCOMPLETE_UNIT = new File( System.getProperty( "user.dir" ), "incomplete.unit" );
			CLASSES_DIR = new File( System.getProperty( "user.dir" ), "classes" );
			TEMP_DIRECTORY = new File( System.getProperty( "user.dir" ), "temp" );

			startTime = 0;
			clientStartedTime = System.currentTimeMillis();
			retried = false;
			currentState = IDLE;
			fileName = null;
			except = null;
			unitID = null;
			algorithmID = null;
			latency = 0;

			//read the version number of the client
			try {
					//get the system properties - must all exist else exit
					SERVERIP = systemInfo.getProperty( "server.ip" );
					SERVERPORT = 15000; //Integer.parseInt( systemInfo.getProperty( "server.port" ) );
					SOCKETPORT = 15001; //Integer.parseInt( systemInfo.getProperty( "socket.port" ) );
					SLEEPTIME = 1000 * 60 * ( new Long( systemInfo.getProperty( "client.timeout" ) ) ).longValue();
					MFLOPS =   new Integer( systemInfo.getProperty( "client.mf" ) ).intValue();

					OS_NAME = System.getProperty( "os.name" );
					OS_ARCH = System.getProperty( "os.arch" );
					OS_VERSION = System.getProperty( "os.version" );

					//find out how much available memory the donor has for the jvm
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
						} else {
							availableMemory = ( availableMemory / ( 1024 * 1024 ) ) + 1;
						}
					CPU_SPEED_MFLOPS = 0;
					AVAILABLE_MEMORY = (int) availableMemory;

					//clear any old problem jar files
					if( CLASSES_DIR.isDirectory() ) {
							recursiveDelete( CLASSES_DIR );
						} else {
							CLASSES_DIR.mkdir();
						}

					//create a temp directory that the client will use
					if( ! TEMP_DIRECTORY.exists() ) {
							TEMP_DIRECTORY.mkdir();
						}

					//clear any old temp files
					clearTempDirectories();

					//create a new algorithm class loader passing parent loader in
					URL[] urls = { CLASSES_DIR.toURL() };
					aLoader = new AlgorithmLoader( urls );

					systemInformation = new Vector();
					systemInformation.add( new Integer( MFLOPS ) );
					systemInformation.add( new Integer( AVAILABLE_MEMORY ) );
					systemInformation.add( OS_NAME );
					systemInformation.add( OS_ARCH );
					systemInformation.add( OS_VERSION );
				} catch( Throwable e ) {
					//get some detailed info from the exception - stack trace
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					PrintStream ps = new PrintStream( bos );
					e.printStackTrace( ps );

					//exception in constructor - remove problem
					System.err.println( bos.toString() );
					return;
				}
		}

		public ClientController() {}

		//methods to get/set state of client
		public int getCurrentState() {
			return currentState;
		}

		public void setCurrentState( int s ) {
			currentState = s;
		}

		public void sendFileName( String name ) {
			fileName = name;
		}

		public void sendException( Throwable ex ) {
			except = ex;
		}

		//start the client running - client runs infinitely
		public void run() {
			try {
					//redirect the standard error to a file
					File stderr = new File( System.getProperty( "user.dir" ), "stderror.out" );
					System.setErr( new PrintStream( new FileOutputStream( stderr ), true ) );

					//redirect the standard out to a file (shouldnt be any output but ya never know!)
					File stdout = new File( System.getProperty( "user.dir" ), "stdout.out" );
					System.setOut( new PrintStream( new FileOutputStream( stdout ), true ) );
				} catch( IOException ioe ) {}

			System.out.println( "Client started successfully" );
			System.out.println( "Check server log files ('system0.log' in 'logs' directory on server) to confirm client is connecting to the server" );

			//set up the RMI comms with the server - get comm stub
			while( true ) {
					try {
							Registry registry = LocateRegistry.getRegistry( SERVERIP, SERVERPORT );
							server = (ClientCommunications) registry.lookup( "server" );
							break;
						} catch( Exception e ) {
							System.out.println( "Failed to connect to server: " + e.toString() );
							//wait and retry to connect in a while
							goAsleep();
						}
				}

			finishTime = System.currentTimeMillis();

			//enter an infinite loop of continuously requesting and processing data units
			while( true ) {
					//default to false (havent retried to run a unit)
					retried = false;

					//store data unit in Vector
					Vector dataUnit = checkForIncomplete(); //see if there is a dataunit on disk (left from an update of client software)

					//run the Linpack measure and see if returns a larger MFLOPS value
					OOLinpack ool = new OOLinpack( 500 );
					int currentMeasure = (int) ool.getMFLOPS();
					if( currentMeasure > CPU_SPEED_MFLOPS ) //update the value if got a greater value this time (CPU more idle)
						{
							CPU_SPEED_MFLOPS = currentMeasure;
							systemInformation.remove( 0 );
							systemInformation.add( 0, new Integer( MFLOPS ) );
						}



					//clear the temmp directory
					clearTempDirectories();

					Vector dynamicInfo = new Vector();
					// AP - send the uptime and total processing time done so far
					dynamicInfo.add(0,new Long( System.currentTimeMillis() - clientStartedTime )); //uptime
					dynamicInfo.add(1, new Long(globalTotalProcessingTime)); // total processing time

					if( dataUnit == null ) {
							//contact the server and request a unit
							while( true ) {
									try {

											long b4ping = System.currentTimeMillis();
											server.ping();
											latency = System.currentTimeMillis() - b4ping;

											dynamicInfo.add(2,new Long(latency));

											dataUnit = server.getDataUnit( systemInformation , dynamicInfo);
											break;
										} catch( Exception e ) {
											//attempt to report the exception to the server
											System.out.println( "Error getting unit: " + e  );
											goAsleep();
											resetServerConn(); //must get a reference to the registry again before retry
										}
								}
						}

					int currentVersion = 0;
					try {
							//check the client version
							currentVersion = ( (Integer) dataUnit.elementAt( 0 ) ).intValue();
						} catch( Exception e ) {
							reportException( "Client - Run - Exception parsing client version from null unit -> " + e, true );
							continue;
						}

					//if the server has a different version of the client software
					if( currentVersion != CLIENTVERSION && currentVersion != 0 ) {
							byte[] clientJar = null;

							while( true ) {
									try {
											//get the newest client jar file and write to disk
											clientJar = server.getNewestVersion();
											break;
										} catch( Exception e ) {
											resetServerConn();
										}
								}

							//if the new jar was downloaded then install it
							if( clientJar != null ) {
									File newJar = null;
									try {
											//replace the old file
											newJar = new File( System.getProperty( "user.dir" ), "client.jar" );

											//delete the old client
											if( newJar.exists() ) {
													newJar.delete();
												}

											//create the new client jar file
											newJar.createNewFile();

											//write the bytes out to the file
											FileOutputStream fos = new FileOutputStream( newJar );
											fos.write( clientJar );
											fos.close();
										} catch( IOException ioe ) {
											if( ioe.toString().startsWith( "java.io.IOException: There is not enough space on the disk" ) ) {
													clearTempDirectories();

													//(re)try to write new client after clearing the directories
													try {
															//create the new client jar file
															newJar.createNewFile();

															//write the bytes out to the file
															FileOutputStream fos = new FileOutputStream( newJar );
															fos.write( clientJar );
															fos.close();
														} catch( IOException e ) {
															reportException( "Exception in Client - run() while writing new client Jar file: " + ioe + ". Attempt to clear temp directories failed to create necessary space", true );
															continue;
														}
												} else {
													reportException( "Exception in Client - run() while writing new client Jar file: " + ioe, true );
													continue;
												}
										}
									catch( Exception e ) {
											reportException( "Exception in Client - run() while writing new client Jar file: " + e, true );
											continue;
										}

									try {
											//if we got a unit to process - then serialise it to disk to restart when client starts again
											if( dataUnit.size() > 1 ) {
													ObjectOutputStream oos = new ObjectOutputStream( new FileOutputStream( INCOMPLETE_UNIT ) );
													oos.writeObject( dataUnit );
													oos.close();
												}

											//exit the client & restart the new version
											return;
										} catch( IOException ioe ) {
											if( ioe.toString().startsWith( "java.io.IOException: There is not enough space on the disk" ) ) {
													clearTempDirectories();
												} else {
													reportException( "Exception in Client - run() while writing work unit to disk: " + ioe, true );
													return;
												}
										}
									catch( Exception e ) {
											reportException( "Exception in Client - run() while writing work unit to disk: " + e, true );
											return;
										}
									return;
								}
						}

					if( dataUnit.size() == 1 ) {
							//if the server has no new data units to issue - then sleep and reset
							goAsleep();
							finishTime = System.currentTimeMillis();  //reset the previous finish time (used to calculate the network time)
							continue;
						}

					byte[] compressedData = null;
					Long timeLimit = null;
					int newestC = 0;
					try {
							//extract the unit info from the data unit
							unitID = (Long) dataUnit.elementAt( 1 );
							algorithmID = (Long) dataUnit.elementAt( 2 );
							timeLimit = (Long) dataUnit.elementAt( 3 );
							compressedData = (byte[]) dataUnit.elementAt( 4 );
						} catch( Exception e ) {
							reportException( "Client - Run() - Exception while reading data unit contents: " + e, true );
							continue;
						}

					//have the algorithm ID -> now check if its already loaded
					Class algorithm = aLoader.findClass( algorithmID );

					//if we dont already have the algorithm cached
					if( algorithm == null ) {
							Throwable t = aLoader.getException();
							if( t != null ) //if there was an exception - report it and goto next unit
								{
									//report the exception to the server
									//get some detailed info from the exception - stack trace
									ByteArrayOutputStream bos = new ByteArrayOutputStream();
									PrintStream ps = new PrintStream( bos );
									t.printStackTrace( ps );

									//exception in DM - remove problem
									reportException( "Exception in AlgorithmLoader: " + bos.toString(), true );
									continue;
								}

							//Algorithm loader returned null and no exception occurred -> download the algorithm
							algorithm = downloadAndLoad( algorithmID );

							if( algorithm == null ) //if the algorithm wasnt loaded successfully
								{
									if( aLoader.getReset() ) //if we have received a different version of a previous algorithm
										{
											//reset the algorithm loader
											resetALoader();

											//try to load the problem files again
											algorithm = downloadAndLoad( algorithmID );
										}

									//if the algorithm is still null - report an exception and goto next unit
									if( algorithm == null ) {
											try {
													server.sendAlgorithmError( unitID, algorithmID,  "Failed to load Algorithm: " + aLoader.getException() );
												} catch( Exception e ) {
													resetServerConn();
												}
											continue;
										}
								}
						}

					//********begin processing the data unit*********
					Vector data;
					Compressor.reset();
					try {
							//first decompress the data unit
							data = (Vector) Compressor.decompress( aLoader, compressedData );
							Compressor.reset();
						} catch( Exception e ) {
							reportException( "Exception in Client - runClient - while decompressing problem data -> AlgorithmID " + algorithmID.longValue() + " Unit ID " + unitID.toString() + " " + e, true );
							continue;
						}

					//set to null
					compressedData = null;

					
					/****** AP  Send in the mflops of the client to the last position of the input data, only for the simulation to work*/
					data.add(new Integer(MFLOPS));
					//create the Algorithm thread monitor
					AlgorithmMonitorThread atm = new AlgorithmMonitorThread( this, data, algorithm, unitID, algorithmID );
					atm.setPriority( Thread.MIN_PRIORITY );

					//begin the timer for the unit
					timerThread timer = new timerThread( timeLimit.longValue(), this );

					//variable to hold how long unit has been running
					long timeRunning = 0;
					int numExtensions = 0;

					//start the processing of the data unit
					startTime = System.currentTimeMillis();
					setCurrentState( ALGORITHM_EXECUTING );

					//start the threads
					timer.start();
					atm.start();

					while( true ) {
							//keeps track of the files that have been requested by this unit from the server - repeat requests for same file -> problem in system or algorithm
							Vector filesRequested = new Vector();

							//state that the client is in when the algorithm is executing
							if( getCurrentState() == ALGORITHM_EXECUTING ) {
									try {
											synchronized( this ) {
													//must wait to be notified by algorithm or timer of an event
													wait();
												}
										} catch( Exception e ) {
											reportException( "Exception in Client - run() while waiting for notify: " + e, true );
											break;
										}

									//tell the timer it is finished - exits gracefully
									if( timer._isAlive() ) {
											timer.finished(); //tell the timer it is finished and not to notify
										}

									//allow the AlgorithmMonitor thread to fully complete

									try { sleep( 300 ); } catch( Exception e ) {}}
							//exception occurred in the algorithm - report it back to the server

							else if( getCurrentState() == ALGORITHM_EXCEPTION ) {
									if( atm.isAlive() ) //check if the AlgorithmMonitorThread is still alive
										{
											return; //ONLY sure way to kill this thread is to reset the client
										}

									//get some detailed info from the exception - stack trace

									ByteArrayOutputStream bos = new ByteArrayOutputStream();
									PrintStream ps = new PrintStream( bos );
									except.printStackTrace( ps );
									String details = bos.toString();

									//see if the exception was caused by running out of disk space - if so - attempt to clear some disk space and re-run
									if( details.startsWith( "java.io.IOException: There is not enough space on the disk" ) && ! retried ) {
											clearTempDirectories();

											//now attempt to run the unit again with the data downloaded
											setCurrentState( ALGORITHM_EXECUTING );
											timer = new timerThread( timeLimit.longValue(), this );
											timer.start();

											//create the Algorithm thread monitor
											atm = new AlgorithmMonitorThread( this, data, algorithm, unitID, algorithmID );
											atm.setPriority( Thread.MIN_PRIORITY );
											atm.start();

											retried = true; //record that its the second attempt to run unit

											continue;
										}

									//send details of the exception to the server
									while( true ) {
											try {
													//class cast exception - report the types in the Vector
													if( details.startsWith( "java.lang.ClassCastException" ) ) {
															details = details + "\nData Unit Classes:";
															//use reflection to figure out whats in the dataunit vector
															for( int i = 0; i < data.size(); i ++ ) {
																	try {
																			Class c = ( (Object) data.get( i ) ).getClass();
																			details = details + "\nElement " + i + " Class: " + c.getName();
																			details = details + "\nContent: " + ( (Object) data.get( i ) ).toString();
																		} catch( Throwable t ) {
																			continue;
																		}
																}
														}

													server.sendAlgorithmError( unitID, algorithmID, details );

													//if a threaddeath exception occurred - restart the client (otherwise the thread may still be running & using up CPU cycles)
													if( details.startsWith( "java.lang.ThreadDeath" ) ) {
															reportException( "ThreadDeath error occurred in Algorithm - resetting client", false );
															return; //restart the client - only way to guarantee thread not still running
														}
													else if( details.startsWith( "java.lang.OutOfMemory" ) ) {
															//reset the client as it ran out of memory on last unit
															reportException( "Client ran out of memory - attempting to reset client", false );
															return;
														}

													setCurrentState( IDLE );
													break; //get out of the inifinite loop
												}
											catch( Exception e ) {
													resetServerConn();
												}
										}
									break; //get out of the infinite state loop and request a new unit from the server
								}
							//algorithm is asking for the problem data to be downloaded from the server
							else if( getCurrentState() == GET_PROBLEM_DATA ) {
									if( atm.isAlive() ) //check if the AlgorithmMonitorThread is still alive
										{
											return; //ONLY sure way to kill this thread is to reset the client
										}

									if( filesRequested.contains( fileName ) ) {
											//already requested this file - problem in algorithm -> report
											reportException( "Client - run - while() - GET_PROBLEM_DATA - duplicate file request sent to server", true );
											break;
										} else {
											filesRequested.add( fileName );
										}

									if( downloadFile( algorithmID, fileName ) ) {
											//now attempt to run the unit again with the data downloaded
											setCurrentState( ALGORITHM_EXECUTING );
											timer = new timerThread( timeLimit.longValue(), this );
											timer.start();

											//create the Algorithm thread monitor
											atm = new AlgorithmMonitorThread( this, data, algorithm, unitID, algorithmID );
											atm.setPriority( Thread.MIN_PRIORITY );
											atm.start();
										} else {
											break; //get out of the inifinite state loop and request a new unit
										}
								}
							//the unit has finished executing - send back results















							else if( getCurrentState() == ALGORITHM_FINISHED ) {
									//get the results and send them back to the server
									Long[] timing = new Long[4];
									timing[ 0 ] = new Long( startTime - finishTime ); //previous transfer time - seconds
									finishTime = System.currentTimeMillis();
									timing[ 1 ] = new Long( finishTime - startTime ); //this units processing time - seconds
									globalTotalProcessingTime += timing[ 1 ].longValue();

									Vector results = atm.getResults();

									//compress the results
									byte[] r = Compressor.compress( results );
									Compressor.reset();

									Vector res = new Vector( 2, 1 );
									res.add( 0, timing);
									res.add( 1, r );

									int retries = 0;
									Vector dynamicInformation = new Vector();
									dynamicInformation.add( 0,new Long( System.currentTimeMillis() - clientStartedTime ) );
									dynamicInformation.add( 1,new Long( globalTotalProcessingTime ) );
									dynamicInformation.add( 2,new Integer( numExtensions ) );
									dynamicInformation.add( 3, new Integer( retries ) );
									while( true ) {
											try {
													long b4ping = System.currentTimeMillis();
													server.ping();
													latency = System.currentTimeMillis() - b4ping;
													dynamicInformation.add(4,new Long(latency));

													server.sendResults( unitID, algorithmID, res, systemInformation, dynamicInformation );
													setCurrentState( IDLE );
													break; //get out of infinite loop
												}
											catch( Exception e ) {
													//wait for a while and retry connection
													resetServerConn();
													reportException( "Client - run - problem sending back results"+ e.toString(), true );
												}
											retries ++;
											dynamicInformation.remove(3 );
											dynamicInformation.add( 3,new Integer( retries ) );
										}

									if( atm.isAlive() ) //check if the AlgorithmMonitorThread is still alive
										{
											return; //ONLY sure way to kill this thread is to reset the client
										}
									break; //get out of state infinite loop
								}
							//the unit has exceeded it allotted time - request an extension


















							else if( getCurrentState() == ALGORITHM_TIMEOUT ) {
									//make sure the unit is still being processed
									if( ! atm.isAlgorithmAlive() ) {
											//report a problem and reset the client
											reportException( "Error: Timeout occurred in UnitID: " + unitID + " AlgorithmID: " + algorithmID + ". Unit not running - resetting client", false );
											break;
										}

									timeRunning += timeLimit.longValue();

									//check how long unit has been running
									if( timeRunning > MAX_PROCESSING_TIME ) {
											//unit has been running for > MAX_PROCESSING_TIME milliseconds- quit it
											try {
													server.sendAlgorithmError( unitID, algorithmID, "Unit running for " + (int) ( MAX_PROCESSING_TIME / 60 ) + " hours - quitting unit" );
												} catch( Exception e ) {}

											if( atm.isAlive() ) //check if the AlgorithmMonitorThread is still alive
												{
													return; //ONLY sure way to kill this thread is to reset the client
												}
										}

									//request an extension from the server





									Long extension = null;
									while( true ) {
											try {
													extension = server.getExtension( unitID, algorithmID );
													break;
												} catch( Exception e ) {
													//wait for a while and retry connection
													resetServerConn();
												}
										}

									if( extension == null || extension.longValue() == 0 ) {
											if( atm.isAlive() ) //check if the AlgorithmMonitorThread is still alive
												{
													return; //ONLY sure way to kill this thread is to reset the client
												}
										}

									//reset the timer with the new time extension

									timer = new timerThread( extension.longValue(), this );
									timer.start();
									setCurrentState( ALGORITHM_EXECUTING );
									numExtensions ++;
								}//end unit timeout state statement
						}//end state infinite while
					//make sure the threads have been stopped





					if( atm.isAlive() ) {
							return; //ONLY sure way to kill this thread is to fully reset the client
						}
					else if( timer._isAlive() ) {
							timer.finished(); //tell the timer that it is finished and not to notify when it wakes up
							timer = null;
						}
					atm = null;
					timer = null;
				}//end overall infinite while
		}//end runClient()

		//method to report client exceptions to server
		//useful when an invalid data set/algorithm is sent down


		private void reportException( String ex, boolean wait ) {
			//contact server and report exception
			while( true ) {
					try {
							server.reportException( ex );

							if( wait ) {
									goAsleep();
								}
							return;
						} catch( Exception e ) {
							resetServerConn();
						}
				}
		}

		//method to make client go to sleep for a while when it cant connect to server
		private void goAsleep() {
			try {
					sleep( SLEEPTIME );
				} catch( Exception e ) {}}

		//check the disk to see if there is a serialised data unit on the disk
		//there would be a unit on disk if the client software was updated and had to restart




		private Vector checkForIncomplete() {
			if( INCOMPLETE_UNIT.isFile() ) {
					try {
							ObjectInputStream oos = new ObjectInputStream( new FileInputStream( INCOMPLETE_UNIT ) );
							Vector dataUnit = (Vector) oos.readObject();
							oos.close();

							//delete the file
							Deleter d = new Deleter( INCOMPLETE_UNIT );
							d.setPriority( Thread.MIN_PRIORITY );
							d.start();

							//check the unit has not been processed somewhere else - ask for an extension
							long extension = ( server.getExtension( (Long) dataUnit.elementAt( 1 ), (Long) dataUnit.elementAt( 2 ) ) ).longValue();

							if( extension > 0 ) {
									//the unit hasnt been processed anywhere else - so start it
									return dataUnit;
								} else {
									return null;
								}
						} catch( Throwable t ) {
							return null;
						}
				}
			return null;
		}

		//reset the connection to the server (i.e. do a new RMI lookup)
		private void resetServerConn() {
			while( true ) {
					try {
							Registry registry = LocateRegistry.getRegistry( SERVERIP, SERVERPORT );
							server = (ClientCommunications) registry.lookup( "server" );
							return; //return when server lookup successful
						}
					catch( Exception e ) {
							//wait a while b4 retrying to contact server
							goAsleep();
						}
				}
		}

		//contact the server and download the file
		private boolean downloadFile( Long algorithmID, String fileName ) {
			//a loop that will go around a number of times (in case the server is already busy servicing file requests (detected by connection closed by peer exception)
			int retries = 0;
			Socket socket = null;
			while( retries < 10 ) {
					try {
							//open socket to server
							socket = new Socket( SERVERIP, SOCKETPORT );

							//request data file
							GZIPOutputStream gzipout = new GZIPOutputStream( socket.getOutputStream() );
							DataOutputStream ds = new DataOutputStream( gzipout );
							ds.writeUTF( "c:" + algorithmID.toString() + ":" + fileName );
							gzipout.finish();
							ds.flush();

							//receive data file
							DataInputStream dis = new DataInputStream( new GZIPInputStream( socket.getInputStream() ) );
							FileOutputStream fos = new FileOutputStream( new File( TEMP_DIRECTORY, fileName ) );
							byte[] buffer = new byte[ socket.getReceiveBufferSize() ];
							int numRead = 1;

							//stream the data file to disk
							while( numRead > 0 ) {
									numRead = dis.read( buffer );
									if( numRead > 0 ) {
											fos.write( buffer, 0, numRead );
										}
								}
							dis.close();
							ds.close();
							fos.flush();
							fos.close();
							socket.close();
							return true;
						} catch( SocketException soe ) {
							if( ! soe.toString().equals( "java.net.SocketException: Connection reset by peer" ) ) {
									ByteArrayOutputStream bos = new ByteArrayOutputStream();
									PrintStream ps = new PrintStream( bos );
									soe.printStackTrace( ps );
									reportException( "AlgorithmID: " + algorithmID.toString() + " Attempt download file: " + fileName + ". Exception: " + bos.toString(), true );
									return false;
								}

							//server busy - increment retries and sleep for a while and retry later
							retries ++;
							goAsleep();
						}
					catch( Exception e ) {
							try {socket.close();} catch( Exception ex ) {}
							ByteArrayOutputStream bos = new ByteArrayOutputStream();
							PrintStream ps = new PrintStream( bos );
							e.printStackTrace( ps );
							reportException( "AlgorithmID: " + algorithmID.toString() + " Attempt download file: " + fileName + ". Exception: " + bos.toString(), true );
							return false;
						}
				}
			return false;
		}

		//method that takes a directory and deletes all the contents
		private void recursiveDelete( File f ) {
			//recusively delete the contents of this directory
			File[] files = f.listFiles();

			//check for an IOexception
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

		//method that resets the algorithm loader and deletes the contents of the classes directory
		private void resetALoader() {
			aLoader = null;
			File[] files = CLASSES_DIR.listFiles();
			if( files != null ) {
					for( int i = 0; i < files.length; i ++ ) {
							if( ! files[ i ].delete() ) {
									int count = 0;
									while( ! files[ i ].delete() && count != 3 ) {
											try { sleep( 3000 ); } catch( Exception e ) {}
											count ++;
										}
								}
						}
				}

			//create a new algorithm class loader passing parent loader in
			URL[] urls = new URL[ 1 ];
			try { urls[ 0 ] = CLASSES_DIR.toURL(); } catch( Exception e ) {}
			aLoader = new AlgorithmLoader( urls );
		}

		//method to clear some space from the temp directories (if needed by client)
		private void clearTempDirectories() {
			//clear the client data directory also
			File f = new File( System.getProperty( "user.dir" ), "temp" );
			recursiveDelete( f );
		}

		//a check performed by the algorithm monitor thread to make sure its unit is the current one being processed
		//just in case multiple algorithm monitor threads exist (failed to kill a currently executing unit)
		public boolean checkCurrentUnit( Long uID, Long aID ) {
			if( uID.longValue() == unitID.longValue() && algorithmID.longValue() == aID.longValue() ) {
					return true;
				} else {
					return false;
				}
		}

		private Class downloadAndLoad( Long algorithmID ) {
			//we dont already have the required algorithm - get it
			Vector algo = null;

			while( true ) {
					try {
							algo = server.getAlgorithm( algorithmID );
							break;
						} catch( Exception e ) {
							//wait and retry connection in a while
							resetServerConn();
						}
				}

			//if the problem doesnt exist any more - reset client
			if( algo == null ) {
					return null;
				}

			//Vector format: 0 - algorithm, 1 - classDefs (Vector), 2 - jarDefs (Vector)
			//now load the algorithms other class definitions
			byte[] algorithmBytes = (byte[]) algo.get( 0 );
			Vector classDefs = (Vector) algo.get( 1 );
			Vector jarDefs = (Vector) algo.get( 2 );
			byte[] hash = (byte[]) algo.get( 3 );

			//get the jar files and add them to the classpath
			if( jarDefs.size() > 0 ) {
					int status = aLoader.loadJarFiles( CLASSES_DIR, jarDefs );

					if( status == 0 ) //there was an exception
						{
							Throwable t = aLoader.getException();
							if( t != null ) {
									if( t.toString().startsWith( "java.io.IOException: There is not enough space on the disk" ) ) {
											clearTempDirectories();
										}

									//report the exception to the server
									//get some detailed info from the exception - stack trace
									ByteArrayOutputStream bos = new ByteArrayOutputStream();
									PrintStream ps = new PrintStream( bos );
									t.printStackTrace( ps );

									try { server.sendAlgorithmError( unitID, algorithmID, "Failed to load Jar files: " + bos.toString() ); }        catch( Exception e ) {}
									return null;
								}
						} else if( status == -1 ) //a jar file is a different version of another library - reset the loader and then load the jar files
						{
							resetALoader(); //reset completely the algorithm loader
							status = aLoader.loadJarFiles( CLASSES_DIR, jarDefs );

							if( status == -1 ) {
									try { server.sendAlgorithmError( unitID, algorithmID, "Failed to load Jar files again" ); } catch( Exception e ) {}
									return null;
								} else if( status == 0 ) {
									//report the exception to the server
									//get some detailed info from the exception - stack trace
									ByteArrayOutputStream bos = new ByteArrayOutputStream();
									PrintStream ps = new PrintStream( bos );
									aLoader.getException().printStackTrace( ps );

									//exception in DM - remove problem
									try { server.sendAlgorithmError( unitID, algorithmID, "Failed to load Jar files: " + bos.toString() ); }        catch( Exception e ) {}
									return null;
								}
						}
				}

			//write the class files to the classes directory
			if( classDefs.size() > 0 ) {
					//make sure the directory exists
					if( ! CLASSES_DIR.isDirectory() ) {
							CLASSES_DIR.mkdir();
						}

					try {
							int status = 1;
							for( int i = 0; i < classDefs.size(); i = i + 2 ) {
									//get the bytes and write to temp dir
									String name = (String) classDefs.get( i );
									byte[] bytes = (byte[]) classDefs.get( i + 1 );
									File classF = new File( CLASSES_DIR, name );

									if( classF.exists() && classF.length() != bytes.length ) {
											//the current class definition is a different version of a current definition - must reset the loader
											resetALoader();

											//must reload the JAR files first
											status = aLoader.loadJarFiles( CLASSES_DIR, jarDefs );

											if( status == -1 ) {
													return null;
												} else if( status == 0 ) {
													//report the exception to the server
													//get some detailed info from the exception - stack trace
													ByteArrayOutputStream bos = new ByteArrayOutputStream();
													PrintStream ps = new PrintStream( bos );
													aLoader.getException().printStackTrace( ps );

													//exception in DM - remove problem
													return null;
												}
											i = -2; //restart the loop - re-write all the class files
										}
									else {
											FileOutputStream fos = new FileOutputStream( classF );
											fos.write( bytes );
											fos.close();
										}
								}
						} catch( Throwable t1 ) {
							if( t1.toString().startsWith( "java.io.IOException: There is not enough space on the disk" ) ) {
									clearTempDirectories();
								}

							//get some detailed info from the exception - stack trace
							ByteArrayOutputStream bos = new ByteArrayOutputStream();
							PrintStream ps = new PrintStream( bos );
							t1.printStackTrace( ps );
							try { server.sendAlgorithmError( unitID, algorithmID, "Failed to write class files: " + bos.toString() ); } catch( Exception e ) {}
							return null;
						}
				}

			//set to null
			classDefs = null;
			jarDefs = null;

			//now attempt to load the algorithm
			return aLoader.loadNewAlgorithm( algorithmID, algorithmBytes, hash );
		}
	}
