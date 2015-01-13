/*
Scheduling with error
 
Copyright (C) 2006 Andrew Page
 
This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General protected License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.
 
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General protected License for more details.
*/

/** Warning I changed the code to make the est error dynamic instead of fixed*/

import java.util.*;

public class ErrorCommonScheduler extends gaCommon implements NewSchedulingAlgorithm  {
		// switches
		boolean DEBUG = false;
		boolean DYNAMIC= false;
		String STRATEGY= "worst"; // mean case, worst case

		// constants
		double ERROR_WEIGHT=2.0;


		protected double[][] CCR;
		protected double[][] FT;
		protected double[]   ST;
		protected double[][] EW;
		protected double[][] EFT;
		protected double[][] EFTX;
		protected double[][] L;
		protected double[][] EL;
		protected double[][] ELX;
		protected double[][] S;
		protected double[][] ES;
		protected double[][] ESX;
		protected double[][] G;
		protected double[][] EG;
		protected double[][] EGX;
		protected boolean[]  mapped;
		protected Vector batch;



		double minTask = 0;
		double sumTasks =0;
		double minCCR = 0;
		double sumCCR = 0;
		double minProc = 0;
		double sumProcs = 0;
		double minComms = 0;
		double sumComms = 0;
		double alpha = 1;
		double beta = 1;



		//constructor
		protected ErrorCommonScheduler() {}

		/** Complexity O()*/
		protected void initialise(Vector inputbatch) {

			batch = inputbatch;
			// need to reinitialise variables here.
			numTasks = batch.size();
			numProcessors = clientDetails.size();

			ips = new String[numProcessors];

			CCR  = new double[numTasks][numProcessors];
			FT   = new double[numTasks][numProcessors];
			ST   = new double[numProcessors];
			EW   = new double[numTasks][numProcessors];
			EFT  = new double[numTasks][numProcessors];
			EFTX = new double[numTasks][numProcessors];
			L    = new double[numTasks][numProcessors];
			EL   = new double[numTasks][numProcessors];
			ELX  = new double[numTasks][numProcessors];
			S    = new double[numTasks][numProcessors];
			ES   = new double[numTasks][numProcessors];
			ESX  = new double[numTasks][numProcessors];
			G    = new double[numTasks][numProcessors];
			EG   = new double[numTasks][numProcessors];
			EGX  = new double[numTasks][numProcessors];


			// mark tasks which have been mapped to processors
			mapped = new boolean[numTasks];
			for(int q = 0; q< numTasks; q++)
				mapped[q] = false;

			int h = 0;
			// this method must be present because it sets the IP addresses
			for (Enumeration e = clientDetails.elements() ; e.hasMoreElements() ;) {
					clientInfo p = (clientInfo)  e.nextElement();
					ips [h] = p.getIP();
					h++;
				}


			calcStartTimes();

			calcCCR();
			calcError();


		}

		// complexity  p*t
		protected void calcStartTimes() {

			if(numTasks == 0)
				return;

			int g = 0;
			// calculate the StartTime of each processor ST
			// this method must be present because it sets the IP addresses
			for (Enumeration e = clientDetails.elements() ; e.hasMoreElements() ;) {
					clientInfo p = (clientInfo)  e.nextElement();

					// calculate when the client will finish what its currently doing
					long lastTime= p.getLastTime();
					// time in milliseconds when last task sent for processing
					double elapsed = (System.currentTimeMillis() - lastTime)/1000 ;

					double curFin = p.getEstFinishingTime() - elapsed;

					if(curFin < 0)
						ST[g] = 0;
					else
						ST[g] = curFin;

					Vector schd = p.getSchedule();
					//ST[g] = 0;
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
			/*
						if(DEBUG) {
								System.out.print("StartTimes =[");
								for(int u = 0 ; u< numProcessors; u++)
									System.out.print( ST[u] + " ");
								System.out.println(" ];");
							}
							*/
		}

		protected void calcCCR() {
			if(numTasks == 0)
				return;

			CCR = new double [numTasks][numProcessors];
			for(int i = 0; i< numTasks; i++) {
					for(int j = 0; j< numProcessors; j++) {
							clientInfo p = (clientInfo) clientDetails.get(ips[j]);
							Vector estTime = p.getEstimatedTime( (Long)(batch.get(i)), 0.0);
							double extime = (((Long)estTime.get(0)).longValue()/1000);
							double commsTime =  (((Long)estTime.get(1)).longValue()/1000);

							if(extime ==0.0) {
									if(p.getMflops() <=0)
										extime = 0;
									else
										extime=((Problem)problems.get((Long)(batch.get(i)))).getAvgMflops()/  p.getMflops();
								}

							if(commsTime == 0)
								CCR[i][j] = 1;
							else
								CCR[i][j] = extime/commsTime;



						}
				}

			if(DEBUG) {
					System.out.print("CCR =[");
					for(int u = 0 ; u< numProcessors; u++) {
							for(int v = 0; v<numTasks; v++) {
									System.out.print(CCR[v][u] + " ");
								}
							System.out.print(" ;");
						}
					System.out.println(" ];");
				}

		}



		protected void calcError() {
			// CCR must be calculated


			if(numTasks == 0)
				return;

			// requires ST
			// caluclate  FT matrix as well as Error weighting
			for(int i = 0; i< numTasks; i++) {
					for(int j = 0; j< numProcessors; j++) {

							Problem t = ((Problem)problems.get((Long) batch.get(i)));

							//EW[i][j] = ( t.getProcEstError())  + (   t.getTaskEstError()/CCR[i][j] );
							//EW[i][j] =(  t.getProcEstError() +    t.getTaskEstError()/CCR[i][j] );
							EW[i][j] =( t.getTaskEstError()/CCR[i][j] );
						    // EW[i][j] =     t.getTaskEstError();
						}
				}


			if(DEBUG) {
					System.out.print("EW =[");
					for(int u = 0 ; u< numProcessors; u++) {
							for(int v = 0; v<numTasks; v++) {
									System.out.print(EW[v][u] + " ");
								}
							System.out.print(" ;");
						}
					System.out.println(" ];");
				}
		}

		protected void calcFinishTime() {

			if(numTasks == 0)
				return;

			calcStartTimes();
			//calcCCR();
			//calcError();

			//calcError();
			// requires ST
			// caluclate  FT matrix as well as Error weighting
			for(int i = 0; i< numTasks; i++) {
					for(int j = 0; j< numProcessors; j++) {

							clientInfo p = (clientInfo) clientDetails.get(ips[j]);
							Vector estTime = p.getEstimatedTime( (Long)(batch.get(i)), 0.0);
							double extime = (((Long)estTime.get(0)).longValue()/1000) + (((Long)estTime.get(1)).longValue()/1000);

							if(extime ==0.0) {
									if(p.getMflops() <=0)
										extime = 0;
									else
										extime=((Problem)problems.get((Long)(batch.get(i)))).getAvgMflops()/  p.getMflops();
								}

							Problem t = ((Problem)problems.get((Long) batch.get(i)));
							FT[i][j] =  ST[j] + extime;
							//EW[i][j] = ( t.getProcEstError())  + (   t.getTaskEstError()/CCR[i][j] );
//EW[i][j] =(  t.getProcEstError() +    t.getTaskEstError()/CCR[i][j] );

EW[i][j] =( t.getTaskEstError()/CCR[i][j] );
 //EW[i][j] =     t.getTaskEstError();
							if(STRATEGY.equals("worst")) {
									FT[i][j] = FT[i][j] + (FT[i][j]*t.getTaskEstError() );
								}




							double a = 1;
							double b = 1;

							if(DYNAMIC) {
									a = Math.pow(FT[i][j],EW[i][j]);
									b = Math.pow(EW[i][j] , FT[i][j]);
									EFT[i][j]  = a * b;
								} else {
									EFT[i][j] = FT[i][j]* Math.pow(EW[i][j],ERROR_WEIGHT);
								}
							if(EW[i][j] == 0)
								EFTX[i][j] = 0;
							else {
									if(DYNAMIC)
										EFTX[i][j]  = a/b;
									else
										EFTX[i][j] = FT[i][j]/Math.pow(EW[i][j],ERROR_WEIGHT);
								}
						}
				}

			if(DEBUG) {
					System.out.print("FT =[");
					for(int u = 0 ; u< numProcessors; u++) {
							for(int v = 0; v<numTasks; v++) {
									System.out.print(FT[v][u] + " ");
								}
							System.out.print(" ;");
						}
					System.out.println(" ];");

					System.out.print("EFTX =[");
					for(int u = 0 ; u< numProcessors; u++) {
							for(int v = 0; v<numTasks; v++) {
									System.out.print(EFTX[v][u] + " ");
								}
							System.out.print(" ;");
						}
					System.out.println(" ];");

					System.out.print("EFT =[");
					for(int u = 0 ; u< numProcessors; u++) {
							for(int v = 0; v<numTasks; v++) {
									System.out.print(EFT[v][u] + " ");
								}
							System.out.print(" ;");
						}
					System.out.println(" ];");
				}
		}



		// complexity (p*2)*n*p
		protected void calcAllLoad() {

			if(numTasks == 0)
				return;

			//	calcError();
			calcStartTimes();
			//calcCCR();
			//calcError();

			L = new double[numTasks][numProcessors];
			// for each proc task pair calc load matrix
			for(int i =0; i< numTasks; i++) {
					for(int j = 0; j< numProcessors; j++) {
							L[i][j] = calcLoad(i,j);

							double a = 1;
							double b =1 ;
							if(DYNAMIC) {
									a = Math.pow(L[i][j],EW[i][j]);
									b = Math.pow(EW[i][j],L[i][j]);
									EL[i][j] = a*b;
								} else {
									EL[i][j] = L[i][j]*Math.pow(EW[i][j],ERROR_WEIGHT);
								}

							if(EW[i][j]==0)
								ELX[i][j] = 0;
							else {
									if(DYNAMIC)
										ELX[i][j]  = a/b;
									else
										ELX[i][j] = L[i][j]/Math.pow(EW[i][j],ERROR_WEIGHT);
								}
						}
				}


			if(DEBUG) {
					System.out.print("L =[");
					for(int u = 0 ; u< numProcessors; u++) {
							for(int v = 0; v<numTasks; v++) {
									System.out.print(L[v][u] + " ");
								}
							System.out.print(" ;");
						}
					System.out.println(" ];");
				}
			if(DEBUG) {
					System.out.print("ELX =[");
					for(int u = 0 ; u< numProcessors; u++) {
							for(int v = 0; v<numTasks; v++) {
									System.out.print(ELX[v][u] + " ");
								}
							System.out.print(" ;");
						}
					System.out.println(" ];");

				}
		}

		/**This will return the load weighting of a processor-task mapping*/
		// complexity p*2
		protected double calcLoad(int i, int j) {


			// requires that ST be run for it to work
			int makespanBefore = 0;
			int makespanAfter = 0;
			double makespanAfterValue = ST[0];
			double extime  = 0;

			// Find largest makespan, both before and after i mapped to j
			for(int a = 0; a< numProcessors; a++) {
					if(ST[a]>ST[makespanBefore])
						makespanBefore = a;

					if(a == j) {
							clientInfo p = (clientInfo) clientDetails.get(ips[j]);
							Vector estTime = p.getEstimatedTime( (Long)(batch.get(i)), 0.0);
							extime = (((Long)estTime.get(0)).longValue()/1000) + (((Long)estTime.get(1)).longValue()/1000);

							if(extime ==0) {
									extime=((Problem)problems.get((Long)(batch.get(i)))).getAvgMflops()/  p.getMflops();
								}

							if((ST[a]+ extime)>ST[makespanAfter]) {
									makespanAfter = a;
									makespanAfterValue = ST[a]+extime;
								}
						}

					if(ST[a]>makespanAfterValue) {
							makespanAfter = a;
							makespanAfterValue = ST[a];
						}
				}

			// now that you've found the largest makespan, work out the difference between current load of each processor
			// and the max load

			double loadBefore = 0;
			double loadAfter = 0;
			for(int a = 0; a< numProcessors; a++) {
					loadBefore +=(ST[makespanBefore] - ST[a]);

					if(a == j)
						loadAfter += makespanAfterValue - (ST[a]+ extime);
					else
						loadAfter += makespanAfterValue - ST[a];
				}

			double loadValue = 0;

			// prevent divide by zero error
			if(loadBefore ==0)
				return 0;//changed from 1
			else
				return loadAfter/loadBefore;

		}

		protected void calcAllGlobal() {
			if(numTasks == 0)
				return;

			//calcStartTimes();
			//calcCCR();


			//calcError();
			calcFinishTime();
			calcAllSuitability();
			calcAllLoad();

			G = new double[numTasks][numProcessors];
			// for each proc task pair calc load matrix
			for(int i =0; i< numTasks; i++) {
					for(int j = 0; j< numProcessors; j++) {
							G[i][j]  = FT[i][j]*S[i][j]*L[i][j];
							EG[i][j] = G[i][j]*Math.pow(EW[i][j],ERROR_WEIGHT);

							if(EW[i][j] == 0)
								EGX[i][j] = 0;
							else
								EGX[i][j] = G[i][j]/Math.pow(EW[i][j],ERROR_WEIGHT);
						}
				}

			if(DEBUG) {
					System.out.print("G =[");
					for(int u = 0 ; u< numProcessors; u++) {
							for(int v = 0; v<numTasks; v++) {
									System.out.print(G[v][u] + " ");
								}
							System.out.print(" ;");
						}
					System.out.println(" ];");

					System.out.print("EG =[");
					for(int u = 0 ; u< numProcessors; u++) {
							for(int v = 0; v<numTasks; v++) {
									System.out.print(EG[v][u] + " ");
								}
							System.out.print(" ;");
						}
					System.out.println(" ];");

					System.out.print("EGX =[");
					for(int u = 0 ; u< numProcessors; u++) {
							for(int v = 0; v<numTasks; v++) {
									System.out.print(EGX[v][u] + " ");
								}
							System.out.print(" ;");
						}
					System.out.println(" ];");
				}
		}

		/** Complexity   n^2*p^2    */
		protected void calcAllSuitability() {

			if(numTasks == 0)
				return;

			calcStartTimes();
			//calcCCR();


			//calcError();
			S = new double[numTasks][numProcessors];
			// for each proc task pair calc load matrix
			for(int i =0; i< numTasks; i++) {
					for(int j = 0; j< numProcessors; j++) {
							S[i][j] = calcSuitability(i,j);

							double a = 1;
							double b =1 ;
							if(DYNAMIC) {
									a = Math.pow(S[i][j], EW[i][j]);
									b = Math.pow(EW[i][j],S[i][j]);
									ES[i][j] = a*b;
								} else {
									ES[i][j] = Math.pow(EW[i][j],ERROR_WEIGHT)* S[i][j];
								}

							if(EW[i][j] == 0)
								ESX[i][j] = 0;
							else {
									if(DYNAMIC)
										ESX[i][j] =  a/ b;
									else
										ESX[i][j] = S[i][j]/ Math.pow(EW[i][j],ERROR_WEIGHT);
								}
						}
				}


			if(DEBUG) {
					System.out.print("S =[");
					for(int u = 0 ; u< numProcessors; u++) {
							for(int v = 0; v<numTasks; v++) {
									System.out.print(S[v][u] + " ");
								}
							System.out.print(" ;");
						}
					System.out.println(" ];");

					System.out.print("ES =[");
					for(int u = 0 ; u< numProcessors; u++) {
							for(int v = 0; v<numTasks; v++) {
									System.out.print(ES[v][u] + " ");
								}
							System.out.print(" ;");
						}
					System.out.println(" ];");

					System.out.print("ESX =[");
					for(int u = 0 ; u< numProcessors; u++) {
							for(int v = 0; v<numTasks; v++) {
									System.out.print(ESX[v][u] + " ");
								}
							System.out.print(" ;");
						}
					System.out.println(" ];");
				}

		}

		protected void initSuitability() {

			minTask = 0;
			sumTasks =0;

			minCCR = 0;
			sumCCR = 0;


			// find min task size in mflops and total
			for(int a = 0; a< numTasks; a++) {
					double curTask =   ((Problem)problems.get((Long)(batch.get(a)))).getAvgMflops();

					double curCCR = 0;
					for(int c = 0; c< numProcessors ; c++) {
							curCCR += CCR[a][c];
						}
					curCCR = curCCR/numProcessors;


					if(a == 0) {
							minCCR = curCCR;
							minTask = curTask;
						}

					if(curTask < minTask)
						minTask = curTask;

					if(curCCR < minCCR)
						minCCR = curCCR;

					sumCCR += curCCR;
					sumTasks += curTask;
				}
			sumCCR = sumCCR - (minCCR*numTasks);
			sumTasks = sumTasks - (minTask*numTasks);


			// find min proc speed
			// find sum of proc speeds
			minProc = 0;
			sumProcs = 0;

			minComms = 0;
			sumComms = 0;

			for(int b = 0; b< numProcessors; b++) {
					clientInfo p = (clientInfo) clientDetails.get(ips[b]);
					double curProc = p.getMflops();

					double curComms = 0 ;
					for(int c = 0 ; c< numTasks; c++) {
							Vector estTime = p.getEstimatedTime( (Long)(batch.get(c)), 0.0);
							curComms  +=  (((Long)estTime.get(1)).longValue()/1000);
						}
					curComms = curComms/numTasks;

					if(b == 0) {
							minProc = curProc;
							minComms = curComms ;
						}
					if(curProc < minProc)
						minProc = curProc;
					if(curComms < minComms)
						minComms = curComms;

					sumProcs +=curProc;
					sumComms +=curComms;
				}
			sumProcs = sumProcs - (minProc*numProcessors);
			sumComms = sumComms - (minComms*numProcessors);

		}






		/**Suitability  complexity  (n*p  + p*n)  */
		protected double calcSuitability(int i , int j) {

			double iTask =  ((Problem)problems.get((Long)(batch.get(i)))).getAvgMflops();

			clientInfo p = (clientInfo) clientDetails.get(ips[j]);
			double jProc =  p.getMflops();

			double tPart = 0;
			if(sumTasks ==0)
				tPart = 0;
			else
				tPart = (iTask - minTask)/sumTasks;

			double pPart = 0;
			if(jProc - minProc ==0)
				pPart =0;
			else
				pPart = sumProcs / (jProc - minProc);


			double mflopsPart =  Math.abs(tPart*pPart);


			double CCRPart = 0;
			if(sumCCR == 0)
				CCRPart = 0;
			else
				CCRPart = CCR[i][j]/sumCCR;

			double commsPart = 0;

			Vector estTime = p.getEstimatedTime( (Long)(batch.get(i)), 0.0);
			double curEstComms =  (((Long)estTime.get(1)).longValue()/1000);


			if(curEstComms  ==0)
				commsPart = 0;
			else
				commsPart = sumComms/curEstComms ;


			double coms = Math.abs(commsPart*CCRPart);

			if(CCR[i][j] == 0)
				return mflopsPart;
			else
				return mflopsPart + (coms/CCR[i][j]);

		}


	}
