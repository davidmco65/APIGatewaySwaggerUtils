package com.mcdaniel.swagger;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.util.ArrayList;


public class JavaClass {

	private String thePackage;
	private ArrayList<String> theImports;
	private String theClassName;
	private Method [] theMethods; 
	private Annotation pathAnnotation; 
	
	public void writeFile(String directory)
	{
		try
		{
			System.out.println("Writing file: " + directory + File.separator + theClassName + ".java");
			File outFile = new File(directory + File.separator + theClassName + ".java");
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outFile));
			
			StringBuffer sb = new StringBuffer();
			sb.append("package " + thePackage + ";\n\n");
			if ( theImports != null )
				for ( String imp : theImports )
				{
					sb.append("import " + imp + ";\n");
				}
			sb.append("\n\n");
			if ( this.pathAnnotation != null )
			{
				sb.append(fixAnnotation(this.pathAnnotation.toString()) + "\n");
			}
			sb.append("public interface " + theClassName + " {");
			sb.append("\n\n");
			for ( Method m : theMethods )
			{
				Annotation[] manns = m.getAnnotations();
				boolean jaxrsAnnotationPresent = false;
				for ( Annotation a : manns )
				{
					Class<? extends Annotation> at = a.annotationType();
					if ( a.toString().startsWith("@javax.ws.rs"))
					{
						sb.append(fixAnnotation(a.toString()) + "\n");
						jaxrsAnnotationPresent = true;
					}
				}
				if ( jaxrsAnnotationPresent )
				{
					Annotation [][] panns = m.getParameterAnnotations();
					sb.append("public " + m.getReturnType().getName() + " " + m.getName() + "(");
					boolean first = true;
					int pcount = 1;
					for ( Class pc : m.getParameterTypes() )
					{
						if ( !first )
							sb.append(", ");
						first = false;
						if ( panns.length >= pcount )	// Parameter has an annotation
						{
							for ( Annotation pa : panns[pcount-1])
							{
								sb.append(fixAnnotation(pa.toString()) + " ");
							}
						}
						sb.append(fixParamTypeName(pc.getName()) + " param" + pcount++);
					}
					sb.append(");\n\n");
				}
			}
			sb.append("\n}\n\n");
			
			bos.write(sb.toString().getBytes());
			bos.close();
			
		}
		catch ( Exception ex )
		{
			ex.printStackTrace();
			System.exit(1);
		}
	}

	private String fixParamTypeName(String name) 
	{
		if ( name.contains("$"))
			return name.replace('$', '.');
		return name;
	}

	private String fixAnnotation(String annotationStr)
	{
		if ( ! annotationStr.contains("="))
			return annotationStr;
		
		int idx = annotationStr.indexOf("=");
		int idx2 = annotationStr.indexOf(")");
		String ret = annotationStr.substring(0, idx+1) + "\"" + 
				annotationStr.substring(idx+1, idx2) + "\")";
		ret = ret.replace("[", "").replace("]", "");
		return ret;
	}
	
	public String getThePackage() {
		return thePackage;
	}
	public void setThePackage(String thePackage) {
		this.thePackage = thePackage;
	}
	public ArrayList<String> getTheImports() {
		if ( theImports == null )
			theImports = new ArrayList<>();
		return theImports;
	}
	public void setTheImports(ArrayList<String> theImports) {
		this.theImports = theImports;
	}
	public String getTheClassName() {
		return theClassName;
	}
	public void setTheClassName(String theClassName) {
		this.theClassName = theClassName;
	}
	public Method [] getTheMethods() {
		return theMethods;
	}
	public void setTheMethods(Method [] theMethods) {
		this.theMethods = theMethods;
	}

	public Annotation getPathAnnotation() {
		return pathAnnotation;
	}

	public void setPathAnnotation(Annotation pathAnnotation) {
		this.pathAnnotation = pathAnnotation;
	}
	
	
}
