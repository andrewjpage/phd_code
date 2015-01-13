/*
Scheduling with error
 
Copyright (C) 2006 Andrew Page
 
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

public class LXScheduler extends ErrorCommonScheduler implements NewSchedulingAlgorithm  {


		//constructor
		public LXScheduler() {}

		/** Complexity O()*/
		public void generateSchedule(Vector batch) {
			initialise(batch);
			if(numTasks == 0 || numProcessors == 0)
				return;


			//calcAllLoad();


			for(int w =0; w< numTasks; w++) {
					calcAllLoad();
					int si = -1;
					int sj = 0;
					boolean zero = true; // check to see if all load values are zero, if so do RR

					// find the smallest value
					for(int i = 0; i< numTasks; i++) {
							if(! mapped[i]) {
									if(si == -1) {si = i;}


									for(int j = 0; j< numProcessors; j++) {
											if(EW[i][j] != 0)
												zero = false;

											if(EW[i][j] > EW[si][sj] ) {
													si = i;
													sj = j;
												}
										}
								}
						}

					int ftj = 0;
					// you now have the largest error task processor pair
					// find the smallest FT for a given task
					for(int j = 0; j< numProcessors; j++) {
							if(L[si][j] < L[si][ftj] )
								ftj = j;
						}


					if(zero &&  !mapped[w]) {
							clientInfo ci  = (clientInfo) clientDetails.get(ips[w%numProcessors]);
							(ci.getSchedule()).add(batch.get(w));
							mapped[w] = true;

						} else if(! mapped[si]) {
							clientInfo ci  = (clientInfo) clientDetails.get(ips[sj]);
							(ci.getSchedule()).add(batch.get(si));
							mapped[si] = true;
						}
				}

		}

	}
