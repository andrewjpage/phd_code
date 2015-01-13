/* Thomas Keane

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
import java.net.*;

class ProblemObjectInputStream extends ObjectInputStream
{
	private ClassLoader ccl;
	
	public ProblemObjectInputStream( InputStream is, ClassLoader a ) throws Exception
	{
		super( is );
		ccl = a;
	}
	
	public Class resolveClass( ObjectStreamClass desc )
	{
		try
		{
			return ccl.loadClass( desc.getName() );
		}
		catch( Throwable t )
		{
			t.printStackTrace( System.err );
			return null;
		}
	}
}
