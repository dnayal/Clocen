package helpers;

import java.io.IOException;
import java.util.Map;

import models.ServiceAccessToken;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.AbstractContentBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.codehaus.jackson.map.ObjectMapper;

@SuppressWarnings("unchecked")
public class WSHelper {

	private static final String COMPONENT_NAME = "WebService Helper";
	

	/**
	 * Makes REST POST calls to upload files. 
	 * We are throwing the exception rather than catching them so that  
	 * the client can logs can show properly where the problem occurred 
	 * 
	 * TODO - this needs to be replaced with WS.post when Play 
	 * starts supporting multipart file upload for WS api
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	public static Map<String, Object> postRequestWithFileUpload(String URL, ServiceAccessToken sat, 
			Map<String, AbstractContentBody> bodyPart) throws ClientProtocolException, IOException {
		
		Map<String, Object> map = null;

		HttpPost postRequest = new HttpPost(URL);
		postRequest.setHeader("Authorization", "Bearer " + sat.getAccessToken());
		
		MultipartEntityBuilder multipartEntity = MultipartEntityBuilder.create();
		// add the body parts 
		// can be a file or a string
		for(String key: bodyPart.keySet())
			multipartEntity.addPart(key, bodyPart.get(key));
		
		postRequest.setEntity(multipartEntity.build());
		
		HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
		CloseableHttpClient httpClient = httpClientBuilder.build();

		HttpResponse httpResponse = httpClient.execute(postRequest);
		HttpEntity httpEntity = httpResponse.getEntity();

		ObjectMapper responseMapper = new ObjectMapper();
		map = responseMapper.readValue(httpEntity.getContent(), Map.class);

		try {
			// we do not want to break the flow of the program 
			// in case we are not able to close the connection
			httpClient.close();
		} catch (Exception exception) {
			UtilityHelper.logError(COMPONENT_NAME, "postRequestWithFileUpload()", exception.getMessage(), exception);
		}

		return map;
	}
}
