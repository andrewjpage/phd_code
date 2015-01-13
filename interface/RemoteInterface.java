/*
Thomas Keane, 16:03 28/01/2003
overall class for the gui - sets up and manages overall window
contains main() function to start gui

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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.io.*;
import java.util.jar.*;
import java.util.Vector;

public class RemoteInterface extends JFrame implements ActionListener
{
	public static final Font defaultFont = new Font( "Arial", Font.PLAIN, 16 );
	public final int JOBS_VIEW = 0;
	public final int JOB_VIEW = 1;
	public final int CONNECT = 2;
	
	private JPanel contentPane; //main content pane of gui
	private JMenuBar menuBar; //top bar
	private GUICommunications communications; //rmi stub for communicating
	private boolean admin; //admin or not
	private int socketPort; //socket streaming port
	private int rmiPort;
	private String serverIP; //ip address
	
	//some information that the interface remembers from last use (the job add screen)
	private File selected_directory;
	private String selected_algorithm;
	private String selected_datamanager;
	private String[] selected_jars;
	private String[] selected_pd;
	private int minCPU;
	private int minMem;

	public RemoteInterface()
	{
		//set up the overall gui window
		super( "Java Distributed Computing" );
		//setSize( new Dimension( 600, 475 ) );
		setSize( new Dimension( 900, 475 ) );
		admin = false;
		rmiPort = 0;
		socketPort = 0;
		serverIP = null;
		selected_directory = null;
		selected_algorithm = "none selected";
		selected_datamanager = "none selected";
		selected_jars = new String[ 1 ];
		selected_jars[ 0 ] = "none selected";
		selected_pd = new String[ 1 ];
		selected_pd[ 0 ] = "none selected";
		minCPU = 10;
		minMem = 60;

		//************set up the top menu***********
		menuBar = new JMenuBar();

		//populate the menu bar
		//File menu
		JMenu menu1 = new JMenu( "File" );

		JMenuItem item = new JMenuItem( "Connect" );
		item.addActionListener( this );
		menu1.add( item );

		item = new JMenuItem( "Exit" );
		item.addActionListener( this );
		menu1.add( item );

		menuBar.add( menu1 );

		//Jobs menu
		JMenu menu2 = new JMenu( "Jobs" );

		item = new JMenuItem( "View" );
		item.addActionListener( this );
		menu2.add( item );

		item = new JMenuItem( "View All" );
		item.addActionListener( this );
		menu2.add( item );

		item = new JMenuItem( "Add" );
		item.addActionListener( this );
		menu2.add( item );

		item = new JMenuItem( "Remove" );
		item.addActionListener( this );
		menu2.add( item );

		item = new JMenuItem( "Configure" );
		item.addActionListener( this );
		menu2.add( item );

		item = new JMenuItem( "Get Results" );
		item.addActionListener( this );
		menu2.add( item );

		menuBar.add( menu2 );

		//Server menu
		JMenu menu3 = new JMenu( "Server" );

		item = new JMenuItem( "Statistics" );
		item.addActionListener( this );
		menu3.add( item );

		item = new JMenuItem( "Control" );
		item.addActionListener( this );
		menu3.add( item );

		item = new JMenuItem( "Update Client" );
		item.addActionListener( this );
		menu3.add( item );

		menuBar.add( menu3 );

		//help menu
		JMenu menu4 = new JMenu( "Help" );

		item = new JMenuItem( "Help" );
		item.addActionListener( this );
		menu4.add( item );

		item = new JMenuItem( "About" );
		item.addActionListener( this );
		menu4.add( item );

		menuBar.add( menu4 );

		//add the menubar to the root pane
		this.getRootPane().setJMenuBar( menuBar );
		
		//gray out options - user must connect first
		grayOut();
		//**************end set up top menu bar**************************

		//default screen is the connect screen
    		contentPane = new JPanel();
		BorderLayout bl = new BorderLayout();
		contentPane.setLayout( bl );
		contentPane.setBorder( new EmptyBorder( 100, 200, 100, 200 ) );

		connectToServer con = new connectToServer( this, serverIP, rmiPort, socketPort ); 
		contentPane.add( con );
		this.getContentPane().add( contentPane, BorderLayout.CENTER );
		this.pack();
		setVisible( true );
	}

	public void actionPerformed( ActionEvent event )
	{
		if( event.getActionCommand().equals( "Connect" ) )
		{
    			remove( contentPane );
    			contentPane = new JPanel();
			BorderLayout bl = new BorderLayout();
			contentPane.setLayout( bl );
			contentPane.setBorder( new EmptyBorder( 100, 200, 100, 200 ) );

    			connectToServer con = new connectToServer( this, serverIP, rmiPort, socketPort ); 
    			contentPane.add( con );
    			this.getContentPane().add( contentPane, BorderLayout.CENTER );
			this.pack();
    			setVisible( true );
		}
		else if( event.getActionCommand().equals( "Exit" ) )
		{
			System.exit(0);
		}
		else if( event.getActionCommand().equals( "View" ) )
		{
     			remove( contentPane );
    			contentPane = new JPanel();
			BorderLayout bl = new BorderLayout();
			contentPane.setLayout( bl );
			contentPane.setBorder( new EmptyBorder( 15, 15, 15, 15 ) );
			//contentPane.setBorder( new EmptyBorder( 120, 35, 120, 35 ) );

    			jobView fv = new jobView( this, communications ); 
    			contentPane.add( fv );
    			this.getContentPane().add( contentPane, BorderLayout.CENTER );
			this.pack();
    			setVisible( true );
		}
		else if( event.getActionCommand().equals( "View All" ) )
		{
     			remove( contentPane );
    			contentPane = new JPanel();
			BorderLayout bl = new BorderLayout();
			contentPane.setLayout( bl );
			contentPane.setBorder( new EmptyBorder( 15, 15, 15, 15 ) );

    			jobsView fv = new jobsView( this, communications ); 
    			contentPane.add( fv );
    			this.getContentPane().add( contentPane, BorderLayout.CENTER );
			this.pack();
    			setVisible( true );
    		}
		else if( event.getActionCommand().equals( "Add" ) )
		{
    			remove( contentPane );
    			contentPane = new JPanel();
			BorderLayout bl = new BorderLayout();
			contentPane.setLayout( bl );
			contentPane.setBorder( new EmptyBorder( 5, 15, 5, 15 ) );

    			jobAdd ja = new jobAdd( this, communications, selected_directory, selected_algorithm, selected_datamanager, selected_jars, selected_pd, minCPU, minMem );
    			contentPane.add( ja );
    			this.getContentPane().add( contentPane, BorderLayout.CENTER );
			this.pack();
    			setVisible( true );
		}
		else if( event.getActionCommand().equals( "Remove" ) )
		{
    			remove( contentPane );
    			contentPane = new JPanel();
			BorderLayout bl = new BorderLayout();
			contentPane.setLayout( bl );
			contentPane.setBorder( new EmptyBorder( 120, 35, 120, 35 ) );

    			jobRemove jr = new jobRemove( this, communications ); 
    			contentPane.add( jr );
    			this.getContentPane().add( contentPane, BorderLayout.CENTER );
			this.pack();
    			setVisible( true );
		}
		else if( event.getActionCommand().equals( "Configure" ) )
		{
    			remove( contentPane );
    			contentPane = new JPanel();
			BorderLayout bl = new BorderLayout();
			contentPane.setLayout( bl );
			contentPane.setBorder( new EmptyBorder( 80, 35, 80, 35 ) );

    			jobConfigure sc = new jobConfigure( this, communications ); 
    			contentPane.add( sc );
    			this.getContentPane().add( contentPane, BorderLayout.CENTER );
			this.pack();
    			setVisible( true );
		}
		else if( event.getActionCommand().equals( "Get Results" ) )
		{
    			remove( contentPane );
    			contentPane = new JPanel();
			BorderLayout bl = new BorderLayout();
			contentPane.setLayout( bl );
			contentPane.setBorder( new EmptyBorder( 80, 35, 80, 35 ) );

    			jobResults jr = new jobResults( this, communications ); 
    			contentPane.add( jr );
    			this.getContentPane().add( contentPane, BorderLayout.CENTER );
			this.pack();
    			setVisible( true );
		}			
		else if( event.getActionCommand().equals( "Statistics" ) )
		{
    			remove( contentPane );
    			contentPane = new JPanel();
			BorderLayout bl = new BorderLayout();
			contentPane.setLayout( bl );
			contentPane.setBorder( new EmptyBorder( 15, 15, 15, 15 ) );

			serverStatistics st = new serverStatistics( this, communications, serverIP, rmiPort );
			contentPane.add( st );
    			this.getContentPane().add( contentPane, BorderLayout.CENTER );
			this.pack();
    			setVisible( true );
		}
		else if( event.getActionCommand().equals( "Control" ) )
		{
    			remove( contentPane );
    			contentPane = new JPanel();
			BorderLayout bl = new BorderLayout();
			contentPane.setLayout( bl );
			contentPane.setBorder( new EmptyBorder( 60, 15, 20, 15 ) );

			serverControl sc = new serverControl( this, communications );
			contentPane.add( sc );
			this.getContentPane().add( contentPane, BorderLayout.CENTER );
			this.pack();
    			setVisible( true );
		}
		else if( event.getActionCommand().equals( "Update Client" ) )
		{
    			remove( contentPane );
    			contentPane = new JPanel();
			BorderLayout bl = new BorderLayout();
			contentPane.setLayout( bl );
			contentPane.setBorder( new EmptyBorder( 60, 15, 20, 15 ) );

			updateClient uc = new updateClient( this, communications );
			contentPane.add( uc );
			this.getContentPane().add( contentPane, BorderLayout.CENTER );
			this.pack();
    			setVisible( true );

		}
		else if( event.getActionCommand().equals("About") )
		{
    			remove( contentPane );
    			contentPane = new JPanel();
			BorderLayout bl = new BorderLayout();
			contentPane.setLayout( bl );
			contentPane.setBorder( new EmptyBorder( 100, 60, 100, 60 ) );

    			aboutPanel ap = new aboutPanel();
    			contentPane.add( ap );
    			this.getContentPane().add( contentPane, BorderLayout.CENTER );
			this.pack();
    			setVisible( true );
		}
		else if( event.getActionCommand().equals("Help") )
		{
			contentPane.setVisible( false );
			JOptionPane.showMessageDialog( this, "Please see user documentation at http://www.cs.may.ie/distributed", "Help", JOptionPane.INFORMATION_MESSAGE );
			if( admin && communications != null )
			{
				setScreen( JOBS_VIEW );
			}
			else if( communications != null )
			{
				setScreen( JOB_VIEW );
			}
			else
			{
				setScreen( CONNECT );
			}
			return;
		}
	}
	
	public void setScreen( int screen )
	{
		switch( screen )
		{
			case JOBS_VIEW:
	 			remove( contentPane );
				contentPane = new JPanel();
				BorderLayout bls = new BorderLayout();
				contentPane.setLayout( bls );
				contentPane.setBorder( new EmptyBorder( 15, 15, 15, 15 ) );

				jobsView fv = new jobsView( this, communications ); 
				contentPane.add( fv );
				this.getContentPane().add( contentPane, BorderLayout.CENTER );
				this.pack();
				setVisible( true );
				break;
			case JOB_VIEW:
				remove( contentPane );
				contentPane = new JPanel();
				BorderLayout bl = new BorderLayout();
				contentPane.setLayout( bl );
				contentPane.setBorder( new EmptyBorder( 15, 15, 15, 15 ) );
				//contentPane.setBorder( new EmptyBorder( 120, 35, 120, 35 ) );

				jobView fv1 = new jobView( this, communications ); 
				contentPane.add( fv1 );
				this.getContentPane().add( contentPane, BorderLayout.CENTER );
				this.pack();
				setVisible( true );
				break;
			case CONNECT:
    				remove( contentPane );
				contentPane = new JPanel();
				BorderLayout blc = new BorderLayout();
				contentPane.setLayout( blc );
				contentPane.setBorder( new EmptyBorder( 100, 200, 100, 200 ) );

				connectToServer con = new connectToServer( this, serverIP, rmiPort, socketPort ); 
				contentPane.add( con );
				this.getContentPane().add( contentPane, BorderLayout.CENTER );
				this.pack();
				setVisible( true );
		}
	}
				

	//grays out certain options when not connected
	private void grayOut()
	{
		//gray out menu items
		JMenuItem conn = menuBar.getMenu( 0 ).getItem( 0 );
		conn.setEnabled( true );

		JMenu jobs = menuBar.getMenu( 1 );
		for( int i = 0; i < jobs.getItemCount(); i ++ ){
			JMenuItem jmi = jobs.getItem( i );
			jmi.setEnabled( false );
		}

		JMenu server = menuBar.getMenu( 2 );
		for( int i = 0; i < server.getItemCount(); i ++ ){
			JMenuItem jmi = server.getItem( i );
			jmi.setEnabled( false );
		}

		//update the view
		this.setVisible( true );
	}

	//when connected - show all options
	private void showAll()
	{
		if( admin )
		{
			//gray out menu items
			JMenuItem conn = menuBar.getMenu( 0 ).getItem( 0 );
			conn.setEnabled( false );

			JMenu jobs = menuBar.getMenu( 1 );
			for( int i = 0; i < jobs.getItemCount(); i ++ ){
				JMenuItem jmi = jobs.getItem( i );
				jmi.setEnabled( true );
			}

			JMenu server = menuBar.getMenu( 2 );
			for( int i = 0; i < server.getItemCount(); i ++ ){
				JMenuItem jmi = server.getItem( i );
				jmi.setEnabled( true );
			}
		}
		else
		{
			JMenuItem conn = menuBar.getMenu( 0 ).getItem( 0 );
			conn.setEnabled( false );

			JMenuItem res = menuBar.getMenu( 1 ).getItem( 5 );
			res.setEnabled( true );

			//only allowed to use view job function
			JMenuItem view = menuBar.getMenu( 1 ).getItem( 0 );
			view.setEnabled( true );
		}

		//update the view
		this.setVisible( true );
	}

	public void setConnected( boolean b )
	{
		if( ! b )
		{
			grayOut();
			communications = null;
		}
		else
		{
			showAll();
		}
	}

	public void setAdmin()
	{
		admin = true;
	}

	public boolean getAdmin()
	{
		return admin;
	}

	public boolean getConnected()
	{
		if( communications != null )
		{
			return true;
		}
		return false;
	}

	public void setCommunications( GUICommunications g, String ip, int rmiP, int sPort ){
		communications = g;
		serverIP = ip;
		socketPort = sPort;
		rmiPort = rmiP;
	}

	//return the socket connection info - needed in problemAdd
	public Vector getSocketInfo()
	{
		Vector v = new Vector();
		v.add( serverIP );
		v.add( new Integer( socketPort ) );
		return v;
	}
	
	public void setSelected( File directory, String al, String dm, String[] jars, String[] pd, int cp, int m )
	{
		selected_directory = directory;
		selected_algorithm = al;
		selected_datamanager = dm;
		selected_jars = jars;
		selected_pd = pd;
		minMem = m;
		minCPU = cp;
	}

	public void resetSelected()
	{
		selected_directory = null;
		selected_algorithm = "none selected";
		selected_datamanager = "none selected";
		selected_jars = new String[ 1 ];
		selected_jars[ 0 ] = "none selected";
		selected_pd = new String[ 1 ];
		selected_pd[ 0 ] = "none selected";
		minCPU = 10;
		minMem = 60;
	}

	public void resetAdd()
	{
		remove( contentPane );
		contentPane = new JPanel();
		BorderLayout bl = new BorderLayout();
		contentPane.setLayout( bl );
		contentPane.setBorder( new EmptyBorder( 5, 15, 5, 15 ) );

    		jobAdd ja = new jobAdd( this, communications, selected_directory, selected_algorithm, selected_datamanager, selected_jars, selected_pd, minCPU, minMem );
    		contentPane.add( ja );
    		this.getContentPane().add( contentPane, BorderLayout.CENTER );
    		setVisible( true );
	}

	public static void main( String args[] )
	{
		try
		{
			//redirect the standard error to a file
			File stderr = new File( "stderror.log" );
			System.setErr( new PrintStream( new FileOutputStream( stderr ), true ) );

			//redirect the standard out to a file (shouldnt be any output but ya never know!)
			File stdout = new File( "stdout.log" );
			System.setOut( new PrintStream( new FileOutputStream( stdout ), true ) );

			UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		}
		catch( Exception e )
		{
		}

		RemoteInterface g = new RemoteInterface();
		g.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
	}
}