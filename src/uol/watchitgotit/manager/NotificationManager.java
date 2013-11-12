package uol.watchitgotit.manager;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.text.Html;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import uol.watchitgotit.R;


public class NotificationManager {

	private Activity caller;
	private ProgressDialog progressDialog;
	
	public NotificationManager(Activity caller){
		this.caller= caller;
	}
	
	private CharSequence formatText(String text, Object... textFormatArgs){
		
		for (Object arg: textFormatArgs)
			if (arg instanceof String) arg= TextUtils.htmlEncode((String) arg);
		
		return Html.fromHtml(String.format(text, textFormatArgs));
	}
	
	private void showToast(String message, Object... messageFormatArgs){
		
		stopProgressDialog();
		Toast.makeText(caller, formatText(message, messageFormatArgs), 
				Toast.LENGTH_LONG).show();
	}
	
	private void showDialog(OnClickListener quitListener, String title, String message, Object... messageFormatArgs){
		
		stopProgressDialog();
		
		AlertDialog.Builder alertBuilder= new AlertDialog.Builder(caller);
		alertBuilder.setTitle(formatText(title));
		alertBuilder.setMessage(formatText(message, messageFormatArgs));
		alertBuilder.setPositiveButton(R.string.button_ok, quitListener);
		alertBuilder.show();
	}
	
	public void showErrorMessage(int messageID, OnClickListener quitListener, Object...formatArgs){
		showDialog(quitListener, caller.getString(R.string.error_title), caller.getString(messageID), formatArgs);
	}
	
	public void showWarningMessage(int messageID, Object...formatArgs){
		showToast(caller.getString(messageID), formatArgs);
	}
	
	public void showConfirmMessage(int messageID, Object...formatArgs){
		showConfirmMessage(caller.getString(messageID), formatArgs);
	}
	
	public void showConfirmMessage(String message, Object...formatArgs){
		showToast(message, formatArgs);
	}
	
	public void showInfoMessage(int titleID, int messageID, Object...messageFormatArgs){
		showDialog(null, caller.getString(titleID), caller.getString(messageID), messageFormatArgs);
	}
	
	public void showInputRequest(InputRequest request){
		
		AlertDialog.Builder alertBuilder= new AlertDialog.Builder(caller);
		alertBuilder.setTitle(request.getTitle());
		alertBuilder.setView(request.getInput());
		alertBuilder.setPositiveButton(R.string.button_ok, request.getOkListener());
		alertBuilder.setNegativeButton(R.string.button_cancel, request.getCancelListener());
		alertBuilder.show();
	}
	
	public void startProgressDialog(int messageID, boolean cancelable, OnCancelListener cancelListener){
		
		stopProgressDialog();
		
		progressDialog = ProgressDialog.show(caller,
				null, caller.getString(messageID),
				true, cancelable, cancelListener);
	}
	
	public void stopProgressDialog(){
		
		if (progressDialog != null) {
			progressDialog.dismiss();
			progressDialog = null;
		}
	}
	
	public static class InputRequest{

		protected CharSequence title;
		protected EditText input;
		protected OnClickListener okListener;
		protected OnClickListener cancelListener;
		
		public InputRequest(CharSequence title, EditText input,
				OnClickListener okListener, OnClickListener cancelListener) {
			super();
			this.title = title;
			this.input = input;
			this.okListener = okListener;
			this.cancelListener = cancelListener;
		}
		
		public InputRequest(CharSequence title, EditText input){
			this(title, input, null, null);
		}

		public CharSequence getTitle() {
			return title;
		}

		public EditText getInput() {
			return input;
		}

		public OnClickListener getOkListener() {
			return okListener;
		}

		public OnClickListener getCancelListener() {
			return cancelListener;
		}
	}
	
}
