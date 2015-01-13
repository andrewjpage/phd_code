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
* An interface class to define the general funcations that should be implemented by a scheduling algorithm.
* This is an updated version which has a simplier interface to allow for a plug and play architecture.
 * Copyright (C) 2005 Andrew Page
 */
public interface NewSchedulingAlgorithm {
		/**
		* Send in the intial hash of clients and problems to initialise the scheduler.
		* This allows the schdeuler from then on to access these datastructures.
		* It must be called from the Scheduler class before the scheduling can begin
		*/
		public void initialise(Hashtable clients, Hashtable Problems);

		/**
		* take in a batch of tasks in the form of Longs which correspond to the problem ID
		*/
		public void generateSchedule(Vector batch);

		/**
		* A client has requested a task to proces. This will return an array of problem IDs
		* sorted according to what the scheduler thinks should be processed next
		* This is a soft list.
		* The Scheduler class then checks to see if the client can actually process the tasks
		*/
		public long[] nextUnitProblemID(String ip);

		/**
		* Returns true if the initialise method has been run.
		*/
		public boolean isInitialised();

		/**
		* Failsafe method. Returns a list of all the problems IDs in the system.
		* This is to minimise the possiblity of a client being idle
		*/
		public long[] defaultList() ;

		/** This method will remove all currently assigned tasks. It removes all assigned tasks from the queues of
		* each processor, then it returns them as a Vector. This vector will then be used as input to the generateSchedule method
		* instead of a new batch of tasks. Thus allowing for preemptive rescheduling. 
		* @return returns a vector containing a list of task IDs in the form of Longs. the first element of the Vector is a Double which contains the ratio of tasks to clients.
		*/
		public Vector removeAssignedTasks() ;

		/** This method will remove all currently assigned tasks. It removes all assigned tasks from the queues of
		* each processor, then it returns them as a Vector. This vector will then be used as input to the generateSchedule method
		* instead of a new batch of tasks. Thus allowing for preemptive rescheduling. 
		* @return returns a vector containing a list of task IDs in the form of Longs. the first element of the Vector is a Double which contains the ratio of tasks to clients.
		*/
		public Vector removeSelectedAssignedTasks(int threshold) ;

		/**
		* Looks at each client and calculates the current estimated mflops assigned.
		* Since it is an estimate it should be calculated everytime the generate scheduler 
		* method is run, but must be called from an overloaded method.
		* Complexity - O(n*p)
		* @return a Hashtable containting the load of each client, with the key being the ip address (String) and the value being the load (Double)
		*/
		public Hashtable currentAssignedLoad();


		public void printLoadTime();

		/** Remove a problem id from all of the client queues*/
		public void removeProblem(long id) ;
	}



