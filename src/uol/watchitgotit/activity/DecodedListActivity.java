package uol.watchitgotit.activity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import uol.watchitgotit.engine.FileFromQrDecoder;
import uol.watchitgotit.engine.NotFileTypeException;
import uol.watchitgotit.manager.FileManager;
import uol.watchitgotit.manager.NotificationManager;
import uol.watchitgotit.manager.FileManager.FileOperationListener;
import uol.watchitgotit.utility.FolderCursor;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import uol.watchitgotit.R;


public class DecodedListActivity extends ListActivity {
	
	private Button buttonDecodeFromFile;
	private Button buttonDecodeFromCamera;
	
	private FileManager fileManager;
	private FolderCursor decodedCursor;
	private NotificationManager notificationManager;
	private FileFromQrDecoder qrDecoder;
	
	private FileOperationListener renameListener= new FileOperationListener() {
		public void onOperationCompleted(boolean abortedByUser, boolean operationSuccessful,
				File file) {
			if (operationSuccessful) decodedCursor.notifyObserversOnChange();							
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.decoded_list);
		registerForContextMenu(getListView());

		fileManager= new FileManager(this);
		notificationManager= new NotificationManager(this);
		qrDecoder= new FileFromQrDecoder(this);
		
		buttonDecodeFromFile= (Button) findViewById(R.id.buttonDecodeFromFile);
		buttonDecodeFromFile.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				fileManager.pickFileToOpen();
			}
		});
		
		buttonDecodeFromCamera= (Button) findViewById(R.id.buttonDecodeFromCamera);
		buttonDecodeFromCamera.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startActivityForResult(
					new Intent(DecodedListActivity.this, CameraDecodeActivity.class), CameraDecodeActivity.CAMERA_CODE);
			}
		});
	}
	
	@Override
	protected void onStart() {
		
		super.onStart();
		fillDecodedList();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
		
		super.onActivityResult(requestCode, resultCode, data);
					
		switch (requestCode) {
		
		case FileManager.OPEN_CODE:
			
			if (resultCode == RESULT_OK && data != null){
				
				decodeFromFile(fileManager.getPickedFile(data));
			}
			else {
				notificationManager.showWarningMessage(R.string.warning_open_aborted);	
			}
			break;
			
		case CameraDecodeActivity.CAMERA_CODE:
			
			if (resultCode == RESULT_OK){
				File decodedFile= (File) data.getSerializableExtra(CameraDecodeActivity.EXTRA_DECODED_FILE);
				onDecodedFile(decodedFile);
			}
			else {
//				notificationManager.showWarningMessage(R.string.warning_decode_aborted);
			}
			break;
		}					
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		getMenuInflater().inflate(R.menu.decoded_options, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		switch (item.getItemId()) {
		
		case R.id.decoded_delete_all:
			fileManager.deleteAll(fileManager.getDecodedFolder());
			decodedCursor.notifyObserversOnChange();
			return true;
		
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		
		getMenuInflater().inflate(R.menu.decoded_context, menu);		
		super.onCreateContextMenu(menu, v, menuInfo);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		File selectedFile= decodedCursor.getFile((int) info.id);
		
		switch(item.getItemId()) {
		
		case R.id.decoded_open:
			fileManager.open(selectedFile);
			return true;
        case R.id.decoded_delete:
            if (fileManager.delete(selectedFile)) decodedCursor.notifyObserversOnChange();
            return true;
		case R.id.decoded_rename:
			fileManager.rename(selectedFile, renameListener);
			return true;
		case R.id.decoded_share:
			fileManager.share(selectedFile);
			return true;
		case R.id.decoded_details:
			fileManager.showDetails(selectedFile);
			return true;
		}
		
		return super.onContextItemSelected(item);
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		
		File selectedFile= decodedCursor.getFile(position);
		fileManager.open(selectedFile);
	}
	
	private void fillDecodedList(){
		
		if (decodedCursor == null){
			decodedCursor= new FolderCursor(fileManager.getDecodedFolder(), this);
			startManagingCursor(decodedCursor);
		}
		
		ListAdapter listAdapter = new SimpleCursorAdapter(
	                this, R.layout.filelist_row, decodedCursor,
	                new String[] {FolderCursor.COLUMN_NAME, FolderCursor.COLUMN_DIMENSION_HUMAN},
	                new int[] {R.id.file_name, R.id.file_dimension});
        setListAdapter(listAdapter);
	}
	
	private void decodeFromFile(final File qrImageFile){
			
		notificationManager.startProgressDialog(R.string.progress_decode_from_file, false, null);
		
		Thread decodeThread= new Thread("DecodeThread"){
			public void run() {
				try {
//					Looper.prepare();
//					
//					LuminanceSource source= qrDecoder.buildLuminanceSourceFromImageFile(qrImageFile);
					File decodedFile= qrDecoder.decodeFromQR(qrImageFile);
					Log.i("TAG", "DecodedListActivity");
					
					Message msg= decodeFromFileHandler.obtainMessage();
					msg.obj= decodedFile;
					decodeFromFileHandler.sendMessage(msg);
					
					Looper.loop();
					
				} catch (Throwable t) {
					Message msg= decodeFromFileHandler.obtainMessage();
					msg.obj= t;
					decodeFromFileHandler.sendMessage(msg);
				}
			};
		};
		
		decodeThread.start();
	}
	
	private Handler decodeFromFileHandler= new Handler(){
		@Override
		public void handleMessage(android.os.Message msg) {
			
			if (msg.obj instanceof File){
				
				File decodedFile= (File) msg.obj;
				onDecodedFile(decodedFile);			
			}
			else if (msg.obj instanceof Throwable){
				
				Throwable t= (Throwable) msg.obj;
				
				if (t instanceof FileNotFoundException) {
					notificationManager.showErrorMessage(R.string.error_decode_notbitmap, null);
				} else if (t instanceof IOException) {
					notificationManager.showErrorMessage(R.string.error_write_failed, null);
				} else if (t instanceof NotFoundException) {
					notificationManager.showErrorMessage(R.string.error_decode_codenotfound, null);
				} else if (t instanceof ChecksumException) {
					notificationManager.showErrorMessage(R.string.error_decode_checksum, null);
				} else if (t instanceof FormatException) {
					notificationManager.showErrorMessage(R.string.error_decode_format, null);
				} else if (t instanceof NotFileTypeException) {
					notificationManager.showErrorMessage(R.string.error_decode_notfiletype, null,
							((NotFileTypeException)t).getQrContent());
				} else if (t instanceof OutOfMemoryError) {
					notificationManager.showErrorMessage(R.string.error_decode_outofmemory, null);
				}
			}
			
			notificationManager.stopProgressDialog();
		};
	};

	private void onDecodedFile(File decodedFile){
		
		notificationManager.showConfirmMessage(
				R.string.confirm_decode_done, decodedFile.getName());
		
		decodedCursor.notifyObserversOnChange();
	}
}
