/*
rmi communication interface between client and server
 
Copyright (C) 2003  Thomas Keane 

Updated by Andrew Page 10 June 2005 
- extra parameters to send back dynamic info
- new ping function to calculate latency 
 
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

public interface ClientCommunications extends Remote {
		//returns a compressed set algorithm + associated class files
		public Vector getAlgorithm( Long algorithmID ) throws RemoteException;

		//returns a Vector: 0-Long unitID, 1-Long algorithmID, 2-Long timeLimit, 3-byte[] unit
		public Vector getDataUnit( Vector systemInformation , Vector dynamicInfo ) throws RemoteException;

		//returns Long extension
		public Long getExtension( Long unitID, Long algorithmID ) throws RemoteException;

		//send results of unit to server
		public void sendResults( Long unitID, Long algorithmID, Vector results, Vector systemInformation, Vector dynamicInformation ) throws RemoteException;

		//send the details of an error at the client when executing the algorithm
		public void sendAlgorithmError( Long unitID, Long algorithmID, String e ) throws RemoteException;

		//client stub checking server to see if there is a newer version of the client
		public byte[] getNewestVersion() throws RemoteException;

		//client reporting some exception occurring in the client - due to info sent by server
		public void reportException( String ex ) throws RemoteException;

		//test latency
		public void ping( ) throws RemoteException;
	}