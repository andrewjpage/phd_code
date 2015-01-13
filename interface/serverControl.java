/*
Thomas Keane, 11:28 AM 2/4/03
class that is used to control certain aspects of the server itself

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

public class serverControl extends JPanel implements ActionListener
{
	private RemoteInterface owner;
	private GUICommunications scheduler;
	private JButton pause;
	private JButton restart;
	private JButton delete;
	private JButton timeout;
	private JButton tolerance;
	private JButton optimal;

	public serverControl( RemoteInterface own, GUICommunications s )
	{
		JPanel info = new JPanel( new GridLayout( 5, 2, 20, 20 ) );
		scheduler = s;
		owner = own;

		pause = new JButton( "Pause" );
		pause.addActionListener( this );
		info.add( pause );

		restart = new JButton( "Restart" );
		restart.addActionListener( this );
		info.add( restart );

		JLabel deleteL = new JLabel( "Delete All Past Problems" );
		delete = new JButton( "Delete" );
		delete.addActionListener( this );
		info.add( deleteL );
		info.add( delete );

		JLabel update = new JLabel( "Update Server Timeout" );
		timeout = new JButton( "Update" );
		timeout.addActionListener( this );
		info.add( update );
		info.add( timeout );

		JLabel update1 = new JLabel( "Update Optimal Unit Time" );
		optimal = new JButton( "Update " );
		optimal.addActionListener( this );
		info.add( update1 );
		info.add( optimal );

		JLabel update2 = new JLabel( "Update Optimal Time Tolerance" );
		tolerance = new JButton( "Update  " );
		tolerance.addActionListener( this );
		info.add( update2 );
		info.add( tolerance );

		this.add( info );
	}

	public void actionPerformed( ActionEvent event )
	{
		//button pressed
		if( event.getActionCommand().equals( "Restart" ) )
		{
			//restart the server
			try
			{
				boolean b = scheduler.restart();

				if( ! b )
				{
					JOptionPane.showMessageDialog( this, "Error - Server already Started", "Error", JOptionPane.INFORMATION_MESSAGE );
				}
				restart.setEnabled( false );
				pause.setEnabled( true );
			}
			catch( Exception e )
			{
				//problem - couldnt get data
				JOptionPane.showMessageDialog( this, "Error - Could not connect to Server", "Error", JOptionPane.INFORMATION_MESSAGE );
				owner.setConnected( false );
				remove( this );
				setVisible( false );
				owner.setScreen( owner.CONNECT );
			}
		}
		else if( event.getActionCommand().equals( "Pause" ) )
		{
			//pause server
			try
			{
				boolean b = scheduler.pause();

				if( ! b )
				{
					JOptionPane.showMessageDialog( this, "Error - Server already Paused", "Error", JOptionPane.INFORMATION_MESSAGE );
				}
				restart.setEnabled( true );
				pause.setEnabled( false );
			}
			catch( Exception e )
			{
				//problem - couldnt get data
				JOptionPane.showMessageDialog( this, "Error - Could not connect to Server", "Error", JOptionPane.INFORMATION_MESSAGE );
				owner.setConnected( false );
				remove( this );
				setVisible( false );
				owner.setScreen( owner.CONNECT );
			}
		}
		else if( event.getActionCommand().equals( "Delete" ) )
		{
			try
			{
				//confirm operation
				int input = JOptionPane.showConfirmDialog( this, "Are you sure you want to delete all past job results and files?", "Delete", JOptionPane.YES_NO_OPTION);

				if( input == JOptionPane.NO_OPTION ){
					return;
				}
				else
				{
					if( scheduler.deleteAllPastProblems() )
					{
						JOptionPane.showMessageDialog( this, "Success - All Past Problems Files Deleted", "Success", JOptionPane.INFORMATION_MESSAGE );
						return;
					}
					else
					{
						JOptionPane.showMessageDialog( this, "Error - Files not Deleted", "Error", JOptionPane.INFORMATION_MESSAGE );
						return;
					}
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
		else if( event.getActionCommand().equals( "Update" ) )
		{
			//request timeout value
			String inputValue = JOptionPane.showInputDialog( "Enter New Timeout" );

			if( inputValue == null )
			{
				return;
			}

			//convert to an number
			Long time = null;
			try
			{
				time = new Long( inputValue );
			}
			catch( NumberFormatException nfe )
			{
				JOptionPane.showMessageDialog( this, "Invalid Input", "Error", JOptionPane.INFORMATION_MESSAGE );
				return;
			}

			//make sure valid number
			if( time.longValue() <= 0 )
			{
				JOptionPane.showMessageDialog( this, "Invalid Input - must be positive", "Error", JOptionPane.INFORMATION_MESSAGE );
				return;
			}

			//change timeout on server
			try
			{
				scheduler.setTimeout( time );
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
			JOptionPane.showMessageDialog( this, "Success - Timeout Updated", "Success", JOptionPane.INFORMATION_MESSAGE );
		}
		else if( event.getActionCommand().equals( "Update " ) )
		{
			//request timeout value
			String inputValue = JOptionPane.showInputDialog( "Enter New Optimal Unit Time" );

			if( inputValue == null )
			{
				return;
			}

			//convert to an number
			int time = 0;
			try
			{
				time = Integer.parseInt( inputValue );
			}
			catch( NumberFormatException nfe )
			{
				JOptionPane.showMessageDialog( this, "Invalid Input", "Error", JOptionPane.INFORMATION_MESSAGE );
				return;
			}

			if( time <= 0 )
			{
				JOptionPane.showMessageDialog( this, "Invalid Input - must be positive", "Error", JOptionPane.INFORMATION_MESSAGE );
				return;
			}

			//change timeout on server
			try
			{
				scheduler.setUnitTimeAndTolerance( time, 0.0f );
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
			JOptionPane.showMessageDialog( this, "Success - Server Updated", "Success", JOptionPane.INFORMATION_MESSAGE );
		}
		else if( event.getActionCommand().equals( "Update  " ) )
		{
			//request timeout value
			String inputValue = JOptionPane.showInputDialog( "Enter New Unit Time Tolerance (decimal less than 1)" );

			if( inputValue == null )
			{
				return;
			}

			//convert to an number
			float tol = 0.0f;
			try
			{
				tol = Float.parseFloat( inputValue );
			}
			catch( NumberFormatException nfe )
			{
				JOptionPane.showMessageDialog( this, "Invalid Input", "Error", JOptionPane.INFORMATION_MESSAGE );
				return;
			}

			//check the value is within bounds
			if( tol > 1 || tol < 0 )
			{
				JOptionPane.showMessageDialog( this, "Invalid Input - must be positive and less than 1", "Error", JOptionPane.INFORMATION_MESSAGE );
				return;
			}

			//change timeout on server
			try
			{
				scheduler.setUnitTimeAndTolerance( 0, tol );
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
			JOptionPane.showMessageDialog( this, "Success - Server Updated", "Success", JOptionPane.INFORMATION_MESSAGE );

		}
	}
}