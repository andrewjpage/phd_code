/*
Thomas Keane, 8:54 PM 1/28/03
scheduler for server - handles multiple algorithms with corresponding priorities
Scheduling algorithm:
 
Copyright (C) 2003  Thomas Keane
 
	Updated by Andrew Page June 2005
	- New scheduling interface
 
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
import java.util.logging.*;
import java.rmi.RemoteException;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.io.*;
import java.lang.reflect.Field;
import java.util.Random;
import java.util.zip.*;
import java.util.jar.*;
import java.security.MessageDigest;
import java.net.*;

/**scheduler for server - handles multiple algorithms with corresponding priorities
 * Scheduling algorithm:
* @author Thomas Keane - updated by Andrew Page
*/

public class Scheduler implements GUICommunications {
		private Hashtable problems; //holds the current set of Problems
		private boolean pauseServer; //set to true when want server to pause and stop issuing units

		private Server server; //pointer to server
		private int clientVersion; //version ID of current client
		private String adminPass; //admin password for system

		//system wide logs
		private Logger errorLog;
		private Logger systemLog;

		//variables related to the optimal unit time and tolerance around it
		private int optimalUnitTime;
		private float optimalTolerance;

		private int epochCounter; //counts units up till reach epochlimit

		private String loadFailReason; //stores the reason why a Problem failed to load

		private Compressor compressor; //a compressor for compressing information before sending it to the remote interface

		private NewSchedulingAlgorithm schedulingAlgorithm; //the scheduling algorithm in use

		private byte[] dataManagerParentBytes; //the bytes of the DataManager parent class
		private byte[] algorithmParentBytes;

		private Vector finishedJobsInfo;


		private Hashtable clients; // hold info about each client
		private int batchSize = 20; // make this dynamic
		private int numScheds = 0;
		private int scheduleCount = 0;

		private double newBatchThreshold = 3.0; // if the ratio of  tasks to clients is below this value then schedule a new batch of tasks

		private boolean DEBUG = true; // controls debug mode
		private long totalSchedTime = 0; // total amount of time spent scheduling in milliseconds ms
		private int threshold = 1;
		private int unitcount = 0;
		private long clientTimeout = 3000000;


		//only constructor for scheduler
		public Scheduler( Server ser, Logger sys, Logger err, String admin, int optimal, float tolerance, byte[] dataMBytes, byte[] algorithmBytes ) {
			server = ser;
			systemLog = sys;
			errorLog = err;
			adminPass = admin;
			optimalUnitTime = optimal;
			optimalTolerance = tolerance;
			dataManagerParentBytes = dataMBytes;
			algorithmParentBytes = algorithmBytes;

			problems = new Hashtable();
			pauseServer = false; //server is initially not paused
			epochCounter = 0;
			loadFailReason = null;

			clients = new Hashtable();

			System.out.println("  scheduler " + server.schedName);
			if(server.schedName.equals( "PN")) {
					schedulingAlgorithm = new PNScheduler();
					System.out.println("  scheduler PN");
				} else if(server.schedName .equals( "ZO")) {
					schedulingAlgorithm = new ZOScheduler();
					System.out.println("  scheduler zo");
				} else if(server.schedName.equals( "TA")) {
					schedulingAlgorithm = new TAScheduler();
					System.out.println("  scheduler ta");
				} else if(server.schedName.equals(  "SA")) {
					schedulingAlgorithm = new SAScheduler();
					System.out.println("  scheduler sa");
				} else if(server.schedName.equals(  "DL")) {
					schedulingAlgorithm = new DLScheduler();
					System.out.println("  scheduler dl");
				} else if(server.schedName.equals(  "MX")) {
					schedulingAlgorithm = new MXScheduler();
					System.out.println("  scheduler mx");
				} else if(server.schedName.equals(  "RC")) {
					schedulingAlgorithm = new RCScheduler();
					System.out.println("  scheduler RC");
				} else if(server.schedName.equals( "MM")) {
					schedulingAlgorithm = new MMScheduler();
					System.out.println("  scheduler mm");
				} else if(server.schedName.equals(  "LL")) {
					schedulingAlgorithm = new LLScheduler();
					System.out.println("  scheduler ll");
				} else if(server.schedName.equals( "EF")) {
					schedulingAlgorithm = new EFScheduler();
					System.out.println("  scheduler ef");
				} else if(server.schedName.equals(  "FA")) {
					schedulingAlgorithm = new FAScheduler();
					System.out.println("  scheduler FA");
				} else if(server.schedName.equals(  "FM")) {
					schedulingAlgorithm = new FMScheduler();
					System.out.println("  scheduler FM");
				} else if(server.schedName.equals("FX")) {
					schedulingAlgorithm = new FXScheduler();
					System.out.println("  scheduler FX");
				} else if(server.schedName.equals("FE")) {
					schedulingAlgorithm = new FEScheduler();
					System.out.println("  scheduler FE");
				} else if(server.schedName.equals("FZ")) {
					schedulingAlgorithm = new FZScheduler();
					System.out.println("  scheduler FZ");
				} else if(server.schedName.equals("LA")) {
					schedulingAlgorithm = new LAScheduler();
					System.out.println("  scheduler LA");
				} else if(server.schedName.equals("LE")) {
					schedulingAlgorithm = new LEScheduler();
					System.out.println("  scheduler LE");
				} else if(server.schedName.equals("LZ")) {
					schedulingAlgorithm = new LZScheduler();
					System.out.println("  scheduler LZ");
				} else if(server.schedName.equals("LM")) {
					schedulingAlgorithm = new LMScheduler();
					System.out.println("  scheduler LM");
				} else if(server.schedName.equals("LX")) {
					schedulingAlgorithm = new LXScheduler();
					System.out.println("  scheduler LX");
				} else if(server.schedName.equals("SB")) {
					schedulingAlgorithm = new SBScheduler();
					System.out.println("  scheduler SB");
				} else if(server.schedName.equals("SE")) {
					schedulingAlgorithm = new SEScheduler();
					System.out.println("  scheduler SE");
				} else if(server.schedName.equals("SZ")) {
					schedulingAlgorithm = new SZScheduler();
					System.out.println("  scheduler SZ");
				} else if(server.schedName.equals("SM")) {
					schedulingAlgorithm = new SMScheduler();
					System.out.println("  scheduler SM");
				} else if(server.schedName.equals("SX")) {
					schedulingAlgorithm = new SXScheduler();
					System.out.println("  scheduler SX");
				} else if(server.schedName.equals("GA")) {
					schedulingAlgorithm = new GAScheduler();
					System.out.println("  scheduler GA");
				} else if(server.schedName.equals("GE")) {
					schedulingAlgorithm = new GEScheduler();
					System.out.println("  scheduler GE");
				} else if(server.schedName.equals("GZ")) {
					schedulingAlgorithm = new GZScheduler();
					System.out.println("  scheduler GZ");
				} else if(server.schedName.equals("GM")) {
					schedulingAlgorithm = new GMScheduler();
					System.out.println("  scheduler GM");
				} else if(server.schedName.equals("GX")) {
					schedulingAlgorithm = new GXScheduler();
					System.out.println("  scheduler GX");
				} else if(server.schedName.equals("NE")) {
					schedulingAlgorithm = new NEScheduler();
					System.out.println("  scheduler NE");

				} else {
					schedulingAlgorithm = new RRScheduler();
					System.out.println("  scheduler rr");
				}



			compressor = new Compressor( ClassLoader.getSystemClassLoader() );

			finishedJobsInfo = new Vector();
			updateFinishedJobsVector();

			// remove
			hardcodeProbs() ;
		}

		//client has requested a data unit - issue one
		//scheduling algorithm coded here
		public Vector generateDataUnit( int clientCPU, int clientMemory , Vector clientDetails ) {
			if(DEBUG)
				systemLog.info( "Received Request for new Data Unit - generating unit" );



			if( problems.size() == 0 || pauseServer ) {
					systemLog.info( "No Problems in system or sytem paused - issuing null unit" );
					return null;
				}

			String clientIP = (String) clientDetails.get(0);

			// AP - update the client details

			int megaflops = ((Integer) clientDetails.get(1)).intValue();
			int  mem =( (Integer) clientDetails.get(2)).intValue();
			boolean newClient = false;
			try {
					// error here. First unit ok. its the second unit that messes up coz a schedules not ready?
					long up =  ( (Long) clientDetails.get(3)).longValue();
					long proctime = ( (Long) clientDetails.get(4)).longValue();
					long latency = ( (Long) clientDetails.get(5)).longValue();




					if(clients.containsKey(clientIP)) {

							((clientInfo) clients.get(clientIP)).update(megaflops,  mem, up,proctime, latency) ;

							if(DEBUG)
								systemLog.info( "Client details updated: IP: " + clientIP+" megaflops: "+megaflops+" memory: "+mem+ " uptime: "+ up+ " totalprocessingtime: " + proctime + " latency: "+ latency);

						} else {

							clientInfo ci = new clientInfo(   clientIP,    megaflops,mem , up, proctime,latency);
							clients.put(clientIP, ci);

							if(DEBUG)
								systemLog.info( "Client details added for the first time:  IP: " + clientIP+" megaflops: "+megaflops+" memory: "+mem+ " uptime: "+ up+ " totalprocessingtime: " + proctime + " latency: "+ latency);

							// since a new client has been added we want to spread out the already assigned tasks to this new processor.

							newClient = true;

						}
					// want this to happen in a thread

					if(!schedulingAlgorithm.isInitialised()) {

							schedulingAlgorithm.initialise(clients, problems);

							Vector newbatch = fillBatch(batchSize);
							int schedSize = newbatch.size();
							long b4 = System.currentTimeMillis() ;
							schedulingAlgorithm.generateSchedule(newbatch);
							long after = System.currentTimeMillis() ;
							totalSchedTime += (after-b4);
							System.out.println("schedtime "+  totalSchedTime + " "+  (after-b4) + " "+ schedSize);


							scheduleCount++;
						}


				} catch(Exception e) {
					errorLog.warning("Error generating dataunit when adding client -> "+ e);
					e.printStackTrace();
				}
			((clientInfo) clients.get(clientIP)).setEstFinishingTime(0);
			((clientInfo) clients.get(clientIP)).setLastTime();


			//no Problems in the system to issue data units or server paused
			if( problems.size() == 0 || pauseServer ) {
					systemLog.info( "No Problems in system or sytem paused - issuing null unit" );
					return null;
				} else {

					if(unitcount%10 ==5)
						reSchedule(30);
					else if(unitcount%20 ==19)
						reSchedule(10);
					else if(unitcount%40==39)
						reSchedule(3);



					if(server.schedName.equals( "PN")) {
							if(unitcount%15 ==7) {
									reSchedule(3);
								} else	if(unitcount%5 ==4) {
									reSchedule(6);
								}
						}



					long[] nextID = schedulingAlgorithm.nextUnitProblemID(clientIP);
					// want this to happen in a separate thread
					if(newClient) {
							nextID = new long[1];
							nextID[0] = randtask().longValue();
						} else if (nextID.length == 0) {
							if(DEBUG)
								systemLog.info( "not enough units scheduled for " + clientIP+ " - generating schedule");
							scheduleTasks();
							nextID = schedulingAlgorithm.nextUnitProblemID(clientIP);
						}
					unitcount++;



					for( int i = 0; i < nextID.length; i ++ ) {
							Problem p = (Problem) problems.get( new Long( nextID[ i ] ) );
							if( p != null ) {
									int ProblemCPU = p.getCPU();
									int ProblemMemory = p.getMemory();

									if( clientCPU >= ProblemCPU && clientMemory >= ProblemMemory ) {
											Vector unit = null;
											try {
													//then hand out a unit from this Problem to the client
													unit = p.getDataUnit(  (clientInfo)(clients.get(clientIP))   );
												} catch( Throwable e ) {
													//check if the errror was due to the server being out of memory
													String err = e.toString();

													//get some detailed info from the exception - stack trace
													ByteArrayOutputStream bos = new ByteArrayOutputStream();
													PrintStream ps = new PrintStream( bos );
													e.printStackTrace( ps );
													if( err.endsWith( "OutOfMemoryError" ) ) {
															errorLog.warning( "Cannot issue new unit - Low Memory. Removing Problem. Consider increasing available memory to server using -Xmx" );
														}

													//exception in DM - remove Problem
													p.logDataManagerError( bos.toString(), "getDataUnit" );
													exceptionKillJob( p.getID() );

													continue; //go to next Problem on the list to get a unit
												}

											/*if got a valid unit - then return it (including the unit requirements)*/
											if( unit != null ) {
													long estFin = 0;
													// Inform the clientInfo class for this client that you have assigned this task
													if((clients.get(clientIP)) == null) {
															//no client data
														}
													else if( ((clientInfo)(clients.get(clientIP))).getMflops()<= 0.0)
														((clientInfo) clients.get(clientIP)).setEstFinishingTime( 0);
													else {
															estFin = (long) (p.getAvgMflops()/((clientInfo)clients.get(clientIP)).getMflops()) ;
															((clientInfo)clients.get(clientIP)).setEstFinishingTime(estFin)  ;

														}
													Long knnEstTaskTime =(Long) (((clientInfo)clients.get(clientIP)).getEstimatedTime(p.getID(),1.0)).get(0);
													Integer numObs =(Integer) (((clientInfo)clients.get(clientIP)).getEstimatedTime(p.getID(),1.0)).get(2);


													double CCR = 1;
													Vector estTime = ((clientInfo)clients.get(clientIP)).getEstimatedTime( p.getID(), 0.0);
													double extime = (((Long)estTime.get(0)).longValue()/1000);
													double commsTime =  (((Long)estTime.get(1)).longValue()/1000);

													if(extime ==0.0) {
															if(((clientInfo)clients.get(clientIP)).getMflops() <=0)
																extime = 0;
															else
																extime=p.getAvgMflops()/  ((clientInfo)clients.get(clientIP)).getMflops();
														}

													if(commsTime == 0)
														CCR = 1;
													else
														CCR = extime/commsTime;

													p.storeProcessingEst(((Long)unit.get(0)).longValue(), estFin,((clientInfo)clients.get(clientIP)).getMflops(), knnEstTaskTime.longValue(),numObs,CCR);
													((clientInfo)clients.get(clientIP)).scheduleTask(p.getID());
													((clientInfo)clients.get(clientIP)).setLastTime();

													unit.add( new Integer( ProblemCPU ) );
													unit.add( new Integer( ProblemMemory ) );
													return unit;
												}
										}
								} else {
									errorLog.warning( "Scheduler reported problem ID that doesnt exist in server " +  nextID[i]);
								}
						}

					//no matching Problem with data units available
					systemLog.info( "No matching Problems currently in system" );
					return null;
				}
		}

		//client returned results set - send to relevant Problem
		public boolean handleResults( Long unitID, Long problemID, byte[] results, Long[] timing, Vector clientDetails , Vector dynamicDetails, String clientIP) {
			String match = "";// debug info for experiements - 1 processor mflops, 2 ,3 processor latency,
			systemLog.info( "Handling results for Problem " + problemID.toString() + " Unit " + unitID.toString() );
			if(DEBUG)
				match = "match ";
			Problem p = (Problem) problems.get( problemID );

			try {

					// AP add the stats of the returned problem to the client info datastore
					int megaflops = ((Integer) clientDetails.get(0)).intValue();
					int  mem =( (Integer) clientDetails.get(1)).intValue();

					long up =  ( (Long)dynamicDetails.get(0)).longValue();
					long proctime = ( (Long) dynamicDetails.get(1)).longValue();
					int numfailed = ( (Integer) dynamicDetails.get(3)).intValue();
					long latency = ( (Long) dynamicDetails.get(4)).longValue();

					if(DEBUG) {
							double m = p.getAvgMflops();
							match += ""+ System.currentTimeMillis() + " "+ megaflops + " " + m+  " "+ (timing[1].longValue()) + " "+  (timing[0].longValue()) + " "+latency + ";\n";
						}

					if(clients.containsKey(clientIP)) {

							((clientInfo) clients.get(clientIP)).updateInfo(megaflops,  mem, numfailed ,up,proctime,latency) ;

						} else {
							// new client so add to the hashtable

							clientInfo ci = new clientInfo(   clientIP,    megaflops,mem , up, proctime,latency);
							clients.put(clientIP, ci);
							((clientInfo) clients.get(clientIP)).updateInfo(megaflops,  mem, numfailed ,up,proctime,latency) ;
						}

				} catch( Exception e ) {
					errorLog.warning( "Error handling results -> client info update in handleResults error -> "+ e );

					e.printStackTrace();
				}


			if( p != null ) {
					if(DEBUG)
						systemLog.info( "Found Problem for results" );

					// add the task processing time for this client to a hashtable for use with the KNN which estimates the unit processing time
					// 1.0 is the size of the unit. this needs to be made dynamic at a later point
					((clientInfo) clients.get(clientIP)).addTask(problemID, timing[1].longValue(), timing[0].longValue(), 1.0);

					boolean b = false;

					try {
							b = p.receiveResults( unitID, results, timing ,         ((clientInfo) clients.get(clientIP)).getMflops());
						} catch( CompressorException ce ) {
							return false; //an exception occurred in the compressor - tell the Server the unit isnt complete
						}
					catch( Throwable e ) {
							//get some detailed info from the exception - stack trace
							ByteArrayOutputStream bos = new ByteArrayOutputStream();
							PrintStream ps = new PrintStream(bos);
							e.printStackTrace( ps );

							//exception in DM - remove Problem
							p.logDataManagerError( bos.toString(), "receive results" );
							exceptionKillJob( p.getID() );

							return true;
						}

					//if the job reported it is finished - then remove it
					if( b ) {
							systemLog.info( "Got Problem finished signal from Problem: " + problemID.toString() );

							//remove the Problem from the system
							server.removeAlgorithm( problemID );
							removeProblem( problemID ); //remove + zip directory from server

							schedulingAlgorithm.removeProblem(problemID.longValue());
							reSchedule(threshold) ;
						}

					if(DEBUG)
						System.out.println(match);



					return true;
				}

			//reached here -> Problem not found
			errorLog.warning( "Unlisted Result set Received: Unit ID: " + unitID.toString() + " Problem ID: " + problemID.toString() );
			return true;
		}

		//log an error that occurred in a particular Problem
		//only one access at any time
		public synchronized void logAlgorithmError( Long unitID, Long ProblemID, String err ) {
			//send to Problem error log
			Problem p = (Problem) problems.get( ProblemID );

			if( p != null ) {
					p.logAlgorithmError( unitID, err );
					return;
				}
		}

		//method to get the number of expired units allowed for a particular Problem
		//only one access at a time
		public synchronized int getExpired( Long ProblemID ) {
			//find the Problem in system
			int allowedExpired = -1;
			Problem p = (Problem) problems.get( ProblemID );
			if( p != null ) {
					allowedExpired = p.getExceptions();
				}
			return allowedExpired;
		}

		//method to get the max number of exceptions allowed for particular Problem
		//only one access at any time
		public synchronized int getExceptions( Long ProblemID ) {
			//find the Problem in system
			int allowedExceptions = -1;
			Problem p = (Problem) problems.get( ProblemID );
			if( p != null ) {
					allowedExceptions = p.getExceptions();
				}
			return allowedExceptions;
		}

		//method to verify that all the directories in the problems directory correspond to valid problems
		//delete any partially uploaded problems etc.
		public synchronized void verifyProblemsDirectory() {
			//get a listing of the directory and make sure all problems are valid
			File[] problemDirContents = ( new File( System.getProperty( "user.dir" ), "problems" ) ).listFiles();

			for( int i = 0; i < problemDirContents.length; i ++ ) {
					try {
							//3 conditions before deleting:
							//1 - it is a problem directory
							//2 - it isnt currently being accessed by either the zipper or deleter
							//3 - it isnt a problem still running in the system
							if( problemDirContents[ i ].isDirectory() && ! DirectoryOperator.isCurrentlyAccessed( problemDirContents[ i ].toString() ) && problems.get( new Long( problemDirContents[ i ].toString() ) ) == null ) {
									//delete the directory
									Deleter d = new Deleter( problemDirContents[ i ] );
									d.setPriority( Thread.MIN_PRIORITY );
									d.start();
								}
						} catch( Exception e ) {
							//e.printStackTrace();
						}
				}
		}

		//***********end Server methods*********

		//***********GUI methods***************
		/*method to add a new Problem to the system*/

		public synchronized boolean addProblem( Long probID, byte[] algorithm, byte[] datamanager, int priority, String description, int expired, int exceptions, Vector classDefinitions, Vector jarDefinitions, int ProblemCPU, int ProblemMemory ) throws RemoteException {
				systemLog.info( "Adding new Problem to system. Description: " + description );
				File ProblemsD = new File( System.getProperty( "user.dir" ), "problems" );
				File dataLocation = new File( ProblemsD.getAbsolutePath(), probID.toString() );

				//make the directory - case where there is no Problem data
				if( ! dataLocation.isDirectory() ) {
						dataLocation.mkdir();
					}

				//put the jar files into a seperate directory that the Problem classloader can see
				File classesDir = new File( dataLocation, "classes" );
				classesDir.mkdir();

				try {
						//write the parent algorithm and datamanager classes to the 'classes' directory
						File dataMParent = new File( classesDir, "DataManager.class" );
						FileOutputStream fos = new FileOutputStream( dataMParent );
						fos.write( dataManagerParentBytes );
						fos.close();

						fos = new FileOutputStream( new File( classesDir, "Algorithm.class" ) );
						fos.write( algorithmParentBytes );
						fos.close();
					} catch( IOException ioe ) {
						errorLog.warning( "Failed to write Algorithm and DataManager parent classes: " + ioe );

						return false;
					}

				URL[] urls = new URL[ (jarDefinitions.size() / 2) + 1 ]; //name, file, name, file, name, file...
				try { urls[ 0 ] = classesDir.toURL(); } catch( Exception e ) {}

				FileOutputStream fos = null;
				//put the Problem jar files into the Problem directory and include them in the classloader classpath list of urls
				if( jarDefinitions.size() > 0 ) {
						try {
								for( int i = 0; i < jarDefinitions.size(); i = i + 2 ) {
										String fileName = (String) jarDefinitions.get( i );
										byte[] bytes = (byte[]) jarDefinitions.get( i + 1 );
										File jarFile = new File( classesDir, fileName );
										jarFile.createNewFile();
										fos = new FileOutputStream( jarFile );
										fos.write( bytes );
										fos.close();
										fos = null;
										urls[ (i + 2) / 2 ] = jarFile.toURL();
									}
							} catch( Throwable t ) {
								errorLog.warning( "Failed to load Jar files. ID " + probID.toString() + " Exception: " + t );
								recursiveDelete( dataLocation );
								dataLocation.delete();
								loadFailReason = "Failed to load Jar files. ID " + probID.toString() + " Exception: " + t;
								return false;
							}
					}

				//put the Problem class files into the Problem classes directory (included in the Problem classloader classpath)
				if( classDefinitions.size() > 0 ) {
						try {
								for( int i = 0; i < classDefinitions.size(); i = i + 2 ) {
										String fileName = (String) classDefinitions.get( i );
										byte[] bytes = (byte[]) classDefinitions.get( i + 1 );
										File classFile = new File( classesDir, fileName );
										classFile.createNewFile();
										fos = new FileOutputStream( classFile );
										fos.write( bytes );
										fos.close();
										fos = null;
									}
							} catch( Throwable t ) {
								errorLog.warning( "Failed to load class files. ID " + probID.toString() + " Exception: " + t );
								recursiveDelete( dataLocation );
								dataLocation.delete();
								loadFailReason = "Failed to load class files. ID " + probID.toString() + " Exception: " + t;
								return false;
							}
					}

				//create the Problem object
				StringTokenizer stk = new StringTokenizer( description, "$%^&" );
				Problem p = new Problem( this, probID, stk.nextToken(), priority, systemLog, errorLog, expired, exceptions, optimalUnitTime, optimalTolerance, ProblemCPU, ProblemMemory, urls, dataLocation, stk.nextToken() );
				if(DEBUG)
					systemLog.info( "Problem Object created" );

				//load and check the Problem algorithm and datamanager
				String loadingProblem = p.loadProblem( algorithm, datamanager );
				if( loadingProblem != null ) {
						errorLog.severe( "Could not load Problem files into system, Problem description: " + description + " ID: " + p.getID().toString() + " Error: " + loadingProblem );
						recursiveDelete( dataLocation );
						dataLocation.delete();
						loadFailReason = loadingProblem;
						return false; //return some information to the user as to why their Problem failed to load
					}

				/*set up the loggers for the Problem*/
				Logger problemLog = Logger.getAnonymousLogger();
				Logger errorL = Logger.getAnonymousLogger();
				Logger timingL = Logger.getAnonymousLogger();

				FileHandler phandler = null;
				FileHandler ehandler = null;
				FileHandler thandler = null;
				File logsDir = new File( dataLocation, "logs" );
				logsDir.mkdir();
				try {
						phandler = new FileHandler( logsDir.getAbsolutePath() + "/problem%g.log", 10000000, 5, false );
						ehandler = new FileHandler( logsDir.getAbsolutePath() + "/error%g.log", 10000000, 5, false );
						thandler = new FileHandler( logsDir.getAbsolutePath() + "/timing%g.log", 10000000, 5, false );
					} catch( IOException e ) {
						errorLog.severe( "Couldnt create Problem log file: " + e );
						recursiveDelete( dataLocation );
						dataLocation.delete();
						loadFailReason = "Couldnt create Problem log file: " + e;
						return false;
					}

				//formatter for the logger
				SimpleFormatter simple = new SimpleFormatter();

				phandler.setFormatter( simple );
				problemLog.setUseParentHandlers( false );
				problemLog.addHandler( phandler );

				errorL.setUseParentHandlers( false );
				ehandler.setFormatter( simple );
				errorL.addHandler( ehandler );

				timingL.setUseParentHandlers( false );
				thandler.setFormatter( new emptyFormatter() );
				timingL.addHandler( thandler );

				//init the datamanager and set up the logs
				try {
						p.setVariables( problemLog, errorL, dataLocation, timingL );
					} catch( Throwable e ) {
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						PrintStream ps = new PrintStream( bos );
						e.printStackTrace( ps );
						errorLog.info( "Error initing DataManager: " + bos.toString() );

						//close the log files
						Handler[] handlers1 = problemLog.getHandlers();
						Handler[] handlers = errorL.getHandlers();
						Handler[] handlers2 = timingL.getHandlers();
						for( int i = 0; i < handlers.length; i ++ ) {
								problemLog.removeHandler( handlers1[ i ] );
								errorL.removeHandler( handlers[ i ] );
								timingL.removeHandler( handlers2[ i ] );
								handlers1[ i ].close();
								handlers[ i ].close();
								handlers2[ i ].close();
							}
						recursiveDelete( dataLocation );
						dataLocation.delete();
						throw new RemoteException( "Error in init() method of DataManager: " + e );
					}

				//add the Problem to the system
				problems.put( probID, p );
				server.addAlgorithm( p.getID(), algorithm, classDefinitions, jarDefinitions );
				//      schedulingAlgorithm.addProblem( probID, priority );

				if(DEBUG)
					systemLog.info( "Success!! Problem fully added to system" );

				return true;
			}

		/** The state of the system has changed so you want to reschedule the tasks already scheduled.*/
		public void reSchedule(int threshold) {
			if( problems.size() == 0)
				return;

			// want this to happen in a thread
			numScheds++;
			if(numScheds >1) {
					numScheds=1;
					return;
				}

			if(!schedulingAlgorithm.isInitialised()) {

					schedulingAlgorithm.initialise(clients, problems);

					Vector newbatch = fillBatch(batchSize);
					int schedSize = newbatch.size();
					long b4 = System.currentTimeMillis() ;
					schedulingAlgorithm.generateSchedule(newbatch);
					long after = System.currentTimeMillis() ;
					totalSchedTime += (after-b4);
					System.out.println("schedtime "+  totalSchedTime + " "+  (after-b4) + " " + schedSize);
					scheduleCount++;
				}


			Vector mt = schedulingAlgorithm. removeSelectedAssignedTasks(threshold);
			double ratio = ((Double)mt.remove(0)).doubleValue();

			//if(ratio < newBatchThreshold) {
			//		Vector newbatch = fillBatch(batchSize);
			//		mt.addAll(newbatch);
			//		if(DEBUG)
			//			systemLog.info( "Ratio of tasks to clients is " + ratio + " which is less than threshold value "+   newBatchThreshold + " so filling new batch");
			//	}
			long b4 = System.currentTimeMillis() ;
			int schedSize = mt.size();

			do {
					// break up large numbers of tasks into batches
					Vector window = new Vector();
					for(int i = 0; (i< batchSize*3 && mt.size()>0); i++) {
							window.add(mt.remove(0));
						}
					schedulingAlgorithm.generateSchedule(window);
				} while(mt.size() >0);

			scheduleCount++;
			long after = System.currentTimeMillis() ;
			totalSchedTime += (after-b4);
			System.out.println("reschdule " + threshold);
			if(schedSize >0) {
					System.out.println("schedtime "+  totalSchedTime + " "+  (after-b4) + " "+ schedSize);

					schedulingAlgorithm.printLoadTime();
				}
			/** print out the current assigned load for testing purposes*/
			/*
			if(DEBUG) {
					String loadRow = "load " + System.currentTimeMillis() + " " ;
					Hashtable load = schedulingAlgorithm.currentAssignedLoad();
					for (Enumeration e = load.elements() ; e.hasMoreElements() ;) {
							double clientLoad = ((Double)  e.nextElement()).doubleValue();
							loadRow += " "+ clientLoad ;
						}
					loadRow += " ;";
					System.out.println(loadRow);

				}
				*/

			numScheds--;
		}


		/** The state of the system has changed so you want to reschedule the tasks already scheduled.*/
		public void scheduleTasks() {
			// want this to happen in a thread
			numScheds++;
			if(numScheds >1) {
					numScheds=1;
					return;
				}

			if(!schedulingAlgorithm.isInitialised()) {

					schedulingAlgorithm.initialise(clients, problems);
					Vector newbatch = fillBatch(batchSize);
					int schedSize = newbatch.size();
					long b4 =  System.currentTimeMillis() ;
					schedulingAlgorithm.generateSchedule(newbatch);
					long after = System.currentTimeMillis() ;
					totalSchedTime += (after-b4);
					System.out.println("schedtime "+  totalSchedTime + " "+  (after-b4) + " "+  schedSize);

					scheduleCount ++;

				}



			Vector newbatch = fillBatch(batchSize);
			newbatch.addAll(timeoutClients(clientTimeout));
			int schedSize = newbatch.size();
			long b4 = System.currentTimeMillis() ;
			schedulingAlgorithm.generateSchedule(newbatch);
			long after = System.currentTimeMillis() ;
			totalSchedTime += (after-b4);


			if(schedSize> 0) {
					System.out.println("schedtime "+  totalSchedTime + " "+  (after-b4) + " "+ schedSize);
					schedulingAlgorithm.printLoadTime();
				}

			scheduleCount ++;
			numScheds--;


		}

		//method to report the reason why a Problem failed to be loaded into the system properly
		public String addProblemFailReason() throws RemoteException {
				return loadFailReason;
			}

		//method to delete a Problems files from the disk
		public boolean deleteAllPastProblems() throws RemoteException {
				systemLog.info( "Entering Delete ALL" );
				//get a list of the directories left in system directory
				File cwd = new File( System.getProperty( "user.dir" ), "problems" );
				if( cwd.isDirectory() ) {
						File[] names = cwd.listFiles();
						if( names != null ) {
								for( int i = 0; i < names.length; i ++ ) {
										//all old result sets, killed Problems and Problems with exceptions
										if( names[ i ].getName().endsWith( ".zip" ) ) {
												names[ i ].delete();
											}
									}
							}
					}
				if(DEBUG)
					systemLog.info( "Problems Directory cleared out" );
				return true;
			}

		//method to check if a Problem is finished - all results received
		public boolean problemFinished( Long ID ) throws RemoteException {
				if(DEBUG)
					systemLog.info( "Checking for Problem finished: " + ID.toString() );
				String name = "R" + ID.toString() + ".zip";
				File f = new File( System.getProperty( "user.dir" ), "problems" );
				File resName = new File( f, name );

				if( resName.isFile() ) {
						return true;
					}

				return false;
			}

		//method to get the status of all of the Problems in the system
		public synchronized byte[] getAllProblemStatus() throws RemoteException {
				if(DEBUG)
					systemLog.info( "Getting status of all Problems" );
				Vector status = new Vector();
				Enumeration e = problems.elements();
				while( e.hasMoreElements() ) {
						Problem p = (Problem) e.nextElement();
						Vector v = p.getStatus();
						status.add( v );
					}

				if( problems.size() == 0 ) {
						Vector v = new Vector();
						v.add( "No jobs currently running in system" );
						status.add( v );
					}

				//add information on the finished/failed jobs
				status.add( finishedJobsInfo );

				//compress the information
				byte[] info = compressor.compress( status );

				return info;
			}

		//get status information on a particular job in system
		public synchronized Vector getProblemStatus( Long ID ) throws RemoteException {
				if(DEBUG)
					systemLog.info( "Checking for status of Problem: " + ID.toString() );
				Problem p = (Problem) problems.get( ID );
				if( p != null ) {
						return p.getStatus();
					}
				return null;
			}

		//change the priority of a particular Problem
		public synchronized boolean changePriority( Long ID, int priority ) throws RemoteException {
				if(DEBUG)
					systemLog.info( "Changing the priority of Problem: " + ID.toString() );
				//find the Problem and change its priority
				Problem p = (Problem) problems.get( ID );

				if( p != null ) {
						p.setPriority( priority );
						//              schedulingAlgorithm.updatePriority( ID, priority );
						return true;
					}
				return false;
			}

		//method to update the current version of the client - will be send to clients
		public synchronized boolean updateClient( byte[] client, String IPRange ) throws RemoteException {
				systemLog.info( "client is being updated" );

				if( IPRange != null ) {
						//check the ip address is valid
						StringTokenizer stk = new StringTokenizer( IPRange, "." );
						while( stk.hasMoreTokens() ) {
								try {
										int ip = Integer.parseInt( stk.nextToken() );
									} catch( Exception e ) {
										errorLog.info( "Invalid ip address sent with client: " + IPRange );
										return false;
									}
							}
					}

				try {
						//write the bytes to disk in order to extract the version info
						File f = new File( System.getProperty( "user.dir" ), "client.jar" );

						if( f.exists() ) {
								f.delete();
							}

						f.createNewFile();

						FileOutputStream fos = new FileOutputStream( f );
						fos.write( client );
						fos.close();

						URL[] urls = { f.toURL() };
						URLClassLoader urlcl = new URLClassLoader( urls );
						Class clientController = urlcl.loadClass( "ClientController" );
						Field version = clientController.getDeclaredField( "CLIENTVERSION" );
						Integer clientVersion = new Integer( version.getInt( clientController.newInstance() ) );

						//delete the file
						f.delete();

						systemLog.info( "New Client version: " + clientVersion );

						server.setClientJar( clientVersion, client, IPRange );

						return true;
					} catch( Exception e ) {
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						PrintStream ps = new PrintStream( bos );
						e.printStackTrace( ps );

						errorLog.severe( "Scheduler - updateClient - Problem updating client version: " + bos.toString() );
						return false;
					}
			}

		//remove the version of the client jar from the server
		public synchronized void clearClient() throws RemoteException {
				server.setClientJar( new Integer( 0 ), null, null );
			}

		//pause server - tell server to stop issuing units
		public synchronized boolean pause() throws RemoteException {
				systemLog.info( "Pausing Server" );
				if( pauseServer ) {
						systemLog.info( "Server already Paused" );
						return false;
					}

				pauseServer = true;
				return true;
			}

		//restart server after a pause
		public synchronized boolean restart() throws RemoteException {
				systemLog.info( "Restarting Server" );
				if( ! pauseServer ) {
						systemLog.info( "Server already started" );
						return false;
					}

				pauseServer = false;
				return true;
			}

		//get some statistics on the server
		public synchronized byte[] getServerStats() throws RemoteException {
				if(DEBUG)
					systemLog.info( "Getting Server Stats" );
				//request some system information from the server
				Vector info = server.getServerStats();

				//tell user if server has been paused
				if( pauseServer ) {
						info.add( "Server currently paused" );
					}

				info.add( problems.size() + " Problems running in System" );

				info.add(schedulingAlgorithm.toString());

				byte[] compressed = compressor.compress( info );
				return compressed;
			}

		//check to see if a job exists on the system
		public synchronized boolean jobRunning( Long jobID ) throws RemoteException {
				if(DEBUG)
					systemLog.info( "GUI checking for job in system" );
				if( problems.get( jobID ) != null ) {
						return true;
					}

				return false;
			}

		//remove a currently running job from the system
		public synchronized void killJob( Long jobID ) throws RemoteException {
				errorLog.warning( "Job being removed from system" );
				Problem p = (Problem) problems.get( jobID );

				if( p != null ) {
						//remove the file locks held on the log files
						boolean closed = p.closeAllResources();

						systemLog.warning( "Job found - removing: " + jobID.toString() );
						p = null;
						problems.remove( jobID );

						//remove the algorithm from the server
						server.removeAlgorithm( jobID );
						//schedulingAlgorithm.removeProblem( jobID );

						//rename the Problem directory so it will be deleted
						File f = new File( System.getProperty( "user.dir"), "problems" );
						File f1 = new File( f, jobID.toString() );

						if( f1.isDirectory() ) {
								Zipper zipper = new Zipper( f1, 'K', this );
								zipper.setPriority( Thread.MIN_PRIORITY );
								zipper.start();
							}
						// remove problem from client schedules
						schedulingAlgorithm.removeProblem(jobID.longValue());
						reSchedule(threshold) ;
						return;
					}
				errorLog.warning( "Couldnt find Problem in system to Kill" );
			}

		//send over the admin password
		public boolean checkAdmin( byte[] pass ) throws RemoteException {
				String connection = null;
				try {
						connection = RemoteServer.getClientHost();
					} catch( Exception e ) {
						errorLog.info( "Couldnt get Remote Interface Connection info" );
						e.printStackTrace();
					}

				systemLog.info( "Client Connection: " + connection + " Admin password being Validated" );
				Calendar cal = Calendar.getInstance();
				int doy = cal.get(Calendar.DAY_OF_YEAR);
				int y = cal.get( Calendar.YEAR );

				Date dateValue = cal.getTime();
				dateValue.setTime( dateValue.getTime() - cal.get( Calendar.ZONE_OFFSET ) );
				int hour = dateValue.getHours();

				//append to pass word
				String admin = adminPass + hour + doy + y;

				MessageDigest md = null;
				try {
						md = MessageDigest.getInstance( "MD5" );
					} catch( Exception e ) {
						errorLog.severe( "Cannot Locate Hashing Algorithm: " + e );
						e.printStackTrace();
						return false;
					}

				byte[] b = admin.getBytes();
				md.update( b );
				byte[] garbled = md.digest();

				//compare byte for byte
				if( garbled.length == pass.length ) {
						for( int i = 0; i < garbled.length; i ++ ) {
								if( garbled[ i ] != pass[ i ] ) {
										systemLog.warning( "Incorrect attempt at Admin password" );
										return false;
									}
							}
						if(DEBUG)
							systemLog.info( "Correct Admin password received" );
						return true;
					}
				return false;
			}

		//return an array of the current algorithm ID's
		public Long[] getCurrentAlgorithmIDs() {
			Long[] ids = new Long[ problems.size() ];
			Enumeration e = problems.elements();
			int counter = 0;
			while( e.hasMoreElements() ) {
					Problem p = (Problem) e.nextElement();
					ids[ counter ] = p.getID();
					counter ++;
				}
			return ids;
		}

		//update the server timeout value
		public synchronized void setTimeout( Long time )throws RemoteException {
				server.updateTimeout( time );
			}

		//check if a given job has been killed by system
		public boolean jobKilled( Long ID ) throws RemoteException {
				//check for killed job directory
				String name = "K" + ID.toString() + ".zip";
				String name1 = "E" + ID.toString() + ".zip";

				File f = new File( System.getProperty( "user.dir" ), "problems" );
				File killedName = new File( f, name );
				File killedName1 = new File( f, name1 );

				if( killedName.isFile() || killedName1.isFile() ) {
						return true;
					}

				return false;
			}

		public synchronized void setUnitTimeAndTolerance( int time, float tolerance ) throws RemoteException {
				if( time > 0 ) {
						optimalUnitTime = time;

						//update the optimal unit time for each problem also
						Enumeration e = problems.elements();
						while( e.hasMoreElements() ) {
								Problem p = (Problem) e.nextElement();
								p.updateOptimalTime( time );
							}
					}

				if( tolerance > 0.0f ) {
						optimalTolerance = tolerance;

						//update the tolerance for each problem also
						Enumeration e = problems.elements();
						while( e.hasMoreElements() ) {
								Problem p = (Problem) e.nextElement();
								p.updateTolerance( tolerance );
							}
					}
			}

		public boolean partialResults( Long job ) throws RemoteException {
				//check if there is a partial results set for this Problem
				File f = new File( System.getProperty( "user.dir" ), "problems" );
				File[] files = f.listFiles();
				String jobID = job.toString();

				if( files != null ) {
						for( int i = 0; i <  files.length; i ++ ) {
								if( files[ i ].isFile() && files[ i ].getName().indexOf( jobID ) != -1 ) {
										return true;
									}
							}
					}
				return false;
			}
		//**********end GUI methods***********/


		//Fill up the batch of tasks to be scheduled next - so this is where prioritys  get handled.
		public Vector fillBatch(int numTasks) {
			// simple round robin filling of the batch
			Vector batch = new Vector();
			int i = 0;

			boolean taskadded = false;
			do {
					taskadded = false;

					for (Enumeration e = problems.elements() ; e.hasMoreElements() ;) {
							Problem prob = (Problem)  e.nextElement();

							if(i<numTasks) {
									boolean added = prob.addTaskToQueue();
									if(added) {
											batch.add(prob.getID());
											taskadded = true;
											i++;
										}
								}
						}
				} while(i<numTasks && taskadded);


			// there seems to be a sync problem, so 1 or 2 units dont get handed out so this should fix it
			// hack

			if(!taskadded) {
					int numTasksScheduled = 0;
					for (Enumeration e = problems.elements() ; e.hasMoreElements() ;) {
							Problem prob = (Problem)  e.nextElement();

							// go through all the currently scheduled tasks and resolve them with the size of the task queue

							int numinst = 0;
							for (Enumeration ex = clients.elements() ; ex.hasMoreElements() ;) {
									clientInfo ID = (clientInfo)  ex.nextElement();

									Vector clientSchedule = ID.getSchedule();
									numTasksScheduled +=clientSchedule.size();

								}
						}

					if(numTasksScheduled == 0) {
							for (Enumeration e = problems.elements() ; e.hasMoreElements() ;) {
									Problem prob = (Problem)  e.nextElement();


									if((prob.taskQueue).size() != 0) {
											for(int pu = 0; pu < Math.abs((prob.taskQueue).size()) ; pu++) {
													batch.add(prob.getID());
													i++;
												}


										}
								}


							errorLog.info("sync error with tasks ");

						}
				}


			if(DEBUG) {
					systemLog.info( "Getting next batch of "+ i + " tasks");
				}


			return batch;
		}

		// random task
		//Fill up the batch of tasks to be scheduled next - so this is where prioritys  get handled.
		public Long randtask() {
			// simple round robin filling of the batch

			int i = 0;
			Random r = new Random();
			int randTask = 0;
			randTask = r.nextInt(problems.size());



			for (Enumeration e = problems.elements() ; e.hasMoreElements() ;) {
					Problem prob = (Problem)  e.nextElement();

					if(i== randTask) {
							boolean added = prob.addTaskToQueue();
							return prob.getID();
						}
					i++;
				}

			return null;
		}



		//remove Problem from system when an exception has occurred in the datamanager
		public void exceptionKillJob( Long problemID ) {
			errorLog.info( "Killing Job: " + problemID.toString() );



			//find the Problem
			Problem p = (Problem) problems.get( problemID );
			if( p != null ) {
					server.removeAlgorithm( problemID );
					//              schedulingAlgorithm.removeProblem( problemID );
					boolean closed = p.closeAllResources();
					problems.remove( problemID );

					//rename the directory accordingly
					File directory = new File( System.getProperty( "user.dir" ), "problems" );
					File problemDir = new File( directory, problemID.toString() );

					if( problemDir.isDirectory() ) {
							Zipper zipper = new Zipper( problemDir, 'E', this );
							zipper.setPriority( Thread.MIN_PRIORITY );
							zipper.start();
						}

					// remove problem from client schedules
					schedulingAlgorithm.removeProblem(problemID.longValue());
					reSchedule(threshold) ;
					return;
				}
		}

		//**********other methods*************/
		//removes a Problem from Problems vector because the Problem is finished executing
		private void removeProblem( Long problemID ) {
			systemLog.info( "Problem being removed internally" );
			Problem p = (Problem) problems.get( problemID );
			if( p != null ) {
					if(DEBUG)
						systemLog.info( "Problem Object Found - closing resources and removing " + problemID.toString());
					boolean closed = p.closeAllResources();
					//called when a Problem is finished - remove from Problems vector
					p = null;
					problems.remove( problemID );
					server.removeAlgorithm( problemID );
					//schedulingAlgorithm.removeProblem( problemID );
					// remove problem from client schedules
					schedulingAlgorithm.removeProblem(problemID.longValue());
					reSchedule(threshold) ;
				} else { return; }

			//create a zip file with all of the Problems results&logs
			File dir = new File( System.getProperty( "user.dir" ), "problems" );
			if(DEBUG)
				systemLog.info( "Creating zip file of results for ID: " + problemID.toString() );
			File actualDirectory = new File( dir, problemID.toString() );

			if( actualDirectory.isDirectory() ) {
					//create thread to do zipping of directory
					Zipper zipper = new Zipper( actualDirectory, 'R', this );
					zipper.setPriority( Thread.MIN_PRIORITY );
					zipper.start();
				}
			if(DEBUG)
				systemLog.info( "Remove Problem completed successfully" );
		}

		//method that takes a directory and deletes all the contents
		private void recursiveDelete( File f ) {
			systemLog.info( "Call to recursive Delete file: " + f.getName() );

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

		//method to extract a file from the server.jar file
		//used to extract the Algorithm and DataManager superclasses (needed for Problem classloaders)
		//return the bytes
		private byte[] extractFile( String fileName ) {
			try {
					//include the Algorithm and Datamanager in the list of urls for the Problem classloader
					JarFile server = new JarFile( new File( System.getProperty( "user.dir" ), "server.jar" ) );
					ZipEntry entry = server.getEntry( fileName );
					InputStream is = server.getInputStream( entry );
					byte[] fileBytes = new byte[ (int) entry.getSize() ];

					int offset = 0;
					int i = 1;
					while( i > 0 ) {
							i = is.read( fileBytes, offset, fileBytes.length - offset );
							offset += i;
						}
					is.close();

					return fileBytes;
				} catch( Exception e ) {
					errorLog.severe( "Failed to extract file " + fileName + " from server.jar: " + e );
					e.printStackTrace();
					return null;
				}
		}

		private void sortByCreationDate( File[] files ) {
			int newLowest = 0;            // index of first comparison
			int newHighest = files.length - 1;  // index of last comparison
			while( newLowest < newHighest ) {
					int highest = newHighest;
					int lowest  = newLowest;
					newLowest = files.length;    // start higher than any legal index
					for( int i = lowest; i < highest; i++ ) {
							if( files[ i ].lastModified() < files[ i + 1 ].lastModified() ) {
									// exchange elements
									File temp = files[ i ];
									files[ i ] = files[ i + 1 ];
									files[ i + 1 ] = temp;

									if( i < newLowest ) {
											newLowest = i - 1;
											if( newLowest < 0 ) {
													newLowest = 0;
												}
										} else if( i > newHighest ) {
											newHighest = i + 1;
										}
								}
						}
				}
		}

		public void updateFinishedJobsVector() {
			//look through the results directory for finished Problems
			File f = new File( System.getProperty( "user.dir" ), "problems" );
			File[] files = f.listFiles();
			sortByCreationDate( files );

			finishedJobsInfo = new Vector();
			finishedJobsInfo.add( "------------------------------------------------\n" );

			try {
					finishedJobsInfo.add( "Finished Jobs:" );
					//get the user names and problem descriptions
					for( int i = 0; i < files.length; i ++ ) {
							if( files[ i ].getName().endsWith( ".zip" ) && files[ i ].getName().charAt( 0 ) == 'R' ) {
									int count = 0;
									ZipFile zf = new ZipFile( files[ i ] );
									ZipEntry entry = zf.getEntry( files[ i ].getName().substring( 1, files[ i ].getName().length() - 4 ) + "/logs/problem" + count + ".log" );
									if( entry == null ) {
											errorLog.warning( "Failed to find problem0.log file in problem results file: "  + files[ i ].getName() );
											continue;
										}

									BufferedReader bf = new BufferedReader( new InputStreamReader( zf.getInputStream( entry ) ) );
									String line = bf.readLine();
									while( line != null && line.length() > 0 ) {
											line = line.substring( 6 ); //remove the logger prefix
											if( line.startsWith( "User:" ) ) {
													String userName = line.substring( 6 ); //get the user name
													bf.readLine();
													line = bf.readLine();
													line = line.substring( 6 );
													String id = line.substring( 4 ); //get the job id
													bf.readLine();
													line = bf.readLine();
													line = line.substring( 6 );
													String description = line.substring( 21 ); //get the description

													//get date last modified
													Date date = new Date( files[ i ].lastModified() );
													String date_ = (date.getHours() < 10 ? "0" + date.getHours() : "" + date.getHours() ) + ":" + (date.getMinutes() < 10 ? "0" + date.getMinutes() : "" + date.getMinutes() ) + " " + date.getDate() + "-" + (date.getMonth() + 1) + "-0" + (date.getYear() - 100) + "\t";

													if( userName.length() < 8 ) {
															finishedJobsInfo.add( id + "\t" + date_ + userName + "\t\t" + description );
														} else {
															finishedJobsInfo.add( id + "\t" + date_ + userName + "\t" + description );
														}
													bf.close();
													break;
												}
											line = bf.readLine();

											if( line == null ) {
													count ++;
													entry = zf.getEntry( files[ i ].getName().substring( 1, files[ i ].getName().length() - 4 ) + "/logs/problem" + count + ".log" );

													if( entry == null ) {
															errorLog.warning( "Failed to find problem" + count + ".log file in problem results file: " + files[ i ].getName() );
															continue;
														}
													bf = new BufferedReader( new InputStreamReader( zf.getInputStream( entry ) ) );
													line = bf.readLine();
												}
										}
								}
						}

					finishedJobsInfo.add( "\nFailed Jobs:" );
					//get the user names and problem descriptions
					for( int i = 0; i < files.length; i ++ ) {
							if( files[ i ].getName().endsWith( ".zip" ) && ( files[ i ].getName().charAt( 0 ) == 'E' || files[ i ].getName().charAt( 0 ) == 'K' ) ) {
									int count = 0;
									ZipFile zf = new ZipFile( files[ i ] );
									ZipEntry entry = zf.getEntry( files[ i ].getName().substring( 1, files[ i ].getName().length() - 4 ) + "/logs/problem" + count + ".log" );

									if( entry == null ) {
											errorLog.warning( "Failed to find problem0.log file in problem results file: " + files[ i ].getName() );
											continue;
										}

									BufferedReader bf = new BufferedReader( new InputStreamReader( zf.getInputStream( entry ) ) );
									String line = bf.readLine();
									while( line != null && line.length() > 0 ) {
											line = line.substring( 6 ); //remove the logger prefix
											if( line.startsWith( "User:" ) ) {
													String userName = line.substring( 6 ); //get the user name
													bf.readLine();
													line = bf.readLine();
													line = line.substring( 6 );
													String id = line.substring( 4 ); //get the job id
													bf.readLine();
													line = bf.readLine();
													line = line.substring( 6 );
													String description = line.substring( 21 ); //get the description

													//get date last modified
													Date date = new Date( files[ i ].lastModified() );
													String date_ = (date.getHours() < 10 ? "0" + date.getHours() : "" + date.getHours() ) + ":" + (date.getMinutes() < 10 ? "0" + date.getMinutes() : "" + date.getMinutes() ) + " " + date.getDate() + "-" + (date.getMonth() + 1) + "-0" + (date.getYear() - 100) + "\t";

													if( userName.length() < 8 ) {
															finishedJobsInfo.add( id + "\t" + date_ + userName + "\t\t" + description );
														} else {
															finishedJobsInfo.add( id + "\t" + date_ + userName + "\t" + description );
														}
													bf.close();
													break;
												}
											line = bf.readLine();

											if( line == null ) {
													count ++;
													entry = zf.getEntry( files[ i ].getName().substring( 1, files[ i ].getName().length() - 4 ) + "/logs/problem" + count + ".log" );

													if( entry == null ) {
															errorLog.warning( "Failed to find problem" + count + ".log file in problem results file: " + files[ i ].getName() );
															continue;
														}
													bf = new BufferedReader( new InputStreamReader( zf.getInputStream( entry ) ) );
													line = bf.readLine();
												}
										}
								}
						}
				} catch( IOException ioe ) {
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					PrintStream ps = new PrintStream( bos );
					ioe.printStackTrace( ps );
					errorLog.warning( "Failed to read info on complete jobs: " + bos.toString() );
				}

			if( finishedJobsInfo.size() > 0 ) {
					finishedJobsInfo.add( "\nNOTE: Result files will be auto-deleted 10 days after problem completion" );
				}
		}

		public void autoAddProblem( File algorithmF, File datamanagerF, String description,  File[]
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


							//upload the files to the server via socket
							for( int i = 0; i < problemDataF.length; i ++ ) {
									int retries = 0;
									Exception exception = null;
									while( retries < 5 ) {
											try {

													String ip = "149.157.247.202";
													int port =  server.socketPort;

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
													errorLog.warning("error " + e);
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
					errorLog.warning("error "+ f.toString());
				}
			//add the job to the server
			boolean b = false;



			try {
					b = this.addProblem( probID, al, dm, 10, description +
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

		public void hardcodeProbs() {
			//System.out.println("algorithm datamanager description classdefsArray  problemDataArray");

			String[] sim = new String[5];
			sim[0] = "/home/andrew/system/problems/sim/alSim.class";
			sim[1] = "/home/andrew/system/problems/tsp/dmSim.class";
			sim[2] = "simulation";
			sim[3] = "";
			sim[4] = "";
			preprocessAutoAdd(sim);
			/*
			// TSP
			String[] TSP = new String[5];
			TSP[0] = "/home/andrew/system/problems/tsp/tspAL3.class";
			TSP[1] = "/home/andrew/system/problems/tsp/tspDM.class";
			TSP[2] = "TSP";
			TSP[3] = "";
			TSP[4] = "";
			preprocessAutoAdd(TSP);

			// photon
			String[] Photon = new String[5];
			Photon[0] = "/home/andrew/system/problems/photon/AlPhotonSys12.class";
			Photon[1] = "/home/andrew/system/problems/photon/DMPhotonSys12.class";
			Photon[2] = "Photon";
			Photon[3] = "";
			Photon[4] = "";
			preprocessAutoAdd(Photon);


			// Pollard Rho
			String[] poll = new String[5];
			poll[0]="/home/andrew/system/problems/pollard/pollardAl3.class";
			poll[1]="/home/andrew/system/problems/pollard/pollarddm54.class";
			poll[2]="Pollard Rho";
			poll[3]="";
			poll[4]="";
			preprocessAutoAdd(poll);

			// MD5
			String[] md5 = new String[5];
			md5[0] = "/home/andrew/system/problems/md5/Alhash.class";
			md5[1] = "/home/andrew/system/problems/md5/DMhash.class";
			md5[2] = "MD5";
			md5[3] = "";
			md5[4] = "/home/andrew/system/problems/md5/passwords1";
			preprocessAutoAdd(md5);

			// SHA 1
			String[] sha = new String[5];
			sha[0] = "/home/andrew/system/problems/sha1/Alsha1.class";
			sha[1] = "/home/andrew/system/problems/sha1/DMsha1.class";
			sha[2] = "SHA1";
			sha[3] = "";
			sha[4] = "/home/andrew/system/problems/sha1/passwordsfile";
			preprocessAutoAdd(sha);


			// dsearch
			
			String[] dsearch = new String[5];
			dsearch[0] = "/home/andrew/system/problems/dsearch/binaries/dsearch_AlgorithmV1.class";
			dsearch[1] = "/home/andrew/system/problems/dsearch/binaries/dsearch_DataManagerV1.class";
			dsearch[2] = "Dsearch";
			dsearch[3] = "/home/andrew/system/problems/dsearch/binaries/NeoBio.jar /home/andrew/system/problems/dsearch/binaries/dsearchV1.jar";
			dsearch[4] = "/home/andrew/system/problems/dsearch/binaries/test1/blosum62.txt /home/andrew/system/problems/dsearch/binaries/test1/estfa4.fas /home/andrew/system/problems/dsearch/binaries/test1/estfa5.fas /home/andrew/system/problems/dsearch/binaries/test1/inputs.txt";
			preprocessAutoAdd(dsearch);
			*/

		}

		public void preprocessAutoAdd(String args[]) {
			String[] classdefs = args[3].split(" ");
			File[] classfiles = new File[classdefs.length];


			for(int i = 0; i< classdefs.length ; i++) {
					classfiles[i] = new File(classdefs[i]);
				}

			String[] probdata = args[4].split(" ");
			File[] probfile = new File[probdata.length];
			for(int i = 0; i< probdata.length; i++) {
					probfile[i] = new File(probdata[i]);
				}

			if(args[3]=="" && args[4] == "") {
					autoAddProblem(new File(args[0]), new File(args[1]), args[2] , null, null);
				} else if(args[3] == "" && args[4] !="") {
					autoAddProblem(new File(args[0]), new File(args[1]), args[2] , null, probfile);
				} else if(args[3]!="" && args[4] =="") {
					autoAddProblem(new File(args[0]), new File(args[1]), args[2] , classfiles, null);
				} else {
					autoAddProblem(new File(args[0]), new File(args[1]), args[2] , classfiles, probfile);
				}
		}

		/** Remove tasks from a client which hasnt contacted recently*/
		private Vector timeoutClients(long timeout) {
			// timeout in seconds
			Vector removedTasks = new Vector();

			//for each client check to see if they havent contacted recently
			for (Enumeration e = clients.elements() ; e.hasMoreElements() ;) {
					clientInfo ID = (clientInfo)  e.nextElement();

					// pop off scheduled tasks and assign them to another computer
					if(  ID.getLastTime()  +timeout <  System.currentTimeMillis()    ) {
							Vector clientSchedule = ID.getSchedule();
							while(clientSchedule.size() !=0) {
									removedTasks.add(  clientSchedule.remove(0) );
								}

							clients.remove(ID.getIP());
						}
				}
			return removedTasks;
		}

	}
