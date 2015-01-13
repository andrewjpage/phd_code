/* Thomas Keane
Purpose: A thread that runs in the background to delete a directory of files

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

import java.io.*;

public class Deleter extends DirectoryOperator
{
	public Deleter( File directory )
	{
		super( directory );
	}

	public void run()
	{
		if( directory.isDirectory() )
		{
			recursiveDelete( directory );
			directory.delete();
		}
		else
		{
			directory.delete();
		}
		
		//finished - remove the name of the dir from the vector
		currentlyAccessed.remove( directory.getName() );
	}
}
