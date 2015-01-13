
import org.coinor.opents.*;
import java.util.*;
import java.lang.*;
import java.io.*;
import java.security.*;
import java.math.*;


public class TASwapMove implements Move {
		public int taskID;
		public int movement;

		public TASwapMove( int customer,  int movement ) {
			this.taskID = customer;
			this.movement = movement;
		}   // end constructor


		public void operateOn( Solution soln) {
			int[] offset = ((TASolution)soln).offset;
			int np = ((TASolution)soln).numProcessors;

			// randomly select a processor
			//int range = Math.abs(movement)%np;

			//Random rand = new Random();
			//int proc =rand.nextInt(range);
			int proc = offset[taskID];

			proc +=movement;

			if(proc >=0 )
				proc = proc % np;
			else
				proc = (np - 1 - ((-1*proc)%np))%np;

			offset[taskID] = proc;


		}   // end operateOn


		/** Identify a move for SimpleTabuList */
		public int hashCode() {
			return taskID;
		}   // end hashCode

	}   // end class MySwapMove
