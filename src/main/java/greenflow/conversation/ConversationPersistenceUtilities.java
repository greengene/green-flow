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

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import greenflow.context.WorkflowContext;
import greenflow.exception.ConversationException;
import greenflow.persistence.WorkflowConfigurationDao;
import greenflow.utilities.DefaultServiceLocator;
import greenflow.utilities.EnhancedResultSet;
import greenflow.workflow.Workflow;
import greenflow.workunit.WorkUnit;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ConversationPersistenceUtilities
{
	private static final Logger logger = LoggerFactory.getLogger(ConversationPersistenceUtilities.class);

	@Autowired
	private DefaultServiceLocator serviceLocator;

	@Autowired
	private WorkflowConfigurationDao workUnitConfigurationDao;

	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	static final String workflowTable = "Configuration_Workflow";
	static final String assignedVariableTable = "Execution_Assigned_Variable";
	static final String transactionTable = "Execution_Transaction";
	static final String conversationTable = "Execution_Conversation";

	public Conversation createConversationByWorkflowName(String workflowName)
	{
		Conversation conversation = null;

		String sql;

		sql = "select id from " + workflowTable + " where name = :value";

		long workflowId = namedParameterJdbcTemplate.queryForObject(sql, ImmutableMap.<String, String>builder().put("value", workflowName).build(), Long.class);

		if (workflowId > 0)
		{
			conversation = createConversationByWorkflowId(workflowId);
		}

		return conversation;
	}

	public Conversation createConversationByWorkflowId(long workflowId)
	{
		Conversation conversation = null;

		String sql;

		if (workflowId > 0)
		{
			KeyHolder keyHolder = new GeneratedKeyHolder();
			sql = "insert into " + conversationTable + " (workflow_id) values (:workflow_id)";

			namedParameterJdbcTemplate.update(sql, new MapSqlParameterSource(ImmutableMap.<String, Long>builder().put("workflow_id", workflowId).build()), keyHolder);

			long conversationId = keyHolder.getKey().longValue();

			conversation = serviceLocator.getInstance(DefaultConversation.class);

			conversation.setId(conversationId);

			conversation.setWorkflowId(workflowId);
		}

		return conversation;
	}

	public Workflow getStatefulWorkflowByConversation(Conversation conversation, WorkflowContext workflowContext)
	{
		Workflow workflow = null;

		workflow = workUnitConfigurationDao.retrieveConfigurationWorkflow(conversation.getWorkflowId(), workflowContext);

		Map<Long, Map<String, Object>> workUnitVariableMap = retrieveWorkflowExecutionState(conversation);

		logger.debug("Loading state into workflow...");

		for (Map.Entry<Long, Map<String, Object>> workUnitVariables : workUnitVariableMap.entrySet())
		{
			logger.debug("Loading " + workUnitVariables.getValue().size() + " variable(s) into workunit persistence-id: " + workUnitVariables.getKey());

			logger.debug("workunit variables: ");

			for (Map.Entry<String, Object> workUnitVariable : workUnitVariables.getValue().entrySet())
			{
				logger.debug("name: " + workUnitVariable.getKey() + ", value: " + workUnitVariable.getValue());
			}
		}

		for(WorkUnit workUnit : workflow.getAllWorkUnits())
		{
			if (workUnitVariableMap.get(workUnit.getId()) != null)
			{
				for (Map.Entry<String, Object> workUnitVariable : workUnitVariableMap.get(workUnit.getId()).entrySet())
				{
					workUnit.getVariables().put(workUnitVariable.getKey(), workUnitVariable.getValue());
				}
			}
		}

		return workflow;
	}

    public void saveTransactionState(Transaction transaction)
    {
    	String sqlString = "insert into " + transactionTable + " (conversation_id, ordinal, suspension_point) values (:conversation_id, :ordinal, :suspension_point)";
    	Map<String, Object> parameters = new HashMap<String, Object>();

		parameters.put("conversation_id", transaction.getConversationId());
		parameters.put("ordinal", transaction.getTransactionOrdinal());
		logger.debug("Saving suspension point: " + transaction.getWorkflow().getExecutionSuspensionPoint());
		parameters.put("suspension_point", transaction.getWorkflow().getExecutionSuspensionPoint());

		namedParameterJdbcTemplate.update(sqlString, new MapSqlParameterSource(parameters));
    }

    public void saveWorkflowState(Transaction transaction)
    {
		StringBuilder sql = new StringBuilder("MERGE INTO " + assignedVariableTable + " AS t USING (VALUES");
		Map<String, Object> parameters = new HashMap<String, Object>();

		int i=0;
		for (Map.Entry<WorkUnit, Set<String>> dirtyVariableSet : transaction.getWorkflow().getDirtyVariables().entrySet())
		{
			for (String dirtyVariable : dirtyVariableSet.getValue())
			{
				sql.append("(:conversation_id").append(i).append(", :workunit_id").append(i).append(", :name").append(i).append(", :value").append(i).append("), ");

				parameters.put("conversation_id" + i, transaction.getConversationId());
				parameters.put("workunit_id" + i, dirtyVariableSet.getKey().getId());
				parameters.put("name" + i, dirtyVariable);

				parameters.put("value" + i, dirtyVariableSet.getKey().getVariables().get(dirtyVariable).toString());

				i++;
			}
		}
		sql.setLength(sql.length()-2);
		sql.append(") AS vals(x,y,w,z) ON t.conversation_id = vals.x AND t.workunit_id = vals.y AND t.name=vals.w ");
		sql.append("WHEN MATCHED THEN UPDATE SET t.value=vals.z ");
		sql.append("WHEN NOT MATCHED THEN INSERT VALUES vals.x, vals.y, vals.w, vals.z");

		if (i>0)
		{
			namedParameterJdbcTemplate.update(sql.toString(), new MapSqlParameterSource(parameters));
		}
    }

    public void endTransaction(Transaction transaction)
    {
		String sqlString = "update " + conversationTable + " set locked_transaction_ordinal = NULL where id = :id and locked_transaction_ordinal = :locked_transaction_ordinal";
		Map<String, Object> parameters = new HashMap<String, Object>();

		parameters.put("id", transaction.getConversationId());
		parameters.put("locked_transaction_ordinal", transaction.getTransactionOrdinal());

		int effectedRows = namedParameterJdbcTemplate.update(sqlString, new MapSqlParameterSource(parameters));

		if (effectedRows == 0)
		{
			throw new ConversationException("Attempting to close an inactive transaction! Conversation id: " + transaction.getConversationId() + ", Transaction ordinal: " + transaction.getTransactionOrdinal() + ".");
		}
    }

    @Transactional
    public Transaction allocateNewTransaction(Conversation conversation, TransactionData transactionData, boolean workflowStatePersisted)
    {
    	Transaction transaction = null;

		String sql = "select conversation_id, ordinal, suspension_point from " + transactionTable + " where conversation_id = :conversation_id order by ordinal desc limit 1";

		List<Triple<Long, Integer, String>> records = namedParameterJdbcTemplate.query(sql, new MapSqlParameterSource(ImmutableMap.<String, Long>builder().put("conversation_id", conversation.getId()).build()), new RowMapper<Triple<Long, Integer, String>>(){
	        public Triple<Long, Integer, String> mapRow(ResultSet resultSet, int i) throws SQLException {
	        	Triple<Long, Integer, String> triple = new ImmutableTriple<Long, Integer, String>(resultSet.getLong("conversation_id"), resultSet.getInt("ordinal"), resultSet.getString("suspension_point"));
	            return triple;
	        }
	    });

		int transactionOrdinal;
		String executionSuspensionPoint = null;

		if (!records.isEmpty())
		{
			transactionOrdinal = records.get(0).getMiddle() + 1;
			executionSuspensionPoint = records.get(0).getRight();

			if (executionSuspensionPoint == null)
			{
				return null;
			}
		}
		else
		{
			transactionOrdinal = 1;
		}

		logger.debug("Transaction ordinal: " + transactionOrdinal);
		logger.debug("Previous suspension point: " + executionSuspensionPoint);

		String sqlString = "update " + conversationTable + " set locked_transaction_ordinal = :locked_transaction_ordinal where id = :id and locked_transaction_ordinal is NULL";
		Map<String, Object> parameters = new HashMap<String, Object>();

		parameters.put("locked_transaction_ordinal", transactionOrdinal);
		parameters.put("id", conversation.getId());

		int effectedRows = namedParameterJdbcTemplate.update(sqlString, new MapSqlParameterSource(parameters));

		if (effectedRows == 0)
		{
			throw new ConversationException("Conversation is still busy with another tsransaction. Try again later.");
		}
		else
		{
			transaction = serviceLocator.getInstance(DefaultTransaction.class);

			transaction.setConversationId(conversation.getId());
			transaction.setTransactionOrdinal(transactionOrdinal);
			transaction.setTransactionData(transactionData);

			WorkflowContext workflowContext = new WorkflowContext(transaction);

			if (workflowStatePersisted)
			{
				Workflow workflow = getStatefulWorkflowByConversation(conversation, workflowContext);

				workflow.setExecutionSuspensionPoint(executionSuspensionPoint);

				transaction.setWorkflow(workflow);
			}
		}

    	return transaction;
    }

    private Map<Long, Map<String, Object>> retrieveWorkflowExecutionState(final Conversation conversation)
    {
    	logger.debug("Retrieving from DB the variables for conversationId: " + conversation.getId());

    	String sql = "select workunit_id, name, value from " + assignedVariableTable + " where conversation_id = :conversation_id";

    	Map<Long, Map<String, Object>> workUnitVariableMap = namedParameterJdbcTemplate.query(sql, new MapSqlParameterSource(ImmutableMap.<String, Long>builder().put("conversation_id", conversation.getId()).build()), 
				new ResultSetExtractor<Map<Long, Map<String, Object>>>()
				{
					public Map<Long, Map<String, Object>> extractData(ResultSet resultSet) throws SQLException, DataAccessException
					{
						EnhancedResultSet enhancedResultSet = new EnhancedResultSet(resultSet);

						Map<Long, Map<String, Object>> map = new HashMap<Long, Map<String, Object>>();

						while (resultSet.next())
						{
							long workunitId = enhancedResultSet.get(assignedVariableTable, "workunit_id");

							Map<String, Object> workunitVariables = map.get(workunitId);

							if(workunitVariables == null)
							{
								workunitVariables = new HashMap<String, Object>();

								map.put(workunitId, workunitVariables);
							}

							workunitVariables.put(enhancedResultSet.<String>get(assignedVariableTable, "name"), enhancedResultSet.<String>get(assignedVariableTable, "value"));
						}
						return map;
					}
				}
			);    	

    	return workUnitVariableMap;
    }

	public Conversation getConversationByConversationId(long conversationId)
	{
		Conversation conversation = null;

		String sql;

		sql = "select workflow_id from " + conversationTable + " where id = :id";

		long workflowId = namedParameterJdbcTemplate.queryForObject(sql, ImmutableMap.<String, Long>builder().put("id", conversationId).build(), Long.class);

		if (workflowId > 0)
		{
			conversation = serviceLocator.getInstance(DefaultConversation.class);

			conversation.setId(conversationId);

			conversation.setWorkflowId(workflowId);
		}

		return conversation;
	}
}