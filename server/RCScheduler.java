/** Andrew Page 20/November/05
* Relative cost scheduler
* A high-perfomance mapping algorithm for heterogeneous computing systems
* Min-You Wu and Wei Shu
* IPDPS San Fran April 2001
 
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
public class RCScheduler extends SchedulerCommon implements
	NewSchedulingAlgorithm  {



		private int numTasks; // the number of tasks to be processed
		private  int numProcessors;
		private  double[][] processor; // 0 mflop/s of processor, 1 is MFLOPs current load
		private String[] ips;
		private double[][] ETC;
		private double[] avgETC;
		private double[][] staticRC;
		private boolean [] scheduled;
		private double [] dynamicRC ;

		public  RCScheduler() {
			super();
		}

		public void generateSchedule(Vector batch) {
			Integer sync = new Integer(0);
			synchronized(sync) {
					batchQueue = batch;

					// need to reinitialise variables here.
					numTasks = batch.size();

					numProcessors = clientDetails.size();
					processor = new double[numProcessors][2];
					ips = new String[numProcessors];

					if(numProcessors <= 0|| numTasks <=0)
						return;

					int i = 0;

					for (Enumeration e = clientDetails.elements() ; e.hasMoreElements() ;) {
							clientInfo p = (clientInfo)  e.nextElement();

							processor[i][0]= p.getMflops() ;
							ips [i] = p.getIP();
							i++;
						}


					Hashtable procLoad = currentAssignedLoad();



					/** ETC matrix*/
					ETC = new double[numTasks][numProcessors];
					avgETC = new double[numTasks];
					scheduled = new boolean[numTasks];
					staticRC = new double[numTasks][numProcessors];
					dynamicRC = new double[numTasks];

					double[][] ct = new double[numTasks][numProcessors];
					double alpha = 0.5 ;// used to control the effect of the static relative cost

					for(int t = 0; t< numTasks;t++) {
							scheduled[t] = false;
							for(int p = 0 ; p< numProcessors; p++) {

									Long probID = ((Long)batch.get(t));
									Vector KNNest = ((clientInfo) clientDetails.get(ips[p])).getEstimatedTime(probID ,1.0);
									long knnProcTime = ((Long)KNNest.get(0)).longValue() ;

									ETC[t][p]=(knnProcTime/1000 );
									avgETC[t] = ( (avgETC[t]*p) + ETC[t][p])/(p+1);

									ct[t][p] = ((Double) procLoad.get( ips[p])).doubleValue()/processor[p][0];

								}
						}

					/** Calculate the static relative cost for each task processor pairing*/

					for(int t = 0; t< numTasks ; t++) {
							for(int p = 0; p< numProcessors; p++) {
									if(avgETC[t]  == 0)
										staticRC[t][p] = 0;
									else
										staticRC[t][p]  = ETC[t][p]/avgETC[t] ;
								}
						}


					// go through each unscheduled task
					for(int t = 0; t< numTasks; t++) {
							int BAk = 0;
							int Ak = 0;

							for(int k = 0; k<numTasks ; k++ ) {
									if(scheduled[k] == false) {
											double ctAvg = 0;
											double ctSum = 0;
											double minct = 0;
											int Bi = 0;


											for(int p = 0; p< numProcessors; p++) {
													ctSum +=ct[k][p] ;
													if(minct ==0 || p == 0 || ct[k][p] < minct) {
															minct = ct[k][p];
															Bi = p;
														}
												}
											ctAvg = ctSum/numProcessors;
											dynamicRC[k] = minct/ctAvg;
											double minAk = 0;
											double cost = Math.pow(staticRC[k][Bi],alpha)  + dynamicRC[k]  ;

											if(minAk == -1 || k == 0 || cost <minAk ) {
													minAk = cost;
													BAk = Bi;
													Ak = k;
												}

										}
								}
							//System.out.println(t);
							scheduled[Ak] = true;
							assignTask(Ak, BAk);

							for(int c = 0; c< numTasks ; c++) {
									ct[c][BAk] +=ETC[c][BAk] ;
								}

						}


				}
		}


		private void assignTask(int taskID, int procID) {

			// calculate the processor to assign the task to
			int proc = procID;
			clientInfo tmpc = (clientInfo) clientDetails.get(ips[proc]) ;
			Vector tmpSched = tmpc.getSchedule();
			tmpSched.add(batchQueue.get( taskID )  );


			// now remove the task from the batch
			//batchQueue.remove( taskID );


		}
	}



