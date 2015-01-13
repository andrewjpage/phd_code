/*
An implementation of an exponential smoothing based scheduler (based on comparing the amount of processing time each problem recevies)
Copyright (C) 2003  Thomas Keane
 
This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.
 
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
*/
import java.util.*;

public class EMWAScheduler implements SchedulingAlgorithm {
		private final double SMOOTHING_CONSTANT = .4;
		//private final double SMOOTHING_CONSTANT = .01;
		//private final double SMOOTHING_CONSTANT = .6;

		private Vector problems;

		//constructor
		public EMWAScheduler() {
			problems = new Vector();
		}

		//updates the counters with a details of a received result set
		public void update( Long ID, long procTime ) {
			//update the value of all problems
			long totalProcessing = 0;
			double totalPriorities = 0.0;
			for( int i = 0; i < problems.size(); i ++ ) {
					IDInfo problem = (IDInfo) problems.get( i );
					//update the problem info corresponding to the ID
					if( problem.getID() == ID.longValue() ) {
							problem.addTime( procTime );
						}

					totalProcessing += problem.getTime();
					totalPriorities += problem.getPriority();
				}

			//now update the value of all problems
			for( int i = 0; i < problems.size(); i ++ ) {
					IDInfo problem = (IDInfo) problems.get( i );
					double oldValue = problem.getValue(); //old servicing value
					double currentProcTime = 0.0 + problem.getTime(); //how much processing time this problem has received since it started
					double currentValue = ( problem.getPriority() / totalPriorities ) - ( currentProcTime / totalProcessing ); //compute new servicing value
					double newValue = SMOOTHING_CONSTANT * currentValue + ( 1 - SMOOTHING_CONSTANT ) * oldValue;
					problem.setValue( newValue );
				}
		}

		//return an ordered list to tell the system which problem to pick for next unit
		public long[] nextUnitProblemID() {
			//get an ordered list of ID's - topmost is most underserviced problem
			long[] ordered = new long[ problems.size() ];
			sortList(); //sort the list by their values

			for( int i = 0; i < problems.size(); i ++ ) {
					ordered[ i ] = ( (IDInfo) problems.get( i ) ).getID();
				}

			return ordered;
		}

		//problem being removed from system
		public void removeProblem( Long ID ) {
			for( int i = 0; i < problems.size(); i ++ ) {
					IDInfo problem = (IDInfo) problems.get( i );
					if( problem.getID() == ID.longValue() ) {
							problems.remove( i );
							break;
						}
				}
		}

		//new problem entered into the system
		public void addProblem( Long ID, int priority ) {
			IDInfo newProb = new IDInfo( ID.longValue(), priority );
			problems.add( newProb );
		}

		public void updatePriority( Long ID, int priority ) {
			for( int i = 0; i < problems.size(); i ++ ) {
					IDInfo problem = (IDInfo) problems.get( i );
					if( problem.getID() == ID.longValue() ) {
							problem.setPriority( priority );
							break;
						}
				}
		}

		private void sortList() {
			//bubble sort the list according to value
			for( int i = 0; i < problems.size(); i ++ ) {
					for( int j = problems.size() - 1; j > i; j -- ) {
							double jthminus1 = ( (IDInfo) problems.get( j - 1 ) ).getValue();
							double jth = ( (IDInfo) problems.get( j ) ).getValue();
							if( jthminus1 < jth ) {
									//swap the Problems
									IDInfo p = (IDInfo) problems.get( j - 1);
									IDInfo p1 = (IDInfo) problems.get( j );
									problems.setElementAt( p, j );
									problems.setElementAt( p1, j - 1 );
								}
						}
				}
		}
	}
