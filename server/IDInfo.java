/*
A class to represent all the information that the ExponentialSmoothing scheduler is required to remember for each problem
 
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

public class IDInfo {
		private long ID;
		private long totalProcessing;
		private int priority;
		private double value;

		public IDInfo( long id, int prior ) {
			ID = id;
			priority = prior;
			value = 0.0; //set the default value as 0.0 (corresponds to a balanced problem)
			totalProcessing = 0;
		}

		public long getID() { return ID; }

		public long getTime() { return totalProcessing; }

		public long getPriority() { return priority; }

		public double getValue() { return value; }

		public void setValue( double d ) { value = d; };

		public void addTime( long t ) { totalProcessing += t; }

		public void setPriority( int p ) { priority = p; }
	}
