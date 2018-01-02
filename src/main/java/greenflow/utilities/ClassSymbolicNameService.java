/*
   Copyright (c) 2018 GreenGene. (https://github.com/greengene/) All Rights Reserved.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package greenflow.utilities;

import org.springframework.stereotype.Service;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

@Service
public abstract class ClassSymbolicNameService<T>
{
	private BiMap<Class<? extends T>, String> classSymbolicNames = HashBiMap.create();
	private BiMap<String, Class<? extends T>> symbolicNameClasses = classSymbolicNames.inverse();	

	abstract protected void syncClassTypeService(Class<? extends T> clazz);

	public String getClassSymbolicName(Class<? extends T> clazz)
	{
		return classSymbolicNames.get(clazz);
	}

	public Class<? extends T> getSymbolicNameClass(String symbolicName)
	{
		return symbolicNameClasses.get(symbolicName);
	}

	synchronized public void registerSymbolicName(Class<? extends T> clazz, String symbolicName)
	{
		if (!classSymbolicNames.containsKey(clazz)) {
			classSymbolicNames.put(clazz, symbolicName);
			syncClassTypeService(clazz);
		}
	}
}