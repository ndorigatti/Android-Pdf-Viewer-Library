package net.sf.andpdf.pdfviewer;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import net.sf.andpdf.nio.ByteBuffer;
import net.sf.andpdf.utils.Utils;
import android.content.res.AssetFileDescriptor;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.util.Pair;
import androswing.tree.DefaultMutableTreeNode;

import com.sun.pdfview.OutlineNode;
import com.sun.pdfview.PDFDestination;
import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPaint;
import com.sun.pdfview.PDFParseException;
import com.sun.pdfview.action.GoToAction;
import com.sun.pdfview.font.PDFFont;

/**
 * U:\Android\android-sdk-windows-1.5_r1\tools\adb push u:\Android\simple_T.pdf /data/test.pdf
 * 
 * @author ferenc.hechler
 */
public abstract class PdfViewerActivity extends FragmentActivity implements Runnable
{
	protected static final String TAG = "PDFVIEWER";
	PDFFile mPdfFile;
	int startPage = 0;
	private int numPages;


	private void parseOutline ( List< DefaultMutableTreeNode > list, List< Pair< String, Integer >> toc ) throws IOException
	{
		for ( DefaultMutableTreeNode child : list )
		{
			OutlineNode outline = ( OutlineNode ) child;
			PDFDestination dst = ( ( GoToAction ) outline.getAction() ).getDestination();
			toc.add( Pair.create( outline.toString(), mPdfFile.getPageNumber( dst.getPage() ) ) );
			parseOutline( child.children, toc );
		}
	}

	protected int getNumContentPages ()
	{
		return numPages - startPage;
	}

	protected int getStartPageNr ()
	{
		return startPage;
	}
	
	@Override
	public final void run ()
	{
		parsePDF( );
	}
	protected final void loadPDFAsync()
	{
		new Thread(this).start();
	}
	public static void copyFdToFile(FileDescriptor src, File dst) throws IOException {
	    FileChannel inChannel = new FileInputStream(src).getChannel();
	    FileChannel outChannel = new FileOutputStream(dst).getChannel();
	    try {
	        inChannel.transferTo(0, inChannel.size(), outChannel);
	    } finally {
	        if (inChannel != null)
	            inChannel.close();
	        if (outChannel != null)
	            outChannel.close();
	    }
	}
	private void parsePDF (  )
	{
	//	FileChannel outChannel = null, inChannel=null, 
				FileChannel	pdfChannel=null;
		final ArrayList< Pair< String, Integer >> toc = new ArrayList< Pair< String, Integer > >();
		try
		{
//			inChannel = getAssets().openFd( "default.mp3" ).createInputStream().getChannel();
//			outChannel=openFileOutput( "default.mp3",Context.MODE_PRIVATE ).getChannel();
//			inChannel.transferTo( 0, inChannel.size(), outChannel );
//			outChannel.close();
//		    AssetFileDescriptor afd = getAssets().openFd( "default.mp3");
//
//		    // Create new file to copy into.
//		    File file = new File(Environment.getDownloadCacheDirectory(),"default.mp3");
//		    file.delete();
//
//		    copyFdToFile(afd.getFileDescriptor(), file);			
			pdfChannel=new FileInputStream( new File(Environment.getExternalStorageDirectory(),"default.pdf") ).getChannel();
//			// now memory-map a byte-buffer
			ByteBuffer bb = ByteBuffer.NEW( pdfChannel.map( FileChannel.MapMode.READ_ONLY, 0, pdfChannel.size() ) );
//			InputStream is = getAssets().open( "book.mp3" );
//			byte[] array=new byte[is.available()];
//			while ()
//			
//			java.nio.ByteBuffer buf=java.nio.ByteBuffer.wrap( array );
			// create a PDFFile from the data
			mPdfFile = new PDFFile( bb );
			numPages=mPdfFile.getNumPages();
			OutlineNode page = mPdfFile.getOutline();
			
			parseOutline( page.children, toc );
			if ( !toc.isEmpty() )
				startPage = toc.get( 0 ).second;
		}
		catch ( PDFParseException e )
		{
			onParseException(e);
		}
		catch ( IOException e )
		{
			onIOException(e);
			e.printStackTrace();
		}
		finally
		{
//			Utils.closeSilently( outChannel );
//			Utils.closeSilently( inChannel );
			Utils.closeSilently( pdfChannel );
			if (numPages<1)
				mPdfFile=null;
		}
		runOnUiThread( new Runnable()
		{
			@Override
			public void run ()
			{
				onParseFinished( toc );
			}
		} );
	}

	protected abstract void onIOException ( IOException e );
	protected abstract void onParseException ( PDFParseException e );
	
	protected PDFFile getPDFFile ()
	{
		return mPdfFile;
	}

	protected abstract void onParseFinished ( ArrayList< Pair< String, Integer >> toc );

	public abstract void onScalechanged ( float scale );
}