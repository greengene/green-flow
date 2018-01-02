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
public abstract class ClassTypeService<T>
{
	private short counter = 0;

	private BiMap<Class<? extends T>, Integer> typeIds = HashBiMap.create();
	private BiMap<Integer, Class<? extends T>> typeClasses = typeIds.inverse();

	synchronized private short getNextType()
	{
		return ++counter;
	}

	public Integer getTypeId(Class<? extends T> clazz)
	{
		try
		{
			return typeIds.get(clazz);
		} catch (NullPointerException e)
		{
			return null;
		}
	}

	public Class<? extends T> getTypeClass(int typeId)
	{
		return typeClasses.get(typeId);
	}

	public void registerType(Class<? extends T> clazz)
	{
		if (!typeIds.containsKey(clazz)) {
			int type = getNextType();
			typeIds.put(clazz, type);
		}
	}
}