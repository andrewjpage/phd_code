/*
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

/**
*Common methods for scheduling.
*This will work as a default scheduler. It is Round robin by default.
*The programmer must overload their own scheduling methods. see 
*the newschedulingalgorithm interface.
*Copyright (C) 2005 Andrew Page
* @author  Andrew Page
*/

public class SchedulerCommon {

		/** This is a hardcoded smoothing function constant currently set to 0.05*/
		protected final double SMOOTHING_CONSTANT = .05;

		/** Hashtable which links to the details of clients based in the scheduler*/
		protected Hashtable clientDetails;

		/** Stores a pointer to the hashtable of problems in the system.*/
		protected Hashtable problems;

		/** stores a pointer to the batch of tasks sent into the scheduler*/
		protected Vector batchQueue;

		/** An array containing the average mflops of each task in the queue*/
		protected double[] taskMflops;

		/** An array of ip addresses of clients */
		protected String[] ips;

		/** This is to ensure that the initiaise method gets called to setup the class. Dont want variables passed into the constructor because
		* client and problem details wont contain anything at that stage.*/
		protected boolean initialised = false;

		/** an array with the estimated finishing time of tasks.*/
		protected double[] estFinTime;

		//constructor
		public SchedulerCommon() {}

		/**
		* @param clients hashtable which links to the main scheduling classes table containing lastest client details and state info
		* @param Problems hashtable which links to problems with lastest details and state info.
		*/
		public void initialise(Hashtable clients, Hashtable Problems) {
			clientDetails  = clients;
			problems = Problems;
			initialised = true;
		}

		/** @return If the initialse method has been called this will return true, otherwise false.*/
		public boolean isInitialised() {
			return initialised;
		}

		/**
		* Default scheduler which must be overloaded. 
		* This is a simple round robin scheduler.
		* @param batch Takes in a Vector containing Long's which correspond to the id numbers of problems
		* Complexity O(n)
		*/
		public void generateSchedule(Vector batch) {

			while(batch.size() != 0) {
					for (Enumeration e = clientDetails.elements() ; e.hasMoreElements() ;) {
							clientInfo ID = (clientInfo)  e.nextElement();
							if(batch.size() == 0 ) {
									// The batch is empty so you can finish
									return;
								}
							(ID.getSchedule()).add(batch.get(0));
							batch.remove(0);


						}
				}
		}

		/**
		* A client has requested a task to proces. This will return an array of problem IDs
		* sorted according to what the scheduler thinks should be processed next.
		*
		* Complexity - worst case is all tasks are scheduled on this processor O(n)
		* @param ip A string containing the ip address of a client
		*/
		public long[] nextUnitProblemID(String ip) {

			try {
					clientInfo ci = (clientInfo) clientDetails.get(ip);
					if(ci.getSchedule() == null) {
							return null;
						}

					Vector sched = (ci.getSchedule());

					//long[] order = new long[sched.size()+problems.size()];
					long[] order = new long[1];
					//for(int i = 0; i< sched.size(); i++) {
					if(sched.size() > 0)
						order[0] = ((Long) sched.get(0)).longValue();
					else
						order = new long[0];
					//	}

					/*
										// include a default list of tasks on the end of the preferred schedule,
										// to avoid clients being idle.
										int i = sched.size();
										for (Enumeration e = problems.elements() ; e.hasMoreElements() ;) {
												Problem prob = (Problem)  e.nextElement();
					 
												order[i] = (prob.getID()).longValue();
												i++;
											}
											*/

					return order;
				} catch(Exception e) {
					System.out.println("error in nextunitproblemid returning default list"+ e);
					e.printStackTrace();
					return defaultList();
				}

		}

		/**
		* Failsafe method. Returns a list of all the problems IDs in the system.
		* This is to minimise the possiblity of a client being idle.
		*
		*Complexity - O(num problems)
		* @return An array of longs containing id numbers of  all problems
		*/
		public long[] defaultList() {
			long[] order = new long[problems.size()];
			int i = 0;
			try {

					for (Enumeration e = problems.elements() ; e.hasMoreElements() ;) {
							Problem prob = (Problem)  e.nextElement();

							order[i] = (prob.getID()).longValue();
							i++;
						}
				} catch(Exception e ) {
					System.out.println("error while generating default list of tasks to be scheduled -> "+e);
					e.printStackTrace();
				}

			// swap around the elements to avoid bottlenecks
			for(int a = 0; a< problems.size(); a++) {
					Random r = new Random();
					int index = r.nextInt(problems.size());
					long t = order[a];
					order[a] = order[index];
					order[index] = t;

				}

			return order;
		}


		/** This method will remove all currently assigned tasks. It removes all assigned tasks from the queues of
		* each processor, then it returns them as a Vector. This vector will then be used as input to the generateSchedule method
		* instead of a new batch of tasks. Thus allowing for preemptive rescheduling. 
		* @return returns a vector containing a list of task IDs in the form of Longs. the first element of the Vector is a Double which contains the ratio of tasks to clients.
		*/
		public Vector removeAssignedTasks() {
			Vector removedTasks = new Vector();
			double ratio = 0;
			long taskCount = 0;
			long clientCount = 0;

			for (Enumeration e = clientDetails.elements() ; e.hasMoreElements() ;) {
					clientInfo ID = (clientInfo)  e.nextElement();

					Vector clientSchedule = ID.getSchedule();
					while(clientSchedule.size() !=0) {
							removedTasks.add(  clientSchedule.remove(0) );
							taskCount ++;
						}
					clientCount++;
				}

			ratio = (1.0*taskCount)/(1.0*clientCount) ;
			removedTasks.add(0,new Double(ratio));

			return removedTasks;
		}

		/** This method will remove all currently assigned tasks. It removes all assigned tasks from the queues of
		* each processor, then it returns them as a Vector. This vector will then be used as input to the generateSchedule method
		* instead of a new batch of tasks. Thus allowing for preemptive rescheduling. 
		* @return returns a vector containing a list of task IDs in the form of Longs. the first element of the Vector is a Double which contains the ratio of tasks to clients.
		*/
		public Vector removeSelectedAssignedTasks(int threshold) {
			Vector removedTasks = new Vector();
			double ratio = 0;
			long taskCount = 0;
			long clientCount = 0;
			long totalNumTasks = 0;

			for (Enumeration e = clientDetails.elements() ; e.hasMoreElements() ;) {
					clientInfo ID = (clientInfo)  e.nextElement();

					Vector clientSchedule = ID.getSchedule();
					totalNumTasks += clientSchedule.size();

					clientCount++;
				}

			for (Enumeration e = clientDetails.elements() ; e.hasMoreElements() ;) {
					clientInfo ID = (clientInfo)  e.nextElement();

					Vector clientSchedule = ID.getSchedule();

					while(clientSchedule.size() >threshold) {
							removedTasks.add(  clientSchedule.remove(0) );
							taskCount ++;
						}

					clientCount++;
				}

			ratio = (1.0*taskCount)/(1.0*clientCount) ;
			removedTasks.add(0,new Double(ratio));

			return removedTasks;
		}



		/**
		* Looks at each client and calculates the current estimated mflops assigned.
		* Since it is an estimate it should be calculated everytime the generate scheduler 
		* method is run, but must be called from an overloaded method.
		* Complexity - O(n*p)
		* @return a Hashtable containting the load of each client, with the key being the ip address (String) and the value being the load (Double)
		*/
		public Hashtable currentAssignedLoad() {
			// I'm calculating this everytime the scheduler is run because its an estimate
			// and the variables it relys on will change as time goes on.
			Hashtable clientLoad = new Hashtable();
			try {
					for (Enumeration e = clientDetails.elements() ; e.hasMoreElements() ;) {
							clientInfo ID = (clientInfo)  e.nextElement();

							double sumLoad = 0.0;
							Vector clientSchedule = ID.getSchedule();

							for(int i=0; i< clientSchedule.size(); i++) {
									sumLoad += ((Problem) problems.get((Long)clientSchedule.get(i))).getAvgMflops();

								}
							clientLoad.put(ID.getIP(), new Double(sumLoad));
						}
				} catch (Exception e ) {
					System.out.println("error finding current assigned load information -> " + e);
					e.printStackTrace();
				}
			return clientLoad;
		}


		/**
		* This will create an array of current assigned task load. 
		* It does not include the task currently being processed.
		* It stores the ip address in one array and the corresponding load (in mflops) in another array as a double.
		* Complexity = O(p)
		*/
		protected void createTaskArray() {
			/*Complexity - O(num clients)*/
			try {
					Hashtable values = currentAssignedLoad() ;
					int i = 0;
					taskMflops = new double[values.size()];
					ips = new String[values.size()];
					for (Enumeration e = values.keys() ; e.hasMoreElements() ;) {
							String id = (String) e.nextElement();

							double cur = ((Double)values.get(id)).doubleValue();
							taskMflops[i] = cur;
							ips[i] = id;
							i++;
						}
				} catch(Exception e) {
					System.out.println("Error creating task array -> "+e);
					e.printStackTrace();
				}

		}

		/**
		*Creates an array which contains the estimated finishing time of each client in seconds.
		* Stored in estFinTime[i] as a double
		* Complexity = O(n*p)
		*/
		protected void estFinishingTime() {
			try {
					createTaskArray();
					estFinTime = new double[taskMflops.length];


					for(int i =0; i< ips.length; i++) {
							estFinTime[i] = ((clientInfo)clientDetails.get(ips[i])).getEstFinishingTime();
							double speed =  ((clientInfo)clientDetails.get(ips[i])).getMflops();
							if(speed > 0)
								estFinTime[i] += (taskMflops[i])/speed;

						}
				} catch(Exception e ) {
					System.out.println("error getting estimated finishing time -> " +e);
					e.printStackTrace();
				}

		}

		public void printLoadTime() {
			String clientProc = "";
			clientProc = "clientProc " ;


			String est = "";
			est = "load " + System.currentTimeMillis() + " " ;
			estFinishingTime() ;

			for(int i = 0; i< estFinTime.length ; i++) {
					est += " "+ estFinTime[i];
				}
			System.out.println(est + ";");


			double[] ST = new double[clientDetails.size()];
			String estLoad = "";
			estLoad = "ldtime "+  System.currentTimeMillis() + " " ;



			int g = 0;
			// calculate the StartTime of each processor ST
			for (Enumeration e = clientDetails.elements() ; e.hasMoreElements() ;) {
					clientInfo p = (clientInfo)  e.nextElement();

					clientProc = clientProc + " "+ p.getclientProcTime();

					Vector schd = p.getSchedule();
					//                    ST[g] = 0;

					// calculate when the client will finish what its currently doing
					long lastTime= p.getLastTime();
					// time in milliseconds when last task sent for processing
					double elapsed = (System.currentTimeMillis() - lastTime)/1000 ;

					double curFin = p.getEstFinishingTime() - elapsed;

					if(curFin < 0)
						ST[g] = 0;
					else
						ST[g] = curFin;

					for(int s =0; s< schd.size(); s++) {
							Vector estTime = p.getEstimatedTime( (Long)(schd.get(s)), 0.0);
							double extime = (((Long)estTime.get(0)).longValue()/1000) + (((Long)estTime.get(1)).longValue()/1000);
							if(extime ==0.0) {
									ST[g]+=((Problem)problems.get((Long) schd.get(s))).getAvgMflops()/p.getMflops();
								} else {
									ST[g]+= extime;
								}
						}
					estLoad+= " "+ ST[g];
					g++;
				}

			System.out.println(estLoad + ";");
System.out.println(clientProc + ";");


		}

		/**
		*  Take in a vector containing task ids (Longs), such as a batch and sort them in ascending order
		*   Using quicksort algorithm.
		* worst O(n^2)
		* average O(nlogn)
		* best O(n)
		* @param taskList takes in a task list of Longs which correspond to the id numbers of problems
		* @return A Vector containing a sorted taskList of Longs
		*/
		protected Vector sortTasksAscending(Vector taskList) {

			sortVector(taskList);
			return taskList;

		}

		/** Takes in a vector containing task IDs (Longs) and sorts them by megaflops, in decending order.
		* @param v The vector is directly modified so there is no need to return anything.*/
		public  void sortVector(Vector v) {
			try {
					quickSortVector(0, v.size() - 1, v);
				} catch(Exception e) {
					System.out.println("Error while sorting list of tasks" + e);
					e.printStackTrace();
				}
		}

		/** Performs quicksort on a vector v. Each element of v is a Double*/
		private void quickSortVector(int head, int tail, Vector v) {
			if (head < tail) {
					int pivotIndex = (head + tail)/2;
					Object pivot = v.elementAt(pivotIndex);
					int i = head - 1;
					int j = tail + 1;
					do {
							do i++; while (! leq(pivot, v.elementAt(i)));
							do j--; while (! leq(v.elementAt(j),pivot));
							if (i < j) swap(i, j,v);
						} while (i < j);
					if (i == j) {
							quickSortVector(head, j - 1,v);
							quickSortVector(i + 1, tail,v);
						} else {
							quickSortVector(head, j,v);
							quickSortVector(i, tail,v);
						}
				}
		}


		/** Takes in 2 task ids, which are Longs, finds the assosiated mflops  and returns true if a <=b */
		private boolean leq (Object a, Object b) {

			double mflopsA =  ((Problem)problems.get((Long) a)).getAvgMflops();
			double mflopsB = ((Problem)problems.get((Long) a)).getAvgMflops();
			if(mflopsA <= mflopsB)
				return true;
			else
				return false;
		}


		/** take in a vector and swap 2 elements. Works on objects so is generic*/
		private   void swap (int i, int j, Vector v) {
			Object obj = v.elementAt(i);
			v.setElementAt(v.elementAt(j), i);
			v.setElementAt(obj, j);
		}

		/**
		*  Take in a vector containing task ids (Longs), such as a batch and sort them in descending order
		*   It utilises sortTasksDecending and reverses the result.
		* @param taskList takes in a task list of Longs which correspond to the id numbers of problems
		* @return A Vector containing a sorted taskList of Longs
		*/
		protected Vector sortTasksDecending(Vector taskList) {
			// reverse list - complexity n + complexity of other sorting alg
			Vector sorted = sortTasksAscending(taskList);
			int s =taskList.size();

			for(int i = 0; i < s; i++) {
					sorted.add(sorted.remove(0));
				}

			return sorted;

		}
		/** print out info about the state of the scheduler.*/
		public String toString() {
			String stats = new String();
			if(initialised)
				stats = "Scheduler is initialised\n";
			else
				stats = "Scheduler is not initialised\n";

			for (Enumeration e = clientDetails.elements() ; e.hasMoreElements() ;) {
					clientInfo ID = (clientInfo)  e.nextElement();

					stats += ID.toString();

				}

			return stats;
		}

		/** Remove a problem id from all of the client queues*/
		public void removeProblem(long id) {
			try {
					Long probID = new Long(id);

					for (Enumeration e = clientDetails.elements() ; e.hasMoreElements() ;) {
							clientInfo ID = (clientInfo)  e.nextElement();

							Vector clientSchedule = ID.getSchedule();
							int schedMax = clientSchedule.size();

							int c = 0;
							while(  clientSchedule.indexOf(probID)     > -1  && c < schedMax  ) {

									int index = clientSchedule.indexOf(probID);
									clientSchedule.remove(index) ;
									c++;
								}
						}
				} catch(Exception e) {
					System.out.println("Error while removing a problem" + e);
					e.printStackTrace();
				}
		}
	}

