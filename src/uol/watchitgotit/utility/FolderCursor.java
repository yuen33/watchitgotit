
package uol.watchitgotit.utility;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import android.app.Activity;
import android.content.ContentResolver;
import android.database.CharArrayBuffer;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;

public class FolderCursor implements Cursor {

	public static String COLUMN_ID= "_id";
	public static String COLUMN_NAME= "Name";
	public static String COLUMN_PATH= "Path";
	public static String COLUMN_DIMENSION= "Dimension";
	public static String COLUMN_DIMENSION_HUMAN= "Dimension_human";
	public static String COLUMN_EXTENSION= "Extension";
	public static String COLUMN_LASTMODIFIED= "LastModified";
	
	private final static IndexMap<String> columns= new IndexMap<String>();
	static {
		columns.put(COLUMN_ID);
		columns.put(COLUMN_NAME);
		columns.put(COLUMN_PATH);
		columns.put(COLUMN_DIMENSION);
		columns.put(COLUMN_DIMENSION_HUMAN);
		columns.put(COLUMN_EXTENSION);
		columns.put(COLUMN_LASTMODIFIED);
	}
	
	private File folder;
	private File[] files;
	private int selected;
	private Comparator<File> alphabeticComparator= new Comparator<File>() {
		public int compare(File f1, File f2) {
			return f1.getName().compareTo(f2.getName());
		}
	};
	
	private boolean isClosed= false;
	private boolean isDeactivated= false;
	
	private ArrayList<ContentObserver> contentObservers;
	private ArrayList<DataSetObserver> dataSetObservers;
	
	public FolderCursor(File folder, Activity caller){
		
		this.folder= folder;
		contentObservers= new ArrayList<ContentObserver>();
		dataSetObservers= new ArrayList<DataSetObserver>();
		requery();
	}
	
	private Object getField(int columnIndex){
		
		if (columnIndex==columns.getIndex(COLUMN_ID)){
			return selected;
		}
		else if (columnIndex==columns.getIndex(COLUMN_NAME)){
			return files[selected].getName();
		}
		else if (columnIndex==columns.getIndex(COLUMN_PATH)){
			return files[selected].getAbsolutePath();
		}
		else if (columnIndex==columns.getIndex(COLUMN_DIMENSION)){
			return files[selected].length();
		}
		else if (columnIndex==columns.getIndex(COLUMN_DIMENSION_HUMAN)){
			return ByteUtilities.getHumanReadableSize(files[selected].length());
		}
		else if (columnIndex==columns.getIndex(COLUMN_EXTENSION)){
			String name= files[selected].getName();
			return name.substring(name.lastIndexOf('.'));
		}
		else if (columnIndex==columns.getIndex(COLUMN_LASTMODIFIED)){
			return files[selected].lastModified();
		}
		return null;
	}
	
	public void close() {
		
		folder= null;
		files= null;
		isClosed= true;
		notifyObserversOnInvalidated();
	}

	public void copyStringToBuffer(int columnIndex, CharArrayBuffer buffer) {
		
		if (isNull(columnIndex)) return;
		char[] text = getString(columnIndex).toCharArray();
		if (buffer.data.length < text.length) buffer.data= text;
		else for (int i=0; i< text.length; i++)
			buffer.data[i]= text[i];
	}

	public void deactivate() { 
		
		files= null;
		isDeactivated= true;
		notifyObserversOnInvalidated();
	}
	
	public File getFile(int position){
		
		if (position<0 || position>=files.length) return null;
		else return files[position];		
	}

	public byte[] getBlob(int columnIndex) {
		
		Object field= getField(columnIndex);
		if (field instanceof byte[]) return (byte[])field;
		else return null;
	}

	public int getColumnCount() {
		return columns.size();
	}

	public int getColumnIndex(String columnName) {
		return columns.getIndex(columnName);
	}

	public int getColumnIndexOrThrow(String columnName)
			throws IllegalArgumentException {
		int index= columns.getIndex(columnName);
		if (index<0) throw new IllegalArgumentException();
		else return index;
	}

	public String getColumnName(int columnIndex) {
		return columns.getValue(columnIndex);
	}

	public String[] getColumnNames() {
		return columns.values().toArray(new String[]{});
	}

	public int getCount() {
		return files.length;
	}

	public double getDouble(int columnIndex) {
		
		Object field= getField(columnIndex);
		if (field instanceof Double) return (Double)field;
		else return Double.valueOf(field.toString());
	}

	public Bundle getExtras() {
		return Bundle.EMPTY;
	}

	public float getFloat(int columnIndex) {
		
		Object field= getField(columnIndex);
		if (field instanceof Float) return (Float)field;
		else return Float.valueOf(field.toString());
	}

	public int getInt(int columnIndex) {
		
		Object field= getField(columnIndex);
		if (field instanceof Integer) return (Integer)field;
		else return Integer.valueOf(field.toString());
	}

	public long getLong(int columnIndex) {
		
		Object field= getField(columnIndex);
		if (field instanceof Long) return (Long)field;
		else return Long.valueOf(field.toString());
	}

	public int getPosition() {
		return selected;
	}

	public short getShort(int columnIndex) {
		
		Object field= getField(columnIndex);
		if (field instanceof Short) return (Short)field;
		else return Short.valueOf(field.toString());
	}

	public String getString(int columnIndex) {
		
		Object field= getField(columnIndex);
		if (field instanceof String) return (String)field;
		else return String.valueOf(field);
	}

	public boolean getWantsAllOnMoveCalls() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isAfterLast() {
		return selected>=files.length;
	}

	public boolean isBeforeFirst() {
		return selected<0;
	}

	public boolean isClosed() {
		return isClosed;
	}
	
	public boolean isEmpty(){
		return files.length==0;
	}

	public boolean isFirst() {
		return selected==0;
	}

	public boolean isLast() {
		return selected==files.length-1;
	}

	public boolean isNull(int columnIndex) {
		return getField(columnIndex)==null;
	}

	public boolean move(int offset) {
		
		selected += offset;
		if (isBeforeFirst()) {
			moveToFirst();
			return false;
		}
		else if (isAfterLast()){
			moveToLast();
			return false;
		}
		return true;
	}

	public boolean moveToFirst() {
		
		selected= 0;
		return !isEmpty();
	}

	public boolean moveToLast() {
		
		if (isEmpty()){
			return false;
		}
		else {
			selected= files.length-1;
			return true;
		}
	}

	public boolean moveToNext() {
		
		if (isAfterLast()){
			return false;
		}
		else {
			selected++;
			return true;
		}
	}

	public boolean moveToPosition(int position) {
		
		if (-1 <= position && position <= files.length){
			selected= position;
			return true;
		}
		else return false;
	}

	public boolean moveToPrevious() {
		
		if (isBeforeFirst()){
			return false;
		}
		else {
			selected--;
			return true;
		}
	}

	public void registerContentObserver(ContentObserver observer) {
		
		if (!contentObservers.contains(observer))
			contentObservers.add(observer);
	}

	public void registerDataSetObserver(DataSetObserver observer) {
		
		if (!dataSetObservers.contains(observer))
			dataSetObservers.add(observer);
	}
	
	public void notifyObserversOnChange(){
		
		for (ContentObserver observer: contentObservers)
			observer.onChange(false);
		for (DataSetObserver observer: dataSetObservers)
			observer.onChanged();
	}
	
	public void notifyObserversOnInvalidated(){
		
		for (DataSetObserver observer: dataSetObservers)
			observer.onInvalidated();
	}

	public boolean requery() {
		
		if (!isClosed){
			files= folder.listFiles();
			Arrays.sort(files, alphabeticComparator);
			selected= -1;
			isDeactivated= false;
			return true;
		}
		else {
			return false;
		}
		
	}

	public Bundle respond(Bundle extras) {
		return Bundle.EMPTY;
	}

	public void setNotificationUri(ContentResolver cr, Uri uri) {
		// TODO Auto-generated method stub

	}

	public void unregisterContentObserver(ContentObserver observer) {
		contentObservers.remove(observer);
	}

	public void unregisterDataSetObserver(DataSetObserver observer) {
		dataSetObservers.remove(observer);
	}

	@Override
	public int getType(int arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

}
