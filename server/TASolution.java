import org.coinor.opents.*;


public class TASolution extends SolutionAdapter {
		public int[] offset;
		public int numProcessors;



		public TASolution() {} // Appease clone()

		public TASolution( int numTasks, int numP , double[][] processor, double[] tasks) {
			numProcessors = numP;

			// assign tasks with round robin first
			offset = new int[numTasks];
			//round robin
			//	for(int i =0; i< numTasks; i++) {
			//			offset[i] = (i%numP);
			//		}

			// current load on processors
			double[] makespan= new double[numProcessors];  // in seconds
			for(int i = 0; i< numProcessors ; i++) {
					if(processor[i][0] > 0.0)
						makespan[i] = processor[i][1]/processor[i][0];
					else
						makespan[i]  = 0;
				}


			for(int k = 0; k< numTasks ; k++) {
					// find earliest finishing time
					int earliest = 0;
					double smallest = 0;
					for(int j = 0; j< numProcessors; j++) {
							double finTime  =  makespan[j] + tasks[k]/processor[j][0];
							if(j ==0 || finTime < smallest) {
									smallest  = finTime;
									earliest = j;
								}
						}
					makespan[earliest] += tasks[k]/processor[earliest][0];
					offset[k] = earliest;
				}
		}   // end constructor




		public Object clone() {
			TASolution copy = (TASolution)super.clone();
			copy.offset = (int[])this.offset.clone();
			return copy;
		}   // end clone


		public String toString() {
			String output = "";
			for( int i = 0; i < offset.length; i++ )
				output+= " "+ offset[i] ;


			return output;
		}   // end toString

	}   // end class MySolution
