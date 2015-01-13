// Simluated annealing - based on http://jannealer.sourceforge.net/
// to compile this make sure the jar file is added as an extension in jedit

//import net.sourceforge.jannealer.AnnealingScheme;
import net.sourceforge.jannealer.*;
import net.sourceforge.jannealer.test.Util;
import java.util.*;
import java.lang.*;
import java.io.*;
import java.security.*;
import java.math.*;


/** shows how to invoke the annealing code using an example from JSimul */
public class testSAScheduler extends SchedulerCommon implements NewSchedulingAlgorithm  {


		protected double[] tasks; // task MFLOPs required,
		protected int numTasks; // the number of tasks to be processed
		protected  int numProcessors;
		protected  double[][] processor; // 0 mflop/s of processor, 1 is MFLOPs current load
		protected String[] ips;

		public static void main(String[] args) {
			testSAScheduler t =new testSAScheduler();


		}


		public testSAScheduler() {
			super();

			for (int i = 50; i< 2000; i+=10) {
					//System.out.println("\n\n elements"+ i+ " proc " + 30);
					double[] a = new double[i];
					for(int b = 0; b< i; b++) {
							a[b] = b+1;
						}
					tasks = a;
					numTasks = i;
					double[][] pp  = {{52,32},{23,34},{45,45},{56,67},{67,56},{56,56},{67,67},{34,34},{45,23},{67,78},{42,62},{36,36},{47,56},{54,45},{24,234},{234,2343},{54,23},{72,56},{20,5},{50,5},{100,80},{90,80},{50,5},{50,5},{50,5},{50,5},{50,5},{50,5},{50,5},{50,5},{50,5},{50,5},{50,5},{50,5},{50,5},{50,5},{50,5},{50,5},{50,5},{50,5},{50,5},{50,5}};
					processor = pp;
					numProcessors = 38;
					long b4 = System.currentTimeMillis() ;
					run( );
					long after = System.currentTimeMillis() ;
					System.out.println("schedtime "+ (after-b4) + " elements "+ i);

				}

		}

		private void run( ) {

			AnnealingScheme scheme = new AnnealingScheme();
			//System.out.println("test------------------------------");
			ObjectiveFunction fn=new SchedulingObjectiveFunction(numTasks,numProcessors,tasks,processor);

			scheme.setFunction(fn);

			// assign tasks with round robin first
			double totalMflops = 0;
			double[] offset = new double[numTasks];
			for(int i =0; i< numTasks; i++) {
					offset[i] = 1.0*(i%numProcessors);
					//System.out.println(""+ offset[i]);
					totalMflops += tasks[i];
				}

			double totalProcMflops = 0;
			for(int i=0; i< numProcessors; i++) {
					totalProcMflops += processor[i][0];
					totalMflops += processor[i][1];
				}

			double tolerance = 0;
			if(totalProcMflops ==0) {
					tolerance  = 0;
				} else {
					tolerance=(totalMflops / totalProcMflops)*1.5;
				}

			scheme.setSolution(offset);

			// figured out these values using ConfigureAnnealing
			scheme.setCoolingRate(40);
			scheme.setTemperature(1e+6);
			//eme.setTolerance(tolerance);// a percentage of the  theoretical optimum
			scheme.setIterations(150);

			System.out.println("Starting search");

			/* figure out where the difference is at a minimum */
			scheme.anneal(20000);

			Util.printSolution(scheme);
			//assignTasks() ;

			// print out "distance" to solution to build a bit of confidence in the result
			System.out.println("Distance " + fn.distance(scheme.getSolution()));


		}


	}



