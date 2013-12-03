package helpers;


import java.io.File;
import java.util.HashMap;
import java.util.HashSet;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.MappingJsonFactory;

import nodes.Node;
import play.Play;

public class ServiceNodeHelper {
	
	private static final String COMPONENT_NAME = "ServiceNode Helper";
	
	/**
	 * Private method to read the [node] directory, and then 
	 * returning the Node interface, by looping through all 
	 * classes in that folder and looking for the one that 
	 * implements Node interface  
	 */
	@SuppressWarnings("rawtypes")
	private static Node getNodeClass(File nodeDirectory) {
		Node node = null;
		// Get all files in the directory
		File classes[] = nodeDirectory.listFiles();
		
		// Loop through all files 
		for(File nodeClass : classes) {
			String className = nodeClass.getName();
			// Get all classes in the node
			if(className.endsWith(".class")) {
				// all nodes must follow this convention nodes.<<node name>>.<<Node class>>
				String fullClassName = "nodes." + nodeDirectory.getName() + "."+ className.substring(0, className.length()-(".class".length()));
				Object object = null;
				try {
					Class cls = Class.forName(fullClassName);
					// do not instantiate an interface
					if (!cls.isInterface())
						object = cls.newInstance();
				} catch (Exception exception) {
					UtilityHelper.logError(COMPONENT_NAME, "getNodeClass()", exception.getMessage(), exception);
				} 
				// If the class is of type Node, then cast it 
				// to the Node interface
				if (object != null && object instanceof Node) 
					node = ((Node)object);
			}
		}
		
		return node;
	}

	
	/**
	 * Returns the Node interface based on the input node id.
	 * It leverages the convention that all nodes will be under package
	 * nodes.<<node package>>
	 */
	public static Node getNode(String nodeId) {
		ClassLoader classLoader = Play.application().classloader();
		// prepare the path of the node package using convention
		String path = "nodes/" + nodeId;

		// Load the directory for that Node
		File nodeDirectory = new File(classLoader.getResource(path).getFile());

		// Get the Node interface
		Node node = getNodeClass(nodeDirectory);
		
		return node;
	}
	
	
	/**
	 * Get all Nodes
	 */
	public static HashMap<String, Node> getAllNodes() {
		HashMap<String, Node> nodeMap = new HashMap<String, Node>();
		ClassLoader classLoader = Play.application().classloader();
		String path = "nodes";
		// Get the parent directory
		File nodesDirectory = new File(classLoader.getResource(path).getFile());
		// Get all nodes within the parent Node directory
		File nodes[] = nodesDirectory.listFiles();

		for(File node: nodes) {
			// If the node is a directory
			if(node.isDirectory()) {
				// pass it to get the Node instance
				Node serviceNode = getNodeClass(node);
				// if the node instance is valid
				if (serviceNode != null) {
					// add it to hashmap
					nodeMap.put(node.getName(), serviceNode);
				}
			}
		}
		
		return nodeMap;
	}
	
	
	/**
	 * Get the ids of all nodes. This using the convention 
	 * that all nodes are under the nodes parent directory 
	 * and have the same package name as the node id
	 */
	public static HashSet<String> getAllNodeIds() {
		HashSet<String> set = new HashSet<String>();
		
		// java.lang.Classloader
		ClassLoader classLoader = Play.application().classloader();
		String path = "nodes";
		File nodesDirectory = new File(classLoader.getResource(path).getFile());
		File nodes[] = nodesDirectory.listFiles();

		for(File node: nodes) {
			// Get all node directories
			if(node.isDirectory())
				set.add(node.getName());
		}
		
		return set;
	}
	
	
	/**
	 * Returns the config json file for the given node
	 */
	public static JsonNode getNodeConfiguration(String nodeId) {
		JsonNode nodeConfig = null;
		try {
			JsonFactory factory = new MappingJsonFactory();
			// play.Application.classLoader
			JsonParser parser = factory.createJsonParser(Play.application().classloader().getResourceAsStream("nodes/" + nodeId + ".json"));
			nodeConfig  = parser.readValueAsTree();
			
			UtilityHelper.logMessage(COMPONENT_NAME, "getNodeConfiguration()", nodeConfig.asText());
		} catch (Exception exception) {
			UtilityHelper.logError(COMPONENT_NAME, "getNodeConfiguration()", exception.getMessage(), exception);
		}

		return nodeConfig;
	}
	
}
