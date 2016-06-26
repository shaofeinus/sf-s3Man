package core;

import com.amazonaws.services.s3.AmazonS3;

public interface S3Connector {
	
	public void connect();
	
	public AmazonS3 getClient();
	
	public String getBucketName();
	
}
