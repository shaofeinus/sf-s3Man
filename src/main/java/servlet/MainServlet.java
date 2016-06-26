package servlet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import bean.FileDesc;

import com.amazonaws.http.HttpResponse;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import core.S3Manager;

/**
 * Servlet implementation class MainServlet
 */
public class MainServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static final Logger LOG = Logger.getLogger(MainServlet.class
			.getName());

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public MainServlet() {
	}

	/**
	 * Handles request to view data
	 * 
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		
		LOG.info("GET request");
		
		String reqUrl = request.getPathInfo();
		String directive = getDirective(reqUrl);
		
		if(directive != null) {
			switch(directive) {
			case "view":
				processView(request, response);
				break;
			case "delete":
				processDelete(request, response);
				break;
			default:
				break;
			}
		} else {
			LOG.info("Nothing is done.");
		}
	}

	/**
	 * Handles request to upload and delete
	 * 
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		
		LOG.info("POST request");
		
		String reqUrl = request.getPathInfo();
		String directive = getDirective(reqUrl);
		
		if(directive != null) {
			switch(directive) {
			case "upload":
				processUpload(request, response);
				break;
			case "setUploadDir":
				processSetUploadDir(request, response);
				break;
			default:
				break;
			}
		} else {
			LOG.info("Nothing is done.");
		}
		
	}
	
	private void processDelete(HttpServletRequest request,
			HttpServletResponse response) {
		S3Manager s3Man = new S3Manager();
		String[] filePaths = request.getParameterValues("filePaths[]");
		
		if(filePaths != null) {
			for(String filePath: filePaths)
				s3Man.deleteFile(filePath);
			response.setStatus(HttpURLConnection.HTTP_OK);
		} else {
			LOG.warning("No 'filePaths' parameter");
			logRequestParamNames(request);
			response.setStatus(HttpURLConnection.HTTP_BAD_REQUEST);
		}
	}

	private void processView(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		S3Manager s3Man;
		s3Man = new S3Manager();
		
		String fileName = request.getParameter("fileName");

		// Get content of a file
		if(fileName != null) {
			
			String content = s3Man.getContent(fileName);
			
			// Write response
			if(content != null) {
				response.setContentType("text/plain");
				response.getWriter().write(content);
			} else {
				response.setStatus(HttpURLConnection.HTTP_NOT_FOUND);
				response.getWriter().write("Content not found!");
			}
		}
		
		// Get list of all files
		else {
			
			List<FileDesc> fileList;
			fileList = s3Man.getFileList();
			
			// Prepare json response
			Map<String, Object> responseMap = new HashMap<String, Object>();
			responseMap.put("fileList", fileList);

			// Write json response
			response.setContentType("application/json");
			Writer writer = response.getWriter();
			writer.write(new GsonBuilder().create().toJson(responseMap));
			
			response.setStatus(HttpURLConnection.HTTP_OK);
		}
	}

	private void processSetUploadDir(HttpServletRequest request,
			HttpServletResponse response) {
		// Set upload directory for the next upload
		String uploadDir = request.getParameter("uploadDir");
		if(uploadDir != null) {
			LOG.info("Set upload directory: " + uploadDir);
			request.getSession().setAttribute("uploadDir", uploadDir);
			response.setStatus(HttpURLConnection.HTTP_OK);
		} else {
			LOG.warning("No 'uploadDir' parameter");
			logRequestParamNames(request);
			response.setStatus(HttpURLConnection.HTTP_BAD_REQUEST);
		}
	}

	private void processUpload(HttpServletRequest request,
			HttpServletResponse response) throws IOException,
			FileNotFoundException {
		
		final int MAX_UPLOAD_MEM_SIZE = 24 * 1024;
		final int MAX_UPLOAD_FILE_SIZE = 100 * 1000 * 1024;
		
		// Initialize upload handler
		String uploadTempDir = System.getProperty("user.dir") + "/temp/uploads";
		DiskFileItemFactory factory = new DiskFileItemFactory();
		factory.setSizeThreshold(MAX_UPLOAD_MEM_SIZE);
		File tempDir = new File(uploadTempDir);
		if(!tempDir.exists()) {
			LOG.info("Created directory: " + tempDir.getAbsolutePath() + " for upload temp files");
			tempDir.mkdirs();
		}
		factory.setRepository(new File(uploadTempDir));
		ServletFileUpload upload = new ServletFileUpload(factory);
		upload.setFileSizeMax(MAX_UPLOAD_FILE_SIZE);
		
		// Handle uploaded files
		try {
			
			// Upload directory was separately set in session
			String uploadDir = (String)request.getSession().getAttribute("uploadDir");
			
			// Store files temporarily and upload to s3
			String s3UploadTempDir = System.getProperty("user.dir") + "/temp/s3";
			tempDir = new File(s3UploadTempDir);
			if(!tempDir.exists()) {
				LOG.info("Created directory: " + tempDir.getAbsolutePath() + " for upload S3 temp files");
				tempDir.mkdirs();
			}
			List<FileItem> fileItems = upload.parseRequest(request);
			
			for(FileItem fileItem: fileItems) {
				// Store file as temp file first
				String fileName = fileItem.getName();
				File tempFile = new File(s3UploadTempDir + "/" + fileName + ".temp");
				InputStream source = fileItem.getInputStream(); 
				OutputStream writer = new FileOutputStream(tempFile);
				byte[] buffer = new byte[8 * 1024];
			    int bytesRead;
			    while ((bytesRead = source.read(buffer)) != -1)
			    	writer.write(buffer, 0, bytesRead);
			    source.close();
			    writer.close();
				
			    // Upload the stored temp file
			    S3Manager s3Man = new S3Manager();
		        s3Man.putFile(uploadDir + fileName, tempFile, fileItem.getContentType());
		        
		        // Delete temp file
		        tempFile.delete();
			}
			
			response.setStatus(HttpURLConnection.HTTP_OK);
			
		} catch (FileUploadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private String getDirective(String reqUrl) {
		LOG.info("Request URL: " + reqUrl);
		if (reqUrl == null || reqUrl.equals("/") || reqUrl.split("/")[1] == "") {
			LOG.info("Request no directive");
			return null;
		} else {
			String directive = reqUrl.split("/")[1];
			LOG.info("Request directive: " + directive);
			return directive;
		}
	}
	
	private void logRequestParamNames(HttpServletRequest request) {
		Enumeration<String> enumer = request.getParameterNames();
		while(enumer.hasMoreElements()) {
			LOG.info("Request parameter: " + enumer.nextElement());
		}
	}
	
//	private String getSubDirective(String reqUrl) {
//		if (reqUrl == null || reqUrl.split("/").length != 3) {
//			LOG.info("Directive no sub-directive");
//			return null;
//		} else {
//			String subDirective = reqUrl.split("/")[2].split("\\?")[0];
//			LOG.info("Sub-directive: " + subDirective);
//			return subDirective;
//		}
//	}
	
}
