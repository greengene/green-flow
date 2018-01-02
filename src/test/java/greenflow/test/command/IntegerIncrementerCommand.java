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

package greenflow.test.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import greenflow.command.Command;
import greenflow.command.CommandResult;
import greenflow.conversation.TransactionData;

@Component
@Scope("prototype")
@Lazy
public class IntegerIncrementerCommand extends Command<Integer>
{
	private static final Logger logger = LoggerFactory.getLogger(IntegerIncrementerCommand.class);

	@Override
	public CommandResult<Integer> execute()
	{
		TransactionData transactionData = getWrapperWorkUnit().getWorkflow().getWorkflowContext().getTransaction().getTransactionData();
		logger.debug("Command access to workflow contxet: Transaction ordinal: " + getWrapperWorkUnit().getWorkflow().getWorkflowContext().getTransaction().getTransactionOrdinal());

		int parameterIndex = 0;

		int input = Integer.parseInt(getWrapperWorkUnit().lookUpVariableValue(getParameters().get(parameterIndex++)).toString());

		final int result = input + 1;

		return new CommandResult<Integer>() {
			public Integer getData()
			{
				return result;
			}
		};
	}

	@Override
	public String getSymbolicName()
	{
		return "integer-incrementer-command";
	}
}