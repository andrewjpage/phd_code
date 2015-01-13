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

/**
*GA scheduler common classes- This class contains all of the commonly used scheduling functions for my experiments with GA's
* Copyright (C) 2004, 2005 Andrew Page.
* Started - 10th June 2004
* This class will be merged with SchedulerCommon eventually
* @author Andrew Page
*/


import java.util.*;
import java.lang.*;
import java.io.*;
import java.security.*;
import java.math.*;

public class gaCommon extends SchedulerCommon {

		protected  int numProcessors;
		protected  Random rand;


		protected double[][] tasks; // 0 = task ID, 1= task MFLOPs required,
		protected int numTasks; // the number of tasks to be processed
		//protected  int numProcessors;
		protected  int generations;
		protected  int slidingWindow;
		protected  double[][] population;
		protected  int stringSize;
		protected  int populationSize;
		protected  double[][] processorSpeed; // the speed of the processors in MFLOPS and the number of each processor type available
		protected  int numProcessorTypes; // this is the number of different types of processors, for a homogeneous system this is 1

		protected  double delimiter = -1.0;
		protected  int numSwaps = 10; // number of random swaps to preform when initalising strings
		protected  int numMutations ;
		protected  int taskBufferOffset;

		protected  double [][]processorQueue; // the processor number is its index and and the existing load in MFLOPS is stored, since tasks cannot be migrated once allocated
		// also [0][] is speed in mflops of processor and [1][] is existing load

		protected  double currentTime; // how much time the system has spent on scheduling.

		protected  int selectForward ; // this is used to tell roulette wheel selection and crossover how many strings are selected to go forwardfrom one pop to the next

		protected boolean[] selectedStrings;

		protected int currentBestIndex; // the current best string in the  population
		protected double BestMaxSpan ; // the maxspan of the current best

		protected Hashtable cache;
		protected int curGen = 0;

		protected  int bestProc = 0;
		protected boolean[] dirty ; // this controls strings in the population, and if  they have been modified will require fitness to be recalculated

		gaCommon() {

			rand = new Random();
			cache = new Hashtable();

		}


		/** Remove this method eventually and just have direct access to processor speed array
		* complexity = 1
		*/
		protected double getProcessorSpeed(int procCount) {
			return processorSpeed[procCount][0];
		}



		/**
		* this will return the index of the fittest string in the population
		* Worst Case: O(s)
		* Average Case: O(s)
		* Best Case: O(s)
		*/
		protected  Vector findBest() {
			int fittest = 0;
			double largestFF = 0;

			/** find the lowest makespan */
			for(int i = 0; i< populationSize; i++) {
					//System.out.println(" " + population[stringSize-1][i] + " " + largestFF + " "+ fittest + " "+ i + " "+ (1/population[stringSize-1][i]) + " "+ (1/ largestFF ));
					if(population[stringSize-1][i] >largestFF || i==0) {
							largestFF = population[stringSize-1][i];
							fittest = i;
						}
				}

			Vector res = maxSpanProcessor(fittest);
			int procID = ((Integer)res.get(0)).intValue();
			double curMaxSpan = ((Double)res.get(1)).doubleValue();


			currentBestIndex = fittest;

			Vector result = new Vector();
			result.add(0,new Integer(fittest));
			result.add(1,new Integer(procID));
			result.add(2,new Double(curMaxSpan));
			return result;
		}

		protected int findWorst() {
			int worst = 0;
			double smallestFF = 0;
			/** find the lowest makespan */
			for(int i = 0; i< populationSize; i++) {

					if(population[stringSize-1][i] < smallestFF || i==0) {
							smallestFF = population[stringSize-1][i];
							worst = i;
						}
				}

			return worst;
		}


		/**
		* given a taskID this method will look up and return the corresponding	size of the task
		* assuming taskID is index of task array, e.g. tasks are assigned id numbers sequenctially
		* complexity = 1
		*/

		protected double getTaskSize(double taskID) {

			return tasks[1][(int)(taskID)];
		}


		/**
		    * this method will get the GA with the best Fitness Function
			* and will use its scheme to assign tasks to processors.
			* complexity = O(s)
			*/
		protected void assignTasks() {
			Vector b =findBest();
			int fittest =  ((Integer)b.get(0)).intValue();

			//allocate the tasks to processors
			// since tasks cant be reassigned after allocation just put in the amount of processing they require to speed things up
			int procNum = 0;

			for(int i = 0; i< stringSize -1; i++) {
					if( population[i][fittest] == delimiter ) {
							procNum++;
						} else {
							clientInfo tmpc = (clientInfo) clientDetails.get(ips[procNum]) ;
							Vector tmpSched = tmpc.getSchedule();

							tmpSched.add(batchQueue.get(((int)population[i][fittest])));

							//System.out.println("GA task "+((Long)batchQueue.get(((int)population[i][fittest]))).longValue()   );
						}
				}
		}



		/** find the total fitness of all the strings in the population
		* Complexity = O(1)
		*/
		protected double totalFitness() {

			double total = 0.0;

			for(int i = 0; i<populationSize; i++ ) {
					total += population[stringSize-1][i];
				}

			return total ;
		}


		/** this will find the relivant processor then work out its completion time for a give string from the population
		* uses knn*/
		protected double processorCompletionTime(int stringIndex, int processorNumber) {
			int procCount = 0;
			int bitCount = 0;

			if(processorNumber > numProcessors || processorNumber < 0) {
					// An error has occured so stop
					return 0.0;
				}

			// find the start of the processor queue
			while(procCount != processorNumber && bitCount< stringSize-1) {
					if(population[bitCount][stringIndex] == delimiter) {
							procCount++;
						}
					bitCount++;
				}

			double totalTime = 0.0;
			double pSpeed = processorQueue[0][procCount]; // the speed in MFLOPS of the processor

			// add up the processing time of each task until the end of the processor queue is reached
			while(bitCount < stringSize-1 &&population[bitCount][stringIndex] !=delimiter) {
					Long probID = ((Long)batchQueue.get((int)population[bitCount][stringIndex]));
					Vector KNNest = ((clientInfo) clientDetails.get(ips[procCount])).getEstimatedTime(probID ,1.0);


					long knnProcTime = ((Long)KNNest.get(0)).longValue() ;
					long knnNetTime = ((Long)KNNest.get(1)).longValue();

					if(knnProcTime > 0) {
							// use knn
							totalTime +=(knnProcTime + knnNetTime);
						} else {
							totalTime += tasks[1][((int)population[bitCount][stringIndex])]/pSpeed;
						}

					bitCount++;
				}
			return totalTime +(processorQueue[1][processorNumber]/pSpeed);
		}


		/**  this method will compute the maxspan of a given string in the population
			* the maxspan is the longest processing time of all of the processors
			* uses KNN estimation
			* @param stringindex the index of the string in the population that you want to find the makespan of
		* Worst Case: O(s)
		* Average Case: O(s)
		* Best Case: O(s)*/
		protected Vector maxSpanProcessor(int stringIndex) {
			double maxTime = 0.0;
			int procCount = 0;
			double pSpeed = processorQueue[0][procCount]; // the speed in MFLOPS of the processor
			double totalTime = (processorQueue[1][procCount]/pSpeed);
			int maxProc = 0;
			long knnProcTime =0;
			long knnNetTime = 0;
			double taskBytes = 0;

			int bitCount=0;
			while( bitCount< stringSize-1) {
					while(bitCount < stringSize-1 && population[bitCount][stringIndex] == delimiter) {

							if(totalTime >maxTime) {
									maxTime = totalTime;
									totalTime = (processorQueue[1][procCount]/pSpeed);
									maxProc = procCount;
								}
							procCount++;
							pSpeed = processorQueue[0][procCount];
							bitCount++;
						}

					if(bitCount < stringSize-1) {
							Long probID = ((Long)batchQueue.get((int)population[bitCount][stringIndex]));
							Vector KNNest = ((clientInfo) clientDetails.get(ips[procCount])).getEstimatedTime(probID ,1.0);
							knnProcTime = ((Long)KNNest.get(0)).longValue() ;


							if(knnProcTime > 0) {
									// use knn
									knnNetTime = ((Long)KNNest.get(1)).longValue();
									totalTime +=(((knnProcTime)/1000) + ((knnNetTime)/1000));
								} else {
									// fall back. if there is not enough info for knn to work then use the estimate of the
									// task mflops and the processor speed.
									totalTime += tasks[1][((int)population[bitCount][stringIndex])]/pSpeed;

									probID = ((Long)batchQueue.get((int)population[bitCount][stringIndex]));
									taskBytes = ((Problem)problems.get(probID)).getAvgTaskSize();

									totalTime +=(((clientInfo) clientDetails.get(ips[procCount])).getLatency())/1000;
								}
							bitCount++;
						}
					// add on the existing processing assigned to processor
					//bitCount++;
				}

			if(totalTime >maxTime) {
					maxTime = totalTime;
					maxProc = procCount;
				}

			Vector result = new Vector();
			result.add(0,new Integer(maxProc));
			result.add(1,new Double(maxTime));
			return result;

		}

		/** this will find the relivant processor then work out its completion time for a give string from the population
		    * complexity for homo & hetero worst case = #processors + size of sliding window
		* Worst Case: O(s)
		* Average Case: O(s/2 + p/n)
		* Best Case: O(1)
		*/
		protected double commsProcessorCompletionTime(int stringIndex, int processorNumber) {
			int procCount = 0;
			int bitCount = 0;

			if(processorNumber >= numProcessors || processorNumber < 0) {
					// An error has occured so stop
					System.out.println(processorNumber +" invalid processor number in processor completion time");
					return 0.0;
				}

			// find the start of the processor queue
			while(procCount != processorNumber && bitCount< stringSize-1) {
					if(population[bitCount][stringIndex] == delimiter) {
							procCount++;
						}
					bitCount++;
				}

			double totalTime = 0.0;
			double pSpeed = processorQueue[0][procCount]; // the speed in MFLOPS of the processor



			// add up the processing time of each task until the end of the processor queue is reached
			while(bitCount < stringSize-1 &&population[bitCount][stringIndex] !=delimiter) {
					totalTime += tasks[1][((int)population[bitCount][stringIndex])]/pSpeed;

					Long probID = ((Long)batchQueue.get((int)population[bitCount][stringIndex]));
					double taskBytes = ((Problem)problems.get(probID)).getAvgTaskSize();

					double bandwidth =(((clientInfo) clientDetails.get(ips[processorNumber])).getBandwidth());
					if(bandwidth > 0)
						totalTime += taskBytes/bandwidth;

					totalTime +=((clientInfo) clientDetails.get(ips[processorNumber])).getLatency();

					bitCount++;
				}

			// add on the existing work in the processors queue
			return totalTime +(processorQueue[1][processorNumber]/pSpeed);
		}

		/** this method will select strings of the population using the roulette wheel method
		* Worst Case: O(2s)
		* Average Case: O(s + n/2)
		* Best Case: O(2s )
		*/
		protected void rouletteWheel() {
			selectForward = populationSize/2; // hard coded remove later e.g. 50 % of population goes forward in each generation
			selectedStrings = new boolean[populationSize];

			double totalFit = totalFitness();
			double[] probability = new double[populationSize];

			double cumulativeProb = 0.0;
			//double totalInverse = 0.0;

			//for(int i = 0; i< populationSize; i++) {
			//		totalInverse += totalFit/population[stringSize-1][i];
			//	}

			// work out the probabilty of a given string going forward
			for(int i = 0; i< populationSize; i++) {

					//probability[i]  = (totalFit/population[stringSize-1][i])/totalInverse;
					probability[i]  = (population[stringSize-1][i])/totalFit;
					cumulativeProb += probability[i];
					probability[i] = cumulativeProb;
					selectedStrings[i] = false;

				}

			//totalFit = totalInverse;

			probability[populationSize-1] = 1.0; // fix round off error in last probabily value

			//Vector b =findBest();
			//int elite =  ((Integer)b.get(0)).intValue();
			int elite =  bestProc;
			currentBestIndex = elite;
			selectedStrings[elite] = true;

			int a = 1;
			int loopcounter = 0;

			while(a<selectForward && loopcounter < selectForward*10) {
					loopcounter++;
					double randForward = rand.nextDouble(); // get a random number between 0 & 1

					// do a binary search here to reduce complexity
					for(int i =0; i< populationSize;i++) {
							if(randForward< probability[i] && !selectedStrings[i]  ) {


									selectedStrings[i] = true;
									i =  populationSize;
									a++;

								}
						}
				}
		}

		/**
		* crossover two elements in a circular fashion
		* return an array with 2 rows containing the children of the crossedover parents
		*	 a way to reduce complexity is to sort the taskIDs in another array.  (s+p)log(s+p)
		*	 then when it comes to doing the crossover, just swap from one array to the other, thus doing it in O(s+p)
				* Worst Case: O(2s + n^2 + n)
		* Average Case: O(2S + (n^2)/2 + n)
		* Best Case: O(2s + n)
		*/
		protected double[][] cycleCrossover(int first, int second) {
			double[][] noDelim = new double[slidingWindow][2];
			double[][] children = new double[stringSize][2];
			int delim1 = 0;
			int delim2 = 0;

			//remove delimiters and store two strings in tmp array
			for(int i = 0; i< stringSize-1; i++) {
					//System.out.println("" +stringSize + " "+ slidingWindow + " "+ numProcessors);
					//if(delim1 >=slidingWindow || delim2 >=slidingWindow) {
					// error has occured so the string is not valid.
					//		for(int a = 0; a< stringSize; a++) {
					//				children[a][0] = population[a][first];
					//				children[a][1] = population[a][second];
					//			}
					//		return children;
					//	}

					if(population[i][first] != delimiter) {
							noDelim[delim1][0] = population[i][first] ;
							delim1++;

						}

					if(population[i][second] != delimiter) {
							noDelim[delim2][1] = population[i][second] ;
							delim2++;
						}
				}

			int startIndex = rand.nextInt(slidingWindow);

			//int c = 0;
			//int preventInfiniteLoop = 0;
			int[] visited = new int[slidingWindow];
			int currentIndex = startIndex;
			//visited[startIndex] = 1;
			boolean finished = false;
			int tmp = 0;
			boolean stopSearch = false;
			int initSearchIndex = 0;
			int initUpperBound = slidingWindow -1;

			while(!finished) {

					while(initSearchIndex< slidingWindow && visited[initSearchIndex]==1)
						initSearchIndex++;

					int searchIndex = initSearchIndex;

					while(initUpperBound > searchIndex && visited[initUpperBound] == 1)
						initUpperBound--;
					int upperbound = initUpperBound;


					stopSearch = false;

					while(!stopSearch) {

							if(searchIndex >upperbound) {
									stopSearch = true;
									System.out.println("error in cycle crossover");
								} else if(noDelim[currentIndex][1] == noDelim[searchIndex][0]) {

									stopSearch = true;
									visited[searchIndex] = 1;
									currentIndex = searchIndex;
								} else {
									//do {
									searchIndex++;
									//} while(searchIndex<= upperbound && visited[searchIndex]==1);


								}
						}

					if(noDelim[startIndex][0] == noDelim[currentIndex][1]) {
							// you've got a completed cycle
							// make sure that the cycle begins and ends at the same point because you allow duplicate bits
							visited[startIndex] = 1;
							// get out of loop
							finished = true;
						}

					// avoid infinite loops
					if( searchIndex>= slidingWindow) {
							System.out.println("error inf loop avoided");
							finished = true;
							searchIndex = searchIndex%slidingWindow;
						}

				}
			double swapTmp = 0.0;

			// now keep the indexs you visited and swap all the other elements
			for(int i = 0; i< slidingWindow; i++) {

					if(visited[i] != 1) {
							swapTmp = noDelim[i][0];
							noDelim[i][0] = noDelim[i][1];
							noDelim[i][1] = swapTmp;
						}
				}

			// put back in delimiters
			int firstCount = 0;

			int secondCount = 0;

			for(int i = 0; i< stringSize-1; i++) {
					// looks at the original strings in the population and insert the delimiters at the correct indices

					if(population[i][first]== delimiter || firstCount >= slidingWindow) {
							// 	put in a delimiter in the child string
							children[i][0] = delimiter;


						} else {
							children[i][0] = noDelim[firstCount][0];
							firstCount++;
						}

					if(population[i][second]== delimiter || secondCount >=slidingWindow) {
							// 	put in a delimiter in the child string
							children[i][1] = delimiter;

						} else {
							children[i][1] = noDelim[secondCount][1];
							secondCount++;
						}
				}

			return children;

		}


		/**  this method will partition the population into two, then randomly select a parent from each to produce two children
				* Worst Case: O(3s + n^2 + n)
		* Average Case: O(3S + (n^2)/2 + n)
		* Best Case: O(3s + n)
		*/
		protected void crossOver() {
			int numPartition = populationSize/2; // work out number of strings in first partition
			int indexCount =numPartition; // this is the partition point between the two groups of strings
			double[][] nextGenPop = new double[stringSize][populationSize];
			int popCounter = 0;

			// strings from the population have already been selected. This copys the selected strings to the next generation.
			for(int a = 0; a<populationSize; a++) {
					if(selectedStrings[a] ) {
							for(int c= 0; c< stringSize; c++) {
									nextGenPop[c][popCounter]=population[c][a];
								}
							popCounter++;
						}
				}


			for(int i =popCounter ; i< populationSize; i=i+2) {
					int first =0;
					int second =0;

					first = rand.nextInt(indexCount); // select index from first

					second = (rand.nextInt(populationSize-indexCount))+ indexCount; // select index from second

					double[][] children = cycleCrossover(first,second);

					for(int c= 0; c< stringSize; c++) {

							nextGenPop[c][i]=children[c][0];


							if((i+1 )<populationSize) {
									nextGenPop[c][i+1]=children[c][1];
								}
						}

				}
			population = nextGenPop; // update the population for the next generation

		}

		/** prints out a row in the population
		* Complexity = O(s)*/
		public String printRow(int stringIndex) {
			String s = "";

			for(int i =0; i< stringSize; i++) {
					s = s+ " "+ population[i][stringIndex];
				}

			return s;
		}
		/** takes in the index of a string in the population and will return
		      * if it is valid or not*/
		protected boolean isValid(int stringIndex) {
			// check the string is in the population
			if(stringIndex < 0 ||stringIndex >=populationSize) {
					System.out.println("error: invalid stringIndex: " + stringIndex);
					return false;
				}

			// you need at least one processor
			if(numProcessors <1) {
					System.out.println("error: not enough processors: " +numProcessors);
					return false;
				}

			int numDelimiters = 0;
			// check the number of delimiters/ processor queues
			for(int i = 0; i< stringSize -1 ; i++) {
					if(population[i][stringIndex]== delimiter)
						numDelimiters++;
				}

			if(numProcessors != numDelimiters+1) {
					System.out.println("error: number of processor queues doesnt match: numprocs" +
					                   numProcessors + " delimiters "+ numDelimiters);
					return false;
				}



			// check tasks for repeated tasks - task ids are unique
			for(int i = 0; i< stringSize-1 ; i++) {
					for(int b =i+1; b< stringSize-1; b++) {
							if(population[i][stringIndex] == population[b][stringIndex]
							    && population[i][stringIndex] != delimiter) {
									System.out.println("error repeated unique task id: "
									                   + population[i][stringIndex] + " "+
									                   population[b][stringIndex]);
									return false;
								}
						}
					if (population[i][stringIndex] != ((int)
					                                   population[i][stringIndex])*1.0) {
							System.out.println("error unique  task id isnt valid: "+
							                   population[i][stringIndex] );
							return false;
						}
				}

			return true;
		}


		protected double optimalTime(int stringIndex) {
			double totalProcessing = 0;
			double processingPower = 0;
			for(int i =0; i < numProcessors; i++) {
					totalProcessing += (processorCompletionTime(stringIndex, i)*processorQueue[0][i]);
					processingPower += processorQueue[0][i];
				}
			return (1.0*totalProcessing)/processingPower;

		}




	}
