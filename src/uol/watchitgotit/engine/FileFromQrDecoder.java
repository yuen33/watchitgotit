package uol.watchitgotit.engine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Vector;

import uol.watchitgotit.manager.FileManager;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.PixelFormat;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.media.MediaMetadataRetriever;
import android.os.Environment;
import android.util.Log;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
import com.google.zxing.common.HybridBinarizer;

public class FileFromQrDecoder {

	private FileManager fileManager;
	private static int numberOfExtractedFrames=90;//TODO mp4 getTime()/extracting duration
	private int numberOfOriginalFrames=0;//for debuging
	private int decodingPhase = 0;
	private int bufferCapacity = 1000000;
	
	/**For decoding Fast Burst Camera Bitmap Array
	 * @throws IOException 
	 * @throws NotFileTypeException */
	public File decodeFromQR(ArrayList<BinaryBitmap> binBitmapArray) throws IOException, NotFileTypeException{
		
		int i = 0;
		/**
		 * To get the decoding RunTime
		 * */
		long decodingRunTime=0;
		long currentTime= System.currentTimeMillis();
		
		
//		/**
//		 * For get the changing duration of two different adjacent frames
//		 * */
//		long[] changingDurationArray= new long[binBitmapArray.size()];
//		for(int j=0; j<changingDurationArray.length; j++){
//			changingDurationArray[j]=1111;
//		}
//		long changingDuration=0;
//		long currentTime=0;// System.currentTimeMillis();

		ByteBuffer bytebuffer = ByteBuffer.allocate(bufferCapacity);
		
		Result result = null;
		Result lastResult =null;
		String resultText;

		while (decodingPhase<2 && i < binBitmapArray.size()){
			Log.i("Frame Extracting loop: ", "i= " + i);
			BinaryBitmap binBitmap= binBitmapArray.get(i);

			try {
				result = new MultiFormatReader().decode(binBitmap);
				
				String resultString= result.toString();
				System.out.println("\n result.toString(): >>"+resultString+"<<");
				
				if (!resultString.matches("[0-9]{8}")){
					
					if (!resultString.startsWith("START")){
						byte[] resultBytes=getRawBytes(result);
						resultText= new String(resultBytes, FileToQrEncoder.CHAR_ENCODING);
						Log.i("Done: ", "resultText");
//						System.out.println("resultText= >>"+ resultText+"<<");
						
						if (decodingPhase==0 && resultText.startsWith(FileToQrEncoder.HEADER_FILE_PREFIX)){
							decodingPhase=1;
							bytebuffer.put(resultBytes);
							
//							changingDuration= System.currentTimeMillis();//TODO commented
							
							numberOfOriginalFrames++;
							System.out.println("Frame " + i + "is added.");
							/**
							 * lastResult=result
							 * */
							try {
								lastResult = new MultiFormatReader().decode(binBitmap);
							} catch (NotFoundException e) {
								e.printStackTrace();
							}//try..catch
						}//if decodingPhase==0...
						else if (decodingPhase==1 && !Arrays.equals(getRawBytes(lastResult), resultBytes) ){
							bytebuffer.put(resultBytes);
							numberOfOriginalFrames++;
							System.out.println("Frame " + i + "is added.");
							
//							//TODO commented; for measure the changing duration
//							currentTime= System.currentTimeMillis();
//							changingDuration=currentTime-changingDuration;
//							changingDurationArray[i]=changingDuration;
//							changingDuration=currentTime;
							
							/**
							 * lastResult=result
							 * */
							try {
								lastResult = new MultiFormatReader().decode(binBitmap);
							} catch (NotFoundException e) {
								e.printStackTrace();
							}//try..catch
						}//else if not equal
						
					}// if it is not START
					else if(decodingPhase==1 && resultString.startsWith("START")){
						decodingPhase=2;
					}
					
				}//if !=8-digit numbers end
				

			} catch (NotFoundException e) {
				e.printStackTrace();
			}//try..catch end
			
			
			i++;
			
		}// while
		
		Log.i("TAG", "i= " + i + "; decodingPhase= " + decodingPhase + "; Detected " + numberOfOriginalFrames + " Frames \n");
		decodingPhase=0;//reset decodingPhase
		
//		//TODO be commented;
//		System.out.println("changingDurationArray= ");
//		for(int j=0; j<changingDurationArray.length; j++){
//			System.out.print(changingDurationArray[j]+" ");
//		}
		
		bytebuffer.flip();//use current position as limit, then current position move to 0.
		byte[] rawBytes = new byte[bytebuffer.limit()];
		bytebuffer.get(rawBytes);
		
		/**
		 * Print out the rawBytes for debug
		 * */
		System.out.println("\n rawBytes.length= ");
//		for(int j=0; j < rawBytes.length; j++){
//			System.out.print(" " + rawBytes[j]);
//		}
		System.out.println(rawBytes.length);
		

		FileInfo decodedInfo = decodeFile(rawBytes);
//		Log.i("decodeFromQR()", "decodedInfo got");
		
		File decodedFile = new File(fileManager.getDecodedFolder()
				.getAbsolutePath(), decodedInfo.getFileName());

		fileManager.writeToFile(decodedFile, decodedInfo.getBody());
		decodingRunTime=System.currentTimeMillis();
		decodingRunTime= decodingRunTime-currentTime;
		Log.i("decodeFromQR()", "decodingRunTime= "+ decodingRunTime);
		
		Log.i("decodeFromQR()", "decodedFile " +decodedFile.getAbsolutePath());
		return decodedFile;
		
	}

	public FileFromQrDecoder(Activity caller) {

		fileManager = new FileManager(caller);
	}

//	 public File decodeFromQR(Bitmap qrBitmap) throws NotFoundException,
//	 ChecksumException, FormatException, IOException,
//	 NotFileTypeException {
//	 return decodeFromQR(buildLuminanceSourceFromBitmap(qrBitmap));
//	 }
//
//	 public File decodeFromQR(File qrImageFile) throws NotFoundException,
//	 ChecksumException, FormatException, IOException, NotFileTypeException{
//	
//	 return decodeFromQR(buildLuminanceSourceFromImageFile(qrImageFile));
//	 }

	public File decodeFromQR(File file) throws NotFileTypeException, IOException{
		Log.i("decodeFromQR()", "FileFromQrDecoder.decodeFromQR(File file)");
		
		int i = 0;// the frame extracting parameter
//		ArrayList<Byte> rawBytesList = new ArrayList<Byte>();// all frame
//																// contents in a
//																// dynamic size
//																// byte array
		ByteBuffer bytebuffer = ByteBuffer.allocate(bufferCapacity);
		
		
		Result result = null;
		// byte[] resultBytes; //current result to bytes
		Result lastResult =null;
		String resultText;
//		Reader reader = new QRCodeReader();


		while (decodingPhase<2 && i < numberOfExtractedFrames){
			Log.i("Frame Extracting loop: ", "i= " + i);

			Bitmap bitmapFrame = createVideoThumbnail(file.getAbsolutePath(), i);

			LuminanceSource lumSource;
			// to convert the image into a grey scaled array
			lumSource = new RGBLuminanceSource(bitmapFrame);
			// to covert the grey scaled image into monochrome image
			BinaryBitmap binBitmap = new BinaryBitmap(new HybridBinarizer(
					lumSource));
			
			/**
			 * For check the extracted frames
			 */
			// File file = new
			// File(Environment.getExternalStorageDirectory(),System.currentTimeMillis()+".jpg");
			File image = new File(Environment.getExternalStorageDirectory(),
					i + ".jpg");
			FileOutputStream outStream = new FileOutputStream(image);
			bitmapFrame.compress(CompressFormat.JPEG, 100, outStream);
			outStream.close();

//			result = reader.decode(binBitmap);
			try {
				result = new MultiFormatReader().decode(binBitmap);

				String resultString= result.toString();
				System.out.println("\n result.toString(): >>"+resultString+"<<");
				
				if (!resultString.matches("[0-9]{8}")){
				
				if (!result.toString().startsWith("START")){
					byte[] resultBytes=getRawBytes(result);
					resultText= new String(resultBytes, FileToQrEncoder.CHAR_ENCODING);
					Log.i("Done: ", "resultText");
//					System.out.println("resultText= >>"+ resultText+"<<");
					
					if (decodingPhase==0 && resultText.startsWith(FileToQrEncoder.HEADER_FILE_PREFIX)){
						decodingPhase=1;
						bytebuffer.put(resultBytes);
						numberOfOriginalFrames++;
						System.out.println("Frame " + i + "is added.");
						/**
						 * lastResult=result
						 * */
						try {
							lastResult = new MultiFormatReader().decode(binBitmap);
						} catch (NotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}//try..catch
					}//if decodingPhase==0...
					else if (decodingPhase==1 && !Arrays.equals(getRawBytes(lastResult), resultBytes) ){
						bytebuffer.put(resultBytes);
						numberOfOriginalFrames++;
						System.out.println("Frame " + i + "is added.");
						
						/**
						 * lastResult=result
						 * */
						try {
							lastResult = new MultiFormatReader().decode(binBitmap);
						} catch (NotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}//try..catch
					}//else if not equal
					
				}// if it is not START
				else if(decodingPhase==1 && result.toString().startsWith("START")){
					decodingPhase=2;
				}
				
				}//end for if (!resultString.matches("[0-9]")){
				
			} catch (NotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}//try..catch end
			
			
			i++;
			
		}// while
		
		Log.i("TAG", "i= " + i + "; decodingPhase= " + decodingPhase + "; Detected " + numberOfOriginalFrames + " Frames");
		decodingPhase=0;//reset decodingPhase
		
		bytebuffer.flip();//use current position as limit, then current position move to 0.
		byte[] rawBytes = new byte[bytebuffer.limit()];
		bytebuffer.get(rawBytes);
		
		/**
		 * Print out the rawBytes for debug
		 * */
		System.out.println("rawBytes length= ");
//		for(int j=0; j < rawBytes.length; j++){
//			System.out.print(" " + rawBytes[j]);
//		}
		System.out.println(rawBytes.length);
		

		FileInfo decodedInfo = decodeFile(rawBytes);
		Log.i("decodeFromQR()", "decodedInfo got");
		
		File decodedFile = new File(fileManager.getDecodedFolder()
				.getAbsolutePath(), decodedInfo.getFileName());

		fileManager.writeToFile(decodedFile, decodedInfo.getBody());
		
		Log.i("decodeFromQR()", "decodedFile " +decodedFile.getAbsolutePath());
		return decodedFile;
	}

	private FileInfo decodeFile(byte[] rawBytes) throws NotFileTypeException,
			IOException {
		
		Log.i("decodedFile()", "begins");
		
//		String decodedAsText = rawBytes.toString();//output: [B@41b9ca80

		String decodedAsText = new String(rawBytes, FileToQrEncoder.CHAR_ENCODING); 
		
//		System.out.println(decodedAsText);

		if (!decodedAsText.startsWith(FileToQrEncoder.HEADER_FILE_PREFIX)){
			Log.i("decodedFile()","not start with FILE:");
			throw new NotFileTypeException(decodedAsText);
		}
		
		//TODO Remove "System.currentTimeMillis()+" after testing
		String fileName = decodedAsText.substring(
				FileToQrEncoder.HEADER_FILE_PREFIX.length(),
				decodedAsText.indexOf(FileToQrEncoder.HEADER_BODY_SEPARATOR));
		Log.i("decodedFile()", "fileName= "+ fileName);

		int bodyIndex = (FileToQrEncoder.HEADER_FILE_PREFIX + fileName + FileToQrEncoder.HEADER_BODY_SEPARATOR)
				.getBytes(FileToQrEncoder.CHAR_ENCODING).length;
		byte[] bodyBytes = new byte[rawBytes.length - bodyIndex];
		for (int i = 0; i < bodyBytes.length; i++)
			bodyBytes[i] = rawBytes[bodyIndex + i];

//		byte[] unzippedBodyBytes = ByteUtilities.decompress(bodyBytes);
		Log.i("decodedFile()", "ends");

		return new FileInfo(fileName, bodyBytes);
	}

//	public LuminanceSource buildLuminanceSourceFromBitmap(Bitmap bitmap) {
//		return new RGBLuminanceSource(bitmap);
//	}
//
//	public LuminanceSource buildLuminanceSourceFromImageFile(File qr)
//			throws FileNotFoundException {
//		
//		return new RGBLuminanceSource(qr.getAbsolutePath());
//	}

	/**
	 * A factory method to build the appropriate LuminanceSource object based on
	 * the format of the preview buffers, as described by Camera.Parameters.
	 * 
	 * @param previewData
	 *            A preview frame.
	 * @param cameraParameters
	 *            The camera parameters
	 * @return A PlanarYUVLuminanceSource instance.
	 */
	public LuminanceSource buildLuminanceSourceFromCameraPreview(
			byte[] previewData, Parameters cameraParameters) {

		int previewFormat = cameraParameters.getPreviewFormat();
		String previewFormatString = cameraParameters.get("preview-format");
		Size previewSize = cameraParameters.getPreviewSize();

		switch (previewFormat) {
		// This is the standard Android format which all devices are REQUIRED to
		// support.
		// In theory, it's the only one we should ever care about.
		case PixelFormat.YCbCr_420_SP:
			// This format has never been seen in the wild, but is compatible as
			// we only care
			// about the Y channel, so allow it.
		case PixelFormat.YCbCr_422_SP:
			return new PlanarYUVLuminanceSource(previewData, previewSize.width,
					previewSize.height);
		default:
			// The Samsung Moment incorrectly uses this variant instead of the
			// 'sp' version.
			// Fortunately, it too has all the Y data up front, so we can read
			// it.
			if ("yuv420p".equals(previewFormatString)) {
				return new PlanarYUVLuminanceSource(previewData,
						previewSize.width, previewSize.height);
			}
		}
		throw new IllegalArgumentException("Unsupported picture format: "
				+ previewFormat + '/' + previewFormatString);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private byte[] getRawBytes(Result result) {
		Log.i("getRawBytes()", " ");//TODO delete after debugging

		Hashtable hashtable = result.getResultMetadata();
		Vector<byte[]> segments = (Vector<byte[]>) hashtable
				.get(ResultMetadataType.BYTE_SEGMENTS);
		int byteNum = 0;
		for (byte[] array : segments)
			byteNum += array.length;
		byte[] rawBytes = new byte[byteNum];
		int index = 0;
		for (byte[] array : segments)
			for (int i = 0; i < array.length; i++, index++){
				rawBytes[index] = array[i];
//				System.out.print(rawBytes[index] + " ");//TODO delete after debugging
			}

		return rawBytes;
	}

	private class FileInfo {
		
		private String fileName;
		private byte[] body;

		public FileInfo(String fileName, byte[] body) {
			Log.i("FileFromQrDecoder: class FileInfo", " FileInfo()");
			this.fileName = fileName;
			this.body = body;
		}

		public String getFileName() {
			Log.i("subclass FileInfo", "getFileName()");
			return fileName;
		}

		public byte[] getBody() {
			Log.i("subclass FileInfo", "getBody()");
			return body;
		}
	}

	private Bitmap createVideoThumbnail(String filePath, int i) {
		// final int thousand = 1000;
		// int samplingPeriod = thousand * 500;// microseconds(1000*ms)

		Bitmap bitmap = null;
		android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever();
		try {// MODE_CAPTURE_FRAME_ONLY
		// retriever
		// .setMode(android.media.MediaMetadataRetriever.MODE_CAPTURE_FRAME_ONLY);
		// retriever.setMode(MediaMetadataRetriever.MODE_CAPTURE_FRAME_ONLY);
			retriever.setDataSource(filePath);
			// bitmap = retriever.captureFrame();
			String timeString = retriever
					.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
			long time = Long.parseLong(timeString) * 1000;
//			Log.i("TAG", "time = " + time);
			bitmap = retriever.getFrameAtTime(i * time/numberOfExtractedFrames);//not good for the video with long useless frames
//			bitmap = retriever.getFrameAtTime(i * 300000);//get the same frame
			
			// (time/16*(i+1));// /imax*(i+1) better or /(imax-1)*i better?
		} catch (IllegalArgumentException ex) {
			// Assume this is a corrupt video file
		} catch (RuntimeException ex) {
			// Assume this is a corrupt video file.
		} finally {
			try {
				retriever.release();
			} catch (RuntimeException ex) {
				// Ignore failures while cleaning up.
			}
		}
		return bitmap;
	}
}
