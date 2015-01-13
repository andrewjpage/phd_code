/* Thomas Keane
This class must be extended by the user of the system to implement their distributed computation.

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

import java.util.*;
import java.io.File;

/**
 * This class describes how the received data units are to be processed.
 * This class must be extended by the user.
 * The user only has to implement the processUnit method.
 *@author Thomas Keane
*/

public abstract class Algorithm
{
	public static File PROBLEMDIRECTORY = new File( System.getProperty( "user.dir" ), "temp" );
	
	public abstract Vector processUnit( Vector workUnit ) throws Throwable;
}
