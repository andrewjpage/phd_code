

import net.sourceforge.jannealer.ObjectiveFunction;

public final class SchedulingObjectiveFunction implements ObjectiveFunction {
		int numTasks ;
		double[] tasks ;
		int numProcessors;
		double[][] processor ;


		SchedulingObjectiveFunction() {}

		SchedulingObjectiveFunction(int nt, int np, double[] t, double[][] p) {
			numTasks = nt;
			numProcessors = np;
			tasks = t;
			processor = p;
		}

		public int getNdim() {
			// number of tasks
			return numTasks;
		}
		public double distance(double[] p) {

			// current load on processors
			double[] makespan= new double[numProcessors];  // in seconds
			for(int i = 0; i< numProcessors ; i++) {
					if(processor[i][0] > 0.0)
						makespan[i] = processor[i][1]/processor[i][0];
					else
						makespan[i]  = 0;
				}

			// assign a task to a processor
			for(int j = 0; j< numTasks; j++) {
					int proc = (int) p[j];
					proc = Math.abs(proc);
					if(proc <0)
						proc = 0;
					else if(proc >=numProcessors)
						proc  = numProcessors -1;

					makespan[proc] += tasks[j]/processor[proc][0];
				}

			// find out over all makespan
			double largestMakespan = 0;
			for(int k = 0 ; k< numProcessors ; k++) {
					if( largestMakespan < 	makespan[k])
						largestMakespan=makespan[k] ;
				}

			return  largestMakespan;
		}
	}
