/*
Thomas Keane, 12:16 PM 2/4/03
class used for viewing status information about current jobs in system

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
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import java.io.*;

public class jobsView extends JPanel implements ActionListener
{
	private GUICommunications scheduler;
	private RemoteInterface owner;
	private JTextArea information;

	public jobsView( RemoteInterface own, GUICommunications s )
	{
		Box b = Box.createVerticalBox();
		scheduler = s;
		owner = own;

		//set up the textarea and button
		information = new JTextArea( "Press Update to get current problems status\n", 20, 90 );
		information.setLineWrap( true );
		information.setEditable( false );
		information.setMargin( new Insets( 5, 5, 5, 5 ) );
		information.setFont( new Font( "Monospaced", Font.PLAIN, 14 ) );
		JScrollPane jsp = new JScrollPane( information );
		JButton update = new JButton( "Update" );
		update.addActionListener( this );

		//add the componenets
		b.add( jsp );
		b.add( Box.createVerticalStrut( 10 ) );
		b.add( Box.createHorizontalStrut( 75 ) );
		b.add( update );
		this.add( b );
	}

	public void actionPerformed( ActionEvent event )
	{
		if( event.getActionCommand().equals( "Update" ) )
		{
			try
			{
				byte[] infor = scheduler.getAllProblemStatus();

				//get updated info from server
				Vector info = (Vector) Compressor.decompress( infor );
				
				if( info.size() == 0 )
				{
					information.append( "No Jobs in System\n" );
					information.setCaretPosition( information.getDocument().getLength() );
				}
				else
				{
					information.setText( null ); //clear the screen

					//display the downloaded info
					for( int i = 0; i < info.size(); i ++ )
					{
						Vector v = (Vector) info.get( i );
						for( int j = 0; j < v.size(); j ++ )
						{
							String s = (String) v.get( j );
							information.append( "\n" );
							information.append( s );
						}

						if( ( (Vector) info.get( i ) ).size() > 0 )
						{
							information.append( "\n" );
						}
					}
					information.setCaretPosition( information.getDocument().getLength() );
				}
			}
			catch( Exception e )
			{
				//get some detailed info from the exception - stack trace
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				PrintStream ps = new PrintStream( bos );
				e.printStackTrace( ps );
				System.err.println( bos.toString() );

				//problem - couldnt get data
				JOptionPane.showMessageDialog( this, "Error - Could not connect to Server", "Click OK", JOptionPane.INFORMATION_MESSAGE );
				owner.setConnected( false );
				remove( this );
				setVisible( false );
				owner.setScreen( owner.CONNECT );
			}
		}
	}
}