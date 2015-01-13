// Simluated annealing - based on http://jannealer.sourceforge.net/
// to compile this make sure the jar file is added as an extension in jedit
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

//import net.sourceforge.jannealer.AnnealingScheme;
import net.sourceforge.jannealer.*;
import net.sourceforge.jannealer.test.Util;
import java.util.*;
import java.lang.*;
import java.io.*;
import java.security.*;
import java.math.*;


/** shows how to invoke the annealing code using an example from JSimul */
public class SAScheduler extends SchedulerCommon implements NewSchedulingAlgorithm  {


		protected double[] tasks; // task MFLOPs required,
		protected int numTasks; // the number of tasks to be processed
		protected  int numProcessors;
		protected  double[][] processor; // 0 mflop/s of processor, 1 is MFLOPs current load
		protected String[] ips;


		public  SAScheduler() {
			super();
		}

		public void generateSchedule(Vector batch) {
			// sliding window to limit the number of parameters to limit the running time to a reasonable amount
			int window = 50;
			Vector win = new Vector();

			while(batch.size() !=0) {
					while(win.size() < window && batch.size() !=0) {
							win.add(batch.remove(0));
						}
					scheduleWindow(win);
					win = new Vector();
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

			AnnealingScheme scheme = new AnnealingScheme();

			ObjectiveFunction fn=new SchedulingObjectiveFunction(numTasks,numProcessors,tasks,processor);

			scheme.setFunction(fn);

			// assign tasks with round robin first
			double[] offset = new double[numTasks];
			for(int i =0; i< numTasks; i++) {
					offset[i] = 1.0*(i%numProcessors);
				}

			scheme.setSolution(offset);

			if(numProcessors <= 1) {
					assignTasks(offset);
					//System.out.println("default RR assigned ");
					return;
				}

			// figured out these values using ConfigureAnnealing
			scheme.setCoolingRate(16);// between 1 and 100
			scheme.setTemperature(1e+6);
			scheme.setIterations(120);


			//System.out.println("Starting search");

			/* figure out where the difference is at a minimum */
			scheme.anneal(10000);

			//Util.printSolution(scheme);
			double[] solution = scheme.getSolution();
			assignTasks(solution) ;

			// print out "distance" to solution to build a bit of confidence in the result
			System.out.println("Distance " + fn.distance(scheme.getSolution()) + "  "+ numTasks + "  "+numProcessors );
		}

		private void assignTasks(double[] solution) {


			for(int i = 0; i< numTasks; i++) {

					// calculate the processor to assign the task to
					int proc = (int) solution[i];
					proc = Math.abs(proc);

					if(proc <0)
						proc = 0;
					else if(proc >=numProcessors)
						proc  = numProcessors -1;


					clientInfo tmpc = (clientInfo) clientDetails.get(ips[proc]) ;
					Vector tmpSched = tmpc.getSchedule();


					tmpSched.add(batchQueue.get( i )  );


				}
		}
	}



