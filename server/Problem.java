/*
Thomas Keane, 8:54 PM 16/10/03
 
Copyright (C) 2003  Thomas Keane
Updated  -  2005 Andrew Page
 
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
import java.util.Vector;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;

/** class describing a problem runnning in the system
* @author Thomas Keane   - updated by Andrew Page
*/
public class Problem {
		private static final double EWMA_ALPHA_CONSTANT = .01;

		private Scheduler owner; //pointer to system scheduler
		private ProblemLoader problemLoader; //classloader for loading problem classes
		private Long problemID;	//unique problem id
		private String description; //english description
		private int priority; //priority in system
		private long unitTime; //current unit time in minutes
		private long resultsCounter; //counts results received
		private long startTime; //in milliseconds
		private long finishTime; //"	"
		private String userName;
		private File problemDirectory;

		//problem specific information
		private int allowExceptions;  //number of exceptions a unit is allowed before quitting the problem
		private int allowExpired;  //number of expired units a problem is allowed before quitting the problem
		private int minProcessorSpeed; //in MFLOPS
		private int requiredMemory; //in MBytes
		private float optimalTolerance; //the tolerance around the optimal unit time that is allowed

		private int unitCounter; //generates unit ID's for units

		//systemLogs
		private Logger systemLog;
		private Logger errorLog;
		private Logger timingLog;

		//problem logs
		private Logger problemErrorLog;
		private Logger problemLog;

		//compressor for the problem
		private Compressor compressor;

		//info about the datamanager status
		private int pendingUnits; //number of units currently being processed

		//name of algorithm and DM class
		private String[] classInfo;

		//the average processing time that each problem attempts to reach (minutes)
		private int optimalUnitTime;

		private long avgProcessingTime; //holds the average processing time (seconds)
		private long lastAverageUpdate; //last time the Datamanager was requested to change its granularity
		private long slowestUnit; //time slowest unit took to process
		private int resultsReceivedSinceLastUpdate; //records how many results were received since the last problem granularity update

		// AP additional scheduling variables
		private int algorithmSize; // size in bytes of algorithm object
		private double avgTaskSize;
		private double avgNetworkTime;
		private double avgProcTime;
		private double avgMFLOPS;

		private boolean DEBUG = true;
		private Hashtable unitEst;

		public Vector taskQueue;

		private Vector procEstError ;
		private Vector procError;


		public Problem( Scheduler own, Long id, String desc, int pr, Logger sys, Logger err, int exceptions, int expired, int optimalUnit, float tolerance, int processor, int memory, URL[] urls, File problemDirectory_, String user ) {
			userName = user;
			owner = own;
			systemLog = sys;
			errorLog = err;
			problemErrorLog = null;
			problemLog = null;
			timingLog = null;
			problemDirectory = problemDirectory_;
			systemLog.info( "Creating Problem Object: " + id.toString() );

			problemID = id;
			description = desc;
			priority = pr;
			allowExceptions = exceptions;
			allowExpired = expired;
			minProcessorSpeed = processor;
			requiredMemory = memory;
			optimalUnitTime = optimalUnit;
			optimalTolerance = tolerance;

			problemLoader = new ProblemLoader( problemID, systemLog, errorLog, urls, problemDirectory ); //create the classloader for this particular problem
			compressor = new Compressor( problemLoader ); //create the compressor for use by this problem

			unitTime = 30; //set the initial unit time at 30mins
			unitCounter = 0; //unit counter to zero
			resultsCounter = 0; //results counter to zero
			startTime = System.currentTimeMillis(); //time problem started
			finishTime = 0; //not finished yet!
			pendingUnits = 0; //zero at start
			slowestUnit = 0; //zero at start
			avgProcessingTime = 0;
			lastAverageUpdate = System.currentTimeMillis();
			resultsReceivedSinceLastUpdate = 0;

			algorithmSize  = 0;
			avgNetworkTime = 0;
			avgProcTime = 0;
			avgMFLOPS = 0;
			avgTaskSize = 0;
			unitEst = new Hashtable();
			taskQueue = new Vector();
			procEstError = new Vector();
			procError = new Vector();


			if(DEBUG)
				systemLog.info( "Problem Object Create Successful: " + id.toString() );
		}

		/**load the problem files into the system (algorithm and datamanager) via the problem loader classloader */
		public String loadProblem( byte[] algorithm, byte[] userDM ) {
			if(DEBUG)
				systemLog.info( "Entering method" );

			algorithmSize = algorithm.length;

			classInfo = new String[ 2 ];
			//now load the algorithm - make sure it loads before sending to clients
			String error = problemLoader.loadAlgorithm( algorithm );
			if( error != null ) {
					errorLog.warning( "Failed to load problem Algorithm: " + error );
					return "Failed to load problem Algorithm: " + error;
				}
			classInfo[ 0 ] = problemLoader.getAlgorithmName();

			//now load the datamanager
			error = problemLoader.loadDM( userDM );
			if( error != null ) {
					errorLog.warning( "Failed to load problem datamanager: " + error );
					return "Failed to load problem datamanager: " + error;
				}
			classInfo[ 1 ] = problemLoader.getDMName();

			return null;
		}

		/** pop a unit off the queue */
		public Vector getDataUnit(clientInfo c) throws Throwable {
				if(taskQueue.size()==0)
					return null;
				else {

						return (Vector) taskQueue.remove(0);
					}
			}

		public boolean addTaskToQueue() {
			try {
					Vector newunit = generateDataUnit();


					if(newunit == null)
						return false;

					taskQueue.add(newunit);
					return true;

				} catch(Throwable e) {
					errorLog.warning( "couldnt add a task to the queue : "+ e);
					return false;
				}
		}


		/**generate a data unit */
		public Vector generateDataUnit() throws Throwable {
				if(DEBUG)
					systemLog.info( "Generating Data Unit from Problem: " + problemID.toString() );

				Vector v = null;
				synchronized( problemLoader ) {
						v = problemLoader.getDataUnit();
					}

				if( v == null ) {
						return null;
					} else {
						byte[] unit = compressor.compress( v );

						//if an exception occurred in the compressor
						if( unit == null ) {
								errorLog.warning( "Exception occurred in Compressor" );
								return null;
							}
						systemLog.info( "ProblemID: " + problemID.toString() + " Unit generated. Size: " + unit.length + " bytes" );
						updateAvgTaskSize(unit.length);

						//create the vector
						Vector unitInfo = new Vector( 4, 1 );
						unitInfo.add( 0, new Long( unitCounter ) ) ;
						unitInfo.add( 1, problemID );
						unitInfo.add( 2, new Long( unitTime * 60000 ) ); //convert to milliseconds for the system
						unitInfo.add( 3, unit );

						pendingUnits ++;
						unitCounter ++;

						return unitInfo;
					}
			}

		/** Store how long you think a unit will run for*/
		public void storeProcessingEst(long unitID, long unitE, double mg, long knnest, Integer ob, double ccr) {
			Long a = new Long(unitID);
			Long b = new Long(unitE);
			Double m = new Double(mg);
			Long k = new Long(knnest);
			Double taskErrorEst = new Double(getTaskEstError());
			Double procErrorEst =  new Double(getProcEstError());
			Double ccrest =  new Double(ccr);

			Vector data = new Vector();
			data.add(0,b);
			data.add(1,m);
			data.add(2,k);
			data.add(3,ob); // size of observation set, if used
			data.add(4,taskErrorEst);
			data.add(5,procErrorEst);
			data.add(6,ccrest);

			unitEst.put(a, data);

		}

		public void removeProcessingEst(long unitID) {

			if(unitEst.contains(new Long(unitID))) {
					unitEst.remove(new Long(unitID));
				}
		}


		/** return the average mflops of a task. uses a smoothing function*/
		public double getAvgMflops() {
			return avgMFLOPS;
		}


		public double getTaskEstError() {

			// if there are no estimates there is 100% error
			if (procEstError.size() == 0) {

					return 1;
				}

			double avgError = 0.0;
			for(int i =0; i< procEstError.size(); i++) {
					avgError = ((avgError*i)+ (((Double)procEstError.get(i)).doubleValue()) )/(i+1);
				}

			if(avgError == 0) {

					return 1;
				}


			return avgError;
		}

		public double getProcEstError() {

			// if there are no estimates there is 100% error
			if (procError.size() == 0)
				return 1;

			double avgError = 0.0;
			for(int i =0; i< procError.size(); i++) {
					avgError = ((avgError*i)+ (((Double)procError.get(i)).doubleValue()) )/(i+1);
				}

			if(avgError == 0)
				return 1;
			return avgError;
		}

		public boolean receiveResults( Long unitID, byte[] results, Long[] processingTime , double mflops) throws Throwable {
				systemLog.info( "Receiving Results set of unitID: " + unitID.toString() + " ProblemID: " + problemID.toString() );
				Vector res = (Vector) compressor.decompress( results );

				try {

						Vector oldest = ((Vector)unitEst.get(unitID));
						double avgProcTimeSec = processingTime[1].longValue()/1000;

						// work out the error in my estimation of task execution time
						double knnError    = Math.abs( (((( (Long)oldest.get(2)).longValue())/1000) - avgProcTimeSec) / avgProcTimeSec);
						double smoothError = Math.abs( (((( (Long)oldest.get(0)).longValue())/1000) -avgProcTimeSec) / avgProcTimeSec);
						double taskEE = 0;
						if(knnError < smoothError && knnError !=0) {
								procEstError.add(0,new Double(knnError));
								taskEE = knnError;
							} else {
								procEstError.add(0,new Double(smoothError));
								taskEE = smoothError;
							}
						if(procEstError.size() > 20) {
								//procEstError.remove(procEstError.size() -1);
							}

						double  procE= (Math.abs(((Double)oldest.get(1)).doubleValue() - mflops))/mflops;
						procError.add(0,new Double(procE));
						if(procError.size() > 20) {
								//procError.remove(procError.size() -1);
							}
						double actCCR =1;
						if(processingTime[ 0 ].longValue() == 0) {} else {

								actCCR = (1.0* processingTime[ 1 ].longValue())/(1.0*processingTime[ 0 ].longValue() );
							}


						System.out.println("es "+System.currentTimeMillis() + " "+ (processingTime[1].longValue()/1000) + " " + ((Long)oldest.get(0)).toString() + " "
						                   +  mflops+ " "+ ((Double)oldest.get(1)).doubleValue() +" " + problemID.longValue()+ " "+ ((((Long)oldest.get(2)).longValue())/1000)
						                   + " "+avgProcessingTime+" "+ ((Integer)oldest.get(3)).intValue() + " " + ((Double)oldest.get(4)).doubleValue()+ " " + taskEE
						                   + " "+((Double)oldest.get(5)).doubleValue() +" " +procE +" " + ((Double)oldest.get(6)).doubleValue() + " "+ actCCR+" ;");


						removeProcessingEst(unitID.longValue());
					} catch(Exception e) {
						problemErrorLog.warning("couldnt print oout exps results "  + e);
					}


				double pt = (1.0*processingTime[1].longValue())/1000;
				// calculate the average amount of mflops in a unit on average
				if(avgMFLOPS ==0)
					avgMFLOPS =(1.0)*(mflops)*(pt);
				else
					avgMFLOPS	 = avgMFLOPS + 0.02*(((1.0)*mflops*pt)-avgMFLOPS  );

				if( res == null ) {
						errorLog.warning( "Exception occurred in Compressor" );
						throw new CompressorException( "Exception occurred in Compressor - see stderr.log for details" );
					}

				//log the timing info
				timingLog.info( unitID.toString() + "\t" + processingTime[ 0 ].toString() + "\t" + processingTime[ 1 ].toString() + "\n" );

				if(avgNetworkTime ==0) {
						avgNetworkTime =(1.0)*processingTime[0].longValue();
					} else {
						// create a rolling average of the network time
						avgNetworkTime	 = avgNetworkTime + EWMA_ALPHA_CONSTANT*(((1.0)*processingTime[0].longValue())-avgNetworkTime  );
					}

				if (avgProcessingTime ==0) {
						avgProcTime = (1.0)*processingTime[1].longValue();
					} else {
						avgProcTime = avgProcTime + EWMA_ALPHA_CONSTANT*(((1.0)*processingTime[1].longValue()) - avgProcTime);
					}

				boolean finished = false;
				synchronized( problemLoader ) {
						//true returned when problem finished
						finished = problemLoader.processResults( unitID, res );
					}

				//update the average processing time using the EWMA function (except for first result)
				if( avgProcessingTime == 0 && resultsCounter == 0 ) {
						//set to the proc time for the first unit back
						avgProcessingTime = processingTime[ 1 ].longValue() / 1000;

						//unitTime = ( (int) avgProcessingTime / 60 ) * 2;
						problemLog.info( "Unit time initially set to " + unitTime + " mins" );
					} else {
						//update the avg proc time using an exponentially smoothing function
						avgProcessingTime = (long) (EWMA_ALPHA_CONSTANT *  ( processingTime[ 1 ].longValue() / 1000 ) + ( 1 - EWMA_ALPHA_CONSTANT ) * avgProcessingTime);
					}

				//update the problem granularity (if necessary) using the EMWA function (based on the average processing time)

				pendingUnits --;
				resultsCounter ++;
				resultsReceivedSinceLastUpdate ++;

				//see if this unit has been slower than previous
				long processingTimeMins = (long) ( processingTime[ 1 ].longValue() / 60000 );
				if( processingTimeMins > slowestUnit ) {
						slowestUnit = processingTimeMins;
					}

				//check how long it is since last time the problem granularity was updated
				int lastUpdatePeriod = (int) ( (System.currentTimeMillis() - lastAverageUpdate) / 60000 );

				//if the granularity hasnt been updated for greater than twice the optimal unit time (allows for last granularity change to have fed back into average processing time)- then check it
				if( lastUpdatePeriod > (2 * optimalUnitTime) || resultsReceivedSinceLastUpdate > 500 ) {
						//reset the counters
						lastAverageUpdate = System.currentTimeMillis();
						resultsReceivedSinceLastUpdate = 0;

						//first update the slowest unit recorded (used to set the pending unit timeout)
						unitTime = slowestUnit + 10;
						slowestUnit = 0;
						problemLog.info( "Setting Unit Time for Problem to " + unitTime + " minutes" );

						//now see if the granularity of the problem needs to be updated
						int average = (int) avgProcessingTime / 60;
						double temp = optimalUnitTime - average;
						temp = ( temp / average ) * 100;
						int percent = (int) temp; //percent is a percentage of the average (i.e. how much to increase or decrease by)

						if( percent > optimalTolerance || percent < -optimalTolerance ) {
								synchronized( problemLoader ) {
										//adjust unit size by a +/- percent according to how far off optimal
										problemLog.info( "Average Processing Time: " + average + " Adjusting unit size by " + percent + "%" );
										try {
												problemLoader.adjustGranularity( percent );
											} catch( NoClassDefFoundError n ) {
												problemErrorLog.warning( "Error in adjustUnitSize: " + n );
											}
									}
							} else {
								problemLog.info( "Average Processing Time within allowed bounds of optimal unit time - no adjustment needed" );
							}
					}

				//the condition for the problem being terminated in the system
				if( finished ) {
						problemLog.info( "Problem finished: " + problemID.toString() );
						systemLog.info( "Problem " + problemID.toString() + " finished" );
						return true;
					} else {
						//not finished so return false
						return false;
					}
			}

		/**log an error in one of the units */
		public void logAlgorithmError( Long unitID, String e ) {
			problemErrorLog.warning( "Algorithm Error in unit: " + unitID.toString() + ". Error: " + e );
		}

		/**log an error in the datamanager */
		public void logDataManagerError( String e, String origin ) {
			problemErrorLog.warning( "DataManager error in " + origin + " : " + e );
		}

		/**return the prob ID */
		public Long getID() {
			return problemID;
		}

		/**return status information on the problem */
		public Vector getStatus() {
			systemLog.info( "Status Request received for problem " + problemID.toString() );
			Vector info = new Vector();
			info.add( "User: " + userName );
			info.add( "Problem Description: " + description );
			info.add( "Problem ID: " + problemID.toString() );
			info.add( "Results Received: " + resultsCounter );
			info.add( "Pending Units: " + pendingUnits );
			info.add( "Units Issued: " + unitCounter );
			//	info.add( "Problem Priority: " + priority );
			//	info.add( "Minimum CPU: " + minProcessorSpeed + " MFLOPS" );
			//	info.add( "Minimum Memory: " + requiredMemory + " Mb" );
			info.add( "Current Average Processing Time: " + ( (int) ( avgProcessingTime / 60 ) ) + " minutes" );

			long timeRunning = ( ( System.currentTimeMillis() - startTime ) / 60000 );
			if( timeRunning > 59 ) {
					long hours = (long) ( timeRunning / 60 );
					int minutes = (int) timeRunning % 60;
					info.add( "Time Running in System: " + hours + " hours " + minutes + " minutes" );
				} else {
					info.add( "Time Running in System: " + timeRunning + " minutes");
				}

			//get some user defined info on the problem
			String s = null;
			try {
					s = problemLoader.getStatus();
				} catch( Throwable e ) {
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					PrintStream ps = new PrintStream( bos );
					e.printStackTrace( ps );
					problemErrorLog.warning( "Exception in getStatus() of DataManager: " + bos.toString() );
				}

			if( s != null && ! s.equals( "" ) ) {
					info.add( s );
				}

			return info;
		}

		/** return the priority of the problem*/
		public int getPriority() {
			return priority;
		}

		public void setVariables( Logger probLog, Logger errorL, File probDir, Logger timingL ) throws Throwable {
				if(DEBUG)
					systemLog.info( "Setting DM variables for problem " + problemID.toString() );

				problemErrorLog = errorL;
				problemLog = probLog;
				timingLog = timingL;
				timingLog.info( "UnitID	Network Time\tProcessing Time\n" );

				problemLog.info( "Algorithm: " + classInfo[ 0 ] );
				problemLog.info( "DataManager: " + classInfo[ 1 ] );
				classInfo = null;
				problemLog.info( "User: " + userName );
				problemLog.info( "ID: " + problemID.toString() );
				problemLog.info( "Problem Description: " + description );
				problemLog.info( "Unit Time set to: " + unitTime + " minutes" );
			}

		public void setPriority( int pr ) {
			problemLog.info( "Remote Interface Updating Problem Priority: " + pr );
			priority = pr;
		}

		/**stop the datamanager and set it to null
		* remove the file locks held by the logs */
		public boolean closeAllResources() {
			problemLog.info( "Closing all resources held by problem" );
			//close any files etc.
			synchronized( problemLoader ) {
					//close datamanger resources and stop the datamanger
					try {
							problemLoader.closeResources();
						} catch( Throwable e ) {
							ByteArrayOutputStream bos = new ByteArrayOutputStream();
							PrintStream ps = new PrintStream( bos );
							e.printStackTrace( ps );

							problemErrorLog.warning( "Error closing resources " + bos.toString() );
						}
				}

			//record how long the problem was running for
			long timeRunning = ( ( System.currentTimeMillis() - startTime ) / 60000 );
			if( timeRunning > 59 ) {
					long hours = (long) ( timeRunning / 60 );
					int minutes = (int) timeRunning % 60;
					problemLog.info( "Total Running time in System: " + hours + " hours " + minutes + " minutes" );
				} else {
					problemLog.info( "Total Running time in System: " + timeRunning + " minutes" );
				}

			if( problemErrorLog != null && problemLog != null ) {
					//get the handlers for each log
					Handler[] handlers = problemErrorLog.getHandlers();
					Handler[] handlers1 = problemLog.getHandlers();
					Handler[] handlers2 = timingLog.getHandlers();

					for( int i = 0; i < handlers.length; i ++ ) {
							problemErrorLog.removeHandler( handlers[ i ] );
							handlers[ i ].flush();
							handlers[ i ].close();
						}

					for( int i = 0; i < handlers1.length; i ++ ) {
							problemLog.removeHandler( handlers1[ i ] );
							handlers1[ i ].flush();
							handlers1[ i ].close();
						}

					for( int i = 0; i < handlers2.length; i ++ ) {
							timingLog.removeHandler( handlers2[ i ] );
							handlers2[ i ].flush();
							handlers2[ i ].close();
						}
				}

			//delete the classes directory
			File classesDir = new File( problemDirectory, "classes" );
			if( classesDir.isDirectory() ) {
					File[] files = classesDir.listFiles();
					for( int i = 0; i < files.length; i ++ ) {
							files[ i ].delete();
						}
					classesDir.delete();
				}

			//set to null
			problemErrorLog = null;
			problemLog = null;
			timingLog = null;
			problemLoader = null;
			return true;
		}

		public void updateOptimalTime( int t ) {
			optimalUnitTime = t;
		}

		public void updateTolerance( float t ) {
			optimalTolerance = t;
		}

		public int getExceptions() { return allowExceptions; }

		public int getExpired() { return allowExpired; }

		public int getMemory() { return requiredMemory; }

		public int getCPU() { return minProcessorSpeed; }

		/** Returns the average size in bytes of a task  */
		public double getAvgTaskSize() {
			return  avgTaskSize;
		}

		/** updates the average size of a task in bytes using a smoothing function*/
		public void updateAvgTaskSize(double t) {
			if(avgTaskSize ==0)
				avgTaskSize =t;
			else
				avgTaskSize	 = avgTaskSize + EWMA_ALPHA_CONSTANT*(t-avgTaskSize );

		}

		protected double CCRest() {

			/*
						clientInfo p = (clientInfo) clientDetails.get(ips[j]);
						Vector estTime = p.getEstimatedTime( (Long)(batch.get(i)), 0.0);
						double extime = (((Long)estTime.get(0)).longValue()/1000);
						double commsTime =  (((Long)estTime.get(1)).longValue()/1000);
			 
						if(extime ==0.0) {
								if(p.getMflops() <=0)
									extime = 0;
								else
									extime=((Problem)problems.get((Long)(batch.get(i)))).getAvgMflops()/  p.getMflops();
							}
			 
						if(commsTime == 0)
							CCR[i][j] = 1;
						else
							CCR[i][j] = extime/commsTime;
			*/
			return 0;
		}




	}
