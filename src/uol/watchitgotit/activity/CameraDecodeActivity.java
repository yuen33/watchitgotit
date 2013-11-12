package uol.watchitgotit.activity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;

import uol.watchitgotit.R;
import uol.watchitgotit.engine.FileFromQrDecoder;
import uol.watchitgotit.engine.NotFileTypeException;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ZoomControls;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

/**
 * This class is to do camera fast burst, 
 * and return captured preview pictures 
 * to @see uol.watchitgotit.engine.FileFromQrDecoder
 * 
 * @author Shanyue HUANG
 * */
public class CameraDecodeActivity extends Activity implements SurfaceHolder.Callback {

	public final static String EXTRA_DECODED_FILE= "DecodedFile";
	public final static int CAMERA_CODE = 2;

	/**
	 * @param every 800milliseconds do once smooth autoFocus
	 * */
	private final static int AUTOFOCUS_DELAY= 800;
//	private final static int PREVIEW_MAX_SIZE= 640;
	
	/**
	 * @param every 40milliseconds captures one frame
	 * */
	private int ExtractingPeriod= 0;//(int) (0.2*EncodeActivity.FrameDuration);//in milliseconds
//	private final static int ExtractingPeriod=50;
	
	/**
	 * @param preview resolution= 320*240
	 * */
	private int PreviewWidth=320;
	private int PreviewHeight=240;

	
	private int NumberOfBitmaps=0;
	private boolean AutoFocusLoops=true;
	
	private SurfaceView surfaceView;
	private SurfaceHolder surfaceHolder;
	private Camera camera;
	private Button CaptureButton;
	private ZoomControls zoomControls;
	
	private boolean previewRunning = false;

	private FileFromQrDecoder qrDecoder;
	
	private Handler handler;
	private boolean exiting= false;
	private boolean extracting= false;
	private int currentZoomLevel = 0, maxZoomLevel = 0;
	private Parameters cameraParameters;
	
	
//	private LuminanceSource source;
	
//	private NotificationManager notificationManager;
	
	private PowerManager.WakeLock wakeLock;
	
	Timer timer= new Timer();
//	ArrayList<Bitmap> bitmapArray = new ArrayList<Bitmap>();
	ArrayList<BinaryBitmap> binBitmapArray = new ArrayList<BinaryBitmap>();
	

	/**
	 * Close this camera fast bursting camera with captured pictures
	 * 
	 * @param caputured and processed pictures in BinaryBitmap Array List 
	 * */
	private void closeActivity(boolean abort, ArrayList<BinaryBitmap> binBitmapArray){
		exiting= true;
		handler.removeCallbacks(previewRunnable);
		stopAutofocus();
		camera.stopPreview();
		previewRunning = false;
//		camera.release();
		
		if (!abort) {
			try {
				qrDecoder.decodeFromQR(binBitmapArray);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (NotFileTypeException e) {
				e.printStackTrace();
			}
		}
//		else{setResult(RESULT_CANCELED);}
//		setResult(RESULT_OK);
		finish();
	}
	
	/**
	 * Stop smooth autoFocus thread
	 * */
	private void stopAutofocus(){
		handler.removeCallbacks(autoFocusRunnable);
		if (camera != null) {
			camera.cancelAutoFocus();
			AutoFocusLoops=false;
		}
		
	}
	
	/**
	 * Tap screen:
	 * @param AutoFocusLoops: boolean
	 * If true, stop smooth autoFocus thread;
	 * If false, do once quick autoFocus;
	 * */
	private View.OnClickListener tapScreenListener= new View.OnClickListener() {
		public void onClick(View v) {
//			closeActivity(true, null);
			if (AutoFocusLoops){
				stopAutofocus();
			}else{
				camera.autoFocus(null);
			}
		}
	};
	
	/**
	 * Click quit key on phone:
	 * Quit fast burst camera and return to last activity.
	 * */
	private OnClickListener errorQuitListener = new OnClickListener() {
		public void onClick(DialogInterface dialog, int which) {
			closeActivity(true, null);
		}
	};
	
	/**
	 * Before trigger the capturing, do camera setOneShotPreviewCallback();
	 * */
	private Runnable previewRunnable= new Runnable() {
		public void run() {
			if (!exiting) camera.setOneShotPreviewCallback(previewCallback);
		}
	};
	
	/**
	 * Do smooth autoFocus before capturing
	 * */
	private Runnable autoFocusRunnable= new Runnable() {
		public void run() {
			if (!exiting) camera.autoFocus(autoFocusCallback);			
		}
	};
	
	private Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {
		public void onAutoFocus(boolean success, final Camera camera) {
			handler.postDelayed(autoFocusRunnable, AUTOFOCUS_DELAY);
		}
	};

	/**
	 * Create the UI elements: buttons, zoom in/out bar, preview window
	 * */
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		
		if(!MainActivity.textCapturingfps.isEmpty()){
			ExtractingPeriod= (int) ((float)1000/Float.parseFloat(MainActivity.textCapturingfps));
			System.out.println("ExtractingPeriod"+ExtractingPeriod);
		}
//		if(!MainActivity.textPreviewWidth.isEmpty()){
//			PreviewWidth= Integer.parseInt(MainActivity.textPreviewWidth);
//		}
//		if(!MainActivity.textPreviewHeight.isEmpty()){
//			PreviewWidth= Integer.parseInt(MainActivity.textPreviewWidth);
//		}

		setContentView(R.layout.camera);
		surfaceView = (SurfaceView) findViewById(R.id.surface_camera);
		
		/**
		 * Do Zoom in/out control for phones that support zoom function;
		 * */
		zoomControls=(ZoomControls)findViewById(R.id.zoomControls);
		zoomControls.setOnZoomInClickListener(new View.OnClickListener(){
			//@Override
	        public void onClick(View v){
	        if(currentZoomLevel*6 < maxZoomLevel-6){
	            currentZoomLevel++;
	            cameraParameters.setZoom(currentZoomLevel*6);
	            camera.setParameters(cameraParameters);
	        }
	        else{
	        	cameraParameters.setZoom(maxZoomLevel);
	            camera.setParameters(cameraParameters);
	        }
	}
	});

	    zoomControls.setOnZoomOutClickListener(new View.OnClickListener(){
	    	//@Override
	    	public void onClick(View v){
	        if(currentZoomLevel*6 > 0){
	            currentZoomLevel--;
	            cameraParameters.setZoom(currentZoomLevel*6);
	            camera.setParameters(cameraParameters);
	        }else{
	        	cameraParameters.setZoom(0);
	            camera.setParameters(cameraParameters);
	        }
	}
	});
	         
		surfaceView.setOnClickListener(tapScreenListener);
		surfaceHolder = surfaceView.getHolder();
		surfaceHolder.addCallback(this);
		surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		
		/**
		 * Create the capture/stop button;
		 * */
		CaptureButton = (Button) findViewById(R.id.button_capture);
		CaptureButton.setOnClickListener(new View.OnClickListener() {
			//@Override
			public void onClick(View v) {
				if (extracting) {
					extracting=false;
					CaptureButton.setText("Start Capturing");
					timer.cancel();
					System.out.println("binBitmapArray length=" +binBitmapArray.size());
//					System.out.println("NumberOfBitmaps=" +NumberOfBitmaps);//to check whether this two values are the same
					
					//TODO pass the array list
					closeActivity(false, binBitmapArray);
//					closeActivity(true, null);
					
				} 
				
				else {//TODO manual to capture the pictures
					extracting=true;
					stopAutofocus();
					CaptureButton.setText("STOP!");
					
//					setTimerTask(BinaryBitmap binBitmap);
				}
			}
		});

		qrDecoder= new FileFromQrDecoder(this);
		handler= new Handler();
		
//		notificationManager= new NotificationManager(this);
	}
	
	@Override
	protected void onResume() {
		
		super.onResume();
		
		PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(
				PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
				CameraDecodeActivity.class.getName());
		wakeLock.acquire();
	}
	
	@Override
	protected void onPause() {
		
		if (!exiting) closeActivity(true, null);
		wakeLock.release();
		
		super.onPause();
	}

	public void surfaceCreated(SurfaceHolder holder) {

		camera = Camera.open();
		camera.setDisplayOrientation(90);
		if (camera==null){
//			notificationManager.showErrorMessage(R.string.error_camera_preview, errorQuitListener);
		}
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

		if (previewRunning) camera.stopPreview();

		cameraParameters = camera.getParameters();
//		Size previewSize= getBestPreviewSize(cameraParameters);
		cameraParameters.setPreviewSize(PreviewWidth, PreviewHeight);//or 800*480?
//		cameraParameters.setPreviewSize(previewSize.width, previewSize.height);

		currentZoomLevel=cameraParameters.getZoom();
//		cameraParameters.setZoom(currentZoomLevel*10);

        if(cameraParameters.isZoomSupported()){    
        maxZoomLevel = cameraParameters.getMaxZoom();
        zoomControls.setIsZoomInEnabled(true);
        zoomControls.setIsZoomOutEnabled(true);    
        }
		
		camera.setParameters(cameraParameters);

		try {
			camera.setPreviewDisplay(holder);
		} catch (IOException e) {
//			notificationManager.showErrorMessage(R.string.error_camera_preview, errorQuitListener);
		}

		camera.startPreview();
		previewRunning = true;
		
		handler.post(previewRunnable);
		
		String focusMode = camera.getParameters().getFocusMode();
		if (focusMode != Camera.Parameters.FOCUS_MODE_FIXED && 
			focusMode != Camera.Parameters.FOCUS_MODE_INFINITY)
			handler.post(autoFocusRunnable);
	}


	public void surfaceDestroyed(SurfaceHolder holder) {

		camera.stopPreview();
		previewRunning = false;
		timer.cancel();
		camera.release();
	}
	
//	 private void setTimerTask()  
//	    {  
//	        timer.schedule(new TimerTask()  
//	        {  
//	            @Override  
//	            public void run()  
//	            {  
//	            	camera.setOneShotPreviewCallback(previewCallback);
//	            }  
//	  
//	        }, 0, ExtractingPeriod); //delay, period; in milliseconds 
//	    }
	 
		private Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
			
			public void onPreviewFrame(byte[] data, Camera camera) {
				LuminanceSource source= qrDecoder.buildLuminanceSourceFromCameraPreview(data, camera.getParameters());
				
				BinaryBitmap binBitmap = new BinaryBitmap(new HybridBinarizer(source));
				if(!extracting){
					try {
						Result result = new MultiFormatReader().decode(binBitmap);
						
						if(result.toString().startsWith("START")){
							stopAutofocus();
							CaptureButton.setText("STOP!");
							Log.i("Extrating", "Begin!");
							
							extracting=true;
							System.out.println(result.toString());	
						}
						
					} catch (NotFoundException e) {
						
						// Auto-generated catch block
						e.printStackTrace();
					}//try...catch
				}else{
					binBitmapArray.add(binBitmap);
					NumberOfBitmaps++;
					Log.i("Extrating", NumberOfBitmaps +" frames added.");
				}
				handler.postDelayed(previewRunnable, ExtractingPeriod);
				
			}
		};
}
