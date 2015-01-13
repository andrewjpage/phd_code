/** Andrew Page 1/November/05
* Dynamic level scheduling, implimented from
* IEEE trans parallel and dist sys vol 13 no.3 March 2002
* Matching and scheduling algorithms for minimizing exectuion time and 
failure probabliyt of applications in heterogensous computing
*/
/*
Copyright (C) 2005  Andrew J. Page
apage@cs.nuim.ie
 
This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.
 
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
 
You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/


import java.util.*;
import java.lang.*;
import java.io.*;
import java.security.*;
import java.math.*;


/** shows how to invoke the annealing code using an example from JSimul */
public class DLScheduler extends SchedulerCommon implements
	NewSchedulingAlgorithm  {


		protected double[] tasks; // task MFLOPs required,
		protected int numTasks; // the number of tasks to be processed
		protected  int numProcessors;
		protected  double[][] processor; // 0 mflop/s of processor, 1 is MFLOPs current load
		protected String[] ips;


		public  DLScheduler() {
			super();
		}

		public void generateSchedule(Vector batch) {
			// sliding window to limit the number of parameters to limit the running time to a reasonable amount
			int window = 50;
			Vector win = new Vector();


			while(batch.size() !=0) {
					scheduleWindow(batch);
					run();
				}


		}

		public void scheduleWindow(Vector batch) {
			Integer sync = new Integer(0);
			synchronized(sync) {
					batchQueue = batch;

					// need to reinitialise variables here.
					numTasks = batch.size();
					tasks = new double[numTasks];
					numProcessors = clientDetails.size();
					processor = new double[numProcessors][2];
					ips = new String[numProcessors];

					if(numProcessors <= 0|| numTasks <=0)
						return;

					for(int i = 0; i<numTasks ; i++) {
							tasks[i] =  ((Problem)problems.get((Long) batch.get(i))).getAvgMflops();
							if(tasks[i] <= 0.0)
								tasks[i] = 1;
						}

					int i = 0;
					for (Enumeration e = clientDetails.elements() ; e.hasMoreElements() ;) {
							clientInfo p = (clientInfo)  e.nextElement();

							processor[i][0]= p.getMflops() ;
							ips [i] = p.getIP();
							i++;
						}

					Hashtable p = currentAssignedLoad();
					for(int b=0; b< numProcessors; b++) {
							if(processor[b][0]<= 0.0) {
									processor[b][0] = 1;
								}
							processor[b][1] = ((Double) p.get( ips[b])).doubleValue();

						}

					run();
				}
		}

		private void run( ) {

			// find processor and task pair with largest DLS
			int largestTaskIndex = 0;
			int largestProcIndex = 0;
			double largestDLS = 0;
			for(int i =0; i< numTasks; i++) {

					for(int p = 0; p< numProcessors ; p++) {
							double curDL = dynamicLevel(i,p);
							if(curDL > largestDLS) {
									largestProcIndex = p;
									largestDLS = curDL;
									largestTaskIndex = i;
								}
						}

				}

			assignTask(largestTaskIndex, largestProcIndex);


		}

		/** Will return the dynamic level for a given task and processor pair*/
		private double dynamicLevel(int taskID, int procID) {
			// dl = execution time - start time + machine speed difference

			// execution time
			double executionTime = 0;
			if(processor[procID][0]==0)
				executionTime = 0;
			else
				executionTime = tasks[taskID]/processor[procID][0];

			// start time
			double startTime = 0 ;
			if(processor[procID][0] == 0)
				startTime=0;
			else
				startTime = processor[procID][1]/processor[procID][0];

			// machine speed differenece - median execution time of task accross all machines
			// less execution time on this processor
			double medianExecutionTime = 0;
			for(int i = 0 ; i< numProcessors; i++) {
					double curExe = 0;
					if(processor[i][0] ==0)
						curExe = 0;
					else
						curExe = tasks[taskID]/processor[i][0];

					medianExecutionTime = (medianExecutionTime*i + curExe)/(i+1);
				}
			double machineDiff = medianExecutionTime - executionTime;

			return (executionTime -startTime + machineDiff);
		}


		private void assignTask(int taskID, int procID) {

			// calculate the processor to assign the task to
			int proc = procID;
			clientInfo tmpc = (clientInfo) clientDetails.get(ips[proc]) ;
			Vector tmpSched = tmpc.getSchedule();
			tmpSched.add(batchQueue.get( taskID )  );


			// now remove the task from the batch
			batchQueue.remove( taskID );


		}
	}



