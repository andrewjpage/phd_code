/*
Thomas Keane, 11:28 AM 2/4/03
class that is used to get user to add a problem to the server

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
import java.util.jar.*;
import java.util.zip.ZipEntry;

public class aboutPanel extends JPanel
{
	public aboutPanel()
	{
		//display some info about the authors
		super( new GridLayout( 7, 1, 10, 10 ) );
		//setBackground( Color.blue.brighter() );

		JLabel system = new JLabel( "Java Distributed Computing", SwingConstants.CENTER );
		JLabel author = new JLabel( "Author: Thomas Keane", SwingConstants.CENTER );
		JLabel address1 = new JLabel( "Department of Computer Science,", SwingConstants.CENTER );
		JLabel address2 = new JLabel( "National University of Ireland Maynooth,", SwingConstants.CENTER );
		JLabel address3 = new JLabel( "Co. Kildare,", SwingConstants.CENTER );
		JLabel address4 = new JLabel( "Ireland", SwingConstants.CENTER );
		JLabel info = new JLabel( "For information and documentation - goto http://www.cs.may.ie/distributed", SwingConstants.CENTER );
		JLabel help = new JLabel( "with thanks Andrew Page & Tom Naughton", SwingConstants.CENTER );
		
		add( system );
		add( author );
		add( address1 );
		add( address2 );
		add( address3 );
		add( address4 );
		add( help );
	}
}