/*
Earliest first scheduler - assigns jobs to the processor
that will finish them first
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

public class MEScheduler extends gaCommon implements NewSchedulingAlgorithm  {

		private double[][]  FT;
		private double[] ST;
		private double[][] EW;
		private double[][] EFT;

		//constructor
		public MEScheduler() {}

		/** Complexity O(n*p)*/
		public void generateSchedule(Vector batch) {


			// need to reinitialise variables here.
			numTasks = batch.size();
			tasks = new double[2][numTasks];
			numProcessors = clientDetails.size();
			processorSpeed = new double[numProcessors][2];
			FT = new double[numTasks][numProcessors];
			ST = new double[numProcessors];
			EW = new double[numTasks][numProcessors];
			EFT = new double[numTasks][numProcessors];


			for(int i = 0; i<numTasks ; i++) {
					tasks[0][i] = 1.0*i;
					tasks[1][i] =  ((Problem)problems.get((Long) batch.get(i))).getAvgMflops();
				}


			double tmpProcSpeed[] = new double[numProcessors];
			ips = new String[numProcessors];
			int h = 0;
			for (Enumeration e = clientDetails.elements() ; e.hasMoreElements() ;) {
					clientInfo p = (clientInfo)  e.nextElement();
					tmpProcSpeed[h] = p.getMflops() ;
					ips [h] = p.getIP();
					h++;
				}

			int g = 0;
			// calculate the StartTime of each processor ST
			for (Enumeration e = clientDetails.elements() ; e.hasMoreElements() ;) {
					clientInfo p = (clientInfo)  e.nextElement();
					Vector schd = p.getSchedule();
					ST[g] = 0;
					for(int s =0; s< schd.size(); s++) {
							Vector estTime = p.getEstimatedTime( (Long)(schd.get(s)), 0.0);
							double extime = (((Long)estTime.get(0)).longValue()/1000) + (((Long)estTime.get(1)).longValue()/1000);
							if(extime ==0.0) {
									ST[g]+=((Problem)problems.get((Long) schd.get(s))).getAvgMflops()/p.getMflops();
								} else {
									ST[g]+= extime;
								}
						}
					g++;
				}




			// sort processor speeds in ascending order. This is O(n*n)

			for(int a =0; a< numProcessors; a++) {
					for(int b= a; b< numProcessors; b++) {
							if(tmpProcSpeed[a] < tmpProcSpeed[b]  ) {
									double  t= tmpProcSpeed[a];
									tmpProcSpeed[a] =tmpProcSpeed[b];
									tmpProcSpeed[b] = t;
									String s = ips[a];
									ips[a] = ips[b];
									ips[b]  = s;
								}
						}
				}


			// caluclate the FT
			for(int i = 0; i< numTasks; i++) {
					for(int j = 0; j< numProcessors; j++) {

							clientInfo p = (clientInfo) clientDetails.get(ips[j]);
							Vector estTime = p.getEstimatedTime( (Long)(batch.get(i)), 0.0);
							double extime = (((Long)estTime.get(0)).longValue()/1000) + (((Long)estTime.get(1)).longValue()/1000);

							if(extime ==0.0) {
									extime=((Problem)problems.get((Long)(batch.get(i)))).getAvgMflops()/  p.getMflops();
								}

							Problem t = ((Problem)problems.get((Long) batch.get(i)));
							FT[i][j] =  ST[j] + extime;
							EW[i][j] = ((1+ t.getProcEstError())* t.getTaskEstError() );
							EFT[i][j] = FT[i][j]* EW[i][j];
						}
				}


			boolean [] mapped = new boolean[numTasks];
			for(int q = 0; q< numTasks; q++)
				mapped[q] = false;

			for(int w =0; w< numTasks; w++) {
					int si = 0;
					int sj = 0;
					for(int i = 0; i< numTasks; i++) {
							if(! mapped[i]) {
									for(int j = 0; j< numProcessors; j++) {

											if(EW[i][j] < EW[si][sj] )
												si = i;
											sj = j;
										}
								}
						}

					int ftj = 0;
					// you now have the smallest error task processor pair
					// find the smallest FT for a given task
					for(int j = 0; j< numProcessors; j++) {

							if(EW[si][j] < EW[si][ftj] )
								ftj = j;
						}

					//mapped
					if(! mapped[si]) {
							clientInfo ci  = (clientInfo) clientDetails.get(ips[ftj]);
							(ci.getSchedule()).add(batch.get(si));
							mapped[si] = true;
						}
				}





		}
	}
