package ro.cst.tsearch.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class ReflectionUtils {

	private ReflectionUtils(){}
	
	/**
	 * Decide if a class represents a primitive
	 * @param clazz
	 * @return
	 */
	public static boolean isPrimitive(Class<?> clazz){
		return
		 	clazz == java.lang.Boolean.TYPE ||  
			clazz == java.lang.Character.TYPE ||  
			clazz == java.lang.Byte.TYPE || 
			clazz == java.lang.Short.TYPE ||
			clazz == java.lang.Integer.TYPE || 
			clazz == java.lang.Long.TYPE ||
			clazz == java.lang.Float.TYPE  ||
			clazz == java.lang.Double.TYPE ||
			clazz == java.lang.Void.TYPE;  
	}
	
	/**
	 * Nullify all reference fields of an object, so that they can be garbage collected
	 * @param obj
	 */
	public static void nullifyReferenceFields(Object obj){
		
		if(true){
			return;
		}
		
		if(obj == null){
			return;
		}
		
		for(Field field: obj.getClass().getDeclaredFields()){
			
			// make field accessible
			if(!field.isAccessible()){
				field.setAccessible(true);
			}
			// check for final and static
			int modif = field.getModifiers();
			if(Modifier.isFinal(modif) || Modifier.isStatic(modif)){
				continue;
			}
			// check for primitive
			if(isPrimitive(field.getType())){
				continue;
			}
			// null the field
			try {
				field.set(obj, null);
			}catch(IllegalAccessException e){
				e.printStackTrace();
			}
		}
	}
	
}
