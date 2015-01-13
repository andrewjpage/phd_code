/*
Thomas Keane, 11:28 AM 2/4/03
class that is used to get various statistics about the server

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
import java.util.Vector;

public class serverStatistics extends JPanel implements ActionListener
{
	private RemoteInterface owner;
	private GUICommunications scheduler;

	private JButton configureB;
	private JTextArea stats;
	
	private String serverIP;
	private int rmiPort;

	public serverStatistics( RemoteInterface own, GUICommunications s, String sIP, int rmiP )
	{
		Box b = Box.createVerticalBox();
		scheduler = s;
		owner = own;
		serverIP = sIP;
		rmiPort = rmiP;

		stats = new JTextArea( "Press Update to view statistics", 20, 70 );
		stats.setLineWrap( true );
		stats.setEditable( false );
		stats.setMargin( new Insets( 5, 5, 5, 5 ) );
		stats.setFont( new Font( "Monospaced", Font.PLAIN, 14 ) );
		JScrollPane jsp = new JScrollPane( stats );

		configureB = new JButton( "Update" );
		configureB.addActionListener( this );

		b.add( jsp );
		b.add( Box.createVerticalStrut( 10 ) );
		b.add( Box.createHorizontalStrut( 75 ) );
		b.add( configureB );
		this.add( b );
	}

	public void actionPerformed( ActionEvent event )
	{
		//update pressed
		if( event.getActionCommand().equals( "Update" ) )
		{
			try
			{
				//update the text area
        	 		byte[] inf = scheduler.getServerStats();

				stats.append( "\n" );
				stats.append( "Server: " + serverIP + " Port: " + rmiPort );
				Vector info = (Vector) Compressor.decompress( inf );
				for( int i = 0; i < info.size(); i ++ ){
					stats.append( "\n" );
					stats.append( (String) info.get( i ) );
				}
				stats.append( "\n" );
				stats.setCaretPosition( stats.getDocument().getLength() );
			}
			catch( Exception e )
			{
				JOptionPane.showMessageDialog( this, "Error - Could not connect to Server", "Error", JOptionPane.INFORMATION_MESSAGE );
				owner.setConnected( false );
				remove( this );
				setVisible( false );
				owner.setScreen( owner.CONNECT );
			}
		}
	}
}