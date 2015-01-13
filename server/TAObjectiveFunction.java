
import org.coinor.opents.*;


public class TAObjectiveFunction implements ObjectiveFunction {
		int numTasks ;
		double[] tasks ;
		int numProcessors;
		double[][] processor ;



		TAObjectiveFunction(int nt, int np, double[] t, double[][] p) {
			numTasks = nt;
			numProcessors = np;
			tasks = t;
			processor = p;
		}


		public double[] evaluate( Solution solution, Move move ) {
			int[] p = ((TASolution)solution).offset;
			int t = 0;
			int m = 0;
			int mvproc= 0;

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
					//                             t = ((TASwapMove)move).taskID;
					//                             m = ((TASwapMove)move).movement;

					if(move != null) {
							t = ((TASwapMove)move).taskID;
							m = ((TASwapMove)move).movement;

							if(t==j) {
									//t = ((TASwapMove)move).taskID;
									//m = ((TASwapMove)move).movement;
									mvproc= p[t];

									mvproc +=m;

									if(mvproc >=0 )
										mvproc =mvproc % numProcessors;
									else
										mvproc = (numProcessors - 1 - ((-1*mvproc)%numProcessors))%numProcessors;
									makespan[mvproc] += tasks[j]/processor[mvproc][0];
								} else {
									int proc = (int) p[j];
									proc = Math.abs(proc);
									if(proc
									    <0)
										proc
										=
										    0;
									else
										if(proc
										    >=numProcessors)
											proc
											=
											    numProcessors
											    -1;
									makespan[proc]
									+=
									    tasks[j]/processor[proc][0];

								}
						} else {

							int proc = (int) p[j];
							proc = Math.abs(proc);
							if(proc <0)
								proc = 0;
							else if(proc >=numProcessors)
								proc  = numProcessors -1;
							makespan[proc] += tasks[j]/processor[proc][0];
						}
				}

			// find out over all makespan


			double largestMakespan = 0;
			int bigIndex = 0;
			for(int k = 0 ; k< numProcessors ; k++) {
					if( largestMakespan <   makespan[k]) {
							largestMakespan=makespan[k] ;
							bigIndex  = k;
						}
				}



			return new double[]{ largestMakespan, bigIndex*1.0 };


		}


	}   // end evaluate


