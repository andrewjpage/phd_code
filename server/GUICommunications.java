/*
rmi communication interface between server and upper server

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

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.*;
import java.rmi.Naming;
import java.util.jar.*;

public interface GUICommunications extends Remote
{
	// GUI Remote Methods

	//method to add a new problem to the system
	public boolean addProblem( Long probID, byte[] algorithm, byte[] datamanger, int priority, String description, int expired, int exceptions, Vector classDefinitions, Vector jarDefinitions, int problemCPU, int problemMemory ) throws RemoteException;

	//method to get the reason why a problem failed to load
	public String addProblemFailReason() throws RemoteException;

	// This will delete the corresponding job from the system
	public boolean deleteAllPastProblems() throws RemoteException;

	//check to see if a problem is finished
	public boolean problemFinished( Long ID ) throws RemoteException;

	//download a snap shot of the current set of problems status
	public byte[] getAllProblemStatus() throws RemoteException;

	//get info on a particular problems status
	public Vector getProblemStatus( Long ID ) throws RemoteException;

	//change the priority of a particular job
	public boolean changePriority( Long ID, int priority ) throws RemoteException;

	//update the current version of the client
	public boolean updateClient( byte[] client, String IPRange ) throws RemoteException;

	//pause server - tell server to stop issuing units
	public boolean pause() throws RemoteException;

	//restart server after a pause
	public boolean restart() throws RemoteException;

	//get some statistics on the server
	public byte[] getServerStats() throws RemoteException;

	//check to see if a job exists on the system
	public boolean jobRunning( Long jobID ) throws RemoteException;

	//stop a currently running job
	public void killJob( Long jobID ) throws RemoteException;

	//check if a job has been killed by system
	public boolean jobKilled( Long ID ) throws RemoteException;

	//send over the admin password
	public boolean checkAdmin( byte[] pass ) throws RemoteException;

	//update the server timeout
	public void setTimeout( Long timeout )throws RemoteException;

	//update the optimal unit time and/or tolerance
	public void setUnitTimeAndTolerance( int time, float tolerance ) throws RemoteException;

	//check if there are a partial set of results for a problem - i.e. problem that was stopped
	public boolean partialResults( Long job ) throws RemoteException;
	
	//clear the client version that is on the server to update client software
	public void clearClient() throws RemoteException;
}