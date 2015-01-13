// Simluated annealing - based on http://jannealer.sourceforge.net/
// to compile this make sure the jar file is added as an extension in jedit

import org.coinor.opents.*;
import java.util.*;
import java.lang.*;
import java.io.*;
import java.security.*;
import java.math.*;


/** shows how to invoke the annealing code using an example from JSimul */
public class testTAScheduler extends SchedulerCommon implements NewSchedulingAlgorithm  {


		protected double[] tasks; // task MFLOPs required,
		protected int numTasks; // the number of tasks to be processed
		protected  int numProcessors;
		protected  double[][] processor; // 0 mflop/s of processor, 1 is MFLOPs current load
		protected String[] ips;

		public static void main(String[] args) {
			testTAScheduler t =new testTAScheduler();


		}


		public testTAScheduler() {
			super();
			Random rand = new Random();
			for (int i = 1; i< 2000; i++) {
					//System.out.println("\n\n elements"+ i+ " proc " + 30);
					double[] a = new double[i];
					for(int b = 0; b< i; b++) {
							a[b] = rand.nextInt(b+1) + 1;
						}
					tasks = a;
					numTasks = i;
					double[][] pp  = {{52,32},{23,34},{145,45},{56,67},{67,56},{156,36},{67,67},{34,34},{45,23},{67,78},{42,62},{36,36},{47,56},{54,45},{24,234},{24,23},{54,23},{72,56},{20,5},{50,5},{100,80},{90,80},{50,95},{50,50}};
					processor = pp;
					numProcessors = 20;
					long b4 = System.currentTimeMillis() ;
					run( );
					long after = System.currentTimeMillis() ;
					System.out.println("\nschedtime "+ (after-b4) + " elements "+ i);

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
			tabuSearch.setIterationsToGo( 50 );
			tabuSearch.startSolving();
			// Show solution
			TASolution best = (TASolution)tabuSearch.getBestSolution();
			System.out.println( "Best Solution:\n" + best + "");
			double[]distance = objFunc.evaluate(best, new TASwapMove(0,0));
			System.out.println("makespan: "+ distance[0] + "\t"  + distance[1]);
			//	}



			//assignTasks(tour);

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



