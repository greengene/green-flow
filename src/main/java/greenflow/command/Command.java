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

package greenflow.command;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import greenflow.utilities.CommandTypeService;
import greenflow.workunit.WorkUnit;

import javax.annotation.PostConstruct;

public abstract class Command<T> {
	@Autowired
	private CommandTypeService commandTypeService;

	private List<String> parameters;

	private WorkUnit wrapperWorkUnit;

	private String assignTo;

	@PostConstruct
	protected void init()
	{
		Class<? extends Command<?>> clazz = (Class<? extends Command<?>>) getClass();
		commandTypeService.registerType(clazz);
	}

	public abstract WorkUnitResult<T> execute();

	public abstract String getSymbolicName();

	public int getTypeId() {
		return commandTypeService.getTypeId((Class<? extends Command<T>>)getClass());
	}

	public WorkUnit getWrapperWorkUnit() {
		return wrapperWorkUnit;
	}

	public void setWrapperWorkUnit(WorkUnit wrapperWorkUnit) {
		this.wrapperWorkUnit = wrapperWorkUnit;
	}

	public String getAssignTo()
	{
		return assignTo;
	}

	public void setAssignTo(String resultVariable)
	{
		this.assignTo = resultVariable;
	}

	public List<String> getParameters()
	{
		if (parameters == null)
		{
			setParameters(new ArrayList<String>());
		}		
		return parameters;
	}

	private void setParameters(List<String> parameters)
	{
		this.parameters = parameters;
	}
}