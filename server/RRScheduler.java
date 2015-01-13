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
*A simple round robin scheduler. This is the default behaviour in the SchedulerCommon class.
* Copyright (C) 2005 Andrew Page
*/

public class RRScheduler extends SchedulerCommon implements NewSchedulingAlgorithm  {

		//constructor
		public RRScheduler() {}

		public void generateSchedule(Vector batch) {

			while(batch.size() != 0) {

					for (Enumeration e = clientDetails.elements() ; e.hasMoreElements() ;) {

							clientInfo ID = (clientInfo)  e.nextElement();
							if(batch.size() == 0 ) {
									// The batch is empty so you can finish
									return;
								}
							(ID.getSchedule()).add(batch.get(0));
							batch.remove(0);
						}
				}
		}

	}
