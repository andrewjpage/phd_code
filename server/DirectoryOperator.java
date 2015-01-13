/* Thomas Keane
Purpose: A thread that runs in the background to operate on a set of files

Copyright (C) 2004  Thomas Keane

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
*/

import java.io.*;
import java.util.*;

public abstract class DirectoryOperator extends Thread
{
	protected static Vector currentlyAccessed = new Vector(); //list of the set of directories current being operated on
	protected File directory;
	
	public DirectoryOperator( File dir )
	{
		directory = dir;
		currentlyAccessed.add( directory.getName() );
	}
	
	//method that takes a directory and deletes all the contents
	protected void recursiveDelete( File f )
	{
		//recusively delete the contents of this directory
		File[] files = f.listFiles();

		if( files != null )
		{
			for( int i = 0; i < files.length; i ++ )
			{
				if( files[ i ].isFile() )
				{
					files[ i ].delete();
				}
				else if( files[ i ].isDirectory() )
				{
					//delete subdirectory
					recursiveDelete( files[ i ] );
					files[ i ].delete();
				}
			}
		}
	}
	
	public static boolean isCurrentlyAccessed( String name )
	{
		Iterator it = currentlyAccessed.iterator();
		while( it.hasNext() )
		{
			if( it.next().toString().compareTo( name ) == 0 )
			{
				return true;
			}
		}
		return false;
	}
}
