/*
Thomas Keane, 9:21 AM 2/13/03
class to allow user to view details of a particular job

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
import java.net.Socket;
import java.util.zip.*;
import java.io.*;

public class jobView extends JPanel implements ActionListener
{
	private RemoteInterface owner;
	private GUICommunications communications;

	private JButton viewB;
	private JTextField jobID;
	private JTextArea information;
	private JButton update;
	private Long job; //job id
	private Box b;

	public jobView( RemoteInterface own, GUICommunications scheduler )
	{
		b = Box.createVerticalBox();
		owner = own;
		communications = scheduler;
		information = new JTextArea( "Press Update to update information\n", 20, 70 );
		information.setLineWrap( true );
		information.setEditable( false );
		information.setFont( new Font( "Monospaced", Font.PLAIN, 14 ) );
		information.setMargin( new Insets( 5, 5, 5, 5 ) );
		update = null;
		job = null;

		//set up the screen components
		JLabel enterID = new JLabel( "Enter ID" );
		jobID = new JTextField( 10 );
		Box IDinfo = Box.createHorizontalBox();
		IDinfo.add( enterID );
		IDinfo.add( Box.createHorizontalStrut( 5 ) );
		IDinfo.add( jobID );

		viewB = new JButton( "View" );
		viewB.addActionListener( this );

		//add them to the screen
		b.add( IDinfo );
		b.add( Box.createVerticalStrut( 10 ) );
		b.add( viewB );
		this.add( b );
	}

	public void actionPerformed( ActionEvent event )
	{
		if( event.getActionCommand().equals( "View" ) )
		{
			//display info about the job
			//check the input is valid
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
				boolean finished = communications.problemFinished( job );
				boolean running = communications.jobRunning( job );
				if( ! finished && running )
				{
					Vector info = communications.getProblemStatus( job );

					if( info == null )
					{
						JOptionPane.showMessageDialog( this, "Error - Job Does not Exist", "Error", JOptionPane.INFORMATION_MESSAGE );
						return;
					}

					//job is running in system - display info
					removeAll();
					setVisible( false );

					//set up the textarea and button
					b = null;
					b = Box.createVerticalBox();
					JScrollPane jsp = new JScrollPane( information );
					b.add( jsp );
					b.add( Box.createVerticalStrut( 10 ) );
					b.add( Box.createHorizontalStrut( 75 ) );
					JButton update = new JButton( "Update" );
					update.addActionListener( this );
					b.add( update );

					this.add( b );
					setVisible( true );

					for( int i = 0; i < info.size(); i ++ )
					{
						information.append( ( (String) info.get( i ) ) + "\n" );
					}
					information.append( "\n" );
					information.setCaretPosition( information.getDocument().getLength() );
				}
				else if( ! running && finished )
				{
					JOptionPane.showMessageDialog( this, "Error - Job already Finished", "Error", JOptionPane.INFORMATION_MESSAGE );
					remove( this );
					setVisible( false );
					return;
				}
				else if( communications.jobKilled( job ) )
				{
					JOptionPane.showMessageDialog( this, "Error - Job Killed by system - see job log files for details", "Error", JOptionPane.INFORMATION_MESSAGE );
					int input = JOptionPane.showConfirmDialog( this, "Do you want to download the incomplete problem Files?", "Incomplete Job", JOptionPane.YES_NO_OPTION);
					if( input == JOptionPane.NO_OPTION )
					{
						remove( this );
						setVisible( false );
						return;
					}
					else
					{
						Socket socket = null;
						//download the problem zip file
						try
						{
							Vector v = owner.getSocketInfo();
							String ip = (String) v.remove( 0 );
							int port = ( (Integer) v.remove( 0 ) ).intValue();
							socket = new Socket( ip, port );
							GZIPOutputStream gzipout = new GZIPOutputStream( socket.getOutputStream() );
	
							DataOutputStream ds = new DataOutputStream( gzipout );
							ds.writeUTF( "k:" + job.toString() + ":" + job.toString() + ".zip" );
							gzipout.finish();
							ds.flush();

							File f = new File( "K" + job.toString() + ".zip" );
							FileOutputStream fos = new FileOutputStream( f );
							DataInputStream dis = new DataInputStream( new GZIPInputStream( socket.getInputStream() ) );
							byte[] buffer = new byte[ socket.getReceiveBufferSize() ];

							//read in from socket and send to file
							int read = 1;
							//stream the data to file
							while( read > 0 )
							{
								read = dis.read( buffer );
								if( read > 0 )
								{
									fos.write( buffer, 0, read );
								}
							}

							try
							{
								ds.close();
								dis.close();
								fos.close();
								socket.close();
							}
							catch( Exception ex )
							{}

							JOptionPane.showMessageDialog( this, "Success - Job files downloaded to zip file: " + f.getName(), "Success", JOptionPane.INFORMATION_MESSAGE );
							return;
						}
						catch( Exception e )
						{
							JOptionPane.showMessageDialog( this, "Error - Failed to download zip file", "Error", JOptionPane.INFORMATION_MESSAGE );
							socket.close();
							return;
						}
					}
				}
				else{
					JOptionPane.showMessageDialog( this, "Error - Job Does not Exist", "Error", JOptionPane.INFORMATION_MESSAGE );
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
		else if( event.getActionCommand().equals( "Update" ) )
		{
			//update the text area
			try
			{
				Vector info = communications.getProblemStatus( job );

				if( info == null )
				{
					JOptionPane.showMessageDialog( this, "Error - Job not running in system", "Error", JOptionPane.INFORMATION_MESSAGE );
					return;
				}
				else
				{
					for( int i = 0; i < info.size(); i ++ )
					{
						information.append( ( (String) info.get( i ) ) + "\n" );
					}
					information.append( "\n" );
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