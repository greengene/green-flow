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

package greenflow.workflow;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;

import greenflow.container.GenericContainerNode;
import greenflow.container.interaction.TargetContainerElement;
import greenflow.context.WorkflowContext;
import greenflow.exception.WorkflowException;
import greenflow.exception.WorkflowExecutionSuspensionException;
import greenflow.exception.WorkflowExecutionTerminationException;
import greenflow.workunit.WorkUnit;

public class Workflow
{
	private static final Logger logger = LoggerFactory.getLogger(Workflow.class);

	private long Id;

	private WorkUnit rootWorkUnit;

	private GenericContainerNode<WorkUnit> genericContainerNode;

	private String executionTerminationPoint;

	private String executionSuspensionPoint;

	private WorkflowContext workflowContext;

	private Map<WorkUnit, Set<String>> dirtyVariables = new HashMap<WorkUnit, Set<String>>();

	private String name;

	private String description;

	private Date creationDate;

	private String creator;

	public Workflow(WorkflowContext workflowContext)
	{
		setWorkflowContext(workflowContext);

		setAsRootWorkUnit(new WorkUnit());
	}

	private static final AtomicLong LAST_TIME_MS = new AtomicLong();

	private static long uniqueCurrentTimeMS()
	{
		int factor = (int) Math.pow(10, 3);
	    long now = System.currentTimeMillis()*factor;
	    while(true) {
	        long lastTime = LAST_TIME_MS.get();
	        if (lastTime >= now)
	            now = lastTime+1;
	        if (LAST_TIME_MS.compareAndSet(lastTime, now))
	            return now;
	    }
	}

	public static String getDefaultName()
	{
		Calendar cal = new GregorianCalendar();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");

		dateFormat.setCalendar(cal); 
		String currentDate = dateFormat.format(cal.getTime());
		return "workflow-" + currentDate + "-" + uniqueCurrentTimeMS();
	}

	public void execute()
	{
		try
		{
			getRootWorkUnit().execute();

			logger.debug("Execution: workflow completed.");
		}
		catch (WorkflowExecutionTerminationException e)
		{
			setExecutionTerminationPoint(e.getMessage());

			logger.debug("Termination: terminating workflow execution on workunit " + getExecutionTerminationPoint());
		}
		catch (WorkflowExecutionSuspensionException e)
		{
			setExecutionSuspensionPoint(e.getMessage());

			logger.debug("Suspension: suspending workflow execution on workunit " + getExecutionSuspensionPoint());
		}
	}

	public String toString() {
		return getRootWorkUnit().getTargetContainer().toString();
	}

	public WorkUnit getRootWorkUnit() {
		return rootWorkUnit;
	}

	private void setRootWorkUnit(WorkUnit rootWorkUnit)
	{
		this.rootWorkUnit = rootWorkUnit;
	}

	public Collection<WorkUnit> getChildWorkUnits() {
		return getRootWorkUnit().getChildWorkUnits();
	}

	public List<WorkUnit> getAllWorkUnits()
	{
		return getRootWorkUnit().getDescendants();
	}

	public void addChildWorkUnit(WorkUnit workUnit)
	{
		getRootWorkUnit().addChildWorkUnit(workUnit);
	}

	public void setAsRootWorkUnit(WorkUnit workUnit)
	{
		if(workUnit.getFlowController() == null && workUnit.getCommand() == null)
		{
			setRootWorkUnit(workUnit);

			getRootWorkUnit().setWorkflow(this);

			genericContainerNode = new GenericContainerNode<WorkUnit>();
			TargetContainerElement<WorkUnit> adapter = genericContainerNode.insertRoot(getRootWorkUnit());
			getRootWorkUnit().setTargetContainer(adapter);

			getRootWorkUnit().getTargetContainer().setId(getRootWorkUnit().getId());
		}
		else
		{
			throw new WorkflowException("addAsRootWorkUnit error: cannot add a nonempty workunit as root.");
		}
	}

	public WorkUnit getWorkUnitByBreadcrumbId(String breadcrumbId)
	{
		WorkUnit workUnit = getRootWorkUnit();

		try
		{
			for (String crumb : Splitter.on('.').splitToList(breadcrumbId).subList(1, CharMatcher.is('.').countIn(breadcrumbId) + 1))
			{
				workUnit = workUnit.getChildWorkUnits().get(Integer.parseInt(crumb)-1);
			}
		}
		catch (Exception e)
		{
			workUnit = null;
		}

		return workUnit;
	}

	public String getExecutionTerminationPoint()
	{
		return executionTerminationPoint;
	}

	public void setExecutionTerminationPoint(String executionTerminationPoint)
	{
		this.executionTerminationPoint = executionTerminationPoint;
	}

	public String getExecutionSuspensionPoint()
	{
		return executionSuspensionPoint;
	}

	public void setExecutionSuspensionPoint(String executionSuspensionPoint)
	{
		this.executionSuspensionPoint = executionSuspensionPoint;
	}

	public WorkflowContext getWorkflowContext()
	{
		return workflowContext;
	}

	private void setWorkflowContext(WorkflowContext workflowContext)
	{
		this.workflowContext = workflowContext;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public long getId()
	{
		return Id;
	}

	public void setId(long id)
	{
		Id = id;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public Date getCreationDate()
	{
		return creationDate;
	}

	public void setCreationDate(Date creationDate)
	{
		this.creationDate = creationDate;
	}

	public String getCreator()
	{
		return creator;
	}

	public void setCreator(String creator)
	{
		this.creator = creator;
	}

	public Map<WorkUnit, Set<String>> getDirtyVariables()
	{
		return dirtyVariables;
	}
}