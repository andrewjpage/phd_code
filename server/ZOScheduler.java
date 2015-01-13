// Andrew Page 6th of April 2004
// apage@cs.may.ie
/*
Title: Observations on using genetic algorithms for dynamic load-balancing
Authors: Zomaya, A.Y.   Yee-Hwei Teh
Journal: IEEE Transactions on Parallel and Distributed Systems
Date: Sep 2001
Vol & Pages: 899-911  Volume: 12,   Issue: 9
*/
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

public class ZOScheduler extends gaCommon implements NewSchedulingAlgorithm  {

		double startingmakespan = 0.0;
		int genStepSize = 10; // number of generations to perform before reporting back to check efficiency.
		int numRebalance = 1; // number of rebalances per generation per individual in the population
		long startTime = 0;
		String gaBest;
		String gaHeur;
		double[] bestChrom;
		double bestMakespan;

		double lowThres = 0.7;
		double highThres = 1.3;
		double LoadAverage;


		public ZOScheduler() {
			super();
		}

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

					run(500, numTasks, 25, 0.001, 0.02, 0.01);

				}
		}

		private void run( int numGenerations, int slidingWindowSize, int popSize,double commsMinRatio, double commsMaxRatio, double mutations) {
			generations = numGenerations;
			slidingWindow = slidingWindowSize;
			populationSize= popSize;

			int  generationsRun = 0;
			long avgTime = 0;
			double efficiency = 0.0;
			LoadAverage = 0.0;

			taskBufferOffset = 0;
			processorQueue = new double[2][numProcessors];
			for(int i =0; i< numProcessors; i++) {
					Hashtable p = currentAssignedLoad();
					processorQueue[1][i] = ((Double) p.get(ips[i])).doubleValue();
				}



			stringSize = slidingWindow+ numProcessors; // last space is for the Fitness Value;
			population = new double[stringSize][populationSize];
			numMutations =(int )((populationSize*(stringSize-1)) *mutations) ;// number of random mutations to perform every generation

			bestMakespan = -1;
			bestChrom= new double[stringSize];

			int numGenRun  = 0;

			initialise();
			int curGen = 0;

			do {
					runGA(10);
					generationsRun += 10;
					curGen +=10;

				} while(isValid(  ((Integer)((findBest()).get(0))).intValue()  )== false && curGen < 500) ;
			assignTasks();

		}

		private void initialise() {
			int randIndex = 0;
			currentBestIndex = 0; // the current best string in the  population
			BestMaxSpan  = 0;
			// This method will initialise the GA strategy to run using the most into least

			Vector queue = new Vector();

			for(int p = 0; p<numProcessors; p++) {
					processorQueue[0][p] = getProcessorSpeed(p); // initialise processor speeds
				}

			int numRandTasks  = slidingWindow/2;

			if(numRandTasks > slidingWindow) {
					numRandTasks = slidingWindow - 1;
				}


			// randomly initialise each string in the population;
			for(int i = 0; i< populationSize; i++) {

					queue = new Vector();

					// add each task to a queue, then randomly popoff one of the tasks.
					for(int q = 0; q < slidingWindow; q++) {
							queue.add(new Double(tasks[0][q+taskBufferOffset]));
						}

					for(int t = 0;t < slidingWindow; t++) {
							// pick a random task in the queue
							randIndex = rand.nextInt(queue.size());

							double pickedTask = ((Double)queue.get(randIndex)).doubleValue();
							queue.remove(randIndex);
							population[t][i] = pickedTask;

						}




					// now randomly assign queues to processors
					for(int p = 0; p<numProcessors-1; p++) {
							// insert processors at the end of the string
							//System.out.println("init "+population[slidingWindow+p][i]);
							population[slidingWindow+p][i] = delimiter;

							do {
									// randomly select an element of the string which is not a delimiter
									randIndex = rand.nextInt(slidingWindow+p);

								} while(population[randIndex][i] ==delimiter);

							// swap the processor position with the random bit in the string
							double tmp = population[randIndex][i];

							population[randIndex][i]= population[slidingWindow+p][i];

							population[slidingWindow+p][i] = tmp;

						}

					population[stringSize-1][i] = 0.0;

				}




			updateAllFitnessValues();





		}

		private  void runGA(int gen) {
			// run a single generation of the Genetic Algorithm
			// work out the fitness of each string in the population

			for(int b =0; b< gen;b++) {



					rouletteWheel(); // selection



					crossOver();



					randomSwaps(numMutations);// mutation


					updateAllFitnessValues();


				}


		}


		private double fitnessFunction(int stringIndex) {
			// this method will calculate the fitness of a given string
			// fitness = (1/maxSpan)* Average processorUtilization * (no. acceptable queues/ no. processors)

			double maxS = maxSpan(stringIndex);
			double avgUtilization = averageUtilization(stringIndex, maxS);
			double acceptableQueues = (double) numAcceptableQueues(stringIndex, LoadAverage, maxS);

			//System.out.println(maxS + "  "+ avgUtilization+ "  "+ acceptableQueues);



			return ((avgUtilization/maxS)*(acceptableQueues/((double) numProcessors)));

		}



		private double averageFitness(double totalFit) {
			// return the average fitness of the population
			return (totalFit/populationSize);
		}


		private void swap(int stringIndex) {
			// this method will take a string and swap two bits of the string, but will make sure that the bits are not from the same processor

			if(numProcessors <=1 || stringIndex == currentBestIndex) {
					// error checking
					return;
				}

			int loopCount  = 0;

			while (loopCount < stringSize) {
					int first = rand.nextInt(stringSize-1);
					int second = rand.nextInt(stringSize-1);
					loopCount++;

					if(first == second) {} else {
							if(first > second) {
									int tmp = first;
									first = second;
									second = tmp;

								}

							for(int i = first; i <= second; i++) {
									if(population[i][stringIndex] == delimiter) {
											double t = population[first][stringIndex];
											population[first][stringIndex] = population[second][stringIndex];
											population[second][stringIndex] = t;
											return;

										}

								}
						}
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

		private void updateAllFitnessValues() {
			// this method will update all fitness values
			for(int i =0; i< populationSize; i++) {
					population[stringSize-1][i] = fitnessFunction(i);
				}
		}





		private double totalError(int stringIndex) {
			// this will return the total error of the given string
			// sqroot(sum(erroron partition squared))

			double optTime = optimalTime(stringIndex);
			double error = 0.0;

			for(int i =0; i < numProcessors; i++) {
					double curError = Math.abs(optTime - processorCompletionTime(stringIndex, i));
					curError = curError/optTime ;
					curError *= curError;// square it
					error = ((error*i)+ curError)/(i+1);
				}

			return Math.sqrt(error);

		}


		protected double averageUtilization(int stringIndex, double maxS) {
			// this will work out the average processor utilization for a give string
			// average processor utiliazation = processor completion time / maxSpan
			// then the get an average of those values and return

			double avgUtil = 0.0;
			double completionTime = 0.0;

			for(int i =0; i < numProcessors; i++) {
					completionTime = (processorCompletionTime(stringIndex, i)/(maxS));


					avgUtil = ((avgUtil*i) + completionTime)/(i+1);
				}


			return avgUtil;
		}

		protected int numAcceptableQueues(int stringIndex, double avgUtil, double maxS) {
			// this method will find out how many processors are lightly loaded, heavily loaded or are acceptablely loaded
			int acceptable = 0;
			double procLoad = 0.0;

			// Come back to this maxS *****************
			//load average is not a good indicator for heterogeneouse system, hard to compare.
			// use average processorcompletion time
			double lightlyLoaded = avgUtil*lowThres/maxS;
			double heavilyLoaded =  avgUtil*highThres/maxS;

			// find out the loading of each processor in this string
			for(int i=0; i< numProcessors; i++) {
					procLoad = (processorCompletionTime(stringIndex, i)/(maxS));


					if(procLoad > lightlyLoaded && procLoad < heavilyLoaded) {

							acceptable++;
						}

				}

			return acceptable;
		}

		protected double maxSpan(int stringIndex) {
			// this method will compute the maxspan of a given string in the population
			// the maxspan is the longest processing time of all of the processors

			double maxTime = 0.0;
			double tmp =0.0;
			double avgCompletionTime  = 0.0;

			for(int i =0; i<numProcessors; i++) {
					tmp =processorCompletionTime(stringIndex, i);
					avgCompletionTime = (avgCompletionTime*i + tmp)/(i+1);

					if(tmp > maxTime) {
							maxTime = tmp;
						}
				}

			LoadAverage = avgCompletionTime;
			return maxTime;

		}

	}


