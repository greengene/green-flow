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

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultWorkUnit extends WorkUnit
{
	@Override
	protected void execute()
	{
		beforeExecution();

		if (getWorkflow().getExecutionSuspensionPoint() != null)
		{
			if (!getWorkflow().getExecutionSuspensionPoint().equals(getBreadcrumbId()))
			{
				log.debug("Traversing while looking up suspension point: Container Block, id = " + getBreadcrumbId() + " (" + getId() + ")");

				getChildWorkUnits().subList(
						Integer.parseInt(Splitter.on('.').splitToList(getWorkflow().getExecutionSuspensionPoint()).get(CharMatcher.is('.').countIn(getBreadcrumbId()) + 1)) - 1,
						getChildWorkUnits().size())
						.forEach(WorkUnit::execute);
			}
			else
			{
				log.debug("Reached suspension point: " + getBreadcrumbId() + " - Starting Execution...");
			}
		}

		else
		{
			log.debug("Executing: Container Block, id = " + getBreadcrumbId() + " (" + getId() + ")");

			getChildWorkUnits().forEach(WorkUnit::execute);
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

	@Override
	public String getSymbolicName()
	{
		return toString();
	}
}