package net.sf.andpdf.pdfviewer;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import net.sf.andpdf.nio.ByteBuffer;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.util.Pair;
import androswing.tree.DefaultMutableTreeNode;

import com.sun.pdfview.OutlineNode;
import com.sun.pdfview.PDFDestination;
import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFImage;
import com.sun.pdfview.PDFPaint;
import com.sun.pdfview.action.GoToAction;
import com.sun.pdfview.font.PDFFont;

/**
 * U:\Android\android-sdk-windows-1.5_r1\tools\adb push u:\Android\simple_T.pdf /data/test.pdf
 * 
 * @author ferenc.hechler
 */
public abstract class PdfViewerActivity extends FragmentActivity
{
	protected static final String TAG = "PDFVIEWER";
	PDFFile mPdfFile;
	int startPage=0;

	/** Called when the activity is first created. */
	@Override
	public void onCreate ( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );
		PDFImage.sShowImages = true;
		PDFPaint.s_doAntiAlias = true;
		PDFFont.sUseFontSubstitution = false;
	}
	
	private void parseOutline ( List< DefaultMutableTreeNode > list, List< Pair< String, Integer >> toc ) throws IOException
	{
		for ( DefaultMutableTreeNode child : list )
		{
			OutlineNode outline = ( OutlineNode ) child;
			PDFDestination dst = ( ( GoToAction ) outline.getAction() ).getDestination();
			toc.add( Pair.create( outline.toString(), getPDFFile().getPageNumber( dst.getPage() ) ) );
			parseOutline( child.children, toc );
		}
	}
	
	protected int getNumContentPages ()
	{
		return mPdfFile.getNumPages()-startPage;
	}

	protected int getStartPageNr ()
	{
		return startPage;
	}

	protected void parsePDF ( String filename )
	{
		RandomAccessFile raf =null;
		FileChannel channel=null;
		try
		{
			// first open the file for random access
			raf = new RandomAccessFile( filename, "r" );
			// extract a file channel
			channel = raf.getChannel();

			// now memory-map a byte-buffer
			ByteBuffer bb = ByteBuffer.NEW( channel.map( FileChannel.MapMode.READ_ONLY, 0, channel.size() ) );
			// create a PDFFile from the data
			mPdfFile = new PDFFile( bb );
			OutlineNode page = mPdfFile.getOutline();
			ArrayList< Pair< String, Integer >> toc = new ArrayList< Pair< String, Integer > >();
			parseOutline( page.children, toc );
			if (!toc.isEmpty())
				startPage=toc.get( 0 ).second;
			onParseFinished(toc);
		}
		catch ( IOException e )
		{
			e.printStackTrace(); // FIXME error handling
		}
		finally
		{
			closeSilently( channel );
			closeSilently(raf);
		}

	}
	private static void closeSilently(Closeable closeable)
	{
		if (closeable!=null)
			try
			{
				closeable.close();
			}
			catch ( IOException e )
			{
				//igonred
			}
	}
	protected PDFFile getPDFFile()
	{
		return mPdfFile;
	}
	protected abstract void onParseFinished(ArrayList< Pair< String, Integer >> toc);
}