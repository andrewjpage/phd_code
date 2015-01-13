/*
Thomas Keane, Thu Jun 17 15:52:39 BST 2004 @661 /Internet Time/
An alternative to the standard Java StringTokenizer that considers only the total
delimiter string when tokenizing a string (i.e. not substrings of the delimiter)

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

public class GlobalStringTokenizer
{
	//the string and delimiter
	private String actualString;
	private char[] string;
	private char[] delimiter;
	
	private int issued;
	private int numTokens;
	private String[] tokens;
	
	public GlobalStringTokenizer( String s, String d ) throws Exception
	{
		if( s == null || d == null )
		{
			throw new Exception( "Null strings received" );
		}
		
		if( s.length() == 0 || d.length() == 0 )
		{
			throw new Exception( "Strings must of length greater than zero" );
		}
		
		//convert to char arrays
		actualString = s;
		string = s.toCharArray();
		delimiter = d.toCharArray();
		
		Vector t = new Vector();
		int lastStartPoint = 0;
		
		//run down through the string and identify areas where the delimiter exists
		for( int i = 0; i < string.length; i ++ )
		{
			for( int j = 0; j < delimiter.length && ( i + j ) < string.length; j ++ )
			{
				if( delimiter[ j ] != string[ i + j ] )
				{
					break;
				}
				else if( j == delimiter.length - 1 )
				{
					t.add( actualString.substring( lastStartPoint, i ) );
					lastStartPoint = i + j + 1;
				}
			}
		}
		
		t.add( actualString.substring( lastStartPoint ) );
		
		//convert the tokens vector to an array
		tokens = new String[ t.size() ];
		int i = 0;
		Iterator it = t.iterator();
		while( it.hasNext() )
		{
			tokens[ i ] = (String) it.next();
			i ++;
		}
		
		numTokens = tokens.length;
	}
	
	public int countTokens()
	{
		return numTokens;
	}
	
	public boolean hasMoreTokens()
	{
		if( issued < numTokens )
		{
			return true;
		}
		return false;
	}
	
	public String nextToken()
	{
		if( issued == numTokens )
		{
			return null;
		}
		
		String s = tokens[ issued ];
		issued ++;
		return s;
	}
}
