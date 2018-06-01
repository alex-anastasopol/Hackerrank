package ro.cst.tsearch.templates;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject.Kind;

import org.apache.commons.lang.StringUtils;

/**
 * This class facilitates the compilation of java code that is stored only in memory
 * See main() method for an example
 * Not used currently
 * @author Mihai D.
 */
public class MemoryCompiler {
	private String className = ""; 
	private String contents = "";
	private Map<String, JavaFileObject> store = new HashMap<String, JavaFileObject>();
	private DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
	
	public MemoryCompiler(String className, String contents) {
		this.className = className;
		this.contents = contents;
	}
	
	public byte[] compileAndReturnBytecode() {
		try {
			CompilationTask task = makeCompilerTask(new StringJFO(className+".java",contents), store);
			boolean hasCompiled = task.call();
			if (!hasCompiled) {
				System.err.println("Compilation failed");
				System.err.println(getErrors());
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		return ((ByteArrayJFO)store.get(className)).getByteArray();
	}
	
	private CompilationTask makeCompilerTask(StringJFO src, Map<String, JavaFileObject> store) {
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
	    
		// access the compiler provided
		if (compiler == null) {
			System.err.println("Compiler not found");
		}

		// create a collector for diagnostic messages
		// create a manager for the Java file objects
		StandardJavaFileManager fileMan = compiler.getStandardFileManager(diagnostics, null, null);
		ByteJavaFileManager jfm = new ByteJavaFileManager(fileMan, store);

		/* create a compilation task using the supplied file manager,
		diagnostic collector, and applied to the string Java
		file object (in a list) */
		
		return compiler.getTask(null, jfm, diagnostics,null, null, Arrays.asList(src));
	}
	
	private String getErrors() {
		   String ret = "";
	       for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
	          ret += "Error on line "+diagnostic.getLineNumber()+" in " +diagnostic.getSource().toUri()+ " \n";
	       }
	       return ret;
	}
	
	private static class StringJFO extends SimpleJavaFileObject {
		private String codeStr = null;
	
		public StringJFO(String uri, String codeStr) throws Exception { 
			super(new URI(uri), Kind.SOURCE); 
			this.codeStr = codeStr;
		}
	
		public CharSequence getCharContent(boolean errs) throws IOException { 
			return codeStr; 
		}
	}
	
	private static class ByteArrayJFO extends SimpleJavaFileObject {
		private ByteArrayOutputStream baos = null;
		
		public ByteArrayJFO(String className, Kind kind) throws Exception { 
			super( new URI(className), kind); 
		}
		
		public InputStream openInputStream() throws IOException { 
			return new ByteArrayInputStream(baos.toByteArray()); 
		}
		
		public OutputStream openOutputStream() throws IOException { 
			return baos = new ByteArrayOutputStream(); 
		}
		
		public byte[] getByteArray() { 
			return baos.toByteArray(); 
		}
	}
	
	private static class ByteJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {
		private Map<String, JavaFileObject> store = new HashMap<String, JavaFileObject>();
		// maps class names to JFOs containing the classes' byte codes
		public ByteJavaFileManager(StandardJavaFileManager fileManager, Map<String, JavaFileObject> str) { 
			super(fileManager);
			store = str;
		} 
		
		public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind, FileObject sibling) throws IOException {
			try {
				JavaFileObject jfo = new ByteArrayJFO(className, kind);
				store.put(className, jfo);
				return jfo;
			} catch(Exception e) { 
				System.err.println(e);
				return null;
			}
		}
	}
	
	
	public static void execute(String program,String className) {
		execute(program,className,"main",new Class[]{String[].class}, new Object[]{new String[]{className}});
	}
	
	public static void execute(String program,String className, String method, Class[] argumentsTypes, Object[] arguments) {
		if(StringUtils.isEmpty(program)) {
			return;
		}
		program = program.trim();
		
		/* This is the way to compile a class in memory */
		final byte[] bytecode = new MemoryCompiler(className,program).compileAndReturnBytecode();
		
		/* Load the class, and invoke a method to see if it worked */
		ClassLoader cl 		= null;
		try {
		cl = new ClassLoader() {
	         public Class findClass(String name) {
	             return defineClass(name, bytecode, 0, bytecode.length);
	         }
		};
		
		Class templateAttachedClass = cl.loadClass("Test");
		Object obj = templateAttachedClass.newInstance();
		Method m = templateAttachedClass.getMethod(method, argumentsTypes);
		m.invoke(obj, arguments);
		} catch (Exception e){e.printStackTrace();}
	}
	
	public static void main(String... args) {
		String program = 
			"public class Test {\n" +
			"	public static void test() {\n" +
			"   	System.err.println('y');" +
			"   }" +
			"	public static void main(String... args) {\n" +
			"   	System.err.println('x');" +
			"   }" +
			"}";
		
		execute(program,"Test");
	}

}
