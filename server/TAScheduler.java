// Simluated annealing - based on http://jannealer.sourceforge.net/
// to compile this make sure the jar file is added as an extension in jedit

import org.coinor.opents.*;
import java.util.*;
import java.lang.*;
import java.io.*;
import java.security.*;
import java.math.*;


/** shows how to invoke the annealing code using an example from JSimul */
public class TAScheduler extends SchedulerCommon implements NewSchedulingAlgorithm  {


		protected double[] tasks; // task MFLOPs required,
		protected int numTasks; // the number of tasks to be processed
		protected  int numProcessors;
		protected  double[][] processor; // 0 mflop/s of processor, 1 is MFLOPs current load
		protected String[] ips;


		public  TAScheduler() {
			super();
		}

		public void generateSchedule(Vector batch) {
			// sliding window to limit the number of parameters to limit the running time to a reasonable amount
			//int window = 70;
			int window =batch.size();
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

			TAObjectiveFunction objFunc = new TAObjectiveFunction( numTasks, numProcessors, tasks, processor );
			TASolution initialSolution  = new TASolution(  numTasks, numProcessors, processor, tasks );
			TAMoveManager   moveManager = new TAMoveManager();
			TabuList         tabuList = new SimpleTabuList( 10 ); // In OpenTS package

			// Create Tabu Search object
			TabuSearch tabuSearch = new SingleThreadedTabuSearch(
			                            initialSolution,
			                            moveManager,
			                            objFunc,
			                            tabuList,
			                            new BestEverAspirationCriteria(), // In OpenTS package
			                            false ); // maximizing = yes/no; false means minimizing

			// Start solving
			//	for(int i = 0; i< 10; i++) {
			tabuSearch.setIterationsToGo( 500 );
			tabuSearch.startSolving();
			// Show solution
			TASolution best = (TASolution)tabuSearch.getBestSolution();
			System.out.println( "Best Solution:\n" + best + "");
			double[]distance = objFunc.evaluate(best, new TASwapMove(0,0));
			System.out.println("makespan: "+ distance[0] + "\t"  + distance[1]);
			//	}



			assignTasks(best.offset);


		}

		private void assignTasks(int[] solution) {


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



