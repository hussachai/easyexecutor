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
package com.siberhus.easyexecutor.test;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.siberhus.easyexecutor.EasyExecutor;

/**
 * 
 * @author Hussachai Puripunpinyo (http://www.siberhus.com)
 * 
 */
public class EasyExecutorTestAppConfigTest {
	
	static String args[] = new String[]{"classpath:testapp-config.xml"
			,"1","2","3"};
	
	@BeforeClass
	public static void init() throws Throwable{
		EasyExecutor.main(args);
	}
	
	@Test
	public void testLastCalled(){
		FooBarBean bean = (FooBarBean)EasyExecutor.getInstance()
			.getBeanExecutor("foobar.doSomething").getTargetObject();
		String lastCalled = bean.getLastCalled();
		Assert.assertEquals("doSomething", lastCalled);
	}
	
	@Test
	public void printExecutionInfo(){
		System.out.println(EasyExecutor.getInstance().getLocale());
		System.out.println("Command-Line Args: "+Arrays.toString(EasyExecutor.getInstance().getCommandLineArgs()));
		System.out.println("Configuration File: "+EasyExecutor.getInstance().getConfigurationFile());
		System.out.println("Execution Time: "+EasyExecutor.getInstance().getExecutionTime());
	}
}
