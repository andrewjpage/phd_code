/*
Thomas Keane, Wed Nov 17 14:17:29 GMT 2004 @637 /Internet Time/
This class defines a new exception which is throws by the Compressor when
there is a problem decompressing a result set

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

public class CompressorException extends Exception
{
	public CompressorException( String cause )
	{
		super( cause );
	}
}
