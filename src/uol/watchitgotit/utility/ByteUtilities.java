package uol.watchitgotit.utility;

import java.io.*;
import java.util.zip.*;

import android.util.Log;

public class ByteUtilities {
	private static final int DEFAULT_BUFFER_SIZE = 1000000;// for decompress's copystream--->not be used

	public static byte[] compress(byte[] content) {

//		try {
//			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//			GZIPOutputStream gzipOutputStream = new GZIPOutputStream(
//					byteArrayOutputStream);
//			gzipOutputStream.write(content);
//			gzipOutputStream.close();
//			return byteArrayOutputStream.toByteArray();
//
//		} catch (IOException e) {
//			e.printStackTrace();
//			return null;
//		}
		
		return content;

	}

	public static byte[] decompress(byte[] contentBytes) throws IOException {
//		Log.i("ByteUtilities: ", "Decompressing");
//
//		ByteArrayOutputStream out = new ByteArrayOutputStream();
//		
//		Log.i("ByteUtilities: ", "Decompressing: copyStream:");
//		copyStream(
//				new GZIPInputStream(new ByteArrayInputStream(contentBytes)),
//				out);
//		Log.i("ByteUtilities: ", "Decompressing: copyStream finished");
//		
//		return out.toByteArray();
		
		return contentBytes;
	}
	
	

	private static long copyStream(InputStream input, OutputStream output)
			throws IOException {
		byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
		long count = 0;
		int n = 0;
		while (-1 != (n = input.read(buffer))) {
			output.write(buffer, 0, n);
			count += n;
		}
		return count;
	}

	public static byte[] merge(byte[] first, byte[] second) {

		byte[] merged = new byte[first.length + second.length];
		System.arraycopy(first, 0, merged, 0, first.length);
		System.arraycopy(second, 0, merged, first.length, second.length);

		return merged;
	}
	
	/**
     * The number of bytes in a kilobyte.
     */
    public static final long ONE_KB = 1024;

    /**
     * The number of bytes in a megabyte.
     */
    public static final long ONE_MB = ONE_KB * ONE_KB;

    /**
     * The number of bytes in a gigabyte.
     */
    public static final long ONE_GB = ONE_KB * ONE_MB;
	
    /**
     * Returns a human-readable version of the file size, where the input
     * represents a specific number of bytes.
     *
     * @param byteNumber  the number of bytes
     * @return a human-readable display value (includes units)
     */
	public static String getHumanReadableSize(long byteNumber) {
        String displaySize;

        if (byteNumber / ONE_GB > 0) {
            displaySize = String.valueOf(byteNumber / ONE_GB) + " GB";
        } else if (byteNumber / ONE_MB > 0) {
            displaySize = String.valueOf(byteNumber / ONE_MB) + " MB";
        } else if (byteNumber / ONE_KB > 0) {
            displaySize = String.valueOf(byteNumber / ONE_KB) + " KB";
        } else {
            displaySize = String.valueOf(byteNumber) + " bytes";
        }
        return displaySize;
    }
}
