/*
Thomas Keane, 11:28 AM 2/4/03
class that is used to get user to add a problem to the server
only accessible in admin mode

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
import java.util.*;
import java.rmi.RemoteException;
import java.util.zip.*;
import java.util.jar.*;

public class jobAdd extends JPanel implements ActionListener
{
	private RemoteInterface owner; //overall gui class
	private GUICommunications scheduler; //rmi stub

	//components globally needed
	private JButton algorithm;
	private JButton datamanager;
	private JButton problemData;
	private JButton classDefs;
	private JButton clear;
	private JTextField priority;
	private JTextField description;
	private JTextField userName;
	private JButton addB;
	private JProgressBar progress;
	private JTextField allowExceptions;
	private JTextField allowExpired;
	private JTextField problemCPU;
	private JTextField problemMemory;
	
	//text fields to display the currently selecte files
	private JLabel selected_algorithm;
	private JLabel selected_datamanager;
	private JLabel selected_problemdata;
	private JLabel selected_classdefs;

	//files chosen
	private File algorithmF;
	private File datamanagerF;
	private File[] problemDataF;
	private File[] classDefsF;
	private File parentDirectory;

	public jobAdd( RemoteInterface own, GUICommunications sch, File dir, String selected_al, String selected_dm, String[] selected_j, String[] selected_pd, int minCPU, int minMem )
	{
		//box layout
		Box mainB = Box.createVerticalBox();
		JPanel info = new JPanel( new GridLayout( 12, 3, 10, 10 ) );
		owner = own;
		scheduler = sch;
		algorithmF = null;
		datamanagerF = null;
		problemDataF = null;
		description = null;
		progress = new JProgressBar( 0, 100 );

		//set up the components
		JLabel algorithmL = new JLabel( "Algorithm" );
		algorithm = new JButton( "Browse" );
		algorithm.addActionListener( this );
		if( selected_al.startsWith( "none" ) )
		{
			selected_algorithm = new JLabel( selected_al );
		}
		else
		{
			algorithmF = new File( selected_al );
			parentDirectory = dir;
			selected_algorithm = new JLabel( algorithmF.getName() );
		}
		info.add( algorithmL );
		info.add( algorithm );
		info.add( selected_algorithm );

		JLabel datamanagerL = new JLabel( "Datamanager" );
		datamanager = new JButton( "Browse " );
		datamanager.addActionListener( this );
		datamanagerF = new File( selected_dm );
		if( selected_dm.startsWith( "none" ) )
		{
			selected_datamanager = new JLabel( selected_dm );
		}
		else
		{
			datamanagerF = new File( selected_dm );
			selected_datamanager = new JLabel( datamanagerF.getName() );
		}
		info.add( datamanagerL );
		info.add( datamanager );
		info.add( selected_datamanager );
		
		JLabel problemDataL = new JLabel( "Problem Data" );
		problemData = new JButton( "Browse  " );
		problemData.addActionListener( this );
		if( selected_pd[ 0 ].startsWith( "none" ) )
		{
			selected_problemdata = new JLabel( selected_pd[ 0 ] );
		}
		else
		{
			problemDataF = new File[ selected_pd.length ];
			String fileNames = new String();
			for( int i = 0; i < problemDataF.length; i ++ )
			{
				problemDataF[ i ] = new File( selected_pd[ i ] );
				fileNames = fileNames + problemDataF[ i ].getName() + ", ";
			}
			fileNames = fileNames.substring( 0, fileNames.length() - 2 );
			selected_problemdata = new JLabel( fileNames.substring( 0, 10 ) + "..." );
			selected_problemdata.setToolTipText( fileNames );
		}
		
		info.add( problemDataL );
		info.add( problemData );
		info.add( selected_problemdata );

		JLabel classDefsL = new JLabel( "Extra required Classes" );
		classDefs = new JButton( "Browse   " );
		classDefs.addActionListener( this );
		if( selected_j[ 0 ].startsWith( "none" ) )
		{
			selected_classdefs = new JLabel( selected_j[ 0 ] );
		}
		else
		{
			classDefsF = new File[ selected_j.length ];
			String fileNames = new String();
			for( int i = 0; i < classDefsF.length; i ++ )
			{
				classDefsF[ i ] = new File( selected_j[ i ] );
				fileNames = fileNames + classDefsF[ i ].getName() + ", ";
			}
			fileNames = fileNames.substring( 0, fileNames.length() - 2 );
			selected_classdefs = new JLabel( fileNames.substring( 0, 10 ) + "..." );
			selected_classdefs.setToolTipText( fileNames );
		}
		info.add( classDefsL );
		info.add( classDefs );
		info.add( selected_classdefs );

		JLabel priorityL = new JLabel( "Priority" );
		priority = new JTextField( "10", 5 );
		info.add( priorityL );
		info.add( priority );
		info.add( new JLabel( "" ) );

		JLabel userL = new JLabel( "Your Name" );
		userName = new JTextField( 18 );
		info.add( userL );
		info.add( userName );
		info.add( new JLabel( "" ) );
		
		JLabel descriptionL = new JLabel( "Problem Description" );
		description = new JTextField( 18 );
		info.add( descriptionL );
		info.add( description );
		info.add( new JLabel( "" ) );
		
		JLabel cpu = new JLabel( "Min Required CPU (MFLOPS)" );
		Integer cpu_ = new Integer( minCPU );
		problemCPU = new JTextField( cpu_.toString(), 4 );
		info.add( cpu );
		info.add( problemCPU );
		info.add( new JLabel( "" ) );
		
		JLabel mem = new JLabel( "Min Required Memory (MB)" );
		Integer mem_ = new Integer( minMem );
		problemMemory = new JTextField( mem_.toString(), 4 );
		info.add( mem );
		info.add( problemMemory );
		info.add( new JLabel( "" ) );

		JLabel except = new JLabel( "#Allowed Algorithm Exceptions" );
		allowExceptions = new JTextField( "5", 5 );
		info.add( except );
		info.add( allowExceptions );
		info.add( new JLabel( "" ) );

		JLabel expired = new JLabel( "#Allowed Expirations per unit" );
		allowExpired = new JTextField( "5", 5 );
		info.add( expired );
		info.add( allowExpired );
		info.add( new JLabel( "" ) );

		mainB.add( info );
		mainB.add( Box.createVerticalGlue() );

		Box buttons = Box.createHorizontalBox();
		addB = new JButton( "Add" );
		addB.addActionListener( this );
		clear = new JButton( "Clear" );
		clear.addActionListener( this );

		buttons.add( Box.createHorizontalGlue() );
		buttons.add( clear );
		buttons.add( Box.createHorizontalGlue() );
		buttons.add( addB );
		buttons.add( Box.createHorizontalGlue() );
		mainB.add( buttons );

		this.add( mainB ); //add the complete box
	}

	public void actionPerformed( ActionEvent event )
	{
		//check what button was pressed
		if( event.getActionCommand().equals( "Add" ) )
		{
			if( algorithmF == null || datamanagerF == null || ! algorithmF.exists() || ! datamanagerF.exists() )
			{
				JOptionPane.showMessageDialog( this, "Error - No File Selected", "Error", JOptionPane.INFORMATION_MESSAGE );
				return;
			}

			//check arguments and add job if they are ok
			int priorityi = 0, expired = 0, exceptions = 0, cpuSpeed = 0, memory = 0;
			try
			{
				priorityi = ( new Integer( priority.getText() ) ).intValue();
				expired = ( new Integer( allowExceptions.getText() ) ).intValue();
				exceptions = ( new Integer( allowExpired.getText() ) ).intValue();
				cpuSpeed = ( new Integer( problemCPU.getText() ) ).intValue();
				memory = ( new Integer( problemMemory.getText() ) ).intValue();
			}
			catch( NumberFormatException e )
			{
				JOptionPane.showMessageDialog( this, "Error - Check Text Fields Information", "Error", JOptionPane.INFORMATION_MESSAGE );
				return;
			}

			//check the bounds of the values
			if( priorityi < 0 || expired < 0 || exceptions < 0 )
			{
				JOptionPane.showMessageDialog( this, "Error - All values must be greater than zero", "Error", JOptionPane.INFORMATION_MESSAGE );
				return;
			}
			
			if( cpuSpeed < 1 || cpuSpeed > 9999 )
			{
				JOptionPane.showMessageDialog( this, "Error - MFLOPS must be between 1 - 9999", "Error", JOptionPane.INFORMATION_MESSAGE );
				return;
			}
			
			if( memory < 1 || memory > 5000 )
			{
				JOptionPane.showMessageDialog( this, "Error - Memory (Mb) must be between 1 - 5000", "Error", JOptionPane.INFORMATION_MESSAGE );
				return;
			}

			int input = JOptionPane.showConfirmDialog( this, "For problems with large data files - it may take a few minutes to upload the data\nYou will be alerted once the process is complete - Proceed?", "Note", JOptionPane.YES_NO_OPTION );
			if( input == JOptionPane.NO_OPTION )
			{
				return;
			}
			
			//read the selected files in
			byte[] al = getFile( algorithmF );
			byte[] dm = getFile( datamanagerF );

			//if there are any class defs - then get them
			Vector classDefinitions = new Vector();
			Vector jarDefinitions = new Vector();
			try
			{
				if( classDefsF != null )
				{
					for( int i = 0; i < classDefsF.length; i ++ )
					{
						//seperate the jar files from the class files
						if( classDefsF[ i ].getName().endsWith( ".jar" ) )
						{
							//get the bytes for the file
							byte[] file = new byte[ (int) classDefsF[ i ].length() ];
							FileInputStream fis = new FileInputStream( classDefsF[ i ] );
							int offset = 0, read = 1;
							while( read > 0 )
							{
								read = fis.read( file, offset, file.length - offset );
								offset += read;
							}
							fis.close();
							jarDefinitions.add( classDefsF[ i ].getName() );
							jarDefinitions.add( file );
						}
						else if( classDefsF[ i ].getName().endsWith( ".class" ) )
						{
							//get the bytes for the file
							byte[] file = new byte[ (int) classDefsF[ i ].length() ];
							FileInputStream fis = new FileInputStream( classDefsF[ i ] );
							int offset = 0, read = 1;
							while( read > 0 )
							{
								read = fis.read( file, offset, file.length - offset );
								offset += read;
							}
							fis.close();
							classDefinitions.add( classDefsF[ i ].getName() );
							classDefinitions.add( file );
						}
					}
				}
			}
			catch( IOException e )
			{
				JOptionPane.showMessageDialog( this, "Error - Failed to upload Jar File(s): " + e.toString(), "Error", JOptionPane.INFORMATION_MESSAGE );
				return;
			}

			//make sure the class files were read in successfully
			if( al == null || dm == null )
			{
				JOptionPane.showMessageDialog( this, "Error - Failed to upload Class Files", "Error", JOptionPane.INFORMATION_MESSAGE );
				return;
			}

			if( userName.getText().length() == 0 )
			{
				JOptionPane.showMessageDialog( this, "Error - Please enter your name", "Error", JOptionPane.INFORMATION_MESSAGE );
				return;
			}

			Long probID = new Long( Math.abs( ( new Random() ).nextLong() % 10000000 ) );
			if( problemDataF != null )
			{
				Vector v = owner.getSocketInfo();
				//upload the files to the server via socket
				for( int i = 0; i < problemDataF.length; i ++ )
				{
					int retries = 0;
					Exception exception = null;
					while( retries < 5 )
					{
						try
						{
							if( ! problemDataF[ i ].exists() )
							{
								JOptionPane.showMessageDialog( this, "Error - Cannot find Problem Data file: " + problemDataF[ i ].getName(), "Error", JOptionPane.INFORMATION_MESSAGE );
								return;
							}
							String ip = (String) v.get( 0 );
							int port = ( (Integer) v.get( 1 ) ).intValue();

							//open socket and tell server about to upload
							Socket socket = new Socket( ip, port );
							GZIPOutputStream gzipout = new GZIPOutputStream( socket.getOutputStream() );
							DataOutputStream ds = new DataOutputStream( gzipout );
							ds.writeUTF( "g:" + probID + ":" + problemDataF[ i ].getName() );

							ds.flush();

							FileInputStream fis = new FileInputStream( problemDataF[ i ] );
							byte[] buffer = new byte[ socket.getSendBufferSize() ];
							int numRead = 1;

							//stream data to server
							while( numRead > 0 )
							{
								numRead = fis.read( buffer );
								if( numRead > 0 )
								{
									ds.write( buffer, 0, numRead );
								}
								progress.setValue( (int) ( ( i/problemDataF.length ) * 100 ) );
							}
							gzipout.finish();
							ds.flush();

							//wait for server to confirm all file received
							DataInputStream dis = new DataInputStream( new GZIPInputStream( socket.getInputStream() ) );
							String s = dis.readUTF();

							dis.close();
							ds.close();
							break;
						}
						catch( Exception e )
						{
							exception = e;
							retries ++;
						}
					}
					
					if( retries >= 5 )
					{
						JOptionPane.showMessageDialog( this, "Error - Cannot connect to server to upload Problem Data: " + exception.toString(), "Error", JOptionPane.INFORMATION_MESSAGE );
						owner.setConnected( false );
						remove( this );
						setVisible( false );
						owner.setScreen( owner.CONNECT );
						return;
					}
				}
			}

			//add the job to the server
			boolean b = false;
			
			//return names of the selected files (so they will be remembered for next time)
			String[] list = null;
			if( classDefsF != null && classDefsF.length > 0 )
			{
				list = new String[ classDefsF.length ];
				for( int i = 0; i < list.length; i ++ )
				{
					list[ i ] = classDefsF[ i ].getAbsolutePath();
				}
			}
			else
			{
				list = new String[ 1 ];
				list[ 0 ] = "none selected";
			}
				
			String[] pdList = null;
			if( problemDataF != null && problemDataF.length > 0 )
			{
				pdList = new String[ problemDataF.length ];
				for( int i = 0; i < problemDataF.length; i ++ )
				{
					pdList[ i ] = problemDataF[ i ].getAbsolutePath();
				}
			}
			else
			{
				pdList = new String[ 1 ];
				pdList[ 0 ] = "none selected";
			}

			//remember the entries for next time
			owner.setSelected( parentDirectory, algorithmF.getAbsolutePath(), datamanagerF.getAbsolutePath(), list, pdList, cpuSpeed, memory );
			
			try
			{
				b = scheduler.addProblem( probID, al, dm, priorityi, description.getText() + "$%^&" + userName.getText(), expired, exceptions, classDefinitions, jarDefinitions, cpuSpeed, memory );
			}
			catch( RemoteException e )
			{
				StringTokenizer stk = new StringTokenizer( e.toString(), ":" );
				stk.nextToken();
				stk.nextToken();
				stk.nextToken();
				String s = stk.nextToken();
				s = s.trim();
				if( s.startsWith( "Error in init()" ) )
				{
					while( stk.hasMoreTokens() )
					{
						s = s + stk.nextToken();
					}
					JOptionPane.showMessageDialog( this, s, "Error", JOptionPane.INFORMATION_MESSAGE );
					return;
				}
				else
				{
					JOptionPane.showMessageDialog( this, "Error - Could not connect to Server", "Error", JOptionPane.INFORMATION_MESSAGE );
					owner.setConnected( false );
					owner.setScreen( owner.CONNECT );
				}

				//get some detailed info from the exception - stack trace
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				PrintStream ps = new PrintStream( bos );
				e.printStackTrace( ps );
				System.err.println( bos.toString() );

				if( problemDataF != null )
				{
					try
					{
						//remove the file that was uploaded to the server via the socket
						Vector v = owner.getSocketInfo();
						String ip = (String) v.remove( 0 );
						int port = ( (Integer) v.remove( 0 ) ).intValue();

						//open socket and tell server about to upload
						Socket socket = new Socket( ip, port );
						GZIPOutputStream gzipout = new GZIPOutputStream( socket.getOutputStream() );
						DataOutputStream ds = new DataOutputStream( gzipout );
						String filesDelete = "d:" + probID + ":";
						for( int i = 0; i < problemDataF.length; i ++ )
						{
							filesDelete = filesDelete.concat( problemDataF[ i ].getName() );
							if( i != problemDataF.length - 1 )
							{
								filesDelete = filesDelete.concat( ":" );
							}
						}
						ds.writeUTF( filesDelete );
						ds.flush();
						ds.close();
					}
					catch( Exception ex ){}
				}

				remove( this );
				setVisible( false );
				return;
			}
			catch( Exception e )
			{
				//get some detailed info from the exception - stack trace
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				PrintStream ps = new PrintStream(bos);
				e.printStackTrace( ps );

				System.err.println( bos.toString() );
				JOptionPane.showMessageDialog( this, "Error - Check Problem is Valid", "Error", JOptionPane.INFORMATION_MESSAGE );
				if( problemDataF != null )
				{
					try
					{
						//remove the file that was uploaded to the server via the socket
						Vector v = owner.getSocketInfo();
						String ip = (String) v.remove( 0 );
						int port = ( (Integer) v.remove( 0 ) ).intValue();

						//open socket and tell server about to upload
						Socket socket = new Socket( ip, port );
						GZIPOutputStream gzipout = new GZIPOutputStream( socket.getOutputStream() );
						DataOutputStream ds = new DataOutputStream( gzipout );
						String filesDelete = "d:" + probID + ":";
						for( int i = 0; i < problemDataF.length; i ++ )
						{
							filesDelete = filesDelete.concat( problemDataF[ i ].getName() );
							if( i != problemDataF.length - 1 )
							{
								filesDelete = filesDelete.concat( ":" );
							}
						}
						ds.writeUTF( filesDelete );
						ds.flush();
						ds.close();
					}
					catch( Exception ex ){}
				}

				remove( this );
				setVisible( false );
				return;
			}

			if( ! b )
			{
				try
				{
					String reason = scheduler.addProblemFailReason();
					JOptionPane.showMessageDialog( this, "Error: " + reason, "Error", JOptionPane.INFORMATION_MESSAGE );
					return;
				}
				catch( RemoteException e )
				{}
				JOptionPane.showMessageDialog( this, "Error - Problem not added to server", "Error", JOptionPane.INFORMATION_MESSAGE );
			}
			else
			{
				JOptionPane.showMessageDialog( this, "Success - Problem added to server", "Success", JOptionPane.INFORMATION_MESSAGE );
				remove( this );
				setVisible( false );
				owner.setScreen( owner.JOBS_VIEW );

				return;
			}

			//finished - clear the frame
			return;
		}
		else if( event.getActionCommand().equals( "Browse" ) )
		{
			//open a filechooser to locate the file
			JFileChooser algorithmFC = new JFileChooser( parentDirectory );
			algorithmFC.removeChoosableFileFilter( algorithmFC.getFileFilter() );
			algorithmFC.setFileFilter( new filter( "class" ) );
			algorithmFC.setSelectedFile( null );
			algorithmFC.setFileSelectionMode( JFileChooser.FILES_ONLY );
			algorithmFC.setApproveButtonText( "Ok" );
			algorithmFC.setDialogTitle( "Choose your algorithm" );
			int option = algorithmFC.showOpenDialog( this );
			if( option == JFileChooser.APPROVE_OPTION )
			{
				algorithmF = algorithmFC.getSelectedFile();
				parentDirectory = algorithmF.getParentFile();
				selected_algorithm.setText( algorithmF.getName() );
			}
		}
		else if( event.getActionCommand().equals( "Browse " ) )
		{
			//open a filechooser to locate the file
			JFileChooser datamanagerFC = new JFileChooser( parentDirectory );
			datamanagerFC.removeChoosableFileFilter( datamanagerFC.getFileFilter() );
			datamanagerFC.setFileFilter( new filter( "class" ) );
			datamanagerFC.setSelectedFile( null );
			datamanagerFC.setDialogTitle( "Choose your datamanager" );
			datamanagerFC.setFileSelectionMode( JFileChooser.FILES_ONLY );
			datamanagerFC.setApproveButtonText( "Ok" );
			int option = datamanagerFC.showOpenDialog( this );
			if( option == JFileChooser.APPROVE_OPTION )
			{
					datamanagerF = datamanagerFC.getSelectedFile();
					selected_datamanager.setText( datamanagerF.getName() );
			}
		}
		else if( event.getActionCommand().equals( "Browse  " ) )
		{
			//open a filechooser to locate the file
			JFileChooser problemDataFC = new JFileChooser( parentDirectory );
			problemDataFC.setMultiSelectionEnabled( true );
			problemDataFC.setSelectedFiles( null );
			problemDataFC.setDialogTitle( "Choose your problem data files" );
			problemDataFC.setFileSelectionMode( JFileChooser.FILES_ONLY );
			problemDataFC.setApproveButtonText( "Ok" );
			int option = problemDataFC.showOpenDialog( this );
			if( option == JFileChooser.APPROVE_OPTION )
			{
				problemDataF = problemDataFC.getSelectedFiles();
				String fileNames = new String();
				for( int i = 0; i < problemDataF.length; i ++ )
				{
					fileNames = fileNames + problemDataF[ i ].getName() + ", ";
				}
				fileNames = fileNames.substring( 0, fileNames.length() - 2 );
				selected_problemdata.setText( fileNames.substring( 0, 10 ) + "..." );
				//selected_problemdata.setText( fileNames );
				selected_problemdata.setToolTipText( fileNames );
			}
			owner.pack();
		}
		else if( event.getActionCommand().equals( "Browse   " ) )
		{
			//open filechooser to locate files
			JFileChooser classDefsFC = new JFileChooser( parentDirectory );
			classDefsFC.removeChoosableFileFilter( classDefsFC.getFileFilter() );
			String[] extensions = { "class", "jar" };
			classDefsFC.setFileFilter( new MultiFilter( extensions ) );
			classDefsFC.setDialogTitle( "Choose your extra Java libraries" );
			classDefsFC.setMultiSelectionEnabled( true );
			classDefsFC.setFileSelectionMode( JFileChooser.FILES_ONLY );
			classDefsFC.setSelectedFiles( null );
			classDefsFC.setApproveButtonText( "Ok" );
			int option = classDefsFC.showOpenDialog( this );
			if( option == JFileChooser.APPROVE_OPTION )
			{
				classDefsF = classDefsFC.getSelectedFiles();
				String fileNames = new String();
				for( int i = 0; i < classDefsF.length; i ++ )
				{
					fileNames = fileNames + classDefsF[ i ].getName() + ", ";
				}
				fileNames = fileNames.substring( 0, fileNames.length() - 2 );
				selected_classdefs.setText( fileNames.substring( 0, 10 ) + "..." );
				//selected_classdefs.setText( fileNames );
				selected_classdefs.setToolTipText( fileNames );
			}
			owner.pack();
		}
		else if( event.getActionCommand().equals( "Clear" ) )
		{
			//clear all the remembered filenames
			owner.resetSelected();
			owner.resetAdd();
		}
	}

	private byte[] getFile( File f )
	{
		//read in the bytes of the file
		try
		{
			FileInputStream fi = new FileInputStream( f );
			byte[] bytes = new byte[ (int) f.length() ];

			int offset = 0;
			int numRead = 0;

			while( offset < bytes.length && ( numRead = fi.read( bytes, offset, bytes.length - offset ) ) > 0 )
			{
				offset += numRead;
			}

			fi.close();
			return bytes;
		}
		catch( Exception e ){
			return null;
		}
	}
}
