/*
Andrew Page 7 June 2005
 
 
Copyright (C) 2005  Andrew Page
 
 
This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.
 
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
*/


/* Complexity, everythig in here should be constant complexity */

import java.util.*;
import java.math.*;


/**
* Object to hold all the info about a single client in the system
* @author Andrew Page
*/
public class clientInfo {

		/** once set it should never change*/
		private String ip= "127.0.0.1";

		private double mflops = 0;
		private double memory = 0;
		private int numFailedConnections  = 0;  // number of times a client has attempted to connect to the server and failed
		private long upTime = 0;
		private long clientProcTime = 0; // amount of time the client has spent processing
		private long lastTime = 0;  // this is the time when the latest details about the client were included
		private long estFinishingTime = 0;

		private double smoothing = 0.05;

		private double latency = 0.0;
		private double estBandwidth =0.0;// not impliemented yet

		private Hashtable problemTimes;
		private int MAX_OBSERVATIONS = 200;

		/** holds Longs corresponding to problem IDs. It is public to allow schedulers to manipulate the schedule*/
		public Vector schedule;


		/** Constructor called if the client has had no failed connections */
		public clientInfo(String ipaddress, int megaflops,int mem, long up, long proctime, long laty) {
			ip= ipaddress;
			mflops =1.0*megaflops ;
			memory =1.0*mem ;
			upTime = up;
			clientProcTime =proctime ;
			latency = 1.0*laty;
			lastTime = System.currentTimeMillis();
			schedule = new Vector();
			problemTimes = new Hashtable();
		}

		/** Constructor called if the client has had failed connections */
		public clientInfo(String ipaddress,  int megaflops,int mem,  int failedConn, long up, long proctime,long laty) {
			ip= ipaddress;
			mflops =1.0*megaflops ;
			memory =1.0*mem ;
			numFailedConnections  =  failedConn;
			upTime = up;
			clientProcTime =proctime ;
			latency = 1.0*laty;
			lastTime = System.currentTimeMillis();
			schedule = new Vector();
			problemTimes = new Hashtable();
		}


		// update methods ***************************************************************

		/** A task has been returned so update the info for that client */
		public void updateInfo(int megaflops, int mem, int failedConn, long up, long proctime, long lat) {
			updateMegaFlops( (1.0*megaflops));
			updateMemory( (1.0*mem)) ;
			updateFailedConn( failedConn) ;
			updateUpTime( up) ;
			updateProcTime( proctime);
			updateLatency(lat);
			lastTime = System.currentTimeMillis();

		}

		/** A client has updated information about its state*/
		public void update(int megaflops, int mem, long up, long proctime, long lat) {
			updateMegaFlops( (1.0*megaflops));
			updateMemory( (1.0*mem)) ;
			updateUpTime( up) ;
			updateProcTime( proctime);
			updateLatency(lat);
			lastTime = System.currentTimeMillis();

		}


		public long getclientProcTime() {
			return clientProcTime;
		}
		/** Uses smoothing function*/
		private void updateMegaFlops(double megaflops) {
			if(mflops == 0)
				mflops = megaflops;
			else
				mflops   = mflops + smoothing*(megaflops - mflops );
		}

		/** Uses smoothing function*/
		private void updateMemory(double mem) {
			if(memory == 0)
				memory = mem;
			else
				memory   = memory + smoothing*(mem- memory );
		}

		/** Uses smoothing function*/
		private void updateLatency(long l) {
			if(l== 0)
				latency = 1.0*l;
			else
				latency          = latency + smoothing*((1.0*l)- latency );
		}

		private void updateFailedConn(int failedConn) {
			numFailedConnections  += failedConn;
		}

		private void updateUpTime(long up) {
			upTime = up;
		}

		private void updateProcTime(long proctime) {
			clientProcTime = proctime;
		}

		public void setLastTime() {
			lastTime = System.currentTimeMillis();
		}

		// get methods *************************************************************************

		public double getMflops() {
			return mflops;
		}

		public Vector getSchedule() {

			return schedule;
		}

		public String getIP() {
			return ip;
		}

		public double getLatency() {
			return latency;
		}

		public long getEstFinishingTime() {
			return estFinishingTime;
		}

		public double getBandwidth() {
			return estBandwidth;
		}

		public long getLastTime() {
			return lastTime;
		}


		// set methods *************************************************************************


		public void setEstFinishingTime(long fin) {
			estFinishingTime = fin;
		}

		public void setSmoothingVal(double sm) {smoothing = sm;}

		/** A task has been sent to the client by the scheduler so remove the first problem ID from the list of tasks scheduled for this client.*/
		public void scheduleTask(Long problemID) {
			for(int i =0; i< schedule.size(); i++) {
					if(((Long)schedule.get(i)).longValue() == ((Long)problemID).longValue()) {
							schedule.remove(i);
							return;
						}
				}
		}


		/** Print out all the info stored in this object*/
		public String toString() {
			String info = new String();

			info += "ip: " + ip +"\n";
			info += "mflops: " +mflops +"\n";
			//			info += "memory: " +memory+"\n";
			//			info += "numFailedConnections: " +numFailedConnections +"\n";
			//			info += "upTime: " +upTime +"\n";
			//			info += "clientProcTime: " +clientProcTime+"\n";
			info += "lastTime: " +lastTime +"\n";
			info += "estFinishingTime: " +estFinishingTime +"\n";
			//			info += "smoothing: " +smoothing +"\n";
			info += "latency: " +latency+"\n";
			//			info += "estBandwidth: " +estBandwidth+"\n";

			info += "Schedule: ";
			for(int i =0; i< schedule.size(); i++) {
					info += " -> "+ ((Long)schedule.get(i)).longValue() ;
				}
			info +=  "\n\n";


			return info;

		}

		/** Record the time a task took to process so that you can use K-NN and L-smoothing for the estimate
		@param taskProcessingTime the time the task took to process in milliseconds
		@param taskNetworkTime the time the task spent communicating in milliseconds
		@param taskSize this is the estimated size of the task. 
		It allows for dynamically resized tasks. The more accurate it is, the better result you can get. 
		1 unit is the size of the first task run from the datamanager. It then gets adjusted by percentages dynamically, which also adjusts this size.
		*/
		public void addTask(Long problemID, long taskProcessingTime, long taskNetworkTime, double taskSize) {
			// Observation point to be stored
			Vector obPoint = new Vector();
			obPoint.add(0,new Long(taskProcessingTime));
			obPoint.add(1,new Long(taskNetworkTime));
			obPoint.add(2,new Double(taskSize));
			obPoint.add(3, new Long(System.currentTimeMillis()));

			if(problemTimes.containsKey(problemID) ) {
					Vector observations = (Vector) problemTimes.get(problemID);
					observations.add(obPoint);

					// A bound is placed on the number of observations to make it scalable. Effectively a sliding window.
					if(observations.size() >MAX_OBSERVATIONS ) {
							// size can be a bit dodgy sometimes, so only calling it once and not using while loop.
							int removeLimit = (observations.size() - MAX_OBSERVATIONS);

							for(int i = 0; i<removeLimit  ; i++) {
									observations.remove(0);
								}
						}
				} else {
					// add this problem to the hashtable for the first time
					Vector observations = new Vector();
					observations.add(obPoint);
					problemTimes.put(problemID,observations  );
				}
		}

		/** @return sends back the estimated processing time in milliseconds in 0, then in 1 send back estimated network time*/
		public Vector getEstimatedTime(Long problemID, double taskSize) {
			int k  = 1 ;

			Vector emp = new  Vector();
			emp.add(0,new Long(0));
			emp.add(1,new Long(0));
			emp.add(2,new Integer(0)); // size of observation set
			if(problemTimes.containsKey(problemID) ) {

					Vector observations = (Vector) problemTimes.get(problemID);

					if(observations.size() >0 ) {
							/* for exp on why to use 0.8 (4/5) see M. A. Iverson, F. Ozguner and L. Potter, “Statistical Prediction of Task
							Execution Times through Analytic Benchmarking for Scheduling in a Heterogeneous Environment,” 
							IEEE Transactions on Computers, vol. 48, no. 12, pp. 1374-1379, December 1999.
							*/
							k =(int) Math.pow(observations.size(), 0.8);
							if(k <1)
								k = 1;

							// find the k-nearest neighbours (KNN)
							// havent implimented the taskSize bit yet.

							// get k most recent
							long[] pt = new long[k];
							long[] nt = new long[k];
							for(int i = 0; i< k; i++) {
									pt[i] = ((Long) ( (Vector) observations.get((observations.size() -i -1)) ).get(0)).longValue()  ;
									nt[i] = ((Long) ( (Vector) observations.get((observations.size() -i -1)) ).get(1)).longValue()  ;

								}
							// sort by processing time in ascending order. this is a quicksort
							Arrays.sort(pt);
							Arrays.sort(nt);


							// remove the top and bottom 5%
							int limit =(int) (pt.length*0.05);
							for(int i = 0; i< limit; i++) {
									pt[i] = -1;// beginning
									pt[pt.length-1-i] = -1; // end

									nt[i] = -1;// beginning
									nt[pt.length-1-i] = -1; // end
								}

							int numPoints = 0;	// calculate the average
							long  total = 0;
							long totalNT = 0;
							for(int i = 0; i< pt.length; i++) {
									if(pt[i] != -1) {
											total+=pt[i];
											numPoints++;
										}
									if(nt[i] != -1) {
											totalNT+=nt[i];
										}

								}

							if(numPoints >0) {
									Vector res = new  Vector();
									res.add(0, new Long(total/numPoints));
									res.add(1,  new Long(totalNT/numPoints));
									res.add(2, new  Integer(pt.length));
									return res;
								} else
								return emp ;

						} else {
							// no observations, so an error has occured somewhere.
							return emp ;
						}

				} else {
					// no observations so cant estimate anything
					return emp ;
				}
		}
	}
