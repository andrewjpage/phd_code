/*
Thomas Keane, 10:41 AM 3/29/03
a low priority thread that is used to zip a problem directory

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

import java.util.zip.*;
import java.io.*;

public class Zipper extends DirectoryOperator
{
	private char mode; //says whether or not to append E or K on front of file name
	private Scheduler owner;

	public Zipper( File dir, char m, Scheduler owner_ )
	{
		super( dir );
		mode = m;
		owner = owner_;
	}

	public void run()
	{
		try
		{
			//zip the contents of the directory
			File[] files = null;
			if( directory.isDirectory() )
			{
				files = directory.listFiles();
				
				//io error
				if( files == null )
				{
					return;
				}
			}
			else
			{
				return;
			}

			// Create a buffer for reading the files
			byte[] buf = new byte[ 4096 ];

			ZipOutputStream out = null;
			File f = null;
		        // Create the ZIP file
			if( mode == 'E' )
			{
				f = new File( directory.getParentFile(), "E" + directory.getName() );
				out = new ZipOutputStream( new FileOutputStream( f ) );
			}
			else if( mode == 'K' )
			{
				f = new File( directory.getParentFile(), "K" + directory.getName() );
				out = new ZipOutputStream( new FileOutputStream( f ) );
			}
			else
			{
				f = new File( directory.getParentFile(), "R" + directory.getName() );
				out = new ZipOutputStream( new FileOutputStream( f ) );
			}

			//maximum compression - maximise disk space
			out.setLevel( 9 );

			//Compress the files
			for( int i = 0; i < files.length; i++ )
			{
				if( files[ i ].isDirectory() )
				{
					recursiveZip( directory.getName() + "/" + files[ i ].getName(), files[ i ], out );
				}
				else if( ! files[ i ].getName().endsWith( ".lck" ) )
				{
					FileInputStream in = new FileInputStream( files[ i ] );

					//Add ZIP entry to output stream
					out.putNextEntry( new ZipEntry( directory.getName() + "/" + files[ i ].getName() ) );

					//Transfer bytes from the file to the ZIP file
					int numRead = 1;
					while( numRead > 0 )
					{
						numRead = in.read( buf );
						
						//if read some bytes - then write them out
						if( numRead > 0 )
						{
							out.write( buf, 0, numRead );
						}
					}

					//Complete the entry
					out.closeEntry();
					in.close();
				}
			}
			out.close();

			//delete the old directory
			recursiveDelete( directory );
			directory.delete();

			f.renameTo( new File( f.getParent(), f.getName() + ".zip" ) );
		}
		catch( Exception e )
		{
			currentlyAccessed.remove( directory.getName() );
		}
		currentlyAccessed.remove( directory.getName() );
		owner.updateFinishedJobsVector();
	}

	//method for recursively traversing the directory structure & zipping files
	private void recursiveZip( String dirPath, File directory, ZipOutputStream out ) throws Exception
	{
		File[] files = directory.listFiles();
		if( files != null )
		{
			for( int i = 0; i < files.length; i ++ )
			{
				//traverse the directory structure to bottom
				if( files[ i ].isDirectory() )
				{
					recursiveZip( dirPath + "/" + files[ i ].getName(), files[ i ], out );
				}
				else if( ! files[ i ].getName().endsWith( ".lck" ) )
				{
					//file -> zip the file
					FileInputStream in = new FileInputStream( files[ i ] );
					byte[] buf = new byte[ 4096 ];

					// Add ZIP entry to output stream
					out.putNextEntry( new ZipEntry( dirPath + "/" + files[ i ].getName() ) );

					// Transfer bytes from the file to the ZIP file
					int numRead = 1;
					while( numRead > 0 )
					{
						numRead = in.read( buf );
						if( numRead > 0 )
						{
							out.write( buf, 0, numRead );
						}
					}

					out.closeEntry();
					in.close();
				}
			}
		}
	}
}
