/*
 
Copyright (C) 2005  Andrew J. Page
apage@cs.nuim.ie
 
This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.
 
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
 
You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 
*/



import java.util.*;
import java.lang.*;
import java.io.*;
import java.security.*;
import java.math.*;

/**
*GA scheduler - It uses multiple heuristics and genetic algorithms to generate a schedule
* PN = Page Naughton and is how the scheduler is refered to in publications.
* Copyright (C) 2004, 2005 
* @author Andrew Page.
* @version Started - 6th of April 2004
* complexity variables -  n = number of tasks.
* p = number of processors.
* s = n + p.
*/
public class PNScheduler extends gaCommon   implements NewSchedulingAlgorithm {

		double startingmakespan = 0.0;
		int genStepSize = 10; // number of generations to perform before reporting back to check efficiency.
		int numRebalance = 20    ; // number of rebalances per generation per individual in the population
		long startTime = 0;
		String gaBest;
		String gaHeur;
		double[] bestChrom;
		double bestMakespan;


		public  PNScheduler() {
			super();
		}


		/** Starts the GA scheduler - The batch contains a list of problem IDs as Longs.
		* This needs to be made into a thread to run in the background.
		* @param batch Takes in a Vector containing Long's which correspond to the id numbers of problems
		* Complexity = O(s^2)
		*/
		public void generateSchedule(Vector batch) {
			Integer sync = new Integer(0);
			synchronized(sync) {

					startTime = (System.currentTimeMillis());
					// need to reinitialise variables here.
					numTasks = batch.size();


					tasks = new double[2][numTasks];
					//numTasks = batch.size();
					batchQueue = batch;

					numProcessors = clientDetails.size();
					numProcessorTypes = numProcessors;
					processorSpeed = new double[numProcessors][2];

					if(numProcessors <= 0|| numTasks <=0)
						return;

					// format tasks

					for(int i = 0; i<numTasks ; i++) {
							tasks[0][i] = 1.0*i;
							tasks[1][i] =  ((Problem)problems.get((Long) batch.get(i))).getAvgMflops();
							//System.out.println("task mflops "+ tasks[1][i]);
						}

					// format processors
					/*copy all of the processor speeds, then sort them*/

					double tmpProcSpeed[] = new double[numProcessors];
					ips = new String[numProcessors];
					int i = 0;
					for (Enumeration e = clientDetails.elements() ; e.hasMoreElements() ;) {
							clientInfo p = (clientInfo)  e.nextElement();

							tmpProcSpeed[i] = p.getMflops() ;
							ips [i] = p.getIP();
							i++;

							if(numTasks ==1) {

									Vector tmpSched = p.getSchedule();

									tmpSched.add(batchQueue.get(0));
									return;
								}
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

					for(i = 0; i< numProcessors; i++) {
							processorSpeed[i][0] = tmpProcSpeed[i];
							processorSpeed[i][1] = 1;
							//System.out.println("proc "+ processorSpeed[i][0] );
						}

					/**************************** hardcoded for testing only ***************************************/



					run(500, numTasks, 30, 0.001, 0.02, 0.01);
				}
		}

		/** this method will initialise the simulation and run it.  It will return a Vector containing the scheduling scheme
		*
		* Worst Case: O(s^2)
		* Average Case: O(s^2)
		* Best Case: O(S^2)
		*/
		private void run( int numGenerations, int slidingWindowSize, int popSize,double commsMinRatio, double commsMaxRatio, double mutations) {
			generations = numGenerations;
			slidingWindow = slidingWindowSize;
			populationSize= popSize;

			int  generationsRun = 0;
			long avgTime = 0;
			double efficiency = 0.0;

			taskBufferOffset = 0;
			processorQueue = new double[2][numProcessors];
			// moved from the loop
			Hashtable p = currentAssignedLoad();
			for(int i =0; i< numProcessors; i++) {
					processorQueue[1][i] = ((Double) p.get(ips[i])).doubleValue();
				}



			stringSize = slidingWindow+ numProcessors; // last space is for the Fitness Value;
			population = new double[stringSize][populationSize];
			numMutations =(int )((populationSize*(stringSize-1)) *mutations) ;// number of random mutations to perform every generation

			bestMakespan = -1;
			bestChrom= new double[stringSize];

			initialise();
			//do {
			int numGenRun = runGA(generations);

			//	curGen += genStepSize ;

			//} while( curGen< generations) ;

			gaBest  = gaBest  +";";
			System.out.println( gaBest  );

			Vector b = saveBest();
			int bestProc = ((Integer)b.get(0)).intValue();
			//Vector makespan = maxSpanProcessor(b);
			double bestMakespan2 = ((Double)b.get(2)).doubleValue();
			//System.out.println(bestMakespan + " "+bestMakespan2 );
			System.out.println( gaHeur+" " + bestMakespan2+ " "+optimalTime(bestProc)+" ;");

			assignTasks();
			double perMutations = (1.0*numMutations)/((1.0*populationSize*((1.0*stringSize)-1.0)));
			System.out.println("gatime "+ ( (System.currentTimeMillis())-startTime )  + " " + slidingWindow+ " " + numProcessors + " "+ numGenRun + " "+perMutations  + " "+ bestMakespan2+ " "+optimalTime(bestProc)+ ";" );
		}


		/** Initialises the population using 8 heuristics, and randomly mutate the others
		* Worst Case: O(N^2 + s*p)
		* Average Case: O(Nlogn + s*p)
		* Best Case: O(S*p)
		*/
		private void initialise() {

			sort(tasks);
			int randIndex = 0;
			currentBestIndex = 0; // the current best string in the  population

			BestMaxSpan  = 0;

			for(int p = 0; p<numProcessors; p++) {
					processorQueue[0][p] = processorSpeed[p][0]; // initialise processor speeds
				}

			int numRandTasks  = slidingWindow/2;

			if(numRandTasks > slidingWindow) {
					numRandTasks = slidingWindow - 1;
				}

			/********* New initialisation heuristic  *********/
			// use multiple heuristics. use the first 8 heuristics untouched, then mutated the rest.
			int numHeur = 8;
			for(int i =0; i<  numHeur; i++) {
					if(i==0)
						heuristic(i,"earliestFirstMinMin");
					if(i==1)
						heuristic(i,"earliestFirstMaxMin");
					if(i==2)
						heuristic(i,"lightestLoadedMinMin");
					if(i==3)
						heuristic(i,"lightestLoadedMaxMin");
					if(i==4)
						heuristic(i,"earliestFirstMinMinComms");
					if(i==5)
						heuristic(i, "earliestFirstMaxMinComms");
					if(i==6)
						heuristic(i,"lightestLoadedMinMinComms");
					if(i==7)
						heuristic(i,"lightestLoadedMaxMinComms");
					/*
					if(i==8) {
							complexHeuristics(i,"RC");
							System.out.println(printRow(8));
						}
					if(i==9) {
							complexHeuristics(i,"DL");
							System.out.println(printRow(9));
						}
						*/
				}

			// copy the results of the heuristics into the rest of the population and mutation them
			for(int i = numHeur; i< populationSize; i++) {

					for(int b = 0; b< stringSize; b++) {
							population[b][i] =  population[b][i%numHeur];
						}

					// make 10% swaps in each sting
					for(int s = 0; s< ((stringSize-1)/15) ; s++)
						swap(i);

				}


			gaBest = "gagen ";
			gaHeur = "gaHeur ";

			for(int i =0; i<  numHeur; i++) {
					// print out the makespans of the initial heuristics
					Vector makespan = maxSpanProcessor(i);
					gaHeur = gaHeur   +" " + ((Double)makespan.get(1)).doubleValue();
				}

			updateAllFitnessValues();

		}



		/** 		run a single generation of the Genetic Algorithm
				* work out the fitness of each string in the population
				* Complexity = O(s^2)
				*/
		private int runGA(int gen) {
			double lastBest = -1;
			double last100Best = -1;

			for(int b =0; b< gen; b++) {
					Vector bestString = saveBest();
					//Vector bestString = findBest();
					int best =  ((Integer)bestString.get(0)).intValue();

					double bestMakespanLocal = ((Double)bestString.get(2)).doubleValue();

					//Vector makespan = maxSpanProcessor(best);
					gaBest  = gaBest  +" " +bestMakespanLocal;


					if(b%10 == 0) {
							double curBestVal = bestMakespanLocal;
							if(curBestVal == 0.0) {
									// do nothing
								}
							else if(lastBest == -1) {
									lastBest = curBestVal ;
								} else if(curBestVal < lastBest) {
									lastBest = curBestVal ;
								} else {
									// increase the amount of mutation by 5%
									if(numMutations < ((int )((populationSize*(stringSize-1)) *0.1)))
										numMutations = (int)(numMutations +
										                     (numMutations*1.02));
									rebalanceAll();

								}
						}
					if(b%50 == 0) {
							double curBestVal = bestMakespanLocal;
							if(curBestVal == 0.0) {
									// do nothing
								}
							else if(last100Best == -1) {
									last100Best = curBestVal ;
								} else if(curBestVal < last100Best) {
									last100Best = curBestVal ;
								} else {
									// Finish because the result has converged.
									return b;
								}
						}

					//if(! checkAllValid())
					//	System.out.println("error invalid string before roulettewheel");
					rouletteWheel(); // selection - n^2

					//if(! checkAllValid())
					//	System.out.println("error invalid string after roulettewheel");

					crossOver();                       // n^2
					//if(! checkAllValid())
					//	System.out.println("error invalid string after crossover");


					randomSwaps(numMutations);// mutations. s^2
					//if(! checkAllValid())
					//	System.out.println("error invalid string after randomswaps");

					updateAllFitnessValues(); // s
					//if(! checkAllValid())


				}
			saveBest();
			return gen;

		}

		/** 		run a single generation of the Genetic Algorithm
			* work out the fitness of each string in the population
			* Complexity = O(s^2)
			*/
		private int runGAfixed(int gen) {
			double lastBest = -1;
			double last100Best = -1;

			for(int b =0; b< gen; b++) {
					Vector bestString = saveBest();
					//Vector bestString = findBest();
					int best =  ((Integer)bestString.get(0)).intValue();

					double bestMakespanLocal = ((Double)bestString.get(2)).doubleValue();

					//Vector makespan = maxSpanProcessor(best);
					gaBest  = gaBest  +" " +bestMakespanLocal;


					rebalanceAll();

					rouletteWheel(); // selection - n^2

					crossOver();                       // n^2

					randomSwaps(numMutations);// mutations. s^2

					updateAllFitnessValues(); // s



				}
			saveBest();
			return gen;

		}



		/** Save the best chromosome to an array
		*/
		private Vector saveBest() {

			Vector b = findBest();
			int best =  ((Integer)b.get(0)).intValue();
			bestProc =  ((Integer)b.get(0)).intValue();
			double m = ((Double)b.get(2)).doubleValue();

			//Vector makespan = maxSpanProcessor(best);
			//double m =  bestMakespan;

			if(bestMakespan == -1) {
					bestMakespan = m;
				} else if(bestMakespan < m) {
					// insert the current best into the population

					// pick a random position to insert it in
					int randIndex = findWorst();
					for(int i = 0; i< stringSize; i++) {
							population[i][randIndex ] = bestChrom[i];
						}
					b = findBest();
					return b;
				}

			// save the current best
			bestChrom= new double[stringSize];
			for(int i = 0; i< stringSize; i++) {
					bestChrom[i] = population[i][best];
				}
			bestMakespan = m;
			b = findBest();
			return b;

		}



		/** this method will calculate the fitness of a given string. it uses a cache so that the same string doesnt have to be calculated more than once
		* Complexity = O(s)
		*/
		private double fitnessFunction(int stringIndex) {
			//double t = checkCache(stringIndex); // complexity too high
			//if(t == -1) {
			Vector result = maxSpanProcessor(stringIndex);
			double max = ((Double)result.get(1)).doubleValue();

			return (1/max);
			//} else
			//return t;
		}


		/**this method will take a string and swap two bits of the string.
		* Complexity = O(s)
		*/
		private void swap(int stringIndex) {

			if(numProcessors <=1 || stringIndex <0 || stringIndex >=populationSize) {
					// error checking
					return;
				}

			int first = rand.nextInt(stringSize-1);
			int second = rand.nextInt(stringSize-1);

			double t = population[first][stringIndex];
			population[first][stringIndex] = population[second][stringIndex];
			population[second][stringIndex] = t;

		}

		/**  this method will update all fitness values  and will perform rebalancing on each individual in the population
		* Worst Case: O(s)
		* Average Case: O(s)
		* Best Case: O(s)
		*/
		private void updateAllFitnessValues() {

			for(int i =0; i< populationSize; i++) {
					population[stringSize-1][i] = fitnessFunction(i);
				}
		}

		private void rebalanceAll() {

			for(int i =0; i< populationSize; i++) {
					for(int b = 0; b< numRebalance; b++) {
							balance(i);
						}
					//population[stringSize-1][i] = fitnessFunction(i);
				}
		}


		/** this method will randomly select a string and randomly swap an element in that string.
		* Complexity = O(s*s)
		*/
		private void randomSwaps(int num) {
			int row = 0;

			for(int i =0; i< num;i++) {
					do {
							row = rand.nextInt(populationSize);
						} while(row ==currentBestIndex);

					swap(row);
				}


		}




		/**  This will randomly select a task from the most heavily loaded procesor and swap it with a smaller task from another processor
				* Worst Case: O(8s)
		* Average Case: O(6s+ 2*(n/p))
		* Best Case: O(5s + 2)
		*/
		private void balance(int stringIndex) {

			int hMin = 0;
			int hMax = stringSize -1;
			double largestTime = 0.0;
			int procID = 0;

			double [] old = new double[stringSize];



			// findout the processor number of the  heavyest loaded processor
			Vector ms = maxSpanProcessor(stringIndex);
			procID = ((Integer)ms.get(0)).intValue();
			largestTime = ((Double)ms.get(1)).doubleValue();
			double oldMax = largestTime;

			//population[stringSize-1][stringIndex] = fitnessFunction(stringIndex);
			// this is in here for efficiency reasons
			population[stringSize-1][stringIndex] = 1/oldMax;

			// save the old string so you can roll back
			for(int i = 0; i< stringSize; i++) {
					//
					old[i]= population[i][stringIndex];
				}



			// find out the bounds of the heaviest loaded processors queue
			int procCount = 0;
			boolean minFound = false;

			for(int i = 0; i< stringSize-1; i++) {
					if(population[i][stringIndex] == delimiter) {
							procCount++;
						}

					if(procCount == procID&& !minFound) {
							hMin = i+1;
							minFound = true;
						}

					if(procCount == (procID +1)) {
							hMax = i;
							i = stringSize-1;// AP added -1 26/10/05
						}

				}

			if(hMin >=stringSize-1 || hMin < 0 || hMax <0 || hMax >=stringSize) {
					return;
				}

			// randomly select a task on the heavy processor and another smaller task on another processor
			int largestRandIndex =0;
			int infCounter = 0;  // prevent infinite loops
			int curRand = 0;
			int larIndex = hMin;

			// find the largest task on the heaviest loaded processor
			for(int lar = hMin ; lar < hMax; lar++) {

					if(tasks[1][((int)population[lar][stringIndex])]>=tasks[1][((int)population[larIndex][stringIndex])]) {
							larIndex = lar;
						}
				}

			int secLar = 0;
			boolean atLeastOnce = false;

			// randomly select 3 tasks and swap the smallest of them with the largest task
			int smallest = 0;

			for(int b =0; b< 3; b++) {
					// randomly select a task
					int infCount = 0;

					do {
							secLar =rand.nextInt(stringSize-1);
							infCount++;
						} while(infCount <stringSize-1
					        && (population[secLar][stringIndex] == delimiter
					            ||population[larIndex][stringIndex]<population[secLar][stringIndex]
					            || (secLar>= hMin && secLar < hMax)) );

					if(population[secLar][stringIndex] < population[smallest][stringIndex]
					    || population[smallest][stringIndex] == delimiter) {
							smallest = secLar;
						}
				}

			secLar = smallest;
			largestRandIndex = larIndex;
			curRand = secLar;
			double tmp =  population[largestRandIndex ][stringIndex];
			population[largestRandIndex][stringIndex] = population[curRand][stringIndex];
			population[curRand][stringIndex] = tmp;


			Vector result = maxSpanProcessor(stringIndex);
			double newMax = ((Double)result.get(1)).doubleValue();

			if(oldMax< newMax ) {
					for(int i = 0; i< stringSize; i++) {

							population[i][stringIndex] =old[i];
						}
				}
			//population[stringSize-1][stringIndex] = fitnessFunction(stringIndex);
			// this is here for effieciency reasons
			population[stringSize-1][stringIndex] = 1/newMax;

		}
		/** Check every string in the population to see if its valid and print out
		* an error message if it isnt*/
		private boolean checkAllValid() {
			int falseCount = 0;
			for(int i = 0; i< populationSize ; i++) {
					if(! isValid(i)) {
							System.out.println("error invalid string, index number: " + i);
							System.out.println(printRow(i)+ "\n\n");
							falseCount++;
						}
				}

			if(falseCount ==0)
				return true;
			else {
					System.out.println("error number of bum strings found: "+ falseCount);
					return false;
				}
		}


		/** Modified version of Suns implementation of CAR Hoares quicksort algorithm

		*/
		void QuickSort(double a[][], int lo0, int hi0)  {
			int lo = lo0;
			int hi = hi0;
			double mid;

			if ( hi0 > lo0) {

					mid = a[1][ ( lo0 + hi0 ) / 2 ];

					while( lo <= hi ) {
							while( ( lo < hi0 ) && ( a[1][lo] < mid ) )
								++lo;

							while( ( hi > lo0 ) && ( a[1][hi] > mid ) )
								--hi;

							if( lo <= hi ) {
									swapSort(a, lo, hi);
									++lo;
									--hi;
								}
						}

					if( lo0 < hi )
						QuickSort( a, lo0, hi );
					if( lo < hi0 )
						QuickSort( a, lo, hi0 );

				}
		}

		private void swapSort(double a[][], int i, int j) {
			double T;
			T = a[1][i];
			a[1][i] = a[1][j];
			a[1][j] = T;

			T = a[0][i];
			a[0][i] = a[0][j];
			a[0][j] = T;
		}

		/** Quick sort
		* Worst Case: O(n^2)
		* Average Case: O(nlogn)
		* Best Case: O(n)*/
		public void sort(double a[][])  {
			QuickSort(a, 0, a.length - 1);
		}
		/**  take a single string and use a heuristic, specified by parameter
			 *assumes that the batch of tasks is sorted in ascending order.
			 * valid heuristics - 
			        *    earliestFirstMinMin
					*	earliestFirstMaxMin
					 *   lightestLoadedMinMin
					*	lightestLoadedMaxMin
					*	earliestFirstMinMinComms
					*	earliestFirstMaxMinComms
					*	lightestLoadedMinMinComms
					*	lightestLoadedMaxMinComms
		*Worst Case: O(2p + s*p)
		* Average Case: O(2p + s*p)
		* Best Case: O(2p + s*p)
			 */
		private void heuristic(int index, String heuristicName) {

			// an array to hold tasks and processor queues
			Vector[] procQueues = new Vector[numProcessors];
			double[][] pq = new double[2][numProcessors];

			// copy the current load on processors to a temp array
			for(int e = 0; e < numProcessors; e++) {
					pq[0][e] = processorQueue[0][e];
					pq[1][e] = processorQueue[1][e];

					// initilise the vector which stores the processor queues.
					procQueues[e] = new Vector();
				}

			if(heuristicName =="earliestFirstMinMin") {
					for(int i = 0; i< slidingWindow; i++) {
							int earliestP = 0;
							double minTime = 0.0;

							for(int p = 0; p< numProcessors; p++) {
									double pTime = (pq[1][p] + tasks[1][i+taskBufferOffset])/pq[0][p] ;
									if((pTime < minTime || p == 0 )&& (pTime >0.0)) {
											minTime = pTime;
											earliestP = p;
										}
								}
							procQueues[earliestP].add(new Double(tasks[0][i+taskBufferOffset]));
							pq[1][earliestP]  = tasks[1][i+taskBufferOffset] + pq[1][earliestP] ;
						}
				} else if(heuristicName =="earliestFirstMaxMin") {
					for(int i = slidingWindow -1; i>=0; i--) {
							int earliestP = 0;
							double minTime = 0.0;

							for(int p = 0; p< numProcessors; p++) {
									double pTime = (pq[1][p] + tasks[1][i+taskBufferOffset])/pq[0][p] ;
									if((pTime < minTime || p == 0 )&& (pTime >0.0)) {
											minTime = pTime;
											earliestP = p;
										}
								}
							procQueues[earliestP].add(new Double(tasks[0][i+taskBufferOffset]));
							pq[1][earliestP]  = tasks[1][i+taskBufferOffset] + pq[1][earliestP] ;
						}
				} else if(heuristicName =="earliestFirstMinMinComms") {
					for(int i = 0; i< slidingWindow; i++) {
							int earliestP = 0;
							double minTime = 0.0;

							for(int p = 0; p< numProcessors; p++) {
									double pTime = ((pq[1][p] + tasks[1][i+taskBufferOffset] )/pq[0][p])   ;

									Long probID = ((Long)batchQueue.get((int)tasks[0][i+ taskBufferOffset]));
									double taskBytes = ((Problem)problems.get(probID)).getAvgTaskSize();

									double bandwidth =(((clientInfo) clientDetails.get(ips[p])).getBandwidth());
									if(bandwidth > 0)
										pTime += taskBytes/bandwidth;

									pTime +=((clientInfo) clientDetails.get(ips[p])).getLatency();

									if((pTime < minTime || p == 0 )&& (pTime >0.0)) {
											minTime = pTime;
											earliestP = p;
										}
								}
							procQueues[earliestP].add(new Double(tasks[0][i+taskBufferOffset]));
							pq[1][earliestP]  = tasks[1][i+taskBufferOffset] + pq[1][earliestP] ;
						}

				} else if(heuristicName =="earliestFirstMaxMinComms") {
					for(int i = slidingWindow -1; i>=0; i--) {
							int earliestP = 0;
							double minTime = 0.0;

							for(int p = 0; p< numProcessors; p++) {
									double pTime = ((pq[1][p] + tasks[1][i+taskBufferOffset])/pq[0][p]) ;
									Long probID = ((Long)batchQueue.get((int)tasks[0][i+ taskBufferOffset]));
									double taskBytes = ((Problem)problems.get(probID)).getAvgTaskSize();
									double bandwidth =(((clientInfo) clientDetails.get(ips[p])).getBandwidth());
									if(bandwidth > 0)
										pTime += taskBytes/bandwidth;

									pTime +=((clientInfo) clientDetails.get(ips[p])).getLatency();

									if((pTime < minTime || p == 0 )&& (pTime >0.0)) {
											minTime = pTime;
											earliestP = p;
										}
								}
							procQueues[earliestP].add(new Double(tasks[0][i+taskBufferOffset]));
							pq[1][earliestP]  = tasks[1][i+taskBufferOffset] + pq[1][earliestP] ;
						}
				} else if(heuristicName =="lightestLoadedMinMin") {
					for(int i = 0; i< slidingWindow; i++) {
							int earliestP = 0;
							double minTime = 0.0;

							for(int p = 0; p< numProcessors; p++) {
									double pTime = (pq[1][p])/pq[0][p] ;
									if(pTime < minTime || p == 0) {
											minTime = pTime;
											earliestP = p;
										}
								}
							procQueues[earliestP].add(new Double(tasks[0][i+taskBufferOffset]));
							pq[1][earliestP]  = tasks[1][i+taskBufferOffset] + pq[1][earliestP] ;
						}
				} else if(heuristicName =="lightestLoadedMinMinComms") {
					for(int i = 0; i< slidingWindow; i++) {
							int earliestP = 0;
							double minTime = 0.0;

							for(int p = 0; p< numProcessors; p++) {
									double pTime = ((pq[1][p])/pq[0][p]);
									Long probID = ((Long)batchQueue.get((int)tasks[0][i+ taskBufferOffset]));
									double taskBytes = ((Problem)problems.get(probID)).getAvgTaskSize();
									double bandwidth =(((clientInfo) clientDetails.get(ips[p])).getBandwidth());
									if(bandwidth > 0)
										pTime += taskBytes/bandwidth;

									pTime +=((clientInfo) clientDetails.get(ips[p])).getLatency();

									if(pTime < minTime || p == 0) {
											minTime = pTime;
											earliestP = p;
										}
								}
							procQueues[earliestP].add(new Double(tasks[0][i+taskBufferOffset]));
							pq[1][earliestP]  = tasks[1][i+taskBufferOffset] + pq[1][earliestP] ;
						}
				} else if(heuristicName =="lightestLoadedMaxMin") {
					for(int i = slidingWindow -1; i>=0; i--) {
							int earliestP = 0;
							double minTime = 0.0;

							for(int p = 0; p< numProcessors; p++) {
									double pTime = (pq[1][p])/pq[0][p] ;
									if(pTime < minTime || p == 0) {
											minTime = pTime;
											earliestP = p;
										}
								}
							procQueues[earliestP].add(new Double(tasks[0][i+taskBufferOffset]));
							pq[1][earliestP]  = tasks[1][i+taskBufferOffset] + pq[1][earliestP] ;
						}
				} else if(heuristicName =="lightestLoadedMaxMinComms") {
					for(int i = slidingWindow -1; i>=0; i--) {
							int earliestP = 0;
							double minTime = 0.0;

							for(int p = 0; p< numProcessors; p++) {
									double pTime = ((pq[1][p])/pq[0][p] );
									Long probID = ((Long)batchQueue.get((int)tasks[0][i+ taskBufferOffset]));
									double taskBytes = ((Problem)problems.get(probID)).getAvgTaskSize();
									double bandwidth =(((clientInfo) clientDetails.get(ips[p])).getBandwidth());
									if(bandwidth > 0)
										pTime += taskBytes/bandwidth;

									pTime +=((clientInfo) clientDetails.get(ips[p])).getLatency();

									if(pTime < minTime || p == 0) {
											minTime = pTime;
											earliestP = p;
										}
								}
							procQueues[earliestP].add(new Double(tasks[0][i+taskBufferOffset]));
							pq[1][earliestP]  = tasks[1][i+taskBufferOffset] + pq[1][earliestP] ;
						}
				}


			int stringcounter = 0;
			for(int i = 0 ; i < numProcessors; i++) {
					for(int k = 0; k < procQueues[i].size(); k++) {
							population[stringcounter][index] = ((Double)procQueues[i].get(k)).doubleValue();
							stringcounter++;
						}
					population[stringcounter][index] = delimiter;
					stringcounter++;
				}
			population[stringSize -1][index] = 0.0;
		}

		public void complexHeuristics(int index, String heuristicName) {
			if(heuristicName =="RC") {
					double[] rcSchedule = convertTaskProcMapping(generateRCSchedule() );
					for(int i = 0; i< stringSize; i++) {
							population[i][index] = rcSchedule[i];
						}
					population[stringSize -1][index] = 0.0;
				} else if(heuristicName =="DL") {
					double[] rcSchedule = convertTaskProcMapping(generateDLschedule( )  );
					for(int i = 0; i< stringSize; i++) {
							population[i][index] = rcSchedule[i];
						}
					population[stringSize -1][index] = 0.0;

				}

		}

		/**This will take in an array with length numTasks. Each element of the array contains a processor number, e.g. mapping
		* It will then convert it to an array so that it can be added to a population of the GA easily*/
		private double[] convertTaskProcMapping(int[] mappings) {
			if(mappings.length != numTasks) {
					System.out.println("error converting task mappings, num tasks not correct");
					return null;
				}



			int i = 0;
			int procCount = 0;
			double [] converted = new double[stringSize];
			while( i< stringSize -1) {

					for(int t = 0; t< numTasks; t++) {
							if(mappings[t] == procCount) {
									converted[i] = t*1.0;
									i++;

								}

						}

					converted[i] = delimiter;

					procCount++;
					i++;
				}




			return converted;
		}


		public int[] generateRCSchedule() {

			int i = 0;


			Hashtable procLoad = currentAssignedLoad();

			/** ETC matrix*/
			double [][] ETC = new double[numTasks][numProcessors];
			double [] avgETC = new double[numTasks];
			boolean [] scheduled = new boolean[numTasks];
			double [][] staticRC = new double[numTasks][numProcessors];
			double [] dynamicRC = new double[numTasks];
			int [] saveSchedule = new int[numTasks];

			double[][] ct = new double[numTasks][numProcessors];
			double alpha = 0.5 ;// used to control the effect of the static relative cost

			for(int t = 0; t< numTasks;t++) {
					scheduled[t] = false;
					for(int p = 0 ; p< numProcessors; p++) {

							Long probID = ((Long)batchQueue.get(t));
							Vector KNNest = ((clientInfo) clientDetails.get(ips[p])).getEstimatedTime(probID ,1.0);
							long knnProcTime = ((Long)KNNest.get(0)).longValue() ;

							ETC[t][p]=(knnProcTime/1000 );
							avgETC[t] = ( (avgETC[t]*p) + ETC[t][p])/(p+1);

							ct[t][p] = ((Double) procLoad.get( ips[p])).doubleValue()/processorQueue[0][p];

						}
				}

			/** Calculate the static relative cost for each task processor pairing*/

			for(int t = 0; t< numTasks ; t++) {
					for(int p = 0; p< numProcessors; p++) {
							if(avgETC[t]  == 0)
								staticRC[t][p] = 0;
							else
								staticRC[t][p]  = ETC[t][p]/avgETC[t] ;
						}
				}


			// go through each unscheduled task
			for(int t = 0; t< numTasks; t++) {
					int BAk = 0;
					int Ak = 0;

					for(int k = 0; k<numTasks ; k++ ) {
							if(scheduled[k] == false) {
									double ctAvg = 0;
									double ctSum = 0;
									double minct = 0;
									int Bi = 0;


									for(int p = 0; p< numProcessors; p++) {
											ctSum +=ct[k][p] ;
											if(minct ==0 || p == 0 || ct[k][p] < minct) {
													minct = ct[k][p];
													Bi = p;
												}
										}
									ctAvg = ctSum/numProcessors;
									dynamicRC[k] = minct/ctAvg;
									double minAk = 0;
									double cost = Math.pow(staticRC[k][Bi],alpha)  + dynamicRC[k]  ;

									if(minAk == -1 || k == 0 || cost <minAk ) {
											minAk = cost;
											BAk = Bi;
											Ak = k;
										}

								}
						}
					//System.out.println(t);
					scheduled[Ak] = true;
					//assignTask(Ak, BAk);
					saveSchedule[Ak] = BAk;

					for(int c = 0; c< numTasks ; c++) {
							ct[c][BAk] +=ETC[c][BAk] ;
						}

				}


			return saveSchedule;
		}

		private int[] generateDLschedule( ) {
			boolean[] scheduled = new boolean[numTasks];
			int[] savedSchedule = new int[numTasks];
			for(int t = 0 ; t< numTasks ; t++) {
					// find processor and task pair with largest DLS
					int largestTaskIndex = 0;
					int largestProcIndex = 0;
					double largestDLS = 0;

					for(int i =0; i< numTasks; i++) {
							if(! scheduled[i]) {
									for(int p = 0; p< numProcessors ; p++) {
											double curDL = dynamicLevel(i,p, savedSchedule, scheduled);
											//System.out.println("dl " + curDL +  " "+ i + " "+ p);
											if(curDL > largestDLS || (i==0 && p==0)) {
													largestProcIndex = p;
													largestDLS = curDL;
													largestTaskIndex = i;
												}
										}
								}
						}

					savedSchedule[largestTaskIndex] = largestProcIndex;
					scheduled[largestTaskIndex] = true;
					//assignTask(largestTaskIndex, largestProcIndex);


				}
			return savedSchedule;
		}

		/** Will return the dynamic level for a given task and processor pair*/
		private double dynamicLevel(int taskID, int procID, int[] savedSchedule, boolean[] scheduled) {
			// dl = execution time - start time + machine speed difference

			// execution time
			double executionTime = 0;
			if(processorQueue[0][procID]==0)
				executionTime = 0;
			else
				executionTime = (tasks[1][taskID])/(processorQueue[0][procID]);

			double curSchedTime = 0;
			for(int i = 0; i< numTasks ; i++) {
					if(scheduled[i] && savedSchedule[i]==procID) {
							if(processorQueue[0][procID]>0)
								curSchedTime += tasks[1][i]/processorQueue[0][i];
						}
				}

			// start time
			double startTime = 0 ;
			if(processorQueue[0][procID]== 0)
				startTime=0;
			else
				startTime = processorQueue[1][procID]/processorQueue[0][procID];

			// machine speed differenece - median execution time of task across all machines
			// less execution time on this processor
			double medianExecutionTime = 0;
			for(int i = 0 ; i< numProcessors; i++) {
					double curExe = 0;
					if(processorQueue[0][i] ==0)
						curExe = 0;
					else
						curExe = tasks[1][taskID]/processorQueue[0][i];

					medianExecutionTime = (medianExecutionTime*i + curExe)/(i+1);
				}
			double machineDiff = medianExecutionTime - executionTime;

			return (executionTime -(startTime +curSchedTime) + machineDiff);
		}

	}
