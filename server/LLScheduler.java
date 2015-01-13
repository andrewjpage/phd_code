/*
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

/**
*Lightest Loaded scheduler - very simply just looks at the cumlative jobs assigned to each client (mflops)
* and assigns jobs to the lightest loaded ones first.
* Copyright (C) 2005 Andrew Page
*/

public class LLScheduler extends SchedulerCommon implements NewSchedulingAlgorithm  {

		//constructor
		public LLScheduler() {}

		/** Complexity O(n*p)*/
		public void generateSchedule(Vector batch) {

			createTaskArray();


			while(batch.size() != 0) {

					double lightestLoaded = -1;
					int lightestIndex = 0;
					// find the most lightly loaded client
					for(int i = 0; i< ips.length; i++) {
							if(taskMflops[i]< lightestLoaded || lightestLoaded == -1) {
									lightestIndex = i;
									lightestLoaded = taskMflops[i];

								}
						}

					clientInfo ci  = (clientInfo) clientDetails.get(ips[lightestIndex]);
					if(batch.size() == 0 ) {
							// The batch is empty so you can finish
							return;
						}
					(ci.getSchedule()).add(batch.get(0));
					taskMflops[lightestIndex] += ((Problem) problems.get(batch.get(0))).getAvgMflops();
					batch.remove(0);




				}
		}





	}
