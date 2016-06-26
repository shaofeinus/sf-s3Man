package core;

import java.util.logging.Logger;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;

public class AmazonS3Connector implements S3Connector {
	
	private static final Logger LOG = Logger.getLogger(AmazonS3Connector.class
			.getName());
	
	private static final String BUCKET_NAME = "shaofei-private";
	private AmazonS3 s3Client;

	@Override
	public void connect() {
		LOG.info("Connnecting to Amazon S3...");
		s3Client = new AmazonS3Client(new ProfileCredentialsProvider());
		LOG.info("Connnected to Amazon S3");
	}

	@Override
	public AmazonS3 getClient() {
		if(s3Client == null) {
			LOG.warning("S3 client has not be created");
			return null;
		}
		return s3Client;
	}

	@Override
	public String getBucketName() {
		LOG.info("Bucket name used: " + BUCKET_NAME);
		return BUCKET_NAME;
	}

}
