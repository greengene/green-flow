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

package greenflow.conversation;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import greenflow.exception.ConversationException;
import greenflow.workunit.WorkUnit;

@Component
@Scope("prototype")
public class DefaultTransaction extends Transaction
{
	@Autowired
	private ConversationPersistenceUtilities workflowConversationPersistenceUtilities;

	private static final Logger logger = LoggerFactory.getLogger(DefaultTransaction.class);

	@Override
	public List<TransactionResult<?>> execute()
	{
		if (isStale())
		{
			throw new ConversationException("Trying to execute a stale transaction! Conversation id: " + getConversationId() + ", Transaction ordinal: " + getTransactionOrdinal() + ".");
		}

		getWorkflow().execute();

		saveTransactionState();

		if ((getWorkflow().getExecutionSuspensionPoint() != null && getWorkflow().getWorkUnitByBreadcrumbId(getWorkflow().getExecutionSuspensionPoint()).isPersistAfterReturn())

				|| getWorkflow().getExecutionSuspensionPoint() == null)
		{
			saveWorkflowState();
		}

		endTransaction();

		setStale(true);

		List<TransactionResult<?>> resultList = new ArrayList<TransactionResult<?>>();

		if (getWorkflow().getExecutionSuspensionPoint() != null)
		{
			final WorkUnit returningWorkUnit = getWorkflow().getWorkUnitByBreadcrumbId(getWorkflow().getExecutionSuspensionPoint());

			for (String returnedVariable : returningWorkUnit.getReturnedVariables())
			{
				final String finalReturnedVariable = returnedVariable;

				resultList.add(new TransactionResult<Object>()
				{
					@Override
					public Object getData()
					{
						return returningWorkUnit.lookUpVariableValue(finalReturnedVariable);
					}
				});
			}
		}

		logger.debug("Returning to transaction client a list of " + resultList.size() + " transaction result object(s).");

		int i=0;
		for (TransactionResult<?> workflowConversationTransactionResult : resultList)
		{
			logger.debug("Result index: " + i++);
			logger.debug("Result value: " + workflowConversationTransactionResult.getData());
		}

		return resultList;
	}

	private void saveTransactionState()
	{
		workflowConversationPersistenceUtilities.saveTransactionState(this);
	}

	private void saveWorkflowState()
	{
		workflowConversationPersistenceUtilities.saveWorkflowState(this);
	}

	private void endTransaction()
	{
		workflowConversationPersistenceUtilities.endTransaction(this);
	}
}