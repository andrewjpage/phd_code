/*
Thomas Keane, 11:28 AM 2/4/03
class that is used by user to remove problems from the system

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

public class jobRemove extends JPanel implements ActionListener
{
	private RemoteInterface owner;
	private GUICommunications communications;

	private JButton removeB;
	private JTextField jobID;

	public jobRemove( RemoteInterface own, GUICommunications scheduler )
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

		removeB = new JButton( "Remove" );
		removeB.addActionListener( this );

		//add them to the screen
		b.add( IDinfo );
		b.add( Box.createVerticalStrut( 10 ) );
		b.add( removeB );
		this.add( b );
	}

	public void actionPerformed( ActionEvent event )
	{
		if( event.getActionCommand().equals( "Remove" ) )
		{
			//see if the job exists on system
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

			//is a valid ID - check if job exists
			try
			{
				boolean finished = communications.problemFinished( job );
				boolean running = communications.jobRunning( job );
				if( ! finished && running )
				{
					int input = JOptionPane.showConfirmDialog( this, "Job is not Finished - remove anyway?", "Incomplete Job", JOptionPane.YES_NO_OPTION);
					if( input == JOptionPane.NO_OPTION )
					{
						remove( this );
						setVisible( false );
						return;
					}
					else
					{
						//kill the job in the system
						communications.killJob( job );
						JOptionPane.showMessageDialog( this, "Success - Job removed from server", "Success", JOptionPane.INFORMATION_MESSAGE );
						remove( this );
						setVisible( false );
						owner.setScreen( owner.JOBS_VIEW );
						return;
					}
				}
				else if( ! running && finished )
				{
						JOptionPane.showMessageDialog( this, "Error - Job already Finished", "Error", JOptionPane.INFORMATION_MESSAGE );
						remove( this );
						setVisible( false );
						return;
				}
				else{
						JOptionPane.showMessageDialog( this, "Error - Job Does not Exist", "Error", JOptionPane.INFORMATION_MESSAGE );
						return;
				}
			}
			catch( Exception e )
			{
System.out.println( e.toString() );
				JOptionPane.showMessageDialog( this, "Error - Could not connect to Server", "Error", JOptionPane.INFORMATION_MESSAGE );
				owner.setConnected( false );
				remove( this );
				setVisible( false );
				owner.setScreen( owner.CONNECT );
				return;
			}

		}
	}
}