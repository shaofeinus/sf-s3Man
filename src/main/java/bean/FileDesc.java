package bean;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import com.amazonaws.services.s3.model.S3ObjectSummary;

public class FileDesc {
	
	private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

	private String name;
	private String directory;
	private String path;
	private double size;	// In KBytes
	private String lastModifiedDate;
	
	public FileDesc(S3ObjectSummary properties) {
		setProperties(properties);
	}
	
	public void setProperties(S3ObjectSummary properties) {
		
		// Set path
		path = properties.getKey();
		
		// Set name and directory
		// Split path by either '/' or '\'
		String[] splitPath = path.split("/").length > 1 ? path.split("/") : path.split("\\\\");
		if(splitPath.length > 1) {
			name = splitPath[splitPath.length - 1];
			directory = path.replace(name, "");
		} else {
			name = path;
			directory = "/";
		}
		
		// Set size
		size = (double) properties.getSize() / 1024; 
		
		// Set last modified date
		lastModifiedDate = new SimpleDateFormat(DATE_FORMAT).format(properties.getLastModified());
	}

}
