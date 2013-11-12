
package uol.watchitgotit.activity;

import java.io.File;

import uol.watchitgotit.manager.FileManager;
import uol.watchitgotit.manager.NotificationManager;
import uol.watchitgotit.manager.FileManager.FileOperationListener;
import uol.watchitgotit.utility.FolderCursor;

import android.app.ListActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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

import uol.watchitgotit.R;

public class EncodedListActivity extends ListActivity {
	
	private Button buttonEncode;
	
	private FileManager fileManager;
	private FolderCursor encodedCursor;
	private NotificationManager notificationManager;
	
	private FileOperationListener renameListener= new FileOperationListener() {
		public void onOperationCompleted(boolean abortedByUser, boolean operationSuccessful,
				File file) {
			if (operationSuccessful) encodedCursor.notifyObserversOnChange();							
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.encoded_list);
		registerForContextMenu(getListView());

		fileManager= new FileManager(this);
		notificationManager= new NotificationManager(this);
		
		buttonEncode= (Button) findViewById(R.id.buttonEncode);
		buttonEncode.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				fileManager.pickFileToOpen();
			}
		});
	}
	
	@Override
	protected void onStart() {
		
		super.onStart();
		fillEncodedList();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		super.onActivityResult(requestCode, resultCode, data);
			
		switch (requestCode) {
		
		case FileManager.OPEN_CODE:
			
			if (resultCode == RESULT_OK && data != null) {
				
				File toBeEncodedFile= fileManager.getPickedFile(data);
				startActivity(
						new Intent(EncodedListActivity.this, EncodeActivity.class)
						.setData(Uri.fromFile(toBeEncodedFile)));
			}
			else {
				notificationManager.showWarningMessage(R.string.warning_open_aborted, false);
			}
			
			break;
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		getMenuInflater().inflate(R.menu.encoded_options, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		switch (item.getItemId()) {
		
		case R.id.encoded_delete_all:
			fileManager.deleteAll(fileManager.getEncodedFolder());
			encodedCursor.notifyObserversOnChange();
			return true;
		
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		
		getMenuInflater().inflate(R.menu.encoded_context, menu);
		super.onCreateContextMenu(menu, v, menuInfo);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		File selectedFile= encodedCursor.getFile((int) info.id);
		
		switch(item.getItemId()) {
		
		case R.id.encoded_open:
			fileManager.open(selectedFile);
			return true;
        case R.id.encoded_delete:
            if (fileManager.delete(selectedFile)) encodedCursor.notifyObserversOnChange();
            return true;
		case R.id.encoded_rename:
			fileManager.rename(selectedFile, renameListener);
			return true;
		case R.id.encoded_share:
			fileManager.share(selectedFile);
			return true;
		case R.id.encoded_details:
			fileManager.showDetails(selectedFile);
			return true;
		}
		
		return super.onContextItemSelected(item);
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		
		File selectedFile= encodedCursor.getFile(position);
		fileManager.open(selectedFile);
	}
	
	private void fillEncodedList(){
		
		if (encodedCursor == null){
			encodedCursor= new FolderCursor(fileManager.getEncodedFolder(), this);
			startManagingCursor(encodedCursor);
		}
		
		ListAdapter listAdapter = new SimpleCursorAdapter(
	                this, R.layout.filelist_row, encodedCursor,
	                new String[] {FolderCursor.COLUMN_NAME, FolderCursor.COLUMN_DIMENSION_HUMAN},
	                new int[] {R.id.file_name, R.id.file_dimension});
        setListAdapter(listAdapter);
	}
}
