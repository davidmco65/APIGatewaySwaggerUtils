package com.mcdaniel.swagger;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Set;

import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;

public class GenerateJersey2Proxy {

	private String outputDir = null;
	
	public static void main(String[] args) {
		if ( args.length < 1 )
		{
			System.out.println("Usage: GenerateJersey2Proxy <output directory>");
			System.exit(0);
		}
		new GenerateJersey2Proxy(args);
	}

	public GenerateJersey2Proxy(String [] args)
	{
		outputDir = args[0];
		
		// Assume that all JARs to search on are the classpath already

		System.out.println("==============================\nStarting GenerateJersey2Proxy\n==============================\n");
		ClassLoader cl = this.getClass().getClassLoader();
		try
		{
			System.out.println("Getting classes...");
			Set<ClassPath.ClassInfo> classesInPackage = ClassPath.from(cl).getTopLevelClassesRecursive("com.microstar.tap3");
			for ( ClassPath.ClassInfo ci : classesInPackage )
			{
				if ( ci.getName().endsWith("Impl"))
				{
					System.out.println(ci.getName() + " : " + ci.getPackageName() + " : " + ci.getResourceName() + " : " + ci.getSimpleName());
					generateInterface(ci);
				}
			}
			System.out.println("Done.");
		}
		catch ( Exception ex )
		{
			ex.printStackTrace();
		}
	}
	
	private void generateInterface(ClassInfo ci) throws InstantiationException, IllegalAccessException, ClassNotFoundException
	{
		JavaClass cl = new JavaClass();
		System.out.println("Class: " + ci.getName());
		cl.setThePackage("com.microstar.tap3.api");
		Class theClazz = Class.forName(ci.getName(), false, this.getClass().getClassLoader());
		Method[] methods = theClazz.getDeclaredMethods();
		Annotation[] anns = theClazz.getAnnotations();

//		AnnotatedType [] annTypes = theClazz.getAnnotatedInterfaces();
//		AnnotatedType[] aa = theClazz.getAnnotatedInterfaces();
//		AnnotatedType bb = theClazz.getAnnotatedSuperclass();
//		Class<?>[] cc = theClazz.getClasses();
//		Annotation[] dd = theClazz.getDeclaredAnnotations();
//		Type[] ee = theClazz.getGenericInterfaces();
//		Method[] ff = theClazz.getMethods();
//		String gg = theClazz.getTypeName();
//		TypeVariable<?>[] hh = theClazz.getTypeParameters();

		Annotation pathAnn = null;
		for ( Annotation ann : anns )
		{
			if ( ann.toString().contains("javax.ws.rs.Path"))
			{
				pathAnn = ann;
			}
		}
		
		cl.setPathAnnotation(pathAnn);
		cl.setTheClassName(ci.getSimpleName().replaceAll("Impl", ""));
		cl.setTheMethods(methods);
		
		cl.writeFile(this.outputDir);
	}
}
