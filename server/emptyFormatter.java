/*
Thomas Keane, 4:09 PM 2/15/03
just returns the actual logging message - takes out all extra insertions in message

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

import java.util.logging.*;

public class emptyFormatter extends Formatter{

	public emptyFormatter(){
	}

	public String formatMessage( LogRecord record ){
		return record.getMessage();
	} 
	
	public String format( LogRecord record ){
		return record.getMessage();
	}
}