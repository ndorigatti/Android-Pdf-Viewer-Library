package net.sf.andpdf.pdfviewer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import net.sf.andpdf.nio.ByteBuffer;
import net.sf.andpdf.refs.HardReference;
import uk.co.senab.photoview.PhotoViewAttacher;
import uk.co.senab.photoview.PhotoViewAttacher.OnMatrixChangedListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androswing.tree.DefaultMutableTreeNode;

import com.sun.pdfview.OutlineNode;
import com.sun.pdfview.PDFDestination;
import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFImage;
import com.sun.pdfview.PDFPage;
import com.sun.pdfview.PDFPaint;
import com.sun.pdfview.action.GoToAction;
import com.sun.pdfview.decrypt.PDFAuthenticationFailureException;
import com.sun.pdfview.decrypt.PDFPassword;
import com.sun.pdfview.font.PDFFont;


/**
 * U:\Android\android-sdk-windows-1.5_r1\tools\adb push u:\Android\simple_T.pdf /data/test.pdf
 * @author ferenc.hechler
 */
public abstract class PdfViewerActivity extends Activity {
	
	private static final String TAG = "PDFVIEWER";
	
    public static final String EXTRA_PDFFILENAME = "net.sf.andpdf.extra.PDFFILENAME";
    public static final String EXTRA_SHOWIMAGES = "net.sf.andpdf.extra.SHOWIMAGES";
    public static final String EXTRA_ANTIALIAS = "net.sf.andpdf.extra.ANTIALIAS";
    public static final String EXTRA_USEFONTSUBSTITUTION = "net.sf.andpdf.extra.USEFONTSUBSTITUTION";
    public static final String EXTRA_KEEPCACHES = "net.sf.andpdf.extra.KEEPCACHES";
	
	public static final boolean DEFAULTSHOWIMAGES = true;
	public static final boolean DEFAULTANTIALIAS = true;
	public static final boolean DEFAULTUSEFONTSUBSTITUTION = false;
	public static final boolean DEFAULTKEEPCACHES = true;
    
	private GraphView mGraphView;
	private String pdffilename;
	private PDFFile mPdfFile;
    private File mTmpFile;


	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
	        mGraphView = new GraphView(this);	        
	        Intent intent = getIntent();

	        boolean showImages = getIntent().getBooleanExtra(PdfViewerActivity.EXTRA_SHOWIMAGES, PdfViewerActivity.DEFAULTSHOWIMAGES);
	        PDFImage.sShowImages = showImages;
	        boolean antiAlias = getIntent().getBooleanExtra(PdfViewerActivity.EXTRA_ANTIALIAS, PdfViewerActivity.DEFAULTANTIALIAS);
	        PDFPaint.s_doAntiAlias = antiAlias;
	    	boolean useFontSubstitution = getIntent().getBooleanExtra(PdfViewerActivity.EXTRA_USEFONTSUBSTITUTION, PdfViewerActivity.DEFAULTUSEFONTSUBSTITUTION);
	        PDFFont.sUseFontSubstitution= useFontSubstitution;
	    	boolean keepCaches = getIntent().getBooleanExtra(PdfViewerActivity.EXTRA_KEEPCACHES, PdfViewerActivity.DEFAULTKEEPCACHES);
	        HardReference.sKeepCaches= keepCaches;
		        
	        if (intent != null) {
	        	if ("android.intent.action.VIEW".equals(intent.getAction())) {
        			pdffilename = storeUriContentToFile(intent.getData());
	        	}
	        	else {
	                pdffilename = getIntent().getStringExtra(PdfViewerActivity.EXTRA_PDFFILENAME);
	        	}
	        }
	        
	        if (pdffilename == null)
	        	pdffilename = "no file selected";
			setContent(null);
	        
    //    }
    }
    	
    

	private void setContent(String password) {
        try { 
    		parsePDF(pdffilename, password);
	        setContentView(mGraphView);
	        mGraphView.init(1, GraphView.STARTZOOM);
    	}
        catch (PDFAuthenticationFailureException e) {
        	setContentView(getPdfPasswordLayoutResource());
           	final EditText etPW= (EditText) findViewById(getPdfPasswordEditField());
           	Button btOK= (Button) findViewById(getPdfPasswordOkButton());
        	Button btExit = (Button) findViewById(getPdfPasswordExitButton());
            btOK.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					String pw = etPW.getText().toString();
		        	setContent(pw);
				}
			});
            btExit.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					finish();
				}
			});
        }
	}

/*	private void nextPage() {
    	if (mPdfFile != null) {
    		if (mPage < mPdfFile.getNumPages()) {
    			mPage += 1;
    			//mGraphView.bZoomOut.setEnabled(true);
    			//mGraphView.bZoomIn.setEnabled(true);
    			startRenderThread(mPage, mZoom);
    		}
    	}
	}

    private void prevPage() {
    	if (mPdfFile != null) {
    		if (mPage > 1) {
    			mPage -= 1;
    			startRenderThread(mPage, mZoom);
    		}
    	}
	}*/
    
	private static class GraphView extends ImageView implements OnMatrixChangedListener 
	{
		private PDFFile pdfFile;
		public static final float STARTZOOM = 3.0f;
		private float mZoom=STARTZOOM;
		
		private static final Handler uiHandler=new Handler();
	    
	    private PDFPage mPdfPage; 
	    
	    private Thread backgroundThread;
		
		private static final float MIN_ZOOM = 3.0f;
		private static final float MAX_ZOOM = 6.0f;
		private static final float ZOOM_INCREMENT = 1.5f;		
		private int mPage=1;
    	private Bitmap mBi;
    	private PhotoViewAttacher mAttacher;
    	private float mOldScale;
        public GraphView(Context context) {
            super(context);
		        //mImageView = new ImageView(context);
		        mAttacher = new PhotoViewAttacher( this );
		        mAttacher.setMinScale( 1.0f );
		        mAttacher.setMaxScale( 3.0f );
		        mOldScale=mAttacher.getScale();
		        mAttacher.setOnMatrixChangeListener( this );
		        
		        setPageBitmap(null);
		        updateImage();
		        RelativeLayout.LayoutParams lp=new RelativeLayout.LayoutParams( LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT );
		        lp.addRule( RelativeLayout.CENTER_IN_PARENT );
		        setLayoutParams(lp);
		        //mImageView.setPadding(5, 5, 5, 5);
		        //vl.addView(mImageView);
		        /*mImageView = (ImageView) findViewById(R.id.pdf_image);
		        if (mImageView == null) {
		        	Log.i(TAG, "mImageView is null!!!!!!");
		        }
		        setPageBitmap(null);
		        updateImage();*/
		        
		        /*
		        navigationPanel = new ViewStub(PdfViewerActivity.this, R.layout.navigation_overlay);
		        final ImageButton previous = (ImageButton)navigationPanel.findViewById(R.id.navigation_previous);
		        previous.setBackgroundDrawable(null);
		        previous.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						prevPage();
					}
				});

		        final ImageButton next = (ImageButton)navigationPanel.findViewById(R.id.navigation_next);
		        next.setBackgroundDrawable(null);
		        previous.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						nextPage();
					}
				});
 
		        //stub.setLayoutParams(Layou)
		        vl.addView(navigationPanel);
		        
		        vl.setOnTouchListener(new OnTouchListener() {
					
					@Override
					public boolean onTouch(View v, MotionEvent event) {
						if (navigationPanel.getVisibility() != View.VISIBLE) {
							navigationPanel.startAnimation(AnimationUtils.loadAnimation(PdfViewerActivity.this,
									R.anim.slide_in));
							navigationPanel.setVisibility(View.VISIBLE);
						}

						return false;
					}
				});
				*/

		        //addNavButtons(vl);
			    
			//setLayoutParams(new LayoutParams(android.view.ViewGroup.LayoutParams.FILL_PARENT, android.view.ViewGroup.LayoutParams.FILL_PARENT, 100));
			setBackgroundColor(Color.LTGRAY);
			setHorizontalScrollBarEnabled(true);
			setHorizontalFadingEdgeEnabled(true);
			setVerticalScrollBarEnabled(true);
			setVerticalFadingEdgeEnabled(true);
			//addView(vl);
        }
        public void setPDFfile(PDFFile pdfFile)
        {
	        this.pdfFile=pdfFile;
        }
    	public void init ( int page, float mZoom )
		{
			startRenderThread( page, mZoom );
			
		}
		private synchronized void startRenderThread(final int page, final float zoom) {
    		if (backgroundThread != null)
    			return;
    		if (mPdfPage!=null)
    		{	
    			int newWidth = ( int ) ( mPdfPage.getWidth()*zoom );
    			int newHeight = ( int ) ( mPdfPage.getHeight()*zoom );
    			if (newWidth>2048||newHeight>2048)
    				return;
    		}
    		showText("reading page "+ page+", zoom:"+zoom);
            backgroundThread = new Thread(new Runnable() {
    			@Override
    			public void run() {
    			        	try
							{
								showPage(page, zoom);
							}
							catch ( Exception e )
							{
								
								e.printStackTrace();
							}
    		        backgroundThread = null;
    			}
    		});
            updateImageStatus();
            backgroundThread.start();
    	}


    	private void updateImageStatus() {
    		if (backgroundThread == null) {
    			return;
    		}
    		postDelayed(new Runnable() {
    			@Override
    			public void run() {
    				updateImageStatus();
    			}
    		}, 1000);
    	}        
    	@Override
    	protected void onDetachedFromWindow ()
    	{
    		if (mAttacher!=null)
    			mAttacher.cleanup();
    		mAttacher=null;
    		super.onDetachedFromWindow();
    	}

		private void showText(String text) {
        	Log.i(TAG, "ST='"+text+"'");
		}

        private void updateImage() {
        	uiHandler.post(new Runnable() {
				@Override
				public void run() {
		        	setImageBitmap(mBi);
				}
			});
		}

		private void setPageBitmap(Bitmap bi) {
			if (bi != null)
				mBi = bi;
		}
        
		@Override
		public void onMatrixChanged ( RectF arg0 )
		{
			float scale=mAttacher.getScale();
			if (Math.abs( scale-mOldScale )<0.05f)
				return;
			mZoom=STARTZOOM*scale;
			if (mZoom<STARTZOOM)
				mZoom=STARTZOOM;
			else
				startRenderThread(mPage, mZoom);
		}
		
		
	    private void showPage(int page, float zoom) throws Exception {
	        //long startTime = System.currentTimeMillis();
	        //long middleTime = startTime;
	    	try {
		        // free memory from previous page
		        setPageBitmap(null);
		        updateImage();
		        
		        // Only load the page if it's a different page (i.e. not just changing the zoom level) 
		        if (mPdfPage == null || mPdfPage.getPageNumber() != page) {
		        	mPdfPage = pdfFile.getPage(page, true);
		        }
		        //int num = mPdfPage.getPageNumber();
		        //int maxNum = mPdfFile.getNumPages();
		        float width = mPdfPage.getWidth();
		        float height = mPdfPage.getHeight();
		        //String pageInfo= new File(pdffilename).getName() + " - " + num +"/"+maxNum+ ": " + width + "x" + height;
		        //mGraphView.showText(pageInfo);
		        //Log.i(TAG, pageInfo);
		        RectF clip = null;
		        //middleTime = System.currentTimeMillis();
		        Bitmap bi = mPdfPage.getImage((int)(width*zoom), (int)(height*zoom), clip, true, true);
		        setPageBitmap(bi);
		        updateImage();

			} catch (Throwable e) {
				Log.e(TAG, e.getMessage(), e);
				showText("Exception: "+e.getMessage());
			}
	        //long stopTime = System.currentTimeMillis();
	        //mGraphView.pageParseMillis = middleTime-startTime;
	        //mGraphView.pageRenderMillis = stopTime-middleTime;
	    }
    }
    
    private List<Pair<String,Integer>> toc =new ArrayList< Pair<String,Integer> >();
    
    private void parsePDF(String filename, String password) throws PDFAuthenticationFailureException {
        //long startTime = System.currentTimeMillis();
    	try {
        	File f = new File(filename);
        	long len = f.length();
        	if (len == 0) {
        		mGraphView.showText("file '" + filename + "' not found");
        	}
        	else {
        		mGraphView.showText("file '" + filename + "' has " + len + " bytes");
    	    	openFile(f, password);
    	    	OutlineNode page = mPdfFile.getOutline();
    	    	parseOutline(page.children);
    	    	for (Pair<String,Integer> entry:toc)
    	    		Log.e( TAG, entry.first+" => "+entry.second );
        	}
    	}
        catch (PDFAuthenticationFailureException e) {
        	throw e; 
		} catch (Throwable e) {
			e.printStackTrace();
			mGraphView.showText("Exception: "+e.getMessage());
		}
        //long stopTime = System.currentTimeMillis();
        //mGraphView.fileMillis = stopTime-startTime;
	}

    private void parseOutline(List<DefaultMutableTreeNode> list) throws IOException
    {
    	for (DefaultMutableTreeNode child:list)
    	{	
    		OutlineNode outline=(OutlineNode)child;
            PDFDestination dst = ((GoToAction)outline.getAction()).getDestination();
            //if (child.children.isEmpty())
            	toc.add( Pair.create( outline.toString(),mPdfFile.getPageNumber( dst.getPage()) ) );
            //else
            	parseOutline( child.children );
            
    	}
    }
    /**
     * <p>Open a specific pdf file.  Creates a DocumentInfo from the file,
     * and opens that.</p>
     *
     * <p><b>Note:</b> Mapping the file locks the file until the PDFFile
     * is closed.</p>
     *
     * @param file the file to open
     * @throws IOException
     */
    public void openFile(File file, String password) throws IOException {
        // first open the file for random access
        RandomAccessFile raf = new RandomAccessFile(file, "r");

        // extract a file channel
        FileChannel channel = raf.getChannel();

        // now memory-map a byte-buffer
        ByteBuffer bb =
                ByteBuffer.NEW(channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size()));
        // create a PDFFile from the data
        if (password == null)
        	mPdfFile = new PDFFile(bb);
        else
        	mPdfFile = new PDFFile(bb, new PDFPassword(password));
        mGraphView.setPDFfile( mPdfFile );
        mGraphView.showText("Anzahl Seiten:" + mPdfFile.getNumPages());
    }
    
	private String storeUriContentToFile(Uri uri) {
    	String result = null;
    	try {
	    	if (mTmpFile == null) {
				File root = Environment.getExternalStorageDirectory();
				if (root == null)
					throw new Exception("external storage dir not found");
				mTmpFile = new File(root,"AndroidPdfViewer/AndroidPdfViewer_temp.pdf");
				mTmpFile.getParentFile().mkdirs();
	    		mTmpFile.delete();
	    	}
	    	else {
	    		mTmpFile.delete();
	    	}
	    	InputStream is = getContentResolver().openInputStream(uri);
	    	OutputStream os = new FileOutputStream(mTmpFile);
	    	byte[] buf = new byte[1024];
	    	int cnt = is.read(buf);
	    	while (cnt > 0) {
	    		os.write(buf, 0, cnt);
		    	cnt = is.read(buf);
	    	}
	    	os.close();
	    	is.close();
	    	result = mTmpFile.getCanonicalPath();
	    	mTmpFile.deleteOnExit();
    	}
    	catch (Exception e) {
    		Log.e(TAG, e.getMessage(), e);
		}
		return result;
	}

    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	if (mTmpFile != null) {
    		mTmpFile.delete();
    		mTmpFile = null;
    	}
    }
   
    public abstract int getPdfPasswordLayoutResource(); // R.layout.pdf_file_password
    public abstract int getPdfPageNumberResource(); // R.layout.dialog_pagenumber
    
    public abstract int getPdfPasswordEditField(); // R.id.etPassword
    public abstract int getPdfPasswordOkButton(); // R.id.btOK
    public abstract int getPdfPasswordExitButton(); // R.id.btExit
    public abstract int getPdfPageNumberEditField(); // R.id.pagenum_edit
}