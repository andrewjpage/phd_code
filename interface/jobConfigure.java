/*
Thomas Keane, 11:28 AM 2/4/03
class that is used by user to configure a job on the server

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

public class jobConfigure extends JPanel implements ActionListener
{
	private RemoteInterface owner; //owner gui object
	private GUICommunications communications; //rmi comms stub

	private JButton configureB; //configure button
	private JTextField jobID; //job id entered here
	private JTextField priority; //new priority entered here

	public jobConfigure( RemoteInterface own, GUICommunications scheduler )
	{
		owner = own;
		communications = scheduler;

		//set up the components
		Box overall = Box.createVerticalBox();

		JLabel enterID = new JLabel( "Enter ID" );
		jobID = new JTextField( 10 );
		Box b = Box.createHorizontalBox();
		b.add( enterID );
		b.add( Box.createHorizontalStrut( 10 ) );
		b.add( jobID );

		JLabel enterP = new JLabel( "Enter Priority" );
		priority =  new JTextField( 10 );
		Box b1 = Box.createHorizontalBox();
		b1.add( enterP );
		b1.add( Box.createHorizontalStrut( 10 ) );
		b1.add( priority );

		configureB = new JButton( "Configure" );
		configureB.addActionListener( this );

		//add the components
		overall.add( b );
		overall.add( Box.createVerticalStrut( 10 ) );
		overall.add( b1 );
		overall.add( Box.createVerticalStrut( 10 ) );
		overall.add( configureB );

		this.add( overall );
	}

	public void actionPerformed( ActionEvent event )
	{
		if( event.getActionCommand().equals( "Configure" ) )
       		{
			//check the parameters are valid
			Long ID = null;
			Integer pr = null;
			Long ut = null;
			try
			{
				ID = new Long( jobID.getText() );
				pr = new Integer( priority.getText() );
			}
			catch( NumberFormatException e )
			{
				//not a valid long
				JOptionPane.showMessageDialog( this, "Error - Invalid Information Entered", "Error", JOptionPane.INFORMATION_MESSAGE );
				return;
			}

			//check the bounds of the priority
			if( pr.intValue() < 0 )
			{
				JOptionPane.showMessageDialog( this, "Error - Priority must be greater than or equal to zero", "Error", JOptionPane.INFORMATION_MESSAGE );
				return;
			}

			//check that the job exists on the server
			try
			{
				boolean exists = communications.jobRunning( ID );
				boolean finished = communications.problemFinished( ID );

				if( ! exists )
				{
					JOptionPane.showMessageDialog( this, "Error - Job does not Exist", "Error", JOptionPane.INFORMATION_MESSAGE );
					return;
				}
				else if( finished )
				{
					JOptionPane.showMessageDialog( this, "Error - Job Finished", "Error", JOptionPane.INFORMATION_MESSAGE );
					return;
				}

				//configure the job
				if( communications.changePriority( ID, pr.intValue() ) )
				{
					JOptionPane.showMessageDialog( this, "Success - Job Updated", "Success", JOptionPane.INFORMATION_MESSAGE );
					remove( this );
					setVisible( false );
					owner.setScreen( owner.JOBS_VIEW );
					return;
				}
				else{
					JOptionPane.showMessageDialog( this, "Error - Job was not Updated", "Error", JOptionPane.INFORMATION_MESSAGE );
					return;
				}
			}
			catch( Exception e )
			{
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