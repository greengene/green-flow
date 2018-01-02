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

package greenflow.test.predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import greenflow.conversation.TransactionData;
import greenflow.predicate.Predicate;

@Component
@Scope("prototype")
@Lazy
public class IntegerLessThanPredicate extends Predicate
{
	private static final Logger logger = LoggerFactory.getLogger(IntegerLessThanPredicate.class);

	@Override
	public boolean isSatisfied()
	{
		TransactionData transactionData = getWrapperFlowController().getWrapperWorkUnit().getWorkflow().getWorkflowContext().getTransaction().getTransactionData();
		logger.debug("Predicate access to workflow contxet: Transaction ordinal: " + getWrapperFlowController().getWrapperWorkUnit().getWorkflow().getWorkflowContext().getTransaction().getTransactionOrdinal());

		int parameterIndex = 0;

		int input = Integer.parseInt(getWrapperFlowController().getWrapperWorkUnit().lookUpVariableValue(getParameters().get(parameterIndex++)).toString());

		int upperBound = Integer.parseInt(getWrapperFlowController().getWrapperWorkUnit().lookUpVariableValue(getParameters().get(parameterIndex++)).toString());

		return input < upperBound;
	}

	@Override
	public String getSymbolicName()
	{
		return "integer-less-than-predicate";
	}
}