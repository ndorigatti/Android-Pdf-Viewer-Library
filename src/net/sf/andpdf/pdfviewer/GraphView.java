package net.sf.andpdf.pdfviewer;

import uk.co.senab.photoview.PhotoView;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;

import com.sun.pdfview.PDFPage;

public final class GraphView extends PhotoView
{
	private static final Handler uiHandler = new Handler();
	private PDFPage mPdfPage;
	private volatile Thread backgroundThread;
	private final int mPage;
	private float scale;
	private PdfViewer viewer;
	private OnScaleChangedListener listener; 

	public GraphView ( Context ctx, int page, PdfViewer viewer )
	{
		super( ctx );
		setMinScale( 1.0f );
		setMaxScale( 1.8f );
		mPage = page + viewer.startPage;
		this.viewer = viewer;
	}

	private synchronized void startRenderThread ( final int viewWith, final int viewHeight )
	{
		if ( backgroundThread != null )
			return;

		backgroundThread = new Thread()
		{
			@Override
			public void run ()
			{
				showPage( viewWith, viewHeight );
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
		listener=null;
	}

	private void recycleOldBitmap ()
	{
		Drawable drw = getDrawable();
		if ( drw instanceof BitmapDrawable )
		{
			Bitmap bmp = ( ( BitmapDrawable ) drw ).getBitmap();
			if ( bmp != null )
				bmp.recycle();
		}
	}

	private void updateImage ( final Bitmap bitmap )
	{
		uiHandler.post( new Runnable()
		{
			@Override
			public void run ()
			{
				setZoomable( true );
				setScaleType( ScaleType.FIT_CENTER );
				setAdjustViewBounds( true );
				recycleOldBitmap();
				setImageBitmap( bitmap );
				zoomTo( scale, 0.0f, 0.0f, false );
			}
		} );
	}

	@Override
	protected void onSizeChanged ( int w, int h, int oldw, int oldh )
	{
		super.onSizeChanged( w, h, oldw, oldh );
		startRenderThread( w, h );
	}

	public void setScale ( float scale )
	{
		zoomTo( scale, 0.0f, 0.0f, false );
		this.scale = scale;
	}

	private void showPage ( int w, int h )
	{
		// Only load the page if it's a different page (i.e. not just changing the zoom level)
		if ( mPdfPage == null || mPdfPage.getPageNumber() != mPage )
			mPdfPage = viewer.mPdfFile.getPage( mPage, true );
		float fwidth = mPdfPage.getWidth();
		float fheight = mPdfPage.getHeight();
		float zoom = h / fheight;
		int oHeight = ( int ) ( fheight * zoom * 1.5f );
		int oWidth = ( int ) ( fwidth * zoom * 1.5f );
		int maxDim = Math.max( oHeight, oWidth );
		if ( maxDim > 2048 )
		{
			zoom *= ( 2048f / maxDim );
			oHeight = ( int ) ( fheight * zoom );
			oWidth = ( int ) ( fwidth * zoom );
		}
		Bitmap bitmap = mPdfPage.getImage( oWidth, oHeight, null, true, true );
		updateImage( bitmap );
	}

	@Override
	public void onScale ()
	{
		scale = getScale();
		if (listener!=null)
			listener.onScalechanged( scale );
	}
	public void setOnScaleChangedListener(OnScaleChangedListener listener)
	{
		this.listener=listener;
	}
}