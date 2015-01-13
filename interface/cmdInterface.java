/*
Thomas Keane, 16:03 28/01/2003
overall class for the gui - sets up and manages overall window
contains main() function to start gui
 
Copyright (C) 2003  Thomas Keane
 
This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.
 
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
*/


import java.io.*;
import java.util.jar.*;
import java.util.Vector;

public class cmdInterface extends RemoteInterface {

		public final int JOBS_VIEW = 0;
		public final int JOB_VIEW = 1;
		public final int CONNECT = 2;
		private GUICommunications communications; //rmi stub for communicating
		private boolean admin = true; //admin or not
		private int socketPort; //socket streaming port
		private int rmiPort;
		private String serverIP; //ip address

		//some information that the interface remembers from last use (the job add screen)
		private File selected_directory;
		private String selected_algorithm;
		private String selected_datamanager;
		private String[] selected_jars;
		private String[] selected_pd;
		private int minCPU;
		private int minMem;


		private cmdAdd j;
		public cmdInterface(String args[]) {




			connectToServer con = new connectToServer( this, "149.157.247.202", 15000, 15001 );
			j = new cmdAdd(this , communications);
			runTest(args);
		}


		//return the socket connection info - needed in problemAdd
		public Vector getSocketInfo() {
			Vector v = new Vector();
			v.add( serverIP );
			v.add( new Integer( socketPort ) );
			return v;
		}

		public static void main( String args[] ) {

			System.out.println("algorithm datamanager description classdefsArray  problemDataArray");
			cmdInterface g = new cmdInterface(args);
			//       System.out.println(args[0] + "\n"+ args[1]+ "\n");
		}
		public void runTest(String args[]) {



			//       j = new cmdAdd(this , communications);
			if(args.length == 3) {
					j.addSmallProblem(new File(args[0]), new File(args[1]), args[2] );
				} else {


					String[] classdefs = args[3].split(" ");
					File[] classfiles = new File[classdefs.length];


					for(int i = 0; i< classdefs.length ; i++) {
							classfiles[i] = new File(classdefs[i]);
						}

					String[] probdata = args[4].split(" ");
					File[] probfile = new File[probdata.length];
					for(int i = 0; i< probdata.length; i++) {
							probfile[i] = new File(probdata[i]);
						}

					if(args[3]=="" && args[4] == "") {
							j.addProblem(new File(args[0]), new File(args[1]), args[2] , null, null);
						} else if(args[3] == "" && args[4] !="") {
							j.addProblem(new File(args[0]), new File(args[1]), args[2] , null, probfile);
						} else if(args[3]!="" && args[4] =="") {
							j.addProblem(new File(args[0]), new File(args[1]), args[2] , classfiles, null);
						} else {
							j.addProblem(new File(args[0]), new File(args[1]), args[2] , classfiles, probfile);
						}
				}
		}
	}
