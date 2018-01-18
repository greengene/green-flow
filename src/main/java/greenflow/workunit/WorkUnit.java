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

import java.util.*;
import java.util.stream.Collectors;

import lombok.Data;
import lombok.experimental.Builder;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import greenflow.container.interaction.TargetContainerElement;
import greenflow.exception.WorkflowExecutionSuspensionException;
import greenflow.workflow.Workflow;

import com.google.common.base.CharMatcher;

@Slf4j
@Data @Builder
public abstract class WorkUnit
{
	private static final Logger logger = LoggerFactory.getLogger(WorkUnit.class);

	private Workflow workflow;

	private long id;

	private Map<String, Variable> variables;

	private boolean returnAtCompletion = false;

	private List<String> returnedVariables;

	private boolean persistAfterReturn = true;

	private TargetContainerElement<WorkUnit> targetContainer;

	public Class returnType;

	public void assignTo() {
	}

	private Map<String, Variable> returnedVariables()
	{
		return getVariables().entrySet()
				.stream()
				.filter(entry -> entry.getValue().isReturned())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	public WorkUnit() {}

	public WorkUnit(Class returnType, WorkUnit[] workUnits)
	{
		setReturnType(returnType);

		for (WorkUnit workUnit: workUnits) addChildWorkUnit(workUnit);
	}

	public void assignResult()
	{
		returnedVariables().entrySet()
				.stream()
				.forEach(entry ->
				{
					String assignTo = entry.getValue().getAssignTo();
					Object result = entry.getValue().getValue();

					WorkUnit variableScopeWorkUnit = lookUpVariableScopeWorkUnit(assignTo);
					variableScopeWorkUnit.getVariables().get(assignTo).setValue(result);

					if (getWorkflow().getDirtyVariables().get(variableScopeWorkUnit) == null)
					{
						getWorkflow().getDirtyVariables().put(variableScopeWorkUnit, new HashSet<String>());
					}
					getWorkflow().getDirtyVariables().get(variableScopeWorkUnit).add(assignTo);

					log.debug("Assigning value to variable! workunit: " + variableScopeWorkUnit.getBreadcrumbId() + ", variable: " + assignTo + ", value: " + variableScopeWorkUnit.getVariables().get(assignTo));
				});
	}

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

		childWorkUnit.setTargetContainer(getTargetContainer().addChildWorkUnit(childWorkUnit));

		childWorkUnit.getTargetContainer().setId(childWorkUnit.getId());
	}

	protected abstract void execute();

	public abstract String getSymbolicName();

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

	public void addVariable()
	{
	}

	public void removeVariable()
	{
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