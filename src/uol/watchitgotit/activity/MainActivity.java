package uol.watchitgotit.activity;

import uol.watchitgotit.manager.NotificationManager;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import uol.watchitgotit.R;

public class MainActivity extends Activity {
	
	private Button buttonDecode;
	private Button buttonEncode;
	public static String textSendingfps;
	public static String textCapturingfps;
	public static String TextBytesPerFrame;
//	public static String textPreviewWidth;
//	public static String textPreviewHeight;
	
	private NotificationManager notificationManager;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		buttonDecode= (Button) findViewById(R.id.buttonDecode);
		buttonDecode.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				final EditText text_capturingfps = (EditText) findViewById(R.id.editText2);
				textCapturingfps = text_capturingfps.getText().toString();
				System.out.println(textCapturingfps);				
				
//				final EditText preview_width = (EditText) findViewById(R.id.preview_width);
//				textPreviewWidth = preview_width.getText().toString();
//				System.out.println(textPreviewWidth);
//				
//				final EditText preview_height = (EditText) findViewById(R.id.preview_height);
//				textPreviewHeight = preview_height.getText().toString();
//				System.out.println(textPreviewHeight);
				
				startActivity(new Intent(MainActivity.this, DecodedListActivity.class));
			}
		});
		
		buttonEncode= (Button) findViewById(R.id.buttonEncode);
		buttonEncode.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				final EditText text_sendingfps = (EditText) findViewById(R.id.sendingfps);
				textSendingfps = text_sendingfps.getText().toString();
				System.out.println(textSendingfps);
				
				final EditText bytes_per_frame = (EditText) findViewById(R.id.bytes_per_frame);
				TextBytesPerFrame = bytes_per_frame.getText().toString();
				System.out.println(TextBytesPerFrame);
				
				startActivity(new Intent(MainActivity.this, EncodedListActivity.class));
			}
		});
		
		notificationManager= new NotificationManager(this);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		getMenuInflater().inflate(R.menu.main_options, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		switch (item.getItemId()) {
		
		case R.id.main_about:
			
			notificationManager.showInfoMessage(
					R.string.info_about_title, R.string.info_about,
					getString(R.string.app_name),
					getString(R.string.app_version),
					getString(R.string.app_license),
					getString(R.string.author),
					getString(R.string.email));
			return true;
		
		}
		
		return super.onOptionsItemSelected(item);
	}
}