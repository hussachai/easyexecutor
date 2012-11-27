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

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author Hussachai Puripunpinyo (http://www.siberhus.com)
 * 
 */
public class CronEntries {
	
	private static final Map<String, String> ENTRY_MAP;
	
	static{
		ENTRY_MAP = new HashMap<String, String>();
		ENTRY_MAP.put("yearly", "0 0 1 1 *");
		ENTRY_MAP.put("annually", "0 0 1 1 *");
		ENTRY_MAP.put("monthly", "0 0 1 * *");
		ENTRY_MAP.put("weekly", "0 0 * * 0");
		ENTRY_MAP.put("daily", "0 0 * * *");
		ENTRY_MAP.put("midnight", "0 0 * * *");
		ENTRY_MAP.put("hourly", "0 * * * *");
	}
	
	public static String resolveEntry(String entry){
		if(entry==null){
			throw new IllegalArgumentException("Cron Entry is required.");
		}
		entry = entry.trim();
		if(entry.startsWith("@")){
			String cronExpression = ENTRY_MAP.get(entry.substring(1));
			if(cronExpression!=null){
				return cronExpression;
			}else{
				throw new IllegalArgumentException("Cron Entry: "+entry+" was not mapped.");
			}
		}
		return entry;
	}
	
}
