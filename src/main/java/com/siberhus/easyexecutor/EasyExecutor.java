/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */
package com.siberhus.easyexecutor;

import it.sauronsoftware.cron4j.Scheduler;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.xml.DOMConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.siberhus.commons.converter.TypeConvertUtils;
import com.siberhus.commons.io.DirectoryFileUtils;
import com.siberhus.commons.io.FileLocker;
import com.siberhus.commons.lang.ClasspathUtils;
import com.siberhus.commons.lang.SystemProperties;
import com.siberhus.commons.properties.PropertiesUtil;
import com.siberhus.commons.util.ElapsedTimeUtils;
import com.siberhus.commons.util.ResourceUtils;
import com.siberhus.commons.xml.DataElement;
import com.siberhus.commons.xml.DomElementFinder;

/**
 * 
 * @author Hussachai Puripunpinyo (http://www.siberhus.com)
 * 
 */
public class EasyExecutor {
	
	public static enum LOG_IMPL {log4j,jdk}
	
	private static Logger logger;
	
	private static final Map<String,Properties> USER_PROPERTIES_MAP = new HashMap<String, Properties>();
	
	private static EasyExecutor instance;
	
	private ApplicationTask appTask;
	private Map<String, BeanExecutor> beanExecutorMap; 
	
	private boolean scheduled = false;
	private ApplicationInfo applicationInfo;
	private FileLocker fileLocker;
	private boolean singleInstance = false;
	private Locale locale = Locale.getDefault();
	private String commandLineArgs[];
	private File configurationFile;
	private Date executionTime;
	
	static{
		/*
		 * Fix Log4j "append to closed appender named [stdout]" issue.
		 * This work around is quite non-sense but it works!
		 */
		System.setProperty("log4j.configuration","");
	}
	
	private EasyExecutor(File configFile, String[] args) throws Exception{
		
		this.executionTime = new Date();
		this.configurationFile = configFile;
		this.commandLineArgs = args;
		
		DomElementFinder def = DomElementFinder.newInstance(configFile);
		
		this.singleInstance = def.getRootElement().getAttribute(Boolean.class,"single-instance");
//		boolean showTray = def.getRootElement().getAttribute(Boolean.class, "show-tray");
		this.locale = def.getRootElement().getAttribute(Locale.class,"locale");
		
		Locale.setDefault(getLocale());
		
		//=========== Recursive configure =============//
		configure(def);
		//=============================================//
		
		appTask = new ApplicationTask();
		DataElement appElem = def.findElement("application");
		
		String appId = appElem.getAttribute("id");
		String appMode = appElem.getAttribute("mode");
		
		String appName = def.getElement(appElem, "name").getValue(appId);
		String appVersion = def.getElement(appElem, "version").getValue("0.0");
		String appDesc = def.getElement(appElem, "description").getValue();
		
		//=========== System Properties ==================//
		System.setProperty("easyx.locale", locale.toString());
		System.setProperty("easyx.app.id",appId);
		System.setProperty("easyx.app.name", appName);
		System.setProperty("easyx.app.version", appVersion);
		
		applicationInfo = new ApplicationInfo();
		applicationInfo.setId(appId);
		applicationInfo.setMode(appMode);
		
		applicationInfo.setName(appName);
		applicationInfo.setVersion(appVersion);
		applicationInfo.setDescription(appDesc);
		
		//=========== Init Logger =======================//
		DataElement logElem = def.findElement(appElem, "logger");
		if(logElem!=null){
			LOG_IMPL logImpl = logElem.getAttribute(LOG_IMPL.class,"impl",LOG_IMPL.log4j);
			File logConfigFile = ResourceUtils.getFile(logElem.getAttribute("config"));
			if(logConfigFile.exists()){
				if(logImpl==LOG_IMPL.log4j){
					org.apache.log4j.Logger.getRootLogger().removeAllAppenders();
					if(logConfigFile.getName().endsWith(".xml")) {
						DOMConfigurator.configure(logConfigFile.getAbsolutePath());
					} else {
						PropertyConfigurator.configure(logConfigFile.getAbsolutePath());
					}
				}else if(logImpl==LOG_IMPL.jdk){
					System.setProperty("java.util.logging.config.file", logConfigFile.getAbsolutePath());
				}
			}
		}
		DataElement cronElem = def.findElement(appElem, "cron");
		if(cronElem!=null){
			scheduled = true;
			String cronExpression = cronElem.getAttribute("expression");
			cronExpression = CronEntries.resolveEntry(cronExpression);
			Scheduler scheduler = new Scheduler();
			scheduler.schedule(cronExpression, appTask);
			scheduler.start();
		}
		logger = LoggerFactory.getLogger(EasyExecutor.class);		
		logger.info("Allow multiple instances? {}", !singleInstance);
		logger.info("Default locale: {}",locale);
		
		//=========== Ensure only one instance is run at a time =========//
		ensureSingleInstance(appId);
		//===============================================================//
		
		beanExecutorMap = new LinkedHashMap<String, BeanExecutor>();
		
		List<DataElement> beanElems = def.findElements(appElem, "beans/bean");
		for(DataElement beanElem : beanElems){
			BeanExecutor be = createBeanExecutor(def, beanElem, args);
			beanExecutorMap.put(be.getId(), be);
		}
		
	}
	
	public static EasyExecutor getInstance(){
		return instance;
	}
	
	public boolean isScheduled() {
		return scheduled;
	}

	public BeanExecutor getBeanExecutor(String id){
		return beanExecutorMap.get(id);
	}
	
	public boolean isSingleInstance(){
		return singleInstance;
	}
	
	public Locale getLocale(){
		return locale;
	}
	
	public ApplicationInfo getApplicationInfo(){
		return applicationInfo;
	}
	
	public Properties getProperties(String name){
		return USER_PROPERTIES_MAP.get(name);
	}
	
	public String[] getCommandLineArgs(){
		return commandLineArgs;
	}
	
	public File getConfigurationFile(){
		return configurationFile;
	}
	
	public Date getExecutionTime(){
		return executionTime;
	}
	
	private void configure(DomElementFinder def) throws SAXException, IOException, ParserConfigurationException{
		
		List<DataElement> importElemList = def.getElements("import");
		for(DataElement importElem : importElemList){
			String location = importElem.getAttribute("location");
			File pConfigFile = ResourceUtils.getFile(location);
			DomElementFinder pDef = DomElementFinder.newInstance(pConfigFile);
			
			configure(pDef);
			
		}
		
		DataElement sysPropElem = def.findElement("system-properties");
		if(sysPropElem!=null){
			String sysPropFilePath = sysPropElem.getAttribute("location");
			if(sysPropFilePath!=null){
				Properties sysProp = PropertiesUtil.create(sysPropFilePath);
				overridePropeties(System.getProperties(), sysProp);
			}
			for(DataElement pElem : def.getElements(sysPropElem, "property")){
				String key = pElem.getAttribute("key");
				String value = pElem.getValue();
				System.getProperties().setProperty(key,value);
			}
		}
		
		List<DataElement> userPropElemList = def.getElements("user-properties");
		for(DataElement userPropElem : userPropElemList){
			String userPropName = userPropElem.getAttribute("name");
			Properties mainUserProp = USER_PROPERTIES_MAP.get(userPropName);
			if(mainUserProp==null){
				mainUserProp = new Properties();
				USER_PROPERTIES_MAP.put(userPropName,mainUserProp);
			}
			String parentUserPropName = userPropElem.getAttribute("extends");
			if(parentUserPropName!=null){
				Properties parentUserProp = USER_PROPERTIES_MAP.get(parentUserPropName);
				if(parentUserProp!=null){
					overridePropeties(mainUserProp, parentUserProp);
				}
			}
			String userPropFilePath = userPropElem.getAttribute("location");
			if(userPropFilePath!=null){
				File userPropFile = ResourceUtils.getFile(userPropFilePath);
				Properties userProp = PropertiesUtil.create(userPropFile);
				overridePropeties(mainUserProp, userProp);
			}
			for(DataElement pElem : def.getElements(userPropElem, "property")){
				String key = pElem.getAttribute("key");
				String value = pElem.getValue();
				mainUserProp.setProperty(key,value);
			}
		}
		
		DataElement classpathElem = def.findElement("classpath");
		if(classpathElem!=null){
			List<DataElement> classesElemList = def.getElements(classpathElem, "classes");
			for(DataElement classesElem : classesElemList){
				File classesPathDir = classesElem.getAttribute(File.class,"path");
				if(classesPathDir.exists()){
					ClasspathUtils.addClassesDir(classesPathDir);
				}
			}
			List<DataElement> libElemList = def.getElements(classpathElem, "lib");
			for(DataElement libElem : libElemList){
				File libPathDir = libElem.getAttribute(File.class,"path");
				if(libPathDir.exists()){
					ClasspathUtils.addLibDir(libPathDir);
				}
			}
		}
		
	}
	
	private BeanExecutor createBeanExecutor(DomElementFinder def
			, DataElement beanElem, String args[]) throws InstantiationException, IllegalAccessException, SecurityException, NoSuchMethodException {
		
		BeanExecutor be = new BeanExecutor();
		
		String beanId = beanElem.getAttribute("id");
		be.setId(beanId);
		Class<?> beanClass = beanElem.getAttribute(Class.class,"class");
		be.setTargetClass(beanClass);
		be.setTargetObject(beanClass.newInstance());
		
		List<DataElement> methodElems = def.findElements(beanElem,"method");
		
		for(DataElement methodElem : methodElems){
			if(methodElem!=null){
				String methodName = methodElem.getAttribute("name");
				Map<String, String> argMap = new HashMap<String, String>();
				for(int i=0;i<args.length;i++){
					argMap.put(String.valueOf(i), args[i]);
				}
				
				List<DataElement> argElemList = def.findElements(methodElem,"arg");
				if(argElemList!=null){
					int argSize = argElemList.size();
					Class<?> argTypes[] = new Class[argSize];
					Object argValues[] = new Object[argSize]; 
					for(int i=0; i< argSize; i++){
						DataElement paramElem = argElemList.get(i);
						Class<?> argType = paramElem.getAttribute(Class.class, "type");
						argTypes[i] = argType;
						String argValueStr = paramElem.getValue();
						argValueStr = StrSubstitutor.replace(argValueStr, argMap);
						argValues[i] = TypeConvertUtils.convert(argValueStr,getLocale(), argType);
					}
					be.addMethodInvoker(methodName, argTypes, argValues);
				}else{
					be.addMethodInvoker(methodName,null,null);
				}
			}else{
				Object passedArgs[] = { args };// Create the actual argument array
				be.addMethodInvoker("main", new Class[]{String[].class}, passedArgs);
			}
		}
		
		return be;
	}
	
	private void ensureSingleInstance(final String appId){
		if(!isSingleInstance()){
			logger.info("This appId: '{}' can be run in multiple instances.",appId);
			return;
		}
		fileLocker = new FileLocker();
		File eeDir = new File(SystemProperties.USER_HOME+File.separator+".easyexecutor");
		if(!eeDir.exists()){
			eeDir.mkdir();
		}
		File lockFile = DirectoryFileUtils.getFile(eeDir, appId+".lck");
		logger.debug("Locking file for: '{}'",appId);
		fileLocker.lock(lockFile);
		
		if(!fileLocker.canLock()){
			logger.error("Another instance is running.");
			System.exit(1);
		}
		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run(){
				logger.debug("Release file locking for: '{}'",appId);
				fileLocker.release();
			}
		});
	}
	
	private void overridePropeties(Properties main, Properties overrider){
		for(Object keyObj : overrider.keySet()){
			String key = (String)keyObj;
			String value = overrider.getProperty(key);
			main.setProperty(key, value);
		}
	}
	
	class ApplicationTask implements Runnable{
		
		@Override
		public void run() {
			logger.info("Executing Application: {}",getApplicationInfo());
			ElapsedTimeUtils.start("app");
			for(Map.Entry<String, BeanExecutor> entry : beanExecutorMap.entrySet()){
				BeanExecutor be = entry.getValue();
				try {
					be.execute();
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
			logger.info("Application '{}' consumes {}"
					,getApplicationInfo(),ElapsedTimeUtils.showElapsedTime("app"));
		}
	}
	
	public static void main(String[] args)throws Throwable {
		
		String configFilePath = args[0];
		File configFile = ResourceUtils.getFile(configFilePath);
		EasyExecutor.instance = new EasyExecutor(configFile,args);
		if(!EasyExecutor.instance.isScheduled()){
			EasyExecutor.instance.appTask.run();
		}
	}
	
}
