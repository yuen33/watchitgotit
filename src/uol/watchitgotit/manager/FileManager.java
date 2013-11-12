package uol.watchitgotit.manager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import uol.watchitgotit.manager.NotificationManager.InputRequest;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.text.format.DateFormat;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.EditText;

import uol.watchitgotit.R;



public class FileManager {

	public final static int OPEN_CODE = 0;
	public final static int SAVE_CODE = 1;
	
	private Activity caller;
	private NotificationManager notificationManager;
	
	public FileManager(Activity activity){
		
		this.caller= activity;
		notificationManager= new NotificationManager(caller);
	}
	
	public String getBaseName(File file){
		
		String fileName= file.getName();
		int pointIndex= fileName.lastIndexOf('.');
		if (pointIndex < 0) return fileName;
		else return fileName.substring(0, pointIndex);
	}
	
	public String getExtension(File file){
		
		String fileName= file.getName();
		int pointIndex= fileName.lastIndexOf('.');
		if (pointIndex < 0) return "";
		else return fileName.substring(pointIndex+1);
	}
	
	public String getMimeType(File file){
		return MimeTypeMap.getSingleton().getMimeTypeFromExtension(getExtension(file));
	}
	
	public void showDetails(File file){
		
		String mime= getMimeType(file);
		if (mime==null) mime= caller.getString(R.string.info_file_mimeunknown);
		
		notificationManager.showInfoMessage(
				R.string.info_file_title, R.string.info_file,
				file.getName(), file.getParent(), file.length()+" bytes", mime, 
				DateFormat.format("MMM dd, yyyy kk:mm", file.lastModified()));
	}
	
	public void open(File file){
		
		Intent intent= new Intent(Intent.ACTION_VIEW);
		Uri uri= Uri.fromFile(file);
		String mime= getMimeType(file);
		if (mime==null) mime="*/*";
		
		intent.setDataAndType(uri, mime);
		
		try { caller.startActivity(intent); }
		catch (ActivityNotFoundException e){
			notificationManager.showWarningMessage(R.string.warning_view_activity_not_found);
		}
	}
	
	public void share(File file){
		
		Intent intent= new Intent(Intent.ACTION_SEND);
		String mime= getMimeType(file);
		if (mime==null) mime="*/*";
		
		intent.setType(mime);
		
		if (mime.equals("text/plain"))  {
			try {
				String text = new String(read(file));
				intent.putExtra(android.content.Intent.EXTRA_TEXT, text);
			} catch (IOException e) {
				notificationManager.showErrorMessage(R.string.error_open_failed, null);
				return;
			}
		} else {
			Uri uri= Uri.fromFile(file);
			intent.putExtra(Intent.EXTRA_STREAM, uri);
		}
		
		try { caller.startActivity(Intent.createChooser(intent, caller.getText(R.string.menu_share_chooser))); }
		catch (ActivityNotFoundException e){
			notificationManager.showWarningMessage(R.string.warning_view_activity_not_found);
		}
	}
	
	public boolean delete(File file){
		
		if (file.delete()) {
			notificationManager.showConfirmMessage(
					caller.getResources().getQuantityString(R.plurals.confirm_delete_done, 1));
			return true;
		}
		else {
			notificationManager.showErrorMessage(R.string.error_delete_failed, null, file.getName());
			return false;
		}
	}
	
	public boolean deleteAll(File folder){
		
		File[] files= folder.listFiles();
		for (File file: files){
			if (!file.delete()){
				notificationManager.showErrorMessage(R.string.error_delete_failed, null, file.getName());
				return false;
			}
		}
		
		notificationManager.showConfirmMessage(
				caller.getResources().getQuantityString(R.plurals.confirm_delete_done, files.length, files.length));
		return true;
	}
	
	public void rename(File file, FileOperationListener renameListener){
		
		RenameFileRequest renameRequest= new RenameFileRequest(file, caller, renameListener);
		notificationManager.showInputRequest(renameRequest);
	}
	
	private class RenameFileRequest extends InputRequest{
		
		private File file;
		private FileOperationListener renameListener;

		public RenameFileRequest(File fileToRename, Activity caller, FileOperationListener fileRenameListener) {
			
			super(
				caller.getText(R.string.menu_rename_view),
				new EditText(caller));
			
			this.file= fileToRename;
			this.renameListener= fileRenameListener;
			
			input.setText(fileToRename.getName());
			
			okListener= new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					File dest= new File(file.getParent(), input.getText().toString());
					if (file.renameTo(dest)) {
						notificationManager.showConfirmMessage(R.string.confirm_rename_done, dest.getName());
						if (renameListener != null) renameListener.onOperationCompleted(false, true, file);
					}
					else {
						notificationManager.showErrorMessage(R.string.error_rename_failed, null, file.getName());
						if (renameListener != null) renameListener.onOperationCompleted(false, false, file);
					}
				}
			};
			
			cancelListener= new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					notificationManager.showWarningMessage(R.string.warning_rename_aborted);
					if (renameListener != null) renameListener.onOperationCompleted(true, false, file);
				}
			};
			
			
		}
		
	}
	
	public interface FileOperationListener{
		public void onOperationCompleted(boolean abortedByUser, boolean operationSuccessful, File file);
	}
	
	public File getApplicationFolder(){
		
		File folder= new File(caller.getString(R.string.app_folder));
		if (!folder.exists()) folder.mkdir();
		return folder;
	}
	
	public File getDecodedFolder(){
		
		getApplicationFolder();
		File folder= new File(caller.getString(R.string.decoded_folder));
		if (!folder.exists()) folder.mkdir();
		return folder;
	}
	
	public File getEncodedFolder(){
		
		getApplicationFolder();
		File folder= new File(caller.getString(R.string.encoded_folder));
		if (!folder.exists()) folder.mkdir();
		return folder;
	}
	
	public byte[] read(File file) throws IOException{
		
		if (file.length() > Integer.MAX_VALUE) 
			throw new IOException("File "+ file.getName() +" is too big."); 
		byte[] bytes = new byte[(int) file.length()];
		
		FileInputStream in= new FileInputStream(file);
				
		// Read in the bytes 
		int offset = 0; 
		int numRead = 0; 
		while (offset < bytes.length &&
				(numRead=in.read(bytes, offset, bytes.length-offset)) >= 0){
			offset += numRead;
		} 
		
		// Ensure all the bytes have been read in 
		if (offset < bytes.length)
			throw new IOException("Could not completely read file "+file.getName()); 
		
		in.close();
		
		return bytes;
	}
	
	public void writeToFile(File file, byte[] body) throws IOException{
		Log.i("FileManager", "writeToFile(File file,byte[] body)");
		
		FileOutputStream outputStream= new FileOutputStream(file);
		outputStream.write(body);
		outputStream.close();
	}
	
	public File getPickedFile(Intent data){
		
		String fileUri = data.getDataString();
		
		if (fileUri != null) {
			
			Uri uri= Uri.parse(fileUri);
			String fileName= uri.getPath();
	
			if (fileName != null) return new File(fileName);
		}
		return null;
	}

	public void pickFileToOpen(){
		sendRequest(
				OPEN_CODE,
				caller.getString(R.string.file_manager_open_title),
				caller.getString(R.string.file_manager_open_button));
	}

	public void pickFileToSave(){
		sendRequest(
				SAVE_CODE,
				caller.getString(R.string.file_manager_save_title),
				caller.getString(R.string.file_manager_save_button));
	}

	private void sendRequest(int requestCode, String title, String buttonText) {
		
		Intent intent = new Intent(caller.getString(R.string.file_manager_intent_action));
		intent.putExtra(caller.getString(R.string.file_manager_intent_extra_title), title);
		intent.putExtra(caller.getString(R.string.file_manager_intent_extra_button), buttonText);
		
		try {
			caller.startActivityForResult(intent, requestCode);
			
		} catch (ActivityNotFoundException e) {
			notificationManager.showErrorMessage(R.string.error_file_manager_not_found, null);
		}
	}
}
