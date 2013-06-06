package net.sf.andpdf.pdfviewer;

import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher.OnMatrixChangedListener;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.Log;

import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;

class GraphView extends PhotoView implements OnMatrixChangedListener
{
	private PDFFile pdfFile;
	public static final float STARTZOOM = 2.0f;
	private float mZoom = STARTZOOM;

	private static final Handler uiHandler = new Handler();

	private PDFPage mPdfPage;

	private volatile Thread backgroundThread;

	private int mPage = 1;
	private Bitmap mBi;
	private float mOldScale;

	public GraphView ( Context context, PDFFile pdfFile )
	{
		super( context );
		this.pdfFile = pdfFile;
		setMinScale( 1.0f );
		setMaxScale( 2.0f );
		mOldScale = getScale();
		setOnMatrixChangeListener( this );

		updateImage();
		setBackgroundColor( Color.LTGRAY );
	}

	public synchronized void startRenderThread ( final int page, final float zoom )
	{
		if ( backgroundThread != null )
			return;
		if ( mPdfPage != null )
		{
			int newWidth = ( int ) ( mPdfPage.getWidth() * zoom );
			int newHeight = ( int ) ( mPdfPage.getHeight() * zoom );
			if ( newWidth > 2048 || newHeight > 2048 )
				return;
		}
		Log.i( PdfViewerActivity.TAG,  "reading page " + page + ", zoom:" + zoom );
		
		backgroundThread = new Thread() 
		{
			@Override
			public void run ()
			{
				try
				{
					showPage( page, zoom );
				}
				catch ( Exception e )
				{
					//FIXME
					e.printStackTrace();
				}
				backgroundThread = null;
			}
		};
		backgroundThread.start();
	}

	@Override
	protected void onDetachedFromWindow ()
	{
		super.onDetachedFromWindow();
		if ( mBi != null )
		{
			mBi.recycle();
			mBi = null;
		}
		Drawable drw=getDrawable();
		if (drw!=null)
		{
			Bitmap bmp=( ( BitmapDrawable ) drw ).getBitmap();
			if (bmp!=null)
				bmp.recycle();
		}	
	}

	private void updateImage ()
	{
		uiHandler.post( new Runnable()
		{
			@Override
			public void run ()
			{
				setImageBitmap( mBi );
			}
		} );
	}

	private void setPageBitmap ( Bitmap bi )
	{
		if ( bi != null )
			mBi = bi;
	}

	@Override
	public void onMatrixChanged ( RectF arg0 )
	{
		float scale = getScale();
		if ( Math.abs( scale - mOldScale ) < 0.1f )
			return;
		mZoom = STARTZOOM * scale;
		if ( mZoom < STARTZOOM )
			mZoom = STARTZOOM;
		else
			startRenderThread( mPage, mZoom );
	}

	private void showPage ( int page, float zoom ) throws Exception
	{
		// long startTime = System.currentTimeMillis();
		// long middleTime = startTime;
		try
		{
			updateImage();

			// Only load the page if it's a different page (i.e. not just changing the zoom level)
			if ( mPdfPage == null || mPdfPage.getPageNumber() != page )
			{
				mPdfPage = pdfFile.getPage( page, true );
			}
			float width = mPdfPage.getWidth();
			float height = mPdfPage.getHeight();
			Bitmap bi = mPdfPage.getImage( ( int ) ( width * zoom ), ( int ) ( height * zoom ), null, true, true );
			setPageBitmap( bi );
			updateImage();
			//TODO optimize
		}
		catch ( Throwable e )
		{
			Log.e( PdfViewerActivity.TAG, e.getMessage(), e );
		}
	}
}