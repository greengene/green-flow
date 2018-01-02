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

public abstract class Conversation
{
	private long id;

	private long workflowId;

	abstract protected Transaction startTransaction(TransactionData transactionData, boolean workflowStatePersisted);

	public Transaction startTransaction(TransactionData transactionData)
	{
		return startTransaction(transactionData, true);
	}

	public Transaction startTransaction(Transaction previousTransaction, TransactionData transactionData)
	{
		Transaction transaction = null;

		if (previousTransaction.getWorkflow() != null)
		{
			transaction = startTransaction(transactionData, false);
			transaction.setWorkflow(previousTransaction.getWorkflow());
		}
		else
		{
			transaction = startTransaction(transactionData);
		}

		return transaction;
	}

	public long getId()
	{
		return id;
	}

	protected void setId(long id)
	{
		this.id = id;
	}

	public long getWorkflowId()
	{
		return workflowId;
	}

	protected void setWorkflowId(long workflowId)
	{
		this.workflowId = workflowId;
	}
}