/* Thomas Keane
Purpose: To store all of the current units that are out being processed by the system.
Functionality to remove the oldest entry - use with expired unit list

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

class Deleter extends Thread
{
	private File f;

	public Deleter( File fi )
	{
		f = fi;
	}

	public void run()
	{
		//try to delete the file
		for( int i = 0; i < 5; i ++ )
		{
			if( f.delete() )
			{
				return;
			}
			try{ sleep( 5000 ); }catch( Exception e ){}
		}
	}
}