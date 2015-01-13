/*
Thomas Keane, 15:09 12/02/2003
class that is used to get the results of a job from the server
the results are streamed from the server via a socket connection

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

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.Vector;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class jobResults extends JPanel implements ActionListener
{
	private RemoteInterface owner;
	private GUICommunications communications;

	private JButton getResultsB;
	private JTextField jobID;
	private JProgressBar progress;

	public jobResults( RemoteInterface own, GUICommunications scheduler )
	{
		Box b = Box.createVerticalBox();
		owner = own;
		communications = scheduler;

		//set up the screen components
		JLabel enterID = new JLabel( "Enter ID" );
		jobID = new JTextField( 15 );

		Box IDinfo = Box.createHorizontalBox();
		IDinfo.add( enterID );
		IDinfo.add( Box.createHorizontalStrut( 5 ) );
		IDinfo.add( jobID );

		getResultsB = new JButton( "Get Results" );
		getResultsB.addActionListener( this );

		//add them to the screen
		b.add( IDinfo );
		b.add( Box.createVerticalStrut( 10 ) );
		b.add( getResultsB );

		this.add( b );
	}

	public void actionPerformed( ActionEvent event )
	{
		if( event.getActionCommand().equals( "Get Results" ) )
		{
			Long job = null;
			try
			{
				job = new Long( jobID.getText() );
			}
			catch( NumberFormatException e )
			{
				//not a valid long
				JOptionPane.showMessageDialog( this, "Error - Not a Valid Job ID", "Error", JOptionPane.INFORMATION_MESSAGE );
				return;
			}

			try
			{
				//check if the job is still in the system first
				if( communications.jobRunning( job ) )
				{
					JOptionPane.showMessageDialog( this, "Error - Job still running in Server", "Error", JOptionPane.INFORMATION_MESSAGE );
					return;
				}

				//check that the problem is finished
				if( ! communications.problemFinished( job ) )
				{
					if( communications.partialResults( job ) )
					{
						String s = downloadResults( job );
						JOptionPane.showMessageDialog( this, "Downloaded Partial Results - check problem log files to file: " + s, "Success", JOptionPane.INFORMATION_MESSAGE );
						return;
					}

					JOptionPane.showMessageDialog( this, "Error - No results for job in Server", "Error", JOptionPane.INFORMATION_MESSAGE );
					return;
				}
			}
			catch( Exception e )
			{
System.out.println( e.toString() );
				//comms problem
				JOptionPane.showMessageDialog( this, "Error - Could not connect to Server", "Error", JOptionPane.INFORMATION_MESSAGE );
				owner.setConnected( false );
				remove( this );
				setVisible( false );
				owner.setScreen( owner.CONNECT );
				return;
			}
				
			try
			{
				String s = downloadResults( job );
				JOptionPane.showMessageDialog( this, "Results downloaded to File: " + s, "Success", JOptionPane.INFORMATION_MESSAGE );
				if( owner.getAdmin() )
				{
					owner.setScreen( owner.JOBS_VIEW );
				}
				else
				{
					remove( this );
					setVisible( false );
				}
				
				return;
			}
			catch( Exception e )
			{
				JOptionPane.showMessageDialog( this, "Error - Could not Download results to Server", "Error", JOptionPane.INFORMATION_MESSAGE );
				return;
			}
		}
	}

	//method to download a partial set of results from the server in the form of a zip file
	private String downloadResults( Long job ) throws Exception
	{
		//ask the user where to save the results to
		File directory = null;
		JFileChooser saveDirectory = new JFileChooser( new File( System.getProperty( "user.dir" ) ) );
		saveDirectory.setFileFilter( new filter( "" ) );
		saveDirectory.setSelectedFile( null );
		saveDirectory.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );
		saveDirectory.setDialogTitle( "Choose a directory" );
		saveDirectory.setApproveButtonText( "Save to" );
		int option = saveDirectory.showOpenDialog( this );
		if( option == JFileChooser.APPROVE_OPTION )
		{
			directory = saveDirectory.getSelectedFile();
		}
		File f = new File( directory, job.toString() + ".zip" );
		
		//contact server via socket and stream download of results
		Vector socketInfo = owner.getSocketInfo();
		Socket socket = new Socket( (String) socketInfo.get( 0 ), ( (Integer) socketInfo.get( 1 ) ).intValue() );
		GZIPOutputStream gzipout = new GZIPOutputStream( socket.getOutputStream() );

		DataOutputStream ds = new DataOutputStream( gzipout );
		ds.writeUTF( "r:" + job.toString() + ":" + job.toString() + ".zip" );
		gzipout.finish();
		ds.flush();
		FileOutputStream fos = new FileOutputStream( f );
		byte[] buffer = new byte[ socket.getReceiveBufferSize() ];
		int numRead = 1;
		DataInputStream dis = new DataInputStream( new GZIPInputStream( socket.getInputStream() ) );

		//stream the results to disk
		while( numRead > 0 )
		{
			numRead = dis.read( buffer );
			if( numRead > 0 )
			{
				fos.write( buffer, 0, numRead );
			}
		}
		ds.close();
		dis.close();
		fos.close();
		
		return f.getAbsolutePath();
	}
}