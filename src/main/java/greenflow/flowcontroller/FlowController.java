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

package greenflow.flowcontroller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;

import greenflow.predicate.Predicate;
import greenflow.utilities.FlowControllerTypeService;
import greenflow.utilities.ServiceLocator;
import greenflow.workunit.WorkUnit;

import javax.annotation.PostConstruct;

@Component
@Scope("prototype")
public abstract class FlowController
{
	private static final Logger logger = LoggerFactory.getLogger(FlowController.class);

	@Autowired
	private FlowControllerTypeService flowControllerTypeService;

	@Autowired
	protected ServiceLocator serviceLocator;

	private Predicate predicate;

	private Predicate evaluatedPredicate;

	private WorkUnit wrapperWorkUnit;

	@PostConstruct
	protected void init()
	{
		Class<? extends FlowController> clazz = getClass();
		flowControllerTypeService.registerType(clazz);
	}

	abstract public void controlFlow();

	abstract public String getSymbolicName();

	public void execute()
	{
		if (getWrapperWorkUnit().getWorkflow().getExecutionSuspensionPoint() != null)
		{
			if (!getWrapperWorkUnit().getWorkflow().getExecutionSuspensionPoint().equals(getWrapperWorkUnit().getBreadcrumbId()))
			{
				logger.debug("Traversing while looking up suspension point: flow controller workunit; workunit: id: " + getWrapperWorkUnit().getBreadcrumbId() + " (" + getWrapperWorkUnit().getId() + "); predicate: name: " + getPredicate().getSymbolicName() + ", id: " + getPredicate().getId());

				for (WorkUnit workUnit :
						getWrapperWorkUnit().getChildWorkUnits().subList(
							Integer.parseInt(Splitter.on('.').splitToList(getWrapperWorkUnit().getWorkflow().getExecutionSuspensionPoint()).get(CharMatcher.is('.').countIn(getWrapperWorkUnit().getBreadcrumbId()) + 1)) - 1,
							getWrapperWorkUnit().getChildWorkUnits().size())
						)
				{
					workUnit.execute();
				}
			}
			else
			{
				logger.debug("Reached suspension point: " + getWrapperWorkUnit().getBreadcrumbId() + " - Starting Execution...");
			}
		}

		else
		{
			logger.debug("Executing: flow controller workunit; workunit: id: " + getWrapperWorkUnit().getBreadcrumbId() + " (" + getWrapperWorkUnit().getId() + "); predicate: name: " + getPredicate().getSymbolicName() + ", id: " + getPredicate().getId());

			for (WorkUnit workUnit : getWrapperWorkUnit().getChildWorkUnits())
			{
				workUnit.execute();
			}
		}
	}

	public int getTypeId()
	{
		return flowControllerTypeService.getTypeId(getClass());
	}

	public Predicate getPredicate()
	{
		return predicate;
	}

	public void setPredicate(Predicate predicate)
	{
		this.predicate = predicate;
		getPredicate().setWrapperFlowController(this);
	}

	public Predicate getEvaluatedPredicate()
	{
		return evaluatedPredicate;
	}

	public void setEvaluatedPredicate(Predicate evaluatedPredicate)
	{
		this.evaluatedPredicate = evaluatedPredicate;
	}

	public WorkUnit getWrapperWorkUnit()
	{
		return wrapperWorkUnit;
	}

	public void setWrapperWorkUnit(WorkUnit parentWorkUnit)
	{
		this.wrapperWorkUnit = parentWorkUnit;
	}
}