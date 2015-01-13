/*
Min-Min scheduler - assigns jobs to processors in batches, by first sorting the tasks in ascending order then using an 
earliest first approch schedules them
Copyright (C) 2005 Andrew Page
 
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

public class MMScheduler extends SchedulerCommon implements NewSchedulingAlgorithm  {
		//constructor
		public MMScheduler() {}

		/**
		* Override default method. 
		* Generates a min min schedule and assigns the tasks to processors
		* complexity
			* worst O(Max(n*p, n^2))
		* average O(max(n*p,nlogn))
		* best O(n*p)
		*/
		public void generateSchedule(Vector batch) {

			estFinishingTime();
			batch =   sortTasksAscending(batch);


			while(batch.size() != 0) {

					double lightestLoaded = -1;
					int lightestIndex = 0;
					// find the most lightly loaded client

					double probMflops = ((Problem) problems.get((Long) batch.get(0))).getAvgMflops();
					for(int i = 0; i< ips.length; i++) {


							double clientMflops = ((clientInfo)clientDetails.get(ips[i])).getMflops();

							double fin = estFinTime[i];
							if(clientMflops >= 0)
								fin +=probMflops/clientMflops;

							if(fin< lightestLoaded || lightestLoaded == -1) {
									lightestIndex = i;
									lightestLoaded =fin;

								}
						}

					clientInfo ci  = (clientInfo) clientDetails.get(ips[lightestIndex]);
					if(batch.size() == 0 ) {
							// The batch is empty so you can finish
							return;
						}
					(ci.getSchedule()).add(batch.get(0));

					double clientMflops = ci.getMflops();

					if(clientMflops >= 0)
						estFinTime[lightestIndex] += probMflops/clientMflops;


					batch.remove(0);


				}
		}
	}
