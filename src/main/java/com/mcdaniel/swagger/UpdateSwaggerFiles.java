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

import com.amazonaws.services.apigateway.AmazonApiGateway;
import com.amazonaws.services.apigateway.AmazonApiGatewayClient;
import com.amazonaws.services.apigateway.AmazonApiGatewayClientBuilder;
import com.amazonaws.services.apigateway.model.CreateRestApiRequest;
import com.amazonaws.services.apigateway.model.GetRestApiRequest;

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

public class UpdateSwaggerFiles {

	private static final String basePath = "/Users/dmcdaniel/git/TAP3/projects";
	
	private List<String> modulePaths = new ArrayList<>();
	
	public static void main(String[] args) {
		new UpdateSwaggerFiles().run();
	}
	
	private void run()
	{
		try
		{
//			AmazonApiGateway apiClient = AmazonApiGatewayClientBuilder.defaultClient();
			
			File xmlFile = new File(basePath + "/pom.xml");
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbFactory.newDocumentBuilder();
			Document doc = db.parse(xmlFile);
			
			doc.getDocumentElement().normalize();
			
			NodeList list = doc.getElementsByTagName("module");
			for ( int i = 0; i < list.getLength(); i ++ )
			{
				String f = list.item(i).getTextContent();
				f = f.replace("../", "");
				System.out.println("Found Module: " + f);
				modulePaths.add(f);
			}
			
			Map<String, Path> allPaths = new HashMap<>();
			Map<String, Model> allModels = new HashMap<>();
			for ( String path : modulePaths )
			{
				String fullPath = basePath + "/" + path + "/target/jaxrs-analyzer/swagger.json";
				SwaggerReturnData pr = processSwagger(fullPath, path);
				
//				if ( pr != null )
//				{
//					// Add these paths to all paths
//					if ( pr.getNewPaths() != null )
//						allPaths.putAll(pr.getNewPaths());
//					
//					// Add the new definitions
//					if ( pr.getModels() != null )
//						allModels.putAll(pr.getModels());
//				}
			}

/*
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
			
			swagger.setVendorExtension("x-amazon-apigateway-binary-media-types", "application/pdf");
			
			swagger.setSecurityDefinitions(securityDefinitions);
			
			// Dump out the new Swagger
			SwaggerSerializers ss = new SwaggerSerializers();
			SwaggerSerializers.setPrettyPrint(true);
			System.out.println("New swagger: ");
			ss.writeTo(swagger, null, null, null, MediaType.APPLICATION_JSON_TYPE, null, System.out);

			File swaggerFile = new File(basePath + "/master-swagger.json");
			OutputStream os = new FileOutputStream(swaggerFile);
			ss.writeTo(swagger, null, null, null, MediaType.APPLICATION_JSON_TYPE, null, os);
			os.flush();
			os.close();
*/

		}
		catch ( Exception ex )
		{
			ex.printStackTrace();
		}
	}

	private SwaggerReturnData processSwagger(String filename, String module) throws IOException
	{
		File f = new File(filename);
		if ( ! f.exists() )
			return null;
		
		boolean isLogin = false;
		String outputPath = basePath + "/" + module + "/target/jaxrs-analyzer/updated-swagger.json";

		if ( filename.contains("TAP3LoginAPI"))
			isLogin = true;
		
		Swagger swagger = new SwaggerParser().read(filename);
		System.out.println("BasePath: " + swagger.getBasePath());
		
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
				postOp.setVendorExtensions(getVendorExts(module, operation));
				if ( !isLogin )
				{
					postOp.addSecurity("TAP3Authenticator", null);
				}
			}
			
			Operation headOp = p.getHead();
			if ( headOp != null )
			{
//				headOp.setProduces(Arrays.asList(MediaType.APPLICATION_JSON));
				operation = "HEAD";
				headOp.setVendorExtensions(getVendorExts(module, operation));
			}
			
			Operation getOp = p.getGet();
			if ( getOp != null )
			{
//				getOp.setProduces(Arrays.asList(MediaType.APPLICATION_JSON));
				operation = "GET";
				getOp.setVendorExtensions(getVendorExts(module, operation));
				if ( !isLogin )
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
				putOp.setVendorExtensions(getVendorExts(module, operation));
				if ( !isLogin )
				{
					putOp.addSecurity("TAP3Authenticator", null);
				}
			}
			
			Operation delOp = p.getDelete();
			if ( delOp != null )
			{
//				delOp.setProduces(Arrays.asList(MediaType.APPLICATION_JSON));
				operation = "DELETE";
				delOp.setVendorExtensions(getVendorExts(module, operation));
				if ( !isLogin )
				{
					delOp.addSecurity("TAP3Authenticator", null);
				}
			}
			
			Operation optionsOp = p.getOptions();
			if ( optionsOp != null )
			{
//				optionsOp.setProduces(Arrays.asList(MediaType.APPLICATION_JSON));
				operation = "OPTIONS";
				optionsOp.setVendorExtensions(getVendorExts(module, operation));
			}
			


		}
		
		swagger.setPaths(newPaths);

		// Dump out the new Swagger
		SwaggerSerializers ss = new SwaggerSerializers();
		SwaggerSerializers.setPrettyPrint(true);
		System.out.println("New swagger: ");
		ss.writeTo(swagger, null, null, null, MediaType.APPLICATION_JSON_TYPE, null, System.out);

		File swaggerFile = new File(outputPath);
		OutputStream os = new FileOutputStream(swaggerFile);
		ss.writeTo(swagger, null, null, null, MediaType.APPLICATION_JSON_TYPE, null, os);
		os.flush();
		os.close();
		
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
		
		if ( module.equals("TAP3FileAPI") && op.equals("POST"))
		{
			responses.put("type", "aws");
			HashMap<String, Object> templates = new HashMap<>();
			templates.put("application/pdf", "{\"operation\": \"create-file\",\"authentication\": \"$context.identity.cognitoIdentityId\",\"customerId\": \"$input.params('customerId')\",\"shipmentId\": \"$input.params('shipmentId')\",\"orderId\": \"$input.params('orderId')\",\"docType\": \"$input.params('docType')\",\"base64Image\": \"$input.body\"}");
			responses.put("requestTemplates", templates);
		}
		
		if ( module.equals("TAP3FileAPI") && op.equals("GET"))
		{
			responses.put("type", "aws");
			HashMap<String, Object> templates = new HashMap<>();
			templates.put("application/pdf", "{\"operation\": \"get-file\",\"authentication\": \"$context.identity.cognitoIdentityId\",\"customerId\": \"$input.params('customerId')\",\"shipmentId\": \"$input.params('shipmentId')\",\"orderId\": \"$input.params('orderId')\",\"docType\": \"$input.params('docType')\",\"base64Image\": \"$input.body\"}");
			responses.put("requestTemplates", templates);
		}
		
		return ret;
	}
}
