package com.mcdaniel.swagger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import io.swagger.jaxrs.listing.SwaggerSerializers;
import io.swagger.models.Info;
import io.swagger.models.Model;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Scheme;
import io.swagger.models.Swagger;
import io.swagger.models.auth.ApiKeyAuthDefinition;
import io.swagger.models.auth.In;
import io.swagger.models.auth.SecuritySchemeDefinition;
import io.swagger.parser.SwaggerParser;

public class CombineSwaggerFiles {

	private String basePath = "/Users/dmcdaniel/git/TAP3/projects";
	
	private List<String> modulePaths = new ArrayList<>();
	
	public static void main(String[] args) {
		new CombineSwaggerFiles().run(args);
	}
	
	private void run(String [] args)
	{
		try
		{
			System.out.println("==============================\nStarting CombineSwaggerFiles\n==============================\n");

			basePath = ".";
			if ( args.length > 0 )
				basePath = args[0];
			
			ArrayList<String> swaggerFiles = new ArrayList<>();
			System.out.println("Searching for swagger files starting in " + new File(basePath).getAbsolutePath());
			
			swaggerFiles.addAll(searchDirectory(new File(basePath), "swagger.json"));
			swaggerFiles.addAll(searchDirectory(new File(basePath), "additional-swagger.json"));
			
			System.out.println("Found " + swaggerFiles.size() + " total files... filtering...");
			for ( String a : swaggerFiles )
			{
				System.out.println("Will process file: " + a);
			}
			
			
//			swaggerFiles.removeIf(x -> !x.contains("target/jaxrs-analyzer"));
//
//			for ( String a : swaggerFiles )
//			{
//				System.out.println("Will process file: " + a);
//			}
//			
			Map<String, Path> allPaths = new HashMap<>();
			Map<String, Model> allModels = new HashMap<>();
			for ( String path : swaggerFiles ) //modulePaths )
			{
				
				String splitStr = File.separator; 
				if ( !splitStr.equals("/") )
					splitStr = "\\\\";
				String[] filePathParts = path.split(splitStr);
				int moduleIndex = 0;
				for ( int i = 0; i < filePathParts.length; i ++ )
				{
					if ( filePathParts[i].equals("target") )
						moduleIndex = i - 1;
				}
				
				String moduleName = filePathParts[moduleIndex];
				System.out.println("Module: " + moduleName);
				boolean addVendorExts = true;
				boolean addSecurity = true;
				
				if ( moduleName.equals("BaseAPI") )//|| moduleName.equals("TAP3FileAPI"))
				{
					addVendorExts = false;
				}
				if ( moduleName.equals("TAP3LoginAPI"))
				{
					addSecurity = false;
				}
				
				System.out.println("addVendorExts = " + addVendorExts);
				System.out.println("addSecurity = " + addSecurity);
				SwaggerReturnData pr = processSwagger(path, moduleName, addVendorExts, addSecurity);
				
				if ( pr != null )
				{
					// Add these paths to all paths
					if ( pr.getNewPaths() != null )
					{	// Must merge operations under a path
						for ( String key : pr.getNewPaths().keySet() )
						{
							System.out.println("Found key " + key);
							if ( allPaths.containsKey(key))
							{
								System.out.println("Merging key!");
								Path path_ = allPaths.get(key);
								List<Operation> ops = path_.getOperations();
								// Merge new operations into existing operations on the path
								if ( pr.getNewPaths().get(key).getDelete() != null )
								{
									path_.setDelete(pr.getNewPaths().get(key).getDelete());
								}
								if ( pr.getNewPaths().get(key).getPost() != null )
								{
									path_.setPost(pr.getNewPaths().get(key).getPost());
								}
								if ( pr.getNewPaths().get(key).getGet() != null )
								{
									path_.setGet(pr.getNewPaths().get(key).getGet());
								}
								if ( pr.getNewPaths().get(key).getPut() != null )
								{
									path_.setPut(pr.getNewPaths().get(key).getPut());
								}
								if ( pr.getNewPaths().get(key).getOptions() != null )
								{
									path_.setOptions(pr.getNewPaths().get(key).getOptions());
								}
							}
							else	// Save the new path
								allPaths.put(key, pr.getNewPaths().get(key));
//								allPaths.putAll(pr.getNewPaths());
						}
					}
					
					// Add the new definitions
					if ( pr.getModels() != null )
						allModels.putAll(pr.getModels());
				}
				
			}
			
			// Now that we're done, create a new Swagger with all of the paths
			Swagger swagger = new Swagger();
			// Add the info
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String version = sdf.format(new Date());
			swagger.setInfo(new Info().version(version).title("TAP 3 API"));
			swagger.setSwagger("2.0");

			// Add the host
			swagger.setHost("api.microstarlogistics.com");
			
			// Fix the base path - need to remove the /v3 from all paths
			swagger.setBasePath("/");
			
			// Set the scheme
			List<Scheme> schemes = new ArrayList<>();
			schemes.add(Scheme.HTTPS);
			swagger.setSchemes(schemes);

			// Set the paths
			swagger.setPaths(allPaths);
			
			// Set the definitions
			swagger.setDefinitions(allModels);

			// Set up the authorizer
			Map<String, SecuritySchemeDefinition> securityDefinitions = new HashMap<>();
			SecuritySchemeDefinition ssd = new ApiKeyAuthDefinition("Authentication", In.HEADER);
			ssd.setDescription("Cognito-based Authentication of user credentials");
			ssd.setType("apiKey");
			ssd.setVendorExtension("x-amazon-apigateway-authtype", "cognito_user_pools");
			
			HashMap<String, Object> authorizer = new HashMap<>();

			ArrayList<String> providerARNs = new ArrayList<>();
			providerARNs.add("arn:aws:cognito-idp:us-west-2:450017183792:userpool/us-west-2_cMJFxRDRs");
			authorizer.put("providerARNs", providerARNs);
			authorizer.put("type", "cognito_user_pools");
			
			ssd.setVendorExtension("x-amazon-apigateway-authorizer", authorizer);
			
			securityDefinitions.put("TAP3Authenticator", ssd);
			
			ArrayList<String> binaryTypes = new ArrayList<>();
			binaryTypes.add("application/pdf");
			swagger.setVendorExtension("x-amazon-apigateway-binary-media-types", binaryTypes );
			
			swagger.setSecurityDefinitions(securityDefinitions);
			
			// Dump out the new Swagger
			SwaggerSerializers ss = new SwaggerSerializers();
			SwaggerSerializers.setPrettyPrint(true);
//			System.out.println("New swagger: ");
//			ss.writeTo(swagger, null, null, null, MediaType.APPLICATION_JSON_TYPE, null, System.out);

			File swaggerFile = new File(basePath + "/master-swagger.json");
			OutputStream os = new FileOutputStream(swaggerFile);
			ss.writeTo(swagger, null, null, null, MediaType.APPLICATION_JSON_TYPE, null, os);
			os.flush();
			os.close();
			

		}
		catch ( Exception ex )
		{
			ex.printStackTrace();
		}
	}

	private ArrayList<String> searchDirectory(File directory, String fileNameToSearch)
	{
		ArrayList<String> ret = new ArrayList<>();
		
		for ( File temp : directory.listFiles() )
		{
			if ( temp.isDirectory() )
			{
				ret.addAll(searchDirectory(temp, fileNameToSearch));
			}
			else
			{
				if ( fileNameToSearch.toLowerCase().equals(temp.getName().toLowerCase()))
				{
					if ( temp.getAbsolutePath().toLowerCase().contains("target"))
					{
						ret.add(temp.getAbsoluteFile().toString());
					}
				}
			}
		}
			
		return ret;
	}
	private SwaggerReturnData processSwagger(String filename, String moduleName, boolean addVendorExts, boolean addSecurity) throws IOException
	{
		File f = new File(filename);
		if ( ! f.exists() )
			return null;
		
		System.out.println("Filename: " + filename);
		
		Swagger swagger = new SwaggerParser().read(filename);
		System.out.println("BasePath: " + swagger.getBasePath());
		
		SwaggerReturnData pr = new SwaggerReturnData();
		
		// Walk the list of paths
		Map<String, Path> oldPaths = swagger.getPaths();
		Map<String, Path> newPaths = new HashMap<>();
		pr.setNewPaths(newPaths);
		pr.setModels(swagger.getDefinitions());
		
		for ( String pathKey : oldPaths.keySet() )
		{
			System.out.println("Processing: " + pathKey);
			Path p = oldPaths.get(pathKey);

			// Remove the key and then add back corrected
			pathKey = pathKey.replace("/v3", "");
			newPaths.put(pathKey, p);
			
			// Fix post method, if exists
			String operation = null;
			Operation postOp = p.getPost();
			if ( postOp != null )
			{
//				postOp.setProduces(Arrays.asList(MediaType.APPLICATION_JSON));
//				postOp.setConsumes(Arrays.asList(MediaType.APPLICATION_JSON));
//				if ( pathKey.equals("/files"))
//					postOp.setConsumes(Arrays.asList("application/pdf"));
				operation = "POST";
				System.out.println("\tOperation: " + operation);
				if ( addVendorExts )
					postOp.setVendorExtensions(getVendorExts(moduleName, operation));
				if ( addSecurity )
				{
					postOp.addSecurity("TAP3Authenticator", null);
				}
			}
			
			Operation headOp = p.getHead();
			if ( headOp != null )
			{
//				headOp.setProduces(Arrays.asList(MediaType.APPLICATION_JSON));
				operation = "HEAD";
				System.out.println("\tOperation: " + operation);
				if ( addVendorExts )
					headOp.setVendorExtensions(getVendorExts(moduleName, operation));
			}
			
			Operation getOp = p.getGet();
			if ( getOp != null )
			{
//				getOp.setProduces(Arrays.asList(MediaType.APPLICATION_JSON));
				operation = "GET";
				System.out.println("\tOperation: " + operation);
				if ( addVendorExts )
					getOp.setVendorExtensions(getVendorExts(moduleName, operation));
				if ( addSecurity && !pathKey.equals("/users/forgot") )
				{
					getOp.addSecurity("TAP3Authenticator", null);
				}
			}
			
			Operation putOp = p.getPut();
			if ( putOp != null )
			{
//				putOp.setProduces(Arrays.asList(MediaType.APPLICATION_JSON));
//				putOp.setConsumes(Arrays.asList(MediaType.APPLICATION_JSON));
				operation = "PUT";
				System.out.println("\tOperation: " + operation);
				if ( addVendorExts )
					putOp.setVendorExtensions(getVendorExts(moduleName, operation));
				if ( addSecurity )
				{
					putOp.addSecurity("TAP3Authenticator", null);
				}
			}
			
			Operation delOp = p.getDelete();
			if ( delOp != null )
			{
//				delOp.setProduces(Arrays.asList(MediaType.APPLICATION_JSON));
				operation = "DELETE";
				System.out.println("\tOperation: " + operation);
				if ( addVendorExts )
					delOp.setVendorExtensions(getVendorExts(moduleName, operation));
				if ( addSecurity )
				{
					delOp.addSecurity("TAP3Authenticator", null);
				}
			}
			
			Operation optionsOp = p.getOptions();
			if ( optionsOp != null )
			{
//				optionsOp.setProduces(Arrays.asList(MediaType.APPLICATION_JSON));
				operation = "OPTIONS";
				System.out.println("\tOperation: " + operation);
				if ( addVendorExts )
					optionsOp.setVendorExtensions(getVendorExts(moduleName, operation));
			}
			
		}
		
		swagger.setPaths(newPaths);
		
		return pr;
	}
	
	
	
	private Map<String, Object> getVendorExts(String module, String op)
	{
		Map<String, Object> ret = new HashMap<>();
		
		Map<String, String> statusCode = new HashMap<>();
		statusCode.put("statusCode", "200");
		Map<String, Object> defaultEntry = new HashMap<>();
		defaultEntry.put("default", statusCode);
		Map<String, Object> responses = new HashMap<>();
		responses.put("responses", defaultEntry);
		
		Map<String, Object> uri = new HashMap<>();
		ret.put("x-amazon-apigateway-integration", responses);
		responses.put("uri", "arn:aws:apigateway:us-west-2:lambda:path/2015-03-31/functions/arn:aws:lambda:us-west-2:450017183792:function:" + module + "/invocations");
		responses.put("passthroughBehavior", "when_no_match");
		responses.put("httpMethod", "POST");
		responses.put("contentHandling", "CONVERT_TO_TEXT");
		responses.put("type", "aws_proxy");
//		responses.put("type", "aws");
		
//		if ( module.equals("TAP3FileAPI") && op.equals("POST"))
//		{
//			responses.put("type", "aws");
//			HashMap<String, Object> templates = new HashMap<>();
//			templates.put("application/pdf", "{\"operation\": \"create-file\",\"authentication\": \"$context.identity.cognitoIdentityId\",\"customerId\": \"$input.params('customerId')\",\"shipmentId\": \"$input.params('shipmentId')\",\"orderId\": \"$input.params('orderId')\",\"docType\": \"$input.params('docType')\",\"base64Image\": \"$input.body\"}");
//			responses.put("requestTemplates", templates);
//		}
//		
//		if ( module.equals("TAP3FileAPI") && op.equals("GET"))
//		{
//			responses.put("type", "aws");
//			HashMap<String, Object> templates = new HashMap<>();
//			templates.put("application/pdf", "{\"operation\": \"get-file\",\"authentication\": \"$context.identity.cognitoIdentityId\",\"customerId\": \"$input.params('customerId')\",\"shipmentId\": \"$input.params('shipmentId')\",\"orderId\": \"$input.params('orderId')\",\"docType\": \"$input.params('docType')\",\"base64Image\": \"$input.body\"}");
//			responses.put("requestTemplates", templates);
//		}
		
		return ret;
	}
}
