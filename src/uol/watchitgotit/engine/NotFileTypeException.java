package uol.watchitgotit.engine;

public class NotFileTypeException extends Exception {

	private static final long serialVersionUID = 6950505632588286897L;

	private String qrContent;
	
	public NotFileTypeException(String qrContent){
		this.qrContent= qrContent;
	}

	public NotFileTypeException() {
		qrContent= null;
	}

	public String getQrContent() {
		return qrContent;
	}
}
