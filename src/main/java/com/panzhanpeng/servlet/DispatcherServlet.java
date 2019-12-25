package com.panzhanpeng.servlet;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.panzhanpeng.annotation.SpringAutoWried;
import com.panzhanpeng.annotation.SpringController;
import com.panzhanpeng.annotation.SpringRequestMapping;
import com.panzhanpeng.annotation.SpringRequestParam;
import com.panzhanpeng.annotation.SpringService;
import com.panzhanpeng.controller.TestController;


public class DispatcherServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	
	private List<String> classNames = new ArrayList<String>();
	private Map<String, Object> beans = new HashMap<String, Object>();
	private Map<String, Object> urlMappingMap = new HashMap<String, Object>();
	
	/**
	 * 1.扫描路径，获取特殊注解类
	 * 2.通过反射创建类实例，并放进map对象中
	 * 3.@Autowried注解能get出来
	 * 4.urlhandlermapping请求路径映射
	 */
	public void init(ServletConfig config) {
		scanPackage("com.panzhanpeng");
		doInstance();
		doAutowried();
		urlMapping();
	}
	
	protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
		this.doPost(httpServletRequest, httpServletResponse);
	}
	
	protected void doPost(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
		String uri = httpServletRequest.getRequestURI();
		String context = httpServletRequest.getContextPath();
		String path = uri.replace(context, "");
		Method method = (Method) urlMappingMap.get(path);
		String[] beanKeys = path.split("\\/");
		String beanKey = beanKeys[1];
		TestController tc = (TestController) beans.get(beanKey);
		Object[] args = getArgs(httpServletRequest, httpServletResponse, method);
		try {
			method.invoke(tc, args);
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * 扫描类路径，获取特殊注解类
	 * @param packageString
	 */
	private void scanPackage(String basePackage) {
		URL url = this.getClass().getClassLoader().getResource("/" + basePackage.replaceAll("\\.", "/"));
		String fileString = url.getFile();
		File file = new File(fileString);
		String[] paths = file.list();
		for (String path : paths) {
			File filePath = new File(fileString + path);
			if (filePath.isDirectory()) {
				scanPackage(basePackage + "." + path);
			} else {
				classNames.add(basePackage + "." + filePath.getName());
			}
		}
	}
	
	/**
	 * 将获取到的@springController和@springService注解类实例化，并放进map容器中
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	private void doInstance() {
		for (String className : classNames) {
			className = className.replace(".class", "");
			try {
				Class<?> clazz = Class.forName(className);
				if (clazz.isAnnotationPresent(SpringController.class)) {
					Object instance = clazz.newInstance();
					String allName = instance.getClass().getName();
					String[] beanKeyNames = allName.split("\\.");
					String beanKeyName = beanKeyNames[beanKeyNames.length-1];
					String beanFirstName = beanKeyName.substring(0, 1).toLowerCase();
					beans.put(beanFirstName.toLowerCase() + beanKeyName.substring(1), instance);
				}
				if (clazz.isAnnotationPresent(SpringService.class)) {
					Object instance = clazz.newInstance();
					String allName = instance.getClass().getName();
					String[] beanKeyNames = allName.split("\\.");
					String beanKeyName = beanKeyNames[beanKeyNames.length-1].replace("Impl", "");
					String beanFirstName = beanKeyName.substring(0, 1).toLowerCase();
					beans.put(beanFirstName.toLowerCase() + beanKeyName.substring(1), instance);
				}
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
	}
	
	/**
	 * 自动装备，扫描到@autowried的类，把相应的实例设置到类中
	 */
	private void doAutowried() {
		for (Map.Entry<String, Object> entry : beans.entrySet()) {
			Object instance = entry.getValue();
			if (instance.getClass().isAnnotationPresent(SpringController.class)) {
				Field[] fields = instance.getClass().getDeclaredFields();
				for (Field field : fields) {
					if (field.isAnnotationPresent(SpringAutoWried.class)) {
						String beanName = field.getName();
						Object value = beans.get(beanName);
						field.setAccessible(true);
						try {
							field.set(instance, value);
						} catch (IllegalArgumentException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IllegalAccessException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		}
	}
	
	/**
	 * 获取controller中的@requestMapping路径,并放进map容器中
	 */
	private void urlMapping() {
		for (Map.Entry<String, Object> entry : beans.entrySet()) {
			Object instance = entry.getValue();
			if (instance.getClass().isAnnotationPresent(SpringController.class)) {
				SpringRequestMapping sr = instance.getClass().getAnnotation(SpringRequestMapping.class);
				String classPath = sr.value();
				Method[] methods = instance.getClass().getDeclaredMethods();
				for (Method method : methods) {
					if (method.isAnnotationPresent(SpringRequestMapping.class)) {
						SpringRequestMapping sr2 = method.getAnnotation(SpringRequestMapping.class);
						String methodPath = sr2.value();
						urlMappingMap.put(classPath + methodPath, method);
					}
				}
			}
		}
	}
	
	/**
	 * 获取方法中的参数
	 * @param request
	 * @param response
	 * @param method
	 * @return
	 */
	private static Object[] getArgs(HttpServletRequest request, HttpServletResponse response, Method method) {
		Class<?>[] paramClasses = method.getParameterTypes();
		Object[] args = new Object[paramClasses.length];
		int args_i = 0;
		int index = 0;
		for (Class<?> paramClass : paramClasses) {
			if (ServletRequest.class.isAssignableFrom(paramClass)) {
				args[args_i++] = request;
			}
			if (ServletResponse.class.isAssignableFrom(paramClass)) {
				args[args_i++] = response;
			}
			Annotation[] paramAns = method.getParameterAnnotations()[index];
			if (paramAns.length > 0) {
				for (Annotation paramAn : paramAns) {
					if (SpringRequestParam.class.isAssignableFrom(paramAn.getClass())) {
						SpringRequestParam srp = (SpringRequestParam) paramAn;
						args[args_i++] = request.getParameter(srp.value());
					}
				}
			}
			index++;
		}
		return args;
	}

}
