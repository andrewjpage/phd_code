/*
Thomas Keane, 11:28 AM 2/4/03
class that is used to get user to initially connect to the server
connection is in 2 modes - admin mode and user mode

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
import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.security.MessageDigest;
import java.util.Calendar;
import java.util.Date;

public class connectToServer extends JPanel implements ActionListener
{
	private RemoteInterface owner; //overall GUI class
	private JTextField port1; //rmi port
	private JTextField ip1; //server ip
	private JTextField port2; //socket port
	private JPasswordField pass; //admin passw

	public connectToServer( RemoteInterface own, String ipA, int rmiP, int socketP )
	{
		super( new GridLayout( 5, 2, 10, 10 ) );
		owner = own;
		//this.setFont( owner.defaultFont );

		//set up the components
		JLabel ip = new JLabel( "IP" );
		//ip.setFont( owner.defaultFont );
		if( ip != null )
		{
			ip1 = new JTextField( ipA, 20 );
		}
		else
		{
			ip1 = new JTextField( "", 20 );
		}

		JLabel port = new JLabel( "RMI Port" );
		if( rmiP != 0 )
		{
			port1 = new JTextField( "" + rmiP, 10 );
		}
		else
		{
			port1 = new JTextField( "14000", 10 );
		}

		JLabel socketPort = new JLabel( "Socket Port" );
		if( socketP != 0 )
		{
			port2 = new JTextField( "" + socketP, 10 );
		}
		else
		{
			port2 = new JTextField( "14001", 10 );
		}

		JLabel admin = new JLabel( "Admin Password (optional)" );
		pass = new JPasswordField( 15 );

		JButton connect = new JButton( "Connect" );
		JButton cancel = new JButton( "Cancel" );

		//add listener to the button
		connect.addActionListener( this );
		cancel.addActionListener( this );

		//add these components
		this.add( ip );
		this.add( ip1 );
		this.add( port );
		this.add( port1 );
		this.add( socketPort );
		this.add( port2 );
		this.add( admin );
		this.add( pass );
		this.add( connect );
		this.add( cancel );
	}

	public void actionPerformed( ActionEvent event )
	{
		//button pressed
       		if( event.getActionCommand().equals( "Connect" ) )
       		{
			if( owner.getConnected() )
			{
				JOptionPane.showMessageDialog( this, "Error - Already connected to Server", "Error", JOptionPane.INFORMATION_MESSAGE );
				return;
			}
				
			try
			{
				if( ip1.getText().length() == 0 || port1.getText().length() == 0 || port2.getText().length() == 0 )
				{
					JOptionPane.showMessageDialog( this, "Error - Insufficient server connection information entered", "Error", JOptionPane.INFORMATION_MESSAGE );
					return;
				}

				//attempt to connect to the server
				Registry registry = LocateRegistry.getRegistry( ip1.getText().trim(), ( new Integer( port1.getText().trim() ) ).intValue() );
				GUICommunications s = ( GUICommunications ) registry.lookup( "scheduler" );
				int socket_port = ( new Integer( port2.getText() ) ).intValue();
				int rmi_port = ( new Integer( port1.getText().trim() ) ).intValue();

				String password = new String( pass.getPassword() );
				if( ! password.equals( "" ) )
				{
					Calendar cal = Calendar.getInstance();
					int doy = cal.get(Calendar.DAY_OF_YEAR);
					int y = cal.get( Calendar.YEAR );

					Date dateValue = cal.getTime();
					dateValue.setTime( dateValue.getTime() - cal.get( Calendar.ZONE_OFFSET ) );
					int hour = dateValue.getHours();

					//append to pass word
					password = password + hour + doy + y;
					MessageDigest md = MessageDigest.getInstance( "MD5" );
					byte[] b = password.getBytes();
					md.update( b );
					byte[] garbled = md.digest();

					//send to server
					if( s.checkAdmin( garbled ) )
					{
						//admin password was correct - admin mode
						owner.setAdmin();
						JOptionPane.showMessageDialog( this, "Success - Connected as Admin", "Admin", JOptionPane.INFORMATION_MESSAGE );
						owner.setCommunications( s, ip1.getText().trim(), rmi_port, socket_port );
						owner.setConnected( true );
						remove( this );
						setVisible( false );
						owner.setScreen( owner.JOBS_VIEW );
						return;
					}
					else
					{
						JOptionPane.showMessageDialog( this, "Incorrect password - try again", "Error", JOptionPane.INFORMATION_MESSAGE );
						return;
					}
				}
				else
				{
					owner.setCommunications( s, ip1.getText().trim(), rmi_port, socket_port );
					owner.setConnected( true );
					JOptionPane.showMessageDialog( this, "Success - Connected to Server", "Success", JOptionPane.INFORMATION_MESSAGE );
					remove( this );
					setVisible( false );
					owner.setScreen( owner.JOB_VIEW );
				}
			}
			catch( Exception e )
			{
System.out.println( e.toString() );
				owner.setConnected( false );

				//tell user not connected
				JOptionPane.showMessageDialog( this, "Error - Could not connect to Server", "Error", JOptionPane.INFORMATION_MESSAGE );
			}
		}
		else if( event.getActionCommand().equals( "Cancel" ) )
       		{
			owner.setConnected( false );
			JOptionPane.showMessageDialog( this, "Error - Not connected to the Server", "Error", JOptionPane.INFORMATION_MESSAGE );
			remove( this );
			setVisible( false );
		}
	}
}