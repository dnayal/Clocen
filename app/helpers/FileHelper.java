package helpers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.io.FileUtils;

import play.Play;

public class FileHelper {

	private static final String COMPONENT_NAME = "File Helper";
	
	private String fileURLorData = null;
	private InputStream inputStream = null;
	private String fileName = null;
	private String fileId = null;
	private SourceFileType sourceType;
	
	enum SourceFileType {
		URL, 
		TEXT,
		INPUTSTREAM,
		FILE
	};
	
	
	public String getFileName() {
		return fileName;
	}
	
	
	public void setFileName(String newFileName) {
		
		if(sourceType==SourceFileType.FILE) {
			File currentFile = new File(getFilePath());
			File newFile = new File(getFileDirectoryPath() + "/"+ newFileName);
			try {
				FileUtils.moveFile(currentFile, newFile);
			} catch (IOException exception) {
				UtilityHelper.logError(COMPONENT_NAME, "setFileName(String newFileName)", exception.getMessage(), exception);
			}
		}
		
		// update the file name to the new one
		fileName = newFileName;
	}
	
	
	/**
	 * Set the file source to a URL
	 */
	public String setFileSource(String name, String url) {
		this.fileURLorData = url;
		this.fileId = UtilityHelper.getUniqueId();
		this.fileName = name;
		this.sourceType = SourceFileType.URL;
		
		return fileId;
	}
	
	
	/**
	 * Set the file source to the text
	 */
	public String setFileSource(String data) {
		this.fileURLorData = data;
		this.fileId = UtilityHelper.getUniqueId();
		this.fileName = fileId + "_TEXT.txt";
		this.sourceType = SourceFileType.TEXT;
		
		return fileId;
	}
	
	
	/**
	 * Set the file source to binary
	 */
	public String setFileSource(InputStream inputStream, String name) {
		this.inputStream = inputStream;
		this.fileId = UtilityHelper.getUniqueId();
		this.sourceType = SourceFileType.INPUTSTREAM;
				
		if(UtilityHelper.isEmptyString(name))
			this.fileName = this.fileId;
		else
			this.fileName = name;
		
		return fileId;
		
	}
	
	
	/**
	 * Copies the file to a location (temp directory for now) and 
	 * returns the fileId that can be used to refer back 
	 * that document 
	 */
	public File getFileFromSource() {
		File file = new File(getFilePath());
		
		try {
			switch(sourceType) {
				case URL:
					URL source = new URL(fileURLorData);
					InputStream input = source.openStream();
					
					FileUtils.copyInputStreamToFile(input, file);
					input.close();
					break;
					
				case TEXT:
					FileUtils.writeStringToFile(file, fileURLorData, Play.application().configuration().getString("application.encoding"));
					break;
					
				case INPUTSTREAM:
					FileUtils.copyInputStreamToFile(inputStream, file);
					inputStream.close();
					// once the input stream has been closed, 
					// we need to store the file locally, to 
					// ensure that it available for other nodes in the process
					//
					// The reason we have to do this is that unlike URL or TEXT, 
					// an inputstream once closed is not available
					this.sourceType = SourceFileType.FILE;
					break;
				case FILE: // return file
					return file;
			}
		} catch (Exception exception) {
			UtilityHelper.logError(COMPONENT_NAME, "getFileFromSource()", exception.getMessage(), exception);
		}
		
		return file;
	}
	
	
	public Boolean deleteFile() {
		Boolean result = false;
		try {
			switch(sourceType) {
				case URL: case TEXT: 
					File directory = new File(getFileDirectoryPath());
					for(File file : directory.listFiles())
						result = file.delete();
					result = directory.delete();
					break;
				case INPUTSTREAM: case FILE:
					// do not delete the file,
					// as it might be required later
					break;
			}
		} catch(Exception exception) {
			UtilityHelper.logError(COMPONENT_NAME, "deleteFile()", exception.getMessage(), exception);
		}

		return result;
	}
	
	
	private String getFilePath() {
		return getFileDirectoryPath() + "/"+ fileName;
	}

	
	private String getFileDirectoryPath() {
		if(Play.isDev() || Play.isTest()) {
			return Play.application().configuration().getString("temp.folder.DEV") + "/" + fileId; 
		} else {
			return Play.application().configuration().getString("temp.folder.PROD") + "/" + fileId; 
		}
	}
	
	
}
