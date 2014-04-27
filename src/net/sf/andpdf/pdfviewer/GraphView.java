package net.sf.andpdf.pdfviewer;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import uk.co.senab.photoview.PhotoView;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.animation.Animation;

import com.sun.pdfview.PDFPage;
import com.sun.pdfview.PDFParseException;

public final class GraphView extends PhotoView
{
	private static final ExecutorService sService=Executors.newSingleThreadExecutor();
	private static final Handler uiHandler = new Handler();
	private PDFPage mPdfPage;
	
	private final int mPage;
	private float scale=1.0f;
	private final float startFactor;
	private PdfViewer viewer;
	private OnScaleChangedListener listener;
	private PageLoadListener plistener; 
	private boolean isPdfReady;
	private volatile Future< ? > task;
	protected float mZoom=1.0f;

	public GraphView ( Context ctx, int page, PdfViewer viewer ,float startFactor)
	{
		super( ctx );
		setMinScale( 1.0f );
		this.startFactor=startFactor;
		setMaxScale( startFactor*1.8f );
		mPage = page + viewer.mStartPage;
		this.viewer = viewer;
	}
	public GraphView ( Context ctx, int page, PdfViewer viewer )
	{
		this( ctx, page, viewer, 1.0f );
	}
	public Bitmap getPageBitmap ()
	{
		Drawable drw=getDrawable();
		if (isPdfReady&&drw instanceof BitmapDrawable)
			return ( ( BitmapDrawable ) drw ).getBitmap();
		return null;
	}
	private synchronized void startRenderThread ( final int viewWith, final int viewHeight )
	{
		if (task==null||task.isDone())
		{	
			Runnable backgroundRunnable = new Runnable()
			{
				@Override
				public void run ()
				{
					try
					{
						showPage( viewWith, viewHeight );
					}
					catch (PDFParseException e)
					{
						viewer.onParseException( e );
					}
					catch ( IOException e )
					{
						viewer.onIOException( e );
					}
					finally 
					{
						if (plistener!=null)
							plistener.onPageLoadingCompleted();
					}
				}
			};
			task=sService.submit( backgroundRunnable );
		}
	}

	@Override
	protected void onDetachedFromWindow ()
	{
		super.onDetachedFromWindow();
		recycleOldBitmap();
		listener=null;
		plistener=null;
		task.cancel( true );
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

	private void updateImage ( final Bitmap bitmap, final float zoom )
	{
		Animation anim=getAnimation();
		if (anim!=null)
		{	
			anim.cancel();
			anim=null;
		}
		uiHandler.post( new Runnable()
		{
			@Override
			public void run ()
			{
				mZoom=zoom;
				setZoomable( true );
				setScaleType( ScaleType.FIT_CENTER );
				recycleOldBitmap();
				setImageBitmap( bitmap );
				setMaxScale( startFactor*getMaxScale()*mZoom );
				setMidScale( getMidScale()*mZoom );
				setMinScale( getMinScale()*mZoom );
				zoomTo( scale*mZoom, 0.0f, 0.0f, false );
				isPdfReady=true;
			}
		} );
	}

	@Override
	protected void onSizeChanged ( int w, int h, int oldw, int oldh )
	{
		super.onSizeChanged( w, h, oldw, oldh );
		if (oldh==0&&oldw==0)
			startRenderThread( w, h );
	}

	public void setScale ( float scale )
	{
		this.scale = scale;
		zoomTo( scale, 0.0f, 0.0f, false );
	}

	private void showPage ( int w, int h ) throws IOException
	{
		// Only load the page if it's a different page (i.e. not just changing the zoom level)
		if ( mPdfPage == null || mPdfPage.getPageNumber() != mPage )
			mPdfPage = viewer.mPdfFile.getPage( mPage, true );
		if (Thread.interrupted())
			return;		
		float fwidth = mPdfPage.getWidth();
		float fheight = mPdfPage.getHeight();
		float zoom = w / fwidth;
		zoom*=startFactor;
		int oHeight = ( int ) ( fheight * zoom * 1.5f );
		int oWidth = ( int ) ( fwidth * zoom * 1.5f );
		int maxDim = Math.max( oHeight, oWidth );
		if ( maxDim > 2048 )
		{
			zoom *= ( 2048f / maxDim );
			oHeight = ( int ) ( fheight * zoom );
			oWidth = ( int ) ( fwidth * zoom );
		}
		if (Thread.interrupted())
			return;		
		Bitmap bitmap = mPdfPage.getImage( oWidth, oHeight, null, true, true );
		if (Thread.interrupted())
			return;
		zoom=zoom/(h/fheight);
		if (zoom<1.0f)
			zoom=1.0f;
		updateImage( bitmap,zoom );
	}

	@Override
	public void onScale ()
	{
		scale = getScale();
		if (listener!=null)
			listener.onScalechanged( scale/mZoom );
	}
	public void setPageLoadListener(PageLoadListener plistener)
	{
		this.plistener=plistener;
	}
	public void setOnScaleChangedListener(OnScaleChangedListener listener)
	{
		this.listener=listener;
	}
}