/*
an interface class to define the general funcations that should be implemented by a scheduling algorithm
 
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

public interface SchedulingAlgorithm {
		//method that tells the system which problem to generate a problem from next
		//returns an ordered list of problems
		public long[] nextUnitProblemID();

		//a result set was received for a particular problem
		//Long ID = problem ID
		//processingTime = how long it took to process the unit
		public void update( Long ID, long processingTime );

		//telling the algorithm that a new problem has been added to the system
		public void addProblem( Long ID, int priority );

		//tell the scheduler that a current problem has been removed from the system
		public void removeProblem( Long ID );

		//priority of a current problem has been changed
		public void updatePriority( Long ID, int priority );
	}