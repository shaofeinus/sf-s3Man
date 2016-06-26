package core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import bean.FileDesc;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

/**
 * This class perform CRUD operations on the S3 object store. 
 * 
 * @author Shao Fei
 *
 */
public class S3Manager {
	
	private static final Logger LOG = Logger.getLogger(S3Manager.class
			.getName());

	private S3Connector s3Conn;
	private AmazonS3 s3Client;
	private String bucketName;

	public S3Manager() {
		// Different implementation for connection to different object stores
		s3Conn = new AmazonS3Connector();
		s3Conn.connect();
		s3Client = s3Conn.getClient();
		bucketName = s3Conn.getBucketName();
	}

	/**
	 * @return list of all file names
	 */
	public List<FileDesc> getFileList() {
		LOG.info("Getting file list of all files");
		ListObjectsRequest listReq = new ListObjectsRequest()
				.withBucketName(bucketName);
		return getFileList(listReq);
	}

	private List<FileDesc> getFileList(ListObjectsRequest listReq) {
		ObjectListing objList = s3Client.listObjects(listReq);
		List<FileDesc> result = new ArrayList<FileDesc>();
		for (S3ObjectSummary objSummary : objList.getObjectSummaries()) {
			FileDesc fileDesc = new FileDesc(objSummary);
			result.add(fileDesc);
		}
		LOG.info("File list obtained");
		return result;
	}

	/**
	 * @param fileName
	 * @return Contents of file. Null if file does not exist.
	 */
	public String getContent(String fileName) {
		LOG.info("Getting contents of file: " + fileName);
		try {
			if(doesFileExist(fileName)) {
				S3Object s3O = s3Client.getObject(new GetObjectRequest(bucketName, fileName));
				InputStream in = s3O.getObjectContent();
				BufferedReader bReader = new BufferedReader(new InputStreamReader(in));
				StringBuilder sb = new StringBuilder();
				String line;
				while((line = bReader.readLine()) != null)
					sb.append(line + "\n");
				in.close();
				s3O.close();
				LOG.info("Contents of " + fileName + " obtained");
				return sb.toString();
			} else {
				return null;
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (AmazonServiceException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @param fileName
	 * @param meta
	 * @param content
	 */
	public void putFile(String fileName, File file, String contentType) {
		if(doesFileExist(fileName)) {
			LOG.info("Overriding contents of file: " + fileName);
		} else {
			LOG.info("Writing new file: " + fileName);
		}
		try {
			InputStream in;
			in = new FileInputStream(file);
			ObjectMetadata meta = new ObjectMetadata();
			meta.setContentLength(file.length());
			if(contentType != null && contentType.equals(""))
				meta.setContentType(contentType);
			s3Client.putObject(new PutObjectRequest(bucketName, fileName,
					in, meta));
			LOG.info(fileName + " written");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			LOG.warning(fileName + " not found. Nothing is written to object store");
		}
		
	}

	/**
	 * @param fileName
	 */
	public void deleteFile(String fileName) {
		if(doesFileExist(fileName)) {
			LOG.info("Deleting file: " + fileName);
			s3Client.deleteObject(new DeleteObjectRequest(bucketName, fileName));
			LOG.info(fileName + " deleted");
		}
	}

	/**
	 * @param fileName
	 * @return True if file exists, false otherwise.
	 */
	public boolean doesFileExist(String fileName) {
		try {
			s3Client.getObject(new GetObjectRequest(bucketName, fileName));
			LOG.info(fileName + " exists");
			return true;
		} catch (AmazonServiceException e) {
			if(e.getStatusCode() == 404) {
				LOG.info(fileName + " does not exist");
				return false;
			} else {
				LOG.info("Unknown error occurred");
				return false;
			}
		}
	}
}
