package net.sf.andpdf.pdfviewer;

import uk.co.senab.photoview.PhotoView;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.Log;

import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;

public final class GraphView extends PhotoView
{
	private PDFFile pdfFile;
	private static final Handler uiHandler = new Handler();
	private PDFPage mPdfPage;
	private volatile Thread backgroundThread;
	private final int mPage;

	public GraphView ( PdfViewerActivity act, int page )
	{
		super( act );
		pdfFile=act.mPdfFile;
		setMinScale( 1.0f );
		setMaxScale( 6.0f );
		setScaleType( ScaleType.FIT_XY );
		setAdjustViewBounds( true );
		setBackgroundColor( Color.LTGRAY );
		mPage=page+act.startPage;
	}

	private synchronized void startRenderThread (final int viewWith, final int viewHeight)
	{
		if ( backgroundThread != null )
			return;
		//Log.i( PdfViewerActivity.TAG,  "reading page " + mPage );
		
		backgroundThread = new Thread() 
		{
			@Override
			public void run ()
			{
				showPage(viewWith ,viewHeight);
				backgroundThread = null;
			}
		};
		backgroundThread.start();
	}

	@Override
	protected void onDetachedFromWindow ()
	{
		super.onDetachedFromWindow();
		recycleOldBitmap();
	}
	
	private void recycleOldBitmap()
	{
		Drawable drw=getDrawable();
		if (drw!=null)
		{
			Bitmap bmp=( ( BitmapDrawable ) drw ).getBitmap();
			if (bmp!=null)
				bmp.recycle();
		}	
	}
	
	private void updateImage (final Bitmap bitmap)
	{
		uiHandler.post( new Runnable()
		{
			@Override
			public void run ()
			{
				recycleOldBitmap();
				setImageBitmap( bitmap );
			}
		} );
	}
	
	@Override
	protected void onSizeChanged ( int w, int h, int oldw, int oldh )
	{
		super.onSizeChanged( w, h, oldw, oldh );
		startRenderThread(w,h);
	}
	
	private void showPage (int w, int h)
	{
		try
		{
			// Only load the page if it's a different page (i.e. not just changing the zoom level)
			if ( mPdfPage == null || mPdfPage.getPageNumber() != mPage )
			{
				mPdfPage = pdfFile.getPage( mPage, true );
			}
			float fwidth = mPdfPage.getWidth();
			float fheight = mPdfPage.getHeight();
			float zoom=h/fheight;
			int oHeight=( int ) ( fheight * zoom );
			int oWidth=( int ) ( fwidth * zoom );
			int maxDim=Math.max( oHeight, oWidth );
			if (maxDim>2048)
			{
				zoom*=(2048f/maxDim);
				oHeight=( int ) ( fheight * zoom );
				oWidth=( int ) ( fwidth * zoom );				
			}	
			Bitmap bitmap= mPdfPage.getImage( oWidth, oHeight, null, true, true );
			updateImage(bitmap);
		}
		catch ( Throwable e )
		{//FIXME handle in act
			Log.e( PdfViewerActivity.TAG, e.getMessage(), e );
		}
	}
	@Override
	public void onScale ()
	{
		((PdfViewerActivity)getContext()).onScalechanged(getScale());
	}
}