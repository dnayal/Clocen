package process;

import helpers.FileHelper;
import helpers.ServiceNodeHelper;
import helpers.UtilityHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import models.Process;
import models.ServiceAccessToken;
import models.ServiceAccessTokenKey;
import nodes.Node;

/**
 * This is the main class that parses the process 
 * data and executes the process
 */
// Read this article - http://wiki.fasterxml.com/JacksonInFiveMinutes
public class ProcessExecutor {
	
	private static final String COMPONENT_NAME = "Process Executor";

	
	public ProcessExecutor() {}
	
	
	/**
	 * Execute processes only with POLL trigger type
	 */
	public static void executePollProcess(Process process) {
		
		// Get the nodes in the process (in array format)
		ArrayList<Map<String, Object>> array = process.getProcessDataAsObject();
		
		try {
			executeProcess(process.getProcessId(), array, Node.TRIGGER_TYPE_POLL, process.getUserId());
		} catch (Exception exception) {
			UtilityHelper.logError(COMPONENT_NAME, "executePollProcess(Process)", "Process # " + process.getProcessId() + " - "+ exception.getMessage(), exception);
		}

	}
	
	
	/**
	 * Executes poll processes called by Node triggers
	 */
	public static void executePollProcess(String processId, ArrayList<Map<String, Object>> array, String userId) {
		
		try {
			executeProcess(processId, array, Node.TRIGGER_TYPE_POLL_CALLED_BY_NODE, userId);
		} catch (Exception exception) {
			UtilityHelper.logError(COMPONENT_NAME, "executePollProcess(ArrayList)", "Process # " + processId + " - "+ exception.getMessage(), exception);
		}

	}

	
	public static void executeHookProcess(String processId, ArrayList<Map<String, Object>> array, String userId) {
		try {
			executeProcess(processId, array, Node.TRIGGER_TYPE_HOOK, userId);
		} catch (Exception exception) {
			UtilityHelper.logError(COMPONENT_NAME, "executeHookProcess()", "User # " + userId + " - "+ exception.getMessage(), exception);
		}
	}
	
	
	/**
	 * This is the main process of this class
	 */
	@SuppressWarnings("unchecked")
	private static void executeProcess(String processId, ArrayList<Map<String, Object>> array, String triggerType, String userId) {
		
		Map<String, Object> previousNode = null;

		try {
			int arrayIndex = 0;
			
			// loop through each node in the array
			for(Map<String, Object> node : array) {
				
				// if it is HOOK type trigger then the first node is already populated
				if ((triggerType.equalsIgnoreCase(Node.TRIGGER_TYPE_HOOK) 
						|| triggerType.equalsIgnoreCase(Node.TRIGGER_TYPE_POLL_CALLED_BY_NODE)) 
							&& arrayIndex==0) {

					previousNode = (Map<String, Object>) node.get("data");
					
				} else {
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
					
					// The output (previousNode) will be the same as the input (data)
					// except that the output variables of the output node will be populated 
					// with the values retrieved from the operation
					ServiceAccessToken sat = ServiceAccessToken.getServiceAccessToken(new ServiceAccessTokenKey(userId, serviceNode.getNodeId()));
					// if the service token is null then the user might have   
					// revoked the rights (or the token would have been deleted) 
					// and there is no point in executing the rest of the process
					if(sat == null)
						return;
					previousNode = serviceNode.executeService(processId, arrayIndex, operation, sat, data); 
				}
				
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
			// we do not need to map inputs for input of type 
			// service, as it is already mapped
			if(!type.equalsIgnoreCase(Node.ATTR_TYPE_SERVICE)) {
				
				String value = (String) input.get("value");
				// if the user has not mapped a value to an input variable
				// then do not process further
				if(UtilityHelper.isEmptyString(value))
					continue;
				
				// boolean variable to check whether a 
				// file needs to be created out of string 
				Boolean createFileFromString = false;
	
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
					
					// get the output for the attribute
					Object output = getOutputForAttribute(mappedAttribute, source);
					// replace the mapping with the actual output,
					// if it is not an empty string or null
					if((output instanceof String) && (UtilityHelper.isEmptyString(((String)output)))) {
						output = "";
					}
	
					if(type.equalsIgnoreCase(Node.ATTR_TYPE_STRING) || type.equalsIgnoreCase(Node.ATTR_TYPE_LONGSTRING)) {
						value = value.replace("##"+(indexOfTargetNode-1)+"."+mappedAttribute+"##", (String)output);
						input.put("value", value);
					}
					// for file type input can be of two types...
					else if(type.equalsIgnoreCase(Node.ATTR_TYPE_FILE)) {
						// either the user will create a string using output of 
						// the previous node such as task name or invoice number 
						// or file description in which case we need to map it 
						// to the string and then create a file out of that string...
						if (output instanceof String) {
							value = value.replace("##"+(indexOfTargetNode-1)+"."+mappedAttribute+"##", (String)output);
							input.put("value", value);
							// we will need to create a file with the string
							createFileFromString = true;
						} 
						// or it can be mapped directly to the file output 
						// of the previous node
						else if (output instanceof ArrayList) {
							
							input.put("value", output);
							// creating a new file from string is not required as 
							// it is mapped to the file of the previous node
							createFileFromString = false;
							break; 
						}
					} 
					// set the start point to the last end point, so that we do not keep getting the same mapping
					mappedAttributeStartPoint = mappedAttributeEndPoint;
				
				} // loop through position in the current input
				
				// if the file needs to be created from the string
				if(createFileFromString) {
					// initialize FileHelper accordingly
					FileHelper fileHelper = new FileHelper();
					fileHelper.setFileSource((String)input.get("value"));
	
					// initialize the attachments object
					ArrayList<Map<String, Object>> attachments = new ArrayList<Map<String, Object>>();
					
					// we are using a Map object here only to comply 
					// with the ObjectMapper created by parsing a json 
					Map<String, Object> map = new HashMap<String, Object>();
					map.put(Node.ATTR_TYPE_FILE, fileHelper);
					
					// add the file to the attachment list
					attachments.add(map);
					
					// set the input value to the new attachments list created
					input.put("value", attachments);
				}
			} // if not service block
		} // loop through for inputs
		return target;
	}
	
	
	/**
	 * Parses through the output variables of node for the given attribute. If a match is found
	 * it gets the output and returns that as the value
	 */
	@SuppressWarnings("unchecked")
	private static Object getOutputForAttribute(String attribute, Map<String, Object> node) {

		// Get all the output variables - configured in json as the array
		ArrayList<Map<String, Object>> outputs = (ArrayList<Map<String, Object>>)node.get("output");
		
		// Go through each input variable
		for(Map<String, Object> output : outputs) {
			String nodeAttribute = (String) output.get("id");
			String attributeType = (String) output.get("type");

			// return the value of the attribute if it is found in the node
			if (nodeAttribute.equalsIgnoreCase(attribute)) { 
				if (attributeType.equalsIgnoreCase(Node.ATTR_TYPE_STRING) || attributeType.equalsIgnoreCase(Node.ATTR_TYPE_LONGSTRING)) {
					// if it is a string return the string value
					return (String) output.get("value");
				} else if (attributeType.equalsIgnoreCase(Node.ATTR_TYPE_FILE)) {
					// if it is an attachments list from the previous 
					// node, then attach it as is
					return (ArrayList<Map<String, Object>>) output.get("value");
				}
			}
		}
		
		// if the code reaches here, it means that 
		// the attribute was not found in the node.
		// so return the null
		return null;
		
	}

}
