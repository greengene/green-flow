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

package greenflow.workunit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import greenflow.predicate.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import greenflow.command.Command;
import greenflow.container.interaction.TargetContainerElement;
import greenflow.exception.WorkflowExecutionSuspensionException;
import greenflow.flowcontroller.FlowController;
import greenflow.workflow.Workflow;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;

public abstract class WorkUnit
{
	private static final Logger logger = LoggerFactory.getLogger(WorkUnit.class);

	private Workflow workflow;

	private long id;

	private Map<String, Object> variables;

	private boolean returnAtCompletion = false;

	private List<String> returnedVariables;

	private boolean persistAfterReturn = true;

	private TargetContainerElement<WorkUnit> targetContainer;

	public WorkUnit() {}

	@Override
	public String toString()
	{
		return getBreadcrumbId() + " (" + getTargetContainer().getId() + ")";
	}

	public List<WorkUnit> getChildWorkUnits()
	{
		List<WorkUnit> childWorkUnits = new ArrayList<WorkUnit>(getTargetContainer().getChildren());

		Collections.sort(childWorkUnits, new Comparator<WorkUnit>() {
				@Override
				public int compare(WorkUnit o1, WorkUnit o2) {
					return new Long(o1.getTargetContainer().getId()).compareTo(new Long(o2.getTargetContainer().getId()));
				}
			});
		return childWorkUnits;
	}

	public List<WorkUnit> getDescendants()
	{
		return getTargetContainer().getDescendants();
	}

	public WorkUnit getParentWorkUnit()
	{
		return getTargetContainer().getParent();
	}

	public String getBreadcrumbId()
	{
		return getTargetContainer().getBreadcrumbId();
	}

	public WorkUnit getPreviousSiblingWorkUnit()
	{
		List<WorkUnit> childWorkUnits = getParentWorkUnit().getChildWorkUnits();
		ListIterator<WorkUnit> listIterator = childWorkUnits.listIterator(childWorkUnits.indexOf(this));
		if (listIterator.hasPrevious())
		{
			return listIterator.previous();
		}
		return null;
	}

	public WorkUnit getNextSiblingWorkUnit()
	{
		List<WorkUnit> childWorkUnits = getParentWorkUnit().getChildWorkUnits();
		ListIterator<WorkUnit> listIterator = childWorkUnits.listIterator(childWorkUnits.indexOf(this));
		if (listIterator.hasNext())
		{
			return listIterator.next();
		}
		return null;
	}

	public void addChildWorkUnit(WorkUnit childWorkUnit)
	{
		childWorkUnit.setWorkflow(getWorkflow());		

		if (getCommand() != null)
		{
			WorkUnit firstChildWorkUnit = new WorkUnit(getCommand());
			setCommand(null);
			addChildWorkUnit(firstChildWorkUnit);
		}

		childWorkUnit.setTargetContainer(getTargetContainer().addChildWorkUnit(childWorkUnit));

		childWorkUnit.getTargetContainer().setId(childWorkUnit.getId());
	}

	public abstract void execute();

	public void execute_REMOVE()
	{
		beforeExecution();

		if (isFlowController())
		{
			getFlowController().controlFlow();
		}

		else if (isCommand())
		{
			logger.debug("Executing: Command (" + getCommand().getSymbolicName() + "), id = " +  getBreadcrumbId() + " (" + getTargetContainer().getId() + ")");

			if (StringUtils.isNotBlank(getCommand().getAssignTo()) && lookUpVariableScopeWorkUnit(getCommand().getAssignTo()) != null)
			{
				WorkUnit variableScopeWorkUnit = lookUpVariableScopeWorkUnit(getCommand().getAssignTo());

				variableScopeWorkUnit.getVariables().put(getCommand().getAssignTo(),  getCommand().execute().getData());

				if (getWorkflow().getDirtyVariables().get(variableScopeWorkUnit) == null)
				{
					getWorkflow().getDirtyVariables().put(variableScopeWorkUnit, new HashSet<String>());
				}
				getWorkflow().getDirtyVariables().get(variableScopeWorkUnit).add(getCommand().getAssignTo());

				logger.debug("Assigning value to variable! workunit: " + variableScopeWorkUnit.getBreadcrumbId() + ", variable: " + getCommand().getAssignTo() + ", value: " + variableScopeWorkUnit.getVariables().get(getCommand().getAssignTo()));
			}
			else
			{
				getCommand().execute();

				logger.debug("Command: done (" + getCommand().getSymbolicName() + ")");
			}
		}		

		else
		{
			if (getWorkflow().getExecutionSuspensionPoint() != null)
			{
				if (!getWorkflow().getExecutionSuspensionPoint().equals(getBreadcrumbId()))
				{
					logger.debug("Traversing while looking up suspension point: Container Block, id = " + getBreadcrumbId() + " (" + getId() + ")");

					for (WorkUnit workUnit :
							getChildWorkUnits().subList(
								Integer.parseInt(Splitter.on('.').splitToList(getWorkflow().getExecutionSuspensionPoint()).get(CharMatcher.is('.').countIn(getBreadcrumbId()) + 1)) - 1,
								getChildWorkUnits().size())
							)
					{
						workUnit.execute();
					}
				}
				else
				{
					logger.debug("Reached suspension point: " + getBreadcrumbId() + " - Starting Execution...");
				}
			}

			else
			{
				logger.debug("Executing: Container Block, id = " + getBreadcrumbId() + " (" + getId() + ")");

				for (WorkUnit workUnit : getChildWorkUnits())
				{
					workUnit.execute();
				}
			}
		}

		if (isReturnAtCompletion())
		{
			if (getWorkflow().getExecutionSuspensionPoint() != null && getWorkflow().getExecutionSuspensionPoint().equals(getBreadcrumbId()))
			{
				getWorkflow().setExecutionSuspensionPoint(null);
			}
			else if (getWorkflow().getExecutionSuspensionPoint() == null)
			{
				doReturn();
			}
		}

		afterExecution();
	}

	public void beforeExecution()
	{
	}

	public void afterExecution()
	{
	}

	public void doReturn()
	{
		String breadcrumbId = getBreadcrumbId();

		logger.debug("Suspending on workunit: " + breadcrumbId);

		getWorkflow().setExecutionSuspensionPoint(breadcrumbId);

		throw new WorkflowExecutionSuspensionException(breadcrumbId);
	}

	public Object lookUpVariableValue(String variable)
	{
		if (variable.matches("^\'(\\w)*\'$")) 
		{
			return CharMatcher.is('\'').removeFrom(variable);
		}
		else
		{
			WorkUnit workUnit =  lookUpVariableScopeWorkUnit(variable);

			if (workUnit != null)
			{
				return workUnit.getVariables().get(variable);
			}
		}
		return null;
	}

	public WorkUnit lookUpVariableScopeWorkUnit(String variable)
	{
		if (variable != null)
		{
			for (WorkUnit worUnit = this; worUnit != null; worUnit = worUnit.getParentWorkUnit())
			{
				if (worUnit.getVariables().containsKey(variable))
				{
					return worUnit;
				}
			}
		}
		return null;
	}

	public TargetContainerElement<WorkUnit> getTargetContainer()
	{
		return targetContainer;
	}

	public void setTargetContainer(TargetContainerElement<WorkUnit> targetContainer)
	{
		this.targetContainer = targetContainer;
	}

	public Workflow getWorkflow()
	{
		return workflow;
	}

	public void setWorkflow(Workflow workflow)
	{
		this.workflow = workflow;
	}

	public long getId()
	{
		return id;
	}

	public void setId(long id)
	{
		this.id = id;
	}

	public Map<String, Object> getVariables()
	{
		if (variables == null)
		{
			setVariables(new LinkedHashMap<String, Object>());
		}		
		return variables;
	}

	public void addVariable()
	{
	}

	public void removeVariable()
	{
	}

	private void setVariables(Map<String, Object> variables)
	{
		this.variables = variables;
	}

	public boolean isReturnAtCompletion()
	{
		return returnAtCompletion;
	}

	public void setReturnAtCompletion(boolean returnAtCompletion)
	{
		this.returnAtCompletion = returnAtCompletion;
	}

	public List<String> getReturnedVariables()
	{
		if (returnedVariables == null)
		{
			setReturnedVariables(new ArrayList<String>());
		}
		return returnedVariables;
	}

	private void setReturnedVariables(List<String> returnedVariables)
	{
		this.returnedVariables = returnedVariables;
	}

	public boolean isPersistAfterReturn()
	{
		return persistAfterReturn;
	}

	public void setPersistAfterReturn(boolean persistAfterReturn)
	{
		this.persistAfterReturn = persistAfterReturn;
	}
}