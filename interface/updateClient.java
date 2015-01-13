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
import java.util.*;
import java.net.Socket;
import java.util.Vector;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.jar.*;

public class updateClient extends JPanel implements ActionListener
{
	private RemoteInterface owner;
	private GUICommunications communications;

	private JTextField ipInfo;
	private JButton browse;
	private JButton upload;
	private JButton clear;
	private JLabel selected;
	private File clientJar;

	public updateClient( RemoteInterface own, GUICommunications scheduler )
	{
		Box b = Box.createVerticalBox();
		owner = own;
		communications = scheduler;
		clientJar = null;

		//set up the screen components
		JLabel enterIP = new JLabel( "IP Range Prefix (optional)" );
		ipInfo = new JTextField( 19 );

		Box IPinfo = Box.createHorizontalBox();
		IPinfo.add( enterIP );
		IPinfo.add( Box.createHorizontalStrut( 5 ) );
		IPinfo.add( ipInfo );

		JLabel client = new JLabel( "Select new client: " );
		browse = new JButton( "Browse" );
		browse.addActionListener( this );
		selected = new JLabel();

		Box JARinfo = Box.createHorizontalBox();
		JARinfo.add( client );
		JARinfo.add( Box.createHorizontalStrut( 5 ) );
		JARinfo.add( browse );
		JARinfo.add( Box.createHorizontalStrut( 5 ) );
		JARinfo.add( selected );

		upload = new JButton( "Upload" );
		upload.addActionListener( this );
		clear = new JButton( "Remove" );
		clear.addActionListener( this );
		
		Box uploadClear = Box.createHorizontalBox();
		uploadClear.add( clear );
		uploadClear.add( Box.createHorizontalStrut( 5 ) );
		uploadClear.add( upload );
		
		//add the boxes to the screen
		b.add( IPinfo );
		b.add( Box.createVerticalStrut( 10 ) );
		b.add( JARinfo );
		b.add( Box.createVerticalStrut( 10 ) );
		b.add( uploadClear );

		this.add( b );
	}

	public void actionPerformed( ActionEvent event )
	{
		if( event.getActionCommand().equals( "Browse" ) )
		{
			//open a filechooser to locate the file
			JFileChooser clientFC = new JFileChooser( new File( System.getProperty( "user.dir" ) ) );
			clientFC.removeChoosableFileFilter( clientFC.getFileFilter() );
			clientFC.setFileFilter( new filter( "jar" ) );
			clientFC.setSelectedFile( null );
			int option = clientFC.showOpenDialog( this );
			if( option == JFileChooser.APPROVE_OPTION )
			{
				clientJar = clientFC.getSelectedFile();
				selected.setText( clientJar.getAbsolutePath() );
			}
		}
		else if( event.getActionCommand().equals( "Upload" ) )
		{
			if( clientJar == null )
			{
				JOptionPane.showMessageDialog( this, "Error - No Jar file selected", "Error", JOptionPane.INFORMATION_MESSAGE );
				return;
			}
			else if( clientJar.getName().compareTo( "client.jar" ) != 0 )
			{
				JOptionPane.showMessageDialog( this, "Error - Selected file not client.jar", "Error", JOptionPane.INFORMATION_MESSAGE );
				return;
			}

			byte[] bytes = null;
			try
			{
				//read in the client jar bytes
				JarFile client = new JarFile( clientJar );

				//read in the bytes of the file
				bytes = new byte[ (int) clientJar.length() ];
				FileInputStream fi = new FileInputStream( clientJar );

				int offset = 0;
				int numRead = 0;
				while( offset < bytes.length && ( numRead = fi.read( bytes, offset, bytes.length - offset ) ) >= 0 )
				{
					offset += numRead;
				}
				fi.close();
			}
			catch( Exception e )
			{
				JOptionPane.showMessageDialog( this, "Error - Failed to read in JAR file: " + e, "Error", JOptionPane.INFORMATION_MESSAGE );
				return;
			}
				
			//check the ip address entered is valid
			boolean uploaded = false;
			String IPRange = ipInfo.getText();
			if( IPRange != null && IPRange.length() > 0 )
			{
				//check the ip address is valid
				StringTokenizer stk = new StringTokenizer( IPRange, "." );
				while( stk.hasMoreTokens() )
				{
					try
					{
						int ip = Integer.parseInt( stk.nextToken() );
					}
					catch( Exception e )
					{
						JOptionPane.showMessageDialog( this, "Error - Invalid IP range", "Error", JOptionPane.INFORMATION_MESSAGE );
						return;
					}
				}
				
				try
				{
					//upload the new client
					uploaded = communications.updateClient( bytes, IPRange );
				}
				catch( Exception rme )
				{
					JOptionPane.showMessageDialog( this, "Error - Failed to upload. Remote Exception: " + rme , "Error", JOptionPane.INFORMATION_MESSAGE );
					owner.setConnected( false );
					remove( this );
					setVisible( false );
					owner.setScreen( owner.CONNECT );
					return;
				}
			}
			else
			{
				try
				{
					//upload the new client
					uploaded = communications.updateClient( bytes, null );
				}
				catch( Exception rme )
				{
					JOptionPane.showMessageDialog( this, "Error - Failed to upload. Remote Exception: " + rme , "Error", JOptionPane.INFORMATION_MESSAGE );
					owner.setConnected( false );
					remove( this );
					setVisible( false );
					owner.setScreen( owner.CONNECT );
					return;
				}
			}
			
			if( ! uploaded )
			{
				JOptionPane.showMessageDialog( this, "Error - Failed to update client on server", "Error", JOptionPane.INFORMATION_MESSAGE );
				return;
			}
			else
			{
				JOptionPane.showMessageDialog( this, "Success - Updated client on server", "Success", JOptionPane.INFORMATION_MESSAGE );
				owner.setScreen( owner.JOBS_VIEW );
				return;
			}
		}
		else if( event.getActionCommand().equals( "Remove" ) )
		{
			try
			{
				communications.clearClient();
			}
			catch( Exception rme )
			{
				JOptionPane.showMessageDialog( this, "Error - Failed to remove. Remote Exception: " + rme , "Error", JOptionPane.INFORMATION_MESSAGE );
				owner.setConnected( false );
				remove( this );
				setVisible( false );
				owner.setScreen( owner.CONNECT );
				return;
			}
			JOptionPane.showMessageDialog( this, "Success - Removed Client on server" , "Success", JOptionPane.INFORMATION_MESSAGE );
		}
	}

	//method to download a partial set of results from the server in the form of a zip file
	private void downloadResults( Long job ) throws Exception
	{
		//contact server via socket and stream download of results
		Vector socketInfo = owner.getSocketInfo();
		Socket socket = new Socket( (String) socketInfo.get( 0 ), ( (Integer) socketInfo.get( 1 ) ).intValue() );
		GZIPOutputStream gzipout = new GZIPOutputStream( socket.getOutputStream() );

		DataOutputStream ds = new DataOutputStream( gzipout );
		ds.writeUTF( "r:" + job.toString() + ":" + job.toString() + ".zip" );
		gzipout.finish();
		ds.flush();
		FileOutputStream fos = new FileOutputStream( new File( System.getProperty( "user.dir"), job.toString() + ".zip" ) );
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
	}
}