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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.siberhus.commons.util.ElapsedTimeUtils;


/**
 * 
 * @author Hussachai Puripunpinyo (http://www.siberhus.com)
 * 
 */
public class BeanExecutor {
	
	private final Logger logger = LoggerFactory.getLogger(BeanExecutor.class);
	
	private String id;
	
	private Class<?> targetClass;
	private Object targetObject;
	
	private List<MethodInvoker> methodInvokerList = new ArrayList<MethodInvoker>();
	
	public void execute() throws Throwable{
		logger.info("Executing bean: {} ({})",id,targetObject);
		logger.info("Total methods of bean '{}' = {}",targetObject,methodInvokerList.size());
		ElapsedTimeUtils.start("bean");
		for(MethodInvoker mi : methodInvokerList){
			logger.info("Invoking method: {}",mi.getTargetMethod());
			logger.info("Arguments: {}", Arrays.toString(mi.getArguments()));
			ElapsedTimeUtils.start("method");
			mi.invoke();
			logger.info("Method '{}' consumes : {}"
					,mi.getTargetMethod(),ElapsedTimeUtils.showElapsedTime("method"));
		}
		logger.info("Bean: {} ({}) consumes : {}"
				,new Object[]{id, targetObject,ElapsedTimeUtils.showElapsedTime("bean")});
	}
	
	public void addMethodInvoker(String methodName, Class<?> argTypes[], Object argValues[]) throws SecurityException, NoSuchMethodException{
		
		MethodInvoker mi = new MethodInvoker(getTargetObject());
		if(argTypes!=null){
			mi.setTargetMethod(getTargetClass().getMethod(methodName, argTypes));
		}else{
			mi.setTargetMethod(getTargetClass().getMethod(methodName));
		}
		
		mi.setArguments(argValues);
		
		methodInvokerList.add(mi);
		
	}
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}

	public Class<?> getTargetClass() {
		return targetClass;
	}

	public void setTargetClass(Class<?> targetClass) {
		this.targetClass = targetClass;
	}

	public Object getTargetObject() {
		return targetObject;
	}

	public void setTargetObject(Object targetObject) {
		this.targetObject = targetObject;
	}
	public static void main(String[] args) {
		System.out.println(Arrays.toString((String[])null));
	}
	
}
