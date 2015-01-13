/*
Thomas Keane, 11:31 AM 2/13/03
A file filter for a file chooser - only display certain files

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

import java.io.File;
import javax.swing.filechooser.FileFilter;

class filter extends javax.swing.filechooser.FileFilter{

	//extension of files to be displayed
	private String extension;
	
	public filter( String ex )
	{
		extension = ex;
	}

	//filter files here
	public boolean accept( File f )
	{
		if( f.isDirectory() )
		{
			return true;
		}

		String name = f.getName();

		//parse the extension from the file
		int pos = name.lastIndexOf( '.' );
		String exten = name.substring( pos + 1 );

		if( exten.equals( extension ) )
		{
			return true;
		}

		return false;
	}

	//english description of acceptable files
	public String getDescription()
	{
		return "." + extension + " Files";
	}
}