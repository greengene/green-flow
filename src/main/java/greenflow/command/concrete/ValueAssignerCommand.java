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

package greenflow.command.concrete;

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
public class ValueAssignerCommand extends Command<Object>
{
	private static final Logger logger = LoggerFactory.getLogger(ValueAssignerCommand.class);

	@Override
	public CommandResult<Object> execute()
	{
		TransactionData transactionData = getWrapperWorkUnit().getWorkflow().getWorkflowContext().getTransaction().getTransactionData();
		logger.debug("Command access to workflow contxet: Transaction ordinal: " + getWrapperWorkUnit().getWorkflow().getWorkflowContext().getTransaction().getTransactionOrdinal());

		Object value = getWrapperWorkUnit().lookUpVariableValue(getParameters().get(1)).toString();

		final Object previousValue = getWrapperWorkUnit().lookUpVariableScopeWorkUnit(getParameters().get(0)).getVariables().put(getParameters().get(0), value);

		return new CommandResult<Object>() {
			public Object getData()
			{
				return previousValue;
			}
		};
	}

	@Override
	public String getSymbolicName()
	{
		return "value-assigner-command";
	}
}