package process;

import helpers.ServiceNodeHelper;
import helpers.UtilityHelper;

import java.util.ArrayList;
import java.util.Map;

import models.Process;
import models.ServiceAccessToken;
import models.ServiceAccessTokenKey;
import nodes.Node;

import org.codehaus.jackson.map.ObjectMapper;

import com.google.common.util.concurrent.Service;

import play.Logger;
import play.libs.Json;

/**
 * This is the main class that parses the process 
 * data and executes the process
 */
// Read this article - http://wiki.fasterxml.com/JacksonInFiveMinutes
public class ProcessExecutor {
	
	private static final String COMPONENT_NAME = "ProcessExecutor";

	
	public ProcessExecutor() {}
	
	
	/**
	 * This is the main process of this class
	 */
	@SuppressWarnings("unchecked")
	public static void executeProcess(Process process) {
		Map<String, Object> previousNode = null;
		ObjectMapper mapper = new ObjectMapper();

		try {
			// Get the nodes in the process (in array format)
			ArrayList<Map<String, Object>> array = mapper.readValue(Json.parse(process.getProcessData()), ArrayList.class);
			int arrayIndex = 0;
			
			// loop through each node in the array
			for(Map<String, Object> node : array) {
				String nodeId = (String) node.get("node");
				
				Map<String, Object> data = (Map<String, Object>) node.get("data");
				String operation = (String) data.get("id");
				
				// If the node is not the first one, then configure its input 
				// to be populated by the output of the previous node 
				// (wherever mapped). The resultant node will have the actual data
				// mapped in its input variables rather than the mappings
				// 
				// If the node is not the first one and the prevousNode variable 
				// is still null, then either the event did not trigger or there 
				// was a error executing the last node
				if(arrayIndex!=0 && previousNode==null) {
					return;
				} else if(arrayIndex!=0 && previousNode!=null) {
					data = mapInputValuesForNode(arrayIndex, data, previousNode);
				}
				
				Node serviceNode = ServiceNodeHelper.getNode(nodeId);
				
				// TODO - implement executeService() method //
				// The output (previousNode) will be the same as the input (data)
				// except that the output variables of the output node will be populated 
				// with the values retrieved from the operation
				ServiceAccessToken sat = ServiceAccessToken.getServiceAccessToken(new ServiceAccessTokenKey(process.getUserId(), serviceNode.getNodeId()));
				previousNode = serviceNode.executeService(operation, sat, data); 
				
				arrayIndex++;
			}
		} catch (Exception exception) {
			UtilityHelper.logError(COMPONENT_NAME, "executeProcess()", exception.getMessage(), exception);
		}
		
	}
	
	
	/**
	 * This method maps the output of the source node to 
	 * the input of the target node, wherever it is mapped
	 */
	@SuppressWarnings("unchecked")
	private static Map<String, Object> mapInputValuesForNode(int indexOfTargetNode, Map<String, Object> target, Map<String, Object> source) {
		if(source==null)
			return target;
		
		// Get all the input variables - configured in json as the array
		ArrayList<Map<String, Object>> inputs = (ArrayList<Map<String, Object>>)target.get("input");
		int mappedAttributeStartPoint = 0, mappedAttributeEndPoint = 0;
		String mappedAttribute;
		
		// Go through each input variable
		for(Map<String, Object> input : inputs) {
			String type = (String) input.get("type");
			if(!type.equalsIgnoreCase("service")) {
				String value = (String) input.get("value");

				// All input fields are mapped as ##0.name## or ##1.description##
				// so here we are getting the index of the previous node.
				// Keep looking for mapping in the input variables and replacing 
				// with the actual values from output of previous node
				while((mappedAttributeStartPoint = value.indexOf("##"+(indexOfTargetNode-1)+".", mappedAttributeStartPoint)) >= 0) {
					// add the length of the characters to the index 
					// to get the actual start point of the attribute 
					mappedAttributeStartPoint+=String.valueOf(indexOfTargetNode-1).length() + 3;
					mappedAttributeEndPoint = value.indexOf("##", mappedAttributeStartPoint+1);
					// get the name of the mapped attribute, for e.g. name, id or description
					mappedAttribute = value.substring(mappedAttributeStartPoint, mappedAttributeEndPoint);
					
					String output = getOutputForAttribute(mappedAttribute, source);

					// replace the mapping with the actual output,
					// if it is not an empty string or null
					if(!UtilityHelper.isEmptyString(output)) {
						value = value.replace("##"+(indexOfTargetNode-1)+"."+mappedAttribute+"##", output);
						input.put("value", value);
					}

					// set the start point to the last end point, so that we do not keep getting the same mapping
					mappedAttributeStartPoint = mappedAttributeEndPoint;
				}
			}
		}
		return target;
	}
	
	
	/**
	 * Parses through the output variables of node for the given attribute. If a match is found
	 * it gets the output and returns that as the value
	 */
	@SuppressWarnings("unchecked")
	private static String getOutputForAttribute(String attribute, Map<String, Object> node) {

		// Get all the output variables - configured in json as the array
		ArrayList<Map<String, Object>> outputs = (ArrayList<Map<String, Object>>)node.get("output");
		
		// Go through each input variable
		for(Map<String, Object> output : outputs) {
			String nodeAttribute = (String) output.get("id");

			// return the value of the attribute if it is found in the node 
			if (nodeAttribute.equalsIgnoreCase(attribute))
				return (String) output.get("value");
		}
		
		// if the code reaches here, it means that 
		// the attribute was not found in the node.
		// so return the null
		return null;
		
	}

}
