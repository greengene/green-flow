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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import greenflow.utilities.WorkflowXmlUtilities;
import greenflow.workflow.Workflow;

public abstract class Transaction
{
	@Autowired
	private WorkflowXmlUtilities workflowXmlUtilities;

	private long conversationId;

	private Workflow workflow;

	private int transactionOrdinal;

	private TransactionData transactionData;

	private boolean stale = false;

	public abstract List<TransactionResult<?>> execute();

	protected Transaction() {}

	public String workflowToString()
	{
		return workflowXmlUtilities.workflowToXml(getWorkflow());
	}

	public long getConversationId()
	{
		return conversationId;
	}

	protected void setConversationId(long conversationId)
	{
		this.conversationId = conversationId;
	}

	protected Workflow getWorkflow()
	{
		return workflow;
	}

	protected void setWorkflow(Workflow workflow)
	{
		this.workflow = workflow;
	}

	public int getTransactionOrdinal()
	{
		return transactionOrdinal;
	}

	protected void setTransactionOrdinal(int transactionOrdinal)
	{
		this.transactionOrdinal = transactionOrdinal;
	}

	public TransactionData getTransactionData()
	{
		return transactionData;
	}

	protected void setTransactionData(TransactionData transactionData)
	{
		this.transactionData = transactionData;
	}

	public boolean isStale()
	{
		return stale;
	}

	protected void setStale(boolean stale)
	{
		this.stale = stale;
	}
}