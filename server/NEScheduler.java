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

public class NEScheduler extends ErrorCommonScheduler implements NewSchedulingAlgorithm  {


        //constructor
        public NEScheduler() {}

        /** Complexity O()*/
        public void generateSchedule(Vector batch) {
        initialise(batch);
                if(numTasks == 0 || numProcessors == 0)
          return;
        
  
        
        calcAllGlobal();
     

            for(int w =0; w< numTasks; w++) {
                    int si = 0;
                    int sj = 0;
                    
                    // find the smallest value
                    for(int i = 0; i< numTasks; i++) {
                            if(! mapped[i]) {
                                    for(int j = 0; j< numProcessors; j++) {

                                            if(G[i][j] < G[si][sj] )
                                                si = i;
                                            sj = j;
                                        }
                                }
                        }


                    //mapped
                    if(! mapped[si]) {
                            clientInfo ci  = (clientInfo) clientDetails.get(ips[sj]);
                            (ci.getSchedule()).add(batch.get(si));
                            mapped[si] = true;
                        }
                }

        }
        
    }
