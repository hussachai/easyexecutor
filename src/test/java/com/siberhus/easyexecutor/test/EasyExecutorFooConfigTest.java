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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.siberhus.easyexecutor.ApplicationInfo;
import com.siberhus.easyexecutor.EasyExecutor;

/**
 * 
 * @author Hussachai Puripunpinyo (http://www.siberhus.com)
 * 
 */
public class EasyExecutorFooConfigTest {
	
	@BeforeClass
	public static void init() throws Throwable{
		String args[] = new String[]{"classpath:foo-config.xml"
				,"cat","dog","donkey"};
		EasyExecutor.main(args);
	}
	
	@Test
	public void testUserProperties()throws Exception{
		//Use user properties from another file via import channel
		Properties fooProps = EasyExecutor.getInstance().getProperties("foo");
		Assert.assertEquals("Foo", fooProps.getProperty("name"));
	}
	
	
	@Test
	public void testApplicationInfo(){
		ApplicationInfo appInfo = EasyExecutor.getInstance().getApplicationInfo();
		Assert.assertEquals("foo-app", appInfo.getId());
		Assert.assertEquals("test", appInfo.getMode());
		Assert.assertEquals("Foo Application", appInfo.getName());
		Assert.assertEquals("0.1", appInfo.getVersion());
		Assert.assertEquals("Trivial Description", appInfo.getDescription());
	}
	
	@Test
	public void testLocale() throws ParseException{
		
		EasyExecutor ee = EasyExecutor.getInstance();
		
		Assert.assertEquals("en", ee.getLocale().getLanguage());
		Assert.assertEquals("US", ee.getLocale().getCountry());
		
		Locale lth = new Locale("en","US");
		Assert.assertEquals(lth, ee.getLocale());
		Assert.assertEquals(lth, Locale.getDefault());
		
		SimpleDateFormat sdfEn = new SimpleDateFormat("dd/MM/yyyy");
		Date dateEn = sdfEn.parse("29/12/1982");
		SimpleDateFormat sdfTh = new SimpleDateFormat("dd/MM/yyyy",new Locale("th","TH"));
		Date dateTh = sdfTh.parse("29/12/2525");
		Assert.assertEquals(dateEn, dateTh);
		
	}
}
