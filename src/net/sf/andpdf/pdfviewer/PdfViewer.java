package net.sf.andpdf.pdfviewer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import net.sf.andpdf.nio.ByteBuffer;
import net.sf.andpdf.utils.Utils;
import android.content.Context;
import android.util.Pair;
import androswing.tree.DefaultMutableTreeNode;

import com.sun.pdfview.OutlineNode;
import com.sun.pdfview.PDFDestination;
import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFParseException;
import com.sun.pdfview.action.GoToAction;

/**
 * U:\Android\android-sdk-windows-1.5_r1\tools\adb push u:\Android\simple_T.pdf /data/test.pdf
 * 
 * @author ferenc.hechler
 */
public abstract class PdfViewer
{
	protected static final String TAG = "PDFVIEWER";
	PDFFile mPdfFile;
	int startPage = 0;
	private int numPages, pdfResId;
	private Context ctx;
	
	public PdfViewer (Context ctx, int pdfResId)
	{
		this.ctx=ctx;
		this.pdfResId=pdfResId;
	}
	
	private void parseOutline ( List< DefaultMutableTreeNode > list, List< Pair< String, Integer >> toc ) throws IOException
	{
		for ( DefaultMutableTreeNode child : list )
		{
			OutlineNode outline = ( OutlineNode ) child;
			PDFDestination dst = ( ( GoToAction ) outline.getAction() ).getDestination();
			toc.add( Pair.create( outline.toString(), mPdfFile.getPageNumber( dst.getPage() ) ) );
			parseOutline( child.getChildren(), toc );
		}
	}

	public int getNumContentPages ()
	{
		return numPages - startPage;
	}

	public int getStartPageNr ()
	{
		return startPage;
	}

	private final Runnable parser = new Runnable()
	{
		@Override
		public final void run ()
		{
			parsePDF();
		}

	};
	
	public final void loadPDFAsync ()
	{
		new Thread( parser ).start();
	}

	private void parsePDF ()
	{
		FileChannel pdfChannel = null;
		InputStream is=null;
		@SuppressWarnings ( "resource" )//bogus
		RandomAccessFile raf=null;
		final ArrayList< Pair< String, Integer >> toc = new ArrayList< Pair< String, Integer > >();
		try
		{
			File dest=new File(ctx.getCacheDir(),String.valueOf( pdfResId ));
			raf=new RandomAccessFile( dest, "rw" );
			//if (!dest.exists())
			{	
				is = ctx.getResources().openRawResource( pdfResId );
				byte [] buf=new byte[4096];
				int bytes;
				while ((bytes=is.read( buf ))>0)
					raf.write( buf, 0, bytes );
			}
			pdfChannel = raf.getChannel();
			
			// // now memory-map a byte-buffer
			ByteBuffer bb = ByteBuffer.NEW( pdfChannel.map( FileChannel.MapMode.READ_ONLY, 0, pdfChannel.size() ) );
			// create a PDFFile from the data
			mPdfFile = new PDFFile( bb );
			numPages = mPdfFile.getNumPages();
			if (shouldParseOutline())
			{	
				OutlineNode page = mPdfFile.getOutline();
				parseOutline( page.getChildren(), toc );
				if ( !toc.isEmpty() )
					startPage = toc.get( 0 ).second;
			}
		}
		catch ( PDFParseException e )
		{
			onParseException( e );
		}
		catch ( IOException e )
		{
			onIOException( e );
			e.printStackTrace();
		}
		finally
		{
			Utils.closeSilently( is );
			Utils.closeSilently( raf );
			Utils.closeSilently( pdfChannel );
			if ( numPages < 1 )
				mPdfFile = null;
		}
		onParseFinished( toc );
	}

	protected abstract void onIOException ( IOException e );

	protected abstract void onParseException ( PDFParseException e );

	public PDFFile getPDFFile ()
	{
		return mPdfFile;
	}
	protected abstract boolean shouldParseOutline();
	protected abstract void onParseFinished ( ArrayList< Pair< String, Integer >> toc );
}