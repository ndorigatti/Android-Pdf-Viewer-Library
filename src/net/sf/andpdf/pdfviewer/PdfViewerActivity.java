package net.sf.andpdf.pdfviewer;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import net.sf.andpdf.nio.ByteBuffer;
import net.sf.andpdf.refs.HardReference;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androswing.tree.DefaultMutableTreeNode;

import com.sun.pdfview.OutlineNode;
import com.sun.pdfview.PDFDestination;
import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFImage;
import com.sun.pdfview.PDFPaint;
import com.sun.pdfview.action.GoToAction;
import com.sun.pdfview.decrypt.PDFAuthenticationFailureException;
import com.sun.pdfview.font.PDFFont;

/**
 * U:\Android\android-sdk-windows-1.5_r1\tools\adb push u:\Android\simple_T.pdf /data/test.pdf
 * 
 * @author ferenc.hechler
 */
public abstract class PdfViewerActivity extends FragmentActivity
{

	static final String TAG = "PDFVIEWER";

	private PDFFile mPdfFile;
	private File mTmpFile;
	private ViewPager pager;

	/** Called when the activity is first created. */
	@Override
	public void onCreate ( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );

		PDFImage.sShowImages = true;
		PDFPaint.s_doAntiAlias = true;
		PDFFont.sUseFontSubstitution = false;
		setContentView( R.layout.main );
		pager=( ViewPager ) findViewById( R.id.pager );
		pager.setOffscreenPageLimit( 1 );
		parsePDF( "/sdcard/default.pdf" );
	}

	private class PagerAdapter extends FragmentStatePagerAdapter
	{
		public PagerAdapter ()
		{
			super( getSupportFragmentManager() );
		}

		@Override
		public Fragment getItem ( int position )
		{
			return PageFragment.newInstance( position );
		}

		@Override
		public int getCount ()
		{
			return mPdfFile.getNumPages();
		}
	}
	public static class PageFragment extends Fragment
	{
		private static final String PAGE_NR="page_nr";
		public static PageFragment newInstance(int pageNr)
		{
			PageFragment frag=new PageFragment();
			Bundle args=new Bundle();
			args.putInt( PAGE_NR, pageNr+1 );
			frag.setArguments( args );
			return frag;
		}
		private GraphView gv;
		@Override
		public View onCreateView ( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState )
		{
			if (gv!=null)
				( ( ViewGroup ) gv.getParent() ).removeView(gv);
			else
				gv=new GraphView( getActivity(),((PdfViewerActivity)getActivity()).mPdfFile );
			gv.startRenderThread( getArguments().getInt( PAGE_NR ), GraphView.STARTZOOM );//TODO persist zoom
			return gv;
		}
	}

	/*
	 * private void nextPage() { if (mPdfFile != null) { if (mPage < mPdfFile.getNumPages()) { mPage += 1; //mGraphView.bZoomOut.setEnabled(true); //mGraphView.bZoomIn.setEnabled(true); startRenderThread(mPage, mZoom); } } }
	 * 
	 * private void prevPage() { if (mPdfFile != null) { if (mPage > 1) { mPage -= 1; startRenderThread(mPage, mZoom); } } }
	 */

	private List< Pair< String, Integer >> toc = new ArrayList< Pair< String, Integer > >();

	private void parsePDF ( String filename )
	{
		// long startTime = System.currentTimeMillis();
		try
		{
			// first open the file for random access
			RandomAccessFile raf = new RandomAccessFile( filename, "r" );

			// extract a file channel
			FileChannel channel = raf.getChannel();

			// now memory-map a byte-buffer
			ByteBuffer bb = ByteBuffer.NEW( channel.map( FileChannel.MapMode.READ_ONLY, 0, channel.size() ) );
			// create a PDFFile from the data
			mPdfFile = new PDFFile( bb );
			pager.setAdapter( new PagerAdapter() );
			
			OutlineNode page = mPdfFile.getOutline();
			parseOutline( page.children );
			for ( Pair< String, Integer > entry : toc )
				Log.e( TAG, entry.first + " => " + entry.second );
		}
		catch ( PDFAuthenticationFailureException e )
		{
			// TODO password error
		}
		catch ( Throwable e )
		{
			e.printStackTrace(); // FIXME error handling
		}

	}

	private void parseOutline ( List< DefaultMutableTreeNode > list ) throws IOException
	{
		for ( DefaultMutableTreeNode child : list )
		{
			OutlineNode outline = ( OutlineNode ) child;
			PDFDestination dst = ( ( GoToAction ) outline.getAction() ).getDestination();
			// if (child.children.isEmpty())
			toc.add( Pair.create( outline.toString(), mPdfFile.getPageNumber( dst.getPage() ) ) );
			// else
			parseOutline( child.children );

		}
	}

	@Override
	protected void onDestroy ()
	{
		super.onDestroy();
		if ( mTmpFile != null )
		{
			mTmpFile.delete();
			mTmpFile = null;
		}
	}

}