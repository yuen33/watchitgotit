package uol.watchitgotit.activity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import uol.watchitgotit.engine.FileToQrEncoder;
import uol.watchitgotit.manager.FileManager;
import uol.watchitgotit.manager.NotificationManager;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;
import android.widget.TextView;

import uol.watchitgotit.R;

public final class EncodeActivity extends Activity {

	private final static String SAVED_IMAGE_PREFIX = "qr-";
	private final static String SAVED_IMAGE_EXTENSION = "png";
	private final static CompressFormat SAVED_IMAGE_COMPRESS_FORMAT = CompressFormat.PNG;// PNG
	private final static int SAVED_IMAGE_DIMENSION = 300;
	public static int FrameDuration = 300;
	private static int HalfFrameDuration = (int) (0.5*FrameDuration);

	private FileManager fileManager;
	private NotificationManager notificationManager;

	private File toBeEncodedFile;
	// TODO
	private AnimationDrawable animation;

	private View layout;
	private TextView filenameView;
	private TextView filesizeView;
	private ImageView imageView;

	private FileToQrEncoder qrCodeEncoder;

	private boolean firstLayout = true;

	/**
	 * This needs to be delayed until after the first layout so that the view
	 * dimensions will be available.
	 */
	private final OnGlobalLayoutListener layoutListener = new OnGlobalLayoutListener() {

		public void onGlobalLayout() {
			if (firstLayout) {

				filenameView.setText(toBeEncodedFile.getName());

				int width = layout.getWidth();
				int height = layout.getHeight();
				int smallerDimension = width < height ? width : height;

				// encodePhase.restart();
				startEncodingForCurrentPhase(R.string.progress_encode,
						smallerDimension);

				firstLayout = false;
			}
		}
	};

	private OnClickListener errorQuitListener = new OnClickListener() {
		public void onClick(DialogInterface dialog, int which) {
			finish();
		}
	};

	private void startEncodingForCurrentPhase(int progressDialogText,
			int qrDimension) {

		notificationManager.startProgressDialog(progressDialogText, true,
				cancelListener);
		// encodePhase.step();

		try {
			qrCodeEncoder.encodeToQR(toBeEncodedFile, qrDimension,
					encodeDoneHandler);
		} catch (IOException e) {
			notificationManager.showErrorMessage(R.string.error_open_failed,
					errorQuitListener);
		}

	}

	private final Handler encodeDoneHandler = new Handler() {

		@SuppressWarnings("unchecked")
		@Override
		public void handleMessage(Message message) {
			if(!MainActivity.textSendingfps.isEmpty()){
				FrameDuration= (int) ((float)1000/Float.parseFloat(MainActivity.textSendingfps));
				HalfFrameDuration = (int) (0.5*FrameDuration);
			}
			
			@SuppressWarnings("deprecation")
			Drawable startFrame = (BitmapDrawable)getResources().getDrawable(R.drawable.start_frame);

			animation = new AnimationDrawable();
			animation.addFrame(startFrame, 800);

			ImageView imageAnim = (ImageView) findViewById(R.id.image_view);
			imageAnim.setBackgroundDrawable(animation);
			imageAnim.post(new Starter());

			switch (message.what) {

			case R.id.encode_succeeded:

				notificationManager.stopProgressDialog();
				ArrayList<Bitmap> bitmapArray = new ArrayList<Bitmap>();
				bitmapArray = (ArrayList<Bitmap>) message.obj;
				System.out.println("get message.obj");

				for (int i = 0; i < bitmapArray.size(); i++) {
					@SuppressWarnings("deprecation")
					Drawable drawable = new BitmapDrawable(bitmapArray.get(i));
					animation.addFrame(drawable, HalfFrameDuration);
					animation.addFrame(drawable, HalfFrameDuration);
//					System.out.println("Adding Frame " + i);
				}
				
//				System.out.println("The number of frames: "+ animation.getNumberOfFrames());
				
//				animation.addFrame(endFrame, 300);
				
				animation.setOneShot(false);
				
				filesizeView.setText("File size: " + FileToQrEncoder.filesize + " Bytes");

				break;

			case R.id.encode_failed:
				// imageView.setImageResource(R.drawable.encode_failed);
				notificationManager.showErrorMessage(
						R.string.error_encode_failed, errorQuitListener);

				break;
			}// switch end

			// imageView.setImageBitmap(image);
			// startEncodingForCurrentPhase(R.string.progress_write,
			// SAVED_IMAGE_DIMENSION);

		}
	};

	private final OnCancelListener cancelListener = new OnCancelListener() {
		public void onCancel(DialogInterface dialog) {
			finish();
		}
	};

	@Override
	public void onCreate(Bundle icicle) {

		super.onCreate(icicle);
		setContentView(R.layout.encode);

		fileManager = new FileManager(this);
		notificationManager = new NotificationManager(this);

		filenameView = (TextView) findViewById(R.id.filename_view);
		filesizeView = (TextView) findViewById(R.id.fileSize);
		imageView = (ImageView) findViewById(R.id.image_view);

		qrCodeEncoder = new FileToQrEncoder(this);

		// encodePhase= new EncodePhase();
	}

	@Override
	protected void onStart() {
		super.onStart();

		toBeEncodedFile = new File(getIntent().getData().getPath());

		layout = findViewById(R.id.encode_view);
		layout.getViewTreeObserver().addOnGlobalLayoutListener(layoutListener);
	}

	// private void saveEncodedImage(Bitmap qrImage){
	//
	// File imageToSave= new File(
	// fileManager.getEncodedFolder(),
	// SAVED_IMAGE_PREFIX + toBeEncodedFile.getName() + "." +
	// SAVED_IMAGE_EXTENSION);
	//
	// try {
	// FileOutputStream out = new FileOutputStream(imageToSave);
	// qrImage.compress(SAVED_IMAGE_COMPRESS_FORMAT, 100, out);
	//
	// notificationManager.showConfirmMessage(R.string.confirm_write_done,
	// imageToSave.getName());
	//
	// } catch (FileNotFoundException e) {
	// notificationManager.showErrorMessage(R.string.error_write_failed, null);
	// }
	// }

	class Starter implements Runnable {
		public void run() {
			animation.start();
		}
	}
}
