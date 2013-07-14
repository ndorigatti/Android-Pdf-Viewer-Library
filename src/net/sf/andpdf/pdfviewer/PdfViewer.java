package net.sf.andpdf.pdfviewer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import net.sf.andpdf.nio.ByteBuffer;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
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
    protected static final String TAG        = "PDFVIEWER";
    PDFFile                       mPdfFile;
    int                           mStartPage = 0;
    public int                    mNumPages;
    private Context               mContext;
    protected Uri                 mUri;
	
	public PdfViewer( Context context, Uri uri ) {
	    Log.d( TAG, String.format( "Creating PdfViewer: %s", uri.toString() ) );
	    mContext = context;
	    mUri = uri;
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

    public int getNumContentPages() {
        return mNumPages - mStartPage;
    }

    public int getStartPageNr() {
        return mStartPage;
    }

	private final Runnable parser = new Runnable() {
		@Override
		public final void run ()
		{
			parsePDF();
		}
	};
	
	public final void loadPDFAsync () {
		new Thread( parser ).start();
	}

    public byte[] readBytes( InputStream inputStream ) throws IOException {

        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

	    int bufferSize = 1024;
	    byte[] buffer = new byte[bufferSize];

        int len = 0;
        while ( ( len = inputStream.read( buffer ) ) != -1 ) {
            byteBuffer.write( buffer, 0, len );
        }

	    return byteBuffer.toByteArray();
	  }
	
	private void parsePDF ()
	{
		final ArrayList< Pair< String, Integer >> toc = new ArrayList< Pair< String, Integer > >();
		try
		{
		    Log.d( TAG, String.format( "Opening: %s", mUri.toString() ) );
		    InputStream inputStream = mContext.getContentResolver().openInputStream( mUri );
		    byte [] pdfBytes = readBytes( inputStream );
		    inputStream.close();

		    Log.d( TAG, String.format( "Read %d bytes", pdfBytes.length ) );
		    
		    ByteBuffer bb = ByteBuffer.NEW( pdfBytes );
		    
			mPdfFile = new PDFFile( bb );
			mNumPages = mPdfFile.getNumPages();
			if (shouldParseOutline()) {
				OutlineNode page = mPdfFile.getOutline();
				parseOutline( page.getChildren(), toc );
				if ( !toc.isEmpty() ) {
					mStartPage = toc.get( 0 ).second;
				}
			}
		} catch ( PDFParseException e ) {
			onParseException( e );
		} catch ( IOException e ) {
			onIOException( e );
			e.printStackTrace();
		} finally {
			if ( mNumPages < 1 ) {
				mPdfFile = null;
			}
		}
		onParseFinished( toc );
	}

    public PDFFile getPDFFile() {
        return mPdfFile;
    }

    protected abstract void onIOException ( IOException e );
	protected abstract void onParseException ( PDFParseException e );
	protected abstract boolean shouldParseOutline();
	protected abstract void onParseFinished ( ArrayList< Pair< String, Integer >> toc );
}