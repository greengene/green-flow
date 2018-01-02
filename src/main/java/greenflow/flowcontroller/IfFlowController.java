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
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import greenflow.predicate.concrete.FalsePredicate;
import greenflow.predicate.concrete.TruePredicate;

@Component
@Scope("prototype")
public class IfFlowController extends FlowController
{
	private static final Logger logger = LoggerFactory.getLogger(IfFlowController.class);

	@Override
	public void controlFlow()
	{
		if (getWrapperWorkUnit().getWorkflow().getExecutionSuspensionPoint() != null)
		{
			logger.debug("Traversing while looking up suspension point: If condition [predicate automatically set to true], workunit: id: " + getWrapperWorkUnit().getBreadcrumbId() + " (" + getWrapperWorkUnit().getId() + "); predicate: name: " + getPredicate().getSymbolicName() + ", id: " + getPredicate().getId());

			setEvaluatedPredicate(serviceLocator.getInstance(TruePredicate.class));
		}
		else
		{
			logger.debug("Executing: If condition [predicate awaiting evaluation], workunit: id: " + getWrapperWorkUnit().getBreadcrumbId() + " (" + getWrapperWorkUnit().getId() + "); predicate: name: " + getPredicate().getSymbolicName() + ", id: " + getPredicate().getId());

			setEvaluatedPredicate(getPredicate().isSatisfied() ? serviceLocator.getInstance(TruePredicate.class) : serviceLocator.getInstance(FalsePredicate.class));
		}

		if (getEvaluatedPredicate().isSatisfied())
		{
			logger.debug("Predicate: evaluated to true; workunit: id: " + getWrapperWorkUnit().getBreadcrumbId() + " (" + getWrapperWorkUnit().getId() + "); predicate: name: " + getPredicate().getSymbolicName() + ", id: " + getPredicate().getId());
			logger.debug("Entering flow controller's content...");

			execute();
		}
		else
		{
			logger.debug("Predicate: evaluated to false; workunit: id: " + getWrapperWorkUnit().getBreadcrumbId() + " (" + getWrapperWorkUnit().getId() + "); predicate: name: " + getPredicate().getSymbolicName() + ", id: " + getPredicate().getId());
			logger.debug("Getting out of flow controller...");
		}
	}

	@Override
	public String getSymbolicName()
	{
		return "if";
	}
}