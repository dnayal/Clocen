package helpers;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.io.FileUtils;

import play.Play;

public class FileHelper {

	private static final String COMPONENT_NAME = "FileHelper";
	
	private String fileURLorData = null;
	private String fileName = null;
	private String fileId = null;
	private SourceFileType sourceType;
	
	enum SourceFileType {
		URL, 
		DATA
	};
	
	
	public String getFileName() {
		return fileName;
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
	 * Set the file source to the data
	 */
	public String setFileSource(String data) {
		this.fileURLorData = data;
		this.fileId = UtilityHelper.getUniqueId();
		this.fileName = fileId + "_TEXT";
		this.sourceType = SourceFileType.DATA;
		
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
				case DATA:
					FileUtils.writeStringToFile(file, fileURLorData, Play.application().configuration().getString("application.encoding"));
					break;
			}
		} catch (Exception exception) {
			UtilityHelper.logError(COMPONENT_NAME, "getFileFromSource()", exception.getMessage(), exception);
		}
		
		return file;
	}
	
	
	public Boolean deleteFile() {
		Boolean result = false;
		
		switch(sourceType) {
			case URL: case DATA:
				File directory = new File(getFileDirectoryPath());
				for(File file : directory.listFiles())
					result = file.delete();
				result = directory.delete();
				break;
		}

		return result;
	}
	
	private String getFilePath() {
		return getFileDirectoryPath() + "/"+ fileName;
	}

	private String getFileDirectoryPath() {
		return FileUtils.getTempDirectoryPath() + "/" + fileId;
	}
	
	
}
