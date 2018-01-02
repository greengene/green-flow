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

package greenflow.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.object.GenericStoredProcedure;
import org.springframework.jdbc.object.StoredProcedure;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import greenflow.command.Command;
import greenflow.context.WorkflowContext;
import greenflow.flowcontroller.FlowController;
import greenflow.predicate.Predicate;
import greenflow.predicate.Predicates;
import greenflow.predicate.concrete.TruePredicate;
import greenflow.predicate.relation.AndPredicate;
import greenflow.predicate.relation.NotPredicate;
import greenflow.predicate.relation.OrPredicate;
import greenflow.predicate.relation.PredicateArray;
import greenflow.utilities.*;
import greenflow.workflow.Workflow;
import greenflow.workunit.WorkUnit;

import javax.sql.DataSource;

@Repository
public class WorkflowConfigurationDao {
	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private DefaultServiceLocator serviceLocator;

	@Autowired
	private CommandTypeService commandTypeService;

	@Autowired
	private PredicateTypeService predicateTypeService;

	@Autowired
	FlowControllerTypeService flowControllerTypeService;

	@Autowired
	private Predicates predicates;

	static final String workflowTable = "Configuration_Workflow";
	static final String workunitTable = "Configuration_Workunit";
	static final String treePathTable = "Configuration_TreePath";
	static final String predicateTable = "Configuration_Predicate";
	static final String declaredVariableTable = "Configuration_Declared_Variable";
	static final String parameterTable = "Configuration_Parameter";
	static final String returnedVariableTable = "Configuration_Returned_Variable";

	static final short containerBlockDiscriminator = 0;
	static final short flowControllerDiscriminator = 1;
	static final short commandDiscriminator = 2;

	public Workflow retrieveConfigurationWorkflow(long workflowId, WorkflowContext workflowContext) {
		String sql =
		"select " +
		workunitTable + ".*, " +
		predicateTable + ".*, " +
		declaredVariableTable + ".*, " +
		parameterTable + ".*, " +
		returnedVariableTable + ".*, " +

		"x.descendant, " +

		"x.ancestors as ancestors " +
		"from " +
		treePathTable + " " +
		"join " + workunitTable + " on " + treePathTable + ".descendant = " + workunitTable + ".id " +

		"left join " + predicateTable + " on " + predicateTable + ".workunit_id = " + workunitTable + ".id " +
		"left join " + declaredVariableTable + " on " + declaredVariableTable + ".workunit_id = " + workunitTable + ".id " +

		"left join " + parameterTable + " on (" + parameterTable + ".workunit_id = " + workunitTable + ".id " + " or " + parameterTable + ".predicate_id = " + predicateTable + ".id) " +
		"left join " + returnedVariableTable + " on " + returnedVariableTable + ".workunit_id = " + workunitTable + ".id " +

		"left join (" +
				"select " + treePathTable + ".descendant, group_concat(distinct " + treePathTable + ".ancestor) as ancestors " +
				"from " + treePathTable + " " +
				"group by " + treePathTable + ".descendant" +
				") x " +
				"on x.descendant = " + workunitTable + ".id " +

		"where " + workunitTable + ".workflow_id = ? " +

		"order by "  + predicateTable + ".id desc, " + 

		declaredVariableTable + ".variable_ordinal asc, " + parameterTable + ".parameter_ordinal asc"; 

		List<Pair<WorkUnit, List<Long>>> workunitsAndTheirAncestorsList = jdbcTemplate.query(sql, new Object[] {workflowId}, 
				new ResultSetExtractor<List<Pair<WorkUnit, List<Long>>>>()
				{
					public List<Pair<WorkUnit, List<Long>>> extractData(ResultSet resultSet) throws SQLException, DataAccessException
					{
						EnhancedResultSet enhancedResultSet = new EnhancedResultSet(resultSet);

						Map<Long, Triple<WorkUnit, List<Long>, Map<Long, Pair<Predicate, Long>>>> map = new HashMap<>();
						WorkUnit workUnit = null;

						Map<Long, List<Predicate>> parentPredicateMap = new HashMap<>();

						Map<Long, Predicate> discoveredPredicates = new HashMap<>();

						while (resultSet.next())
						{
							long id = enhancedResultSet.get(workunitTable, "id");

							Triple<WorkUnit, List<Long>, Map<Long, Pair<Predicate, Long>>> triple = map.get(id);

							if(triple == null) 
							{
								if (enhancedResultSet.<Integer>get(workunitTable, "discriminator") == flowControllerDiscriminator)
								{
									FlowController flowController;

									flowController = serviceLocator.getInstance(flowControllerTypeService.getTypeClass(enhancedResultSet.get(workunitTable, "type")));
									flowController.setPredicate(serviceLocator.getInstance(TruePredicate.class));

									workUnit = new WorkUnit(flowController);
								}

								else if (enhancedResultSet.<Integer>get(workunitTable, "discriminator") == commandDiscriminator)
								{
									Command<?> command = null;

									command = serviceLocator.getInstance(commandTypeService.getTypeClass(enhancedResultSet.<Integer>get(workunitTable, "type")));

									if (!enhancedResultSet.isNull(workunitTable, "assign_to"))
									{
										command.setAssignTo(enhancedResultSet.<String>get(workunitTable, "assign_to"));
									}

									workUnit = new WorkUnit(command);
								}

								else
								{
									workUnit = new WorkUnit();
								}

								workUnit.setId(id);

								workUnit.setReturnAtCompletion(Optional.ofNullable(enhancedResultSet.<Boolean>get(workunitTable, "return")).orElse(false));
								workUnit.setPersistAfterReturn(Optional.ofNullable(enhancedResultSet.<Boolean>get(workunitTable, "persist_after_return")).orElse(false));

								List<Long> ancestorList = new ArrayList<Long>();
								for (String s : Arrays.asList(enhancedResultSet.<String>get("ancestors").split("\\s*,\\s*"))) ancestorList.add(Long.valueOf(s));

								map.put(id, triple = new ImmutableTriple<WorkUnit, List<Long>, Map<Long, Pair<Predicate, Long>>>(workUnit, ancestorList, new HashMap<>()));
							}

							workUnit = triple.getLeft();

							if (!enhancedResultSet.isNull(predicateTable, "id"))
							{
								long predicateId = enhancedResultSet.get(predicateTable, "id");

								if (!discoveredPredicates.containsKey(predicateId)) 
								{
									Predicate predicate = null;

									Class<? extends Predicate> clazz = predicateTypeService.getTypeClass(enhancedResultSet.<Integer>get(predicateTable, "type"));

									if (!parentPredicateMap.containsKey(predicateId)) {
										predicate = serviceLocator.getInstance(clazz);
									}

									else {
										List<Predicate> childPredicates = parentPredicateMap.get(predicateId);

										if (clazz == AndPredicate.class)
										{
											predicate = (AndPredicate) serviceLocator.getInstance(clazz, new PredicateArray(childPredicates.stream().toArray(Predicate[]::new)));
										}
										if (clazz == OrPredicate.class)
										{
											predicate = (OrPredicate) serviceLocator.getInstance(clazz, new PredicateArray(childPredicates.stream().toArray(Predicate[]::new)));
										}
										else if (clazz == NotPredicate.class) {
											predicate = (NotPredicate) serviceLocator.getInstance(clazz, new PredicateArray(childPredicates.stream().toArray(Predicate[]::new)));
										}
									}

									predicate.setId(predicateId);

									if (!enhancedResultSet.isNull(predicateTable, "parent_predicate_id")) {
										long parentPredicateId = enhancedResultSet.get(predicateTable, "parent_predicate_id");

										List<Predicate> siblingPredicates = parentPredicateMap.get(parentPredicateId);

										if (siblingPredicates == null) {
											siblingPredicates = new ArrayList<>();
											parentPredicateMap.put(parentPredicateId, siblingPredicates);
										}

										siblingPredicates.add(predicate);
									} else 
									{
										triple.getLeft().getFlowController().setPredicate(predicate);
									}

									discoveredPredicates.put(predicateId, predicate);
								}
							}

							if (!enhancedResultSet.isNull(declaredVariableTable, "workunit_id") && workUnit.getVariables().size() == enhancedResultSet.<Integer>get(declaredVariableTable, "variable_ordinal"))
							{
								workUnit.getVariables().put(enhancedResultSet.<String>get(declaredVariableTable, "name"), enhancedResultSet.<String>get(declaredVariableTable, "value"));
							}

							if (!enhancedResultSet.isNull(parameterTable, "variable_name"))
							{
								if (!enhancedResultSet.isNull(parameterTable, "workunit_id") && workUnit.getCommand().getParameters().size() == enhancedResultSet.<Integer>get(parameterTable, "parameter_ordinal"))
								{
									workUnit.getCommand().getParameters().add(enhancedResultSet.<String>get(parameterTable, "variable_name"));
								}

								if (!enhancedResultSet.isNull(parameterTable, "predicate_id") && discoveredPredicates.get(enhancedResultSet.<Long>get(parameterTable, "predicate_id")).getParameters().size() == enhancedResultSet.<Integer>get(parameterTable, "parameter_ordinal"))
								{
									discoveredPredicates.get(enhancedResultSet.get(parameterTable, "predicate_id")).getParameters().add(enhancedResultSet.<String>get(parameterTable, "variable_name"));
								}
							}

							if (!enhancedResultSet.isNull(returnedVariableTable, "workunit_id") && workUnit.getReturnedVariables().size() == enhancedResultSet.<Integer>get(returnedVariableTable, "variable_ordinal"))
							{
								workUnit.getReturnedVariables().add(enhancedResultSet.<String>get(returnedVariableTable, "variable_name"));
							}
						}

						List<Triple<WorkUnit, List<Long>, Map<Long, Pair<Predicate, Long>>>> list = new ArrayList<Triple<WorkUnit, List<Long>, Map<Long, Pair<Predicate, Long>>>>(map.values());

						List<Pair<WorkUnit, List<Long>>> workunitsAndTheirAncestorsList = new ArrayList<>();

						for (Triple<WorkUnit, List<Long>, Map<Long, Pair<Predicate, Long>>> triple : list) workunitsAndTheirAncestorsList.add(new ImmutablePair<WorkUnit, List<Long>>(triple.getLeft(), triple.getMiddle()));

						return workunitsAndTheirAncestorsList;
					}
				}
			);

		Workflow workflow = buildTree(workunitsAndTheirAncestorsList, workflowContext);

		workflow.setId(workflowId);

		sql = "select " + workflowTable + ".name from " + workflowTable + " where " + workflowTable + ".id = " + workflowId;
		String name = namedParameterJdbcTemplate.queryForObject(sql, new MapSqlParameterSource(new HashMap<String, Object>()), String.class);

		workflow.setName(name);

		return workflow;
	}

	public Workflow retrieveConfigurationWorkflow(long workflowId)
	{
		return retrieveConfigurationWorkflow(workflowId, null);
	}

	private Workflow buildTree(List<Pair<WorkUnit, List<Long>>> workunitsAndTheirAncestorsList, WorkflowContext workflowContext) {
		Workflow workflow = new Workflow(workflowContext);

		Collections.sort(workunitsAndTheirAncestorsList, new Comparator<Pair<WorkUnit, List<Long>>>() {
				@Override
				public int compare(Pair<WorkUnit, List<Long>> o1, Pair<WorkUnit, List<Long>> o2) {
					return (new Integer(o1.getRight().size()).compareTo(new Integer(o2.getRight().size())) !=0 ? new Integer(o1.getRight().size()).compareTo(new Integer(o2.getRight().size())) : new Long(o1.getLeft().getId()).compareTo(new Long(o2.getLeft().getId())));
				}
			});

		workflow.setAsRootWorkUnit(workunitsAndTheirAncestorsList.get(0).getLeft());

		for (int i=1; i<workunitsAndTheirAncestorsList.size(); i++)
		{
			for (int j=i-1; j>=0 ; j--)
			{
				if (workunitsAndTheirAncestorsList.get(i).getRight().size() == workunitsAndTheirAncestorsList.get(j).getRight().size()+1 && workunitsAndTheirAncestorsList.get(i).getRight().containsAll(workunitsAndTheirAncestorsList.get(j).getRight()))
				{
					workunitsAndTheirAncestorsList.get(j).getLeft().addChildWorkUnit(workunitsAndTheirAncestorsList.get(i).getLeft());
					break;
				}
			}
		}
		return workflow;
	}

	private void addWorkflow(Workflow workflow)
	{
		workflow.setName(Workflow.getDefaultName());

		KeyHolder keyHolder9 = new GeneratedKeyHolder();
		StringBuilder sql0 = new StringBuilder("insert into " + workflowTable + " (name) values (:value1)");

		Map<String, Object> parameters0 = new HashMap<String, Object>();
		parameters0.put("value1", workflow.getName());

		namedParameterJdbcTemplate.update(sql0.toString(), new MapSqlParameterSource(parameters0), keyHolder9);

		long containerId = keyHolder9.getKey().longValue();

		workflow.setId(containerId);
	}

	private List<Map<String,Object>> addWorkUnits(Workflow workflow)
	{
		List<WorkUnit> workUnits = new ArrayList<WorkUnit>(workflow.getRootWorkUnit().getTargetContainer().getDescendants());

		KeyHolder keyHolder = new GeneratedKeyHolder();

		StringBuilder sql = new StringBuilder("insert into " + workunitTable + " (workflow_id, discriminator, type, \"return\", persist_after_return, assign_to) values ");
		Map<String, Object> parameters = new HashMap<String, Object>();

		{
			{
				int i=0;
				for (WorkUnit workUnit : workUnits) {
					sql.append("(:workflow_id").append(i).append(", :discriminator").append(i).append(", :type").append(i).append(", :return").append(i).append(", :persist_after_return").append(i).append(", :assign_to").append(i).append("), ");
					parameters.put("workflow_id" + i, workflow.getId());
					if (workUnit.isFlowController())
					{
						parameters.put("discriminator" + i, flowControllerDiscriminator);
						parameters.put("type" + i, workUnit.getFlowController().getTypeId());
						parameters.put("return" + i, workUnit.isReturnAtCompletion());
						parameters.put("persist_after_return" + i, workUnit.isPersistAfterReturn());
						parameters.put("assign_to" + i, null);
					}
					else if (workUnit.isCommand())
					{
						parameters.put("discriminator" + i, commandDiscriminator);
						parameters.put("type" + i, workUnit.getCommand().getTypeId());
						parameters.put("return" + i, null);
						parameters.put("persist_after_return" + i, null);
						parameters.put("assign_to" + i, workUnit.getCommand().getAssignTo());
					}
					else
					{
						parameters.put("discriminator" + i, containerBlockDiscriminator);

						parameters.put("type" + i, 0);
						parameters.put("return" + i, workUnit.isReturnAtCompletion());
						parameters.put("persist_after_return" + i, workUnit.isPersistAfterReturn());
						parameters.put("assign_to" + i, null);
					}
					i++;
				}
			}			
			sql.setLength(sql.length()-2);

			namedParameterJdbcTemplate.update(sql.toString(), new MapSqlParameterSource(parameters), keyHolder);
		}

		List<Map<String,Object>> insertedWorkUnits = keyHolder.getKeyList();

		Iterator<WorkUnit> it1 = workUnits.iterator();
		Iterator<Map<String, Object>> it2 = insertedWorkUnits.iterator();
		while(it1.hasNext() && it2.hasNext())
		{
			WorkUnit workUnit = it1.next();
			Map<String, Object> insertedWorkUnit = it2.next();

			long generatedKey = (long) insertedWorkUnit.entrySet().iterator().next().getValue();
			workUnit.setId(generatedKey);
			workUnit.getTargetContainer().setId(generatedKey);
		}

		return insertedWorkUnits;
	}

	private void addTreePaths(Workflow workflow)
	{
		Collection<Triple<WorkUnit, WorkUnit, Long>> paths = workflow.getRootWorkUnit().getTargetContainer().getPaths();

		StringBuilder sql3 = new StringBuilder("INSERT INTO " + treePathTable + " (ancestor, descendant, length) VALUES ");
		List<Object> values3 = new ArrayList<Object>();

		{
			int i=0;
			for (Triple<WorkUnit, WorkUnit, Long> path : paths) {
				sql3.append("(:value" + i + ", :value" + (i+1) + ", :value" + (i+2) + "), ");
				values3.add(path.getLeft().getTargetContainer().getId());
				values3.add(path.getMiddle().getTargetContainer().getId());
				values3.add(path.getRight());

				i=i+3;
			}
		}
		sql3.setLength(sql3.length()-2);

		Map<String, Object> parameters3 = new HashMap<String, Object>();
		for (int i=0; i<values3.size(); i=i+3) {
			parameters3.put("value" + i, values3.get(i));
			parameters3.put("value" + (i+1), values3.get(i+1));
			parameters3.put("value" + (i+2), values3.get(i+2));
		}

		namedParameterJdbcTemplate.update(sql3.toString(), parameters3);
	}

	private List<Map<String,Object>> addPredicates(Workflow workflow, List<Map<String,Object>> insertedWorkUnits)
	{
		List<WorkUnit> workUnits7 = new ArrayList<>(workflow.getRootWorkUnit().getTargetContainer().getDescendants());

		KeyHolder keyHolder9 = new GeneratedKeyHolder();

		StringBuilder sql9 = new StringBuilder("insert into " + predicateTable + " (id, workunit_id, parent_predicate_id, type, relation_type) values ");
		Map<String, Object> parameters9 = new HashMap<>();

		{
			int j=0;
			for (WorkUnit workUnit: workUnits7)
			{
				if (workUnit.getFlowController() != null)
				{
					StoredProcedure procedure = new GenericStoredProcedure();
					DataSource dataSource = (DataSource) serviceLocator.getInstance("dataSource");

					procedure.setDataSource(dataSource);
					procedure.setSql("NEXTVALS");
					procedure.setFunction(false); 

					SqlParameter[] parameters = {
							new SqlOutParameter("current_value", Types.BIGINT),

							new SqlParameter(Types.INTEGER)
					};

					procedure.setParameters(parameters);
					procedure.compile();

					int predicateNumber = workUnit.getFlowController().getPredicate().getDescendants().size();
					Map<String, Object> result = procedure.execute( predicateNumber);

					long predicateFirstId = (long) result.get("current_value");

					for (Predicate predicate: workUnit.getFlowController().getPredicate().getDescendants()) predicate.setId(predicateFirstId++);

					sql9.append("(:value").append((j+1)*5-4).append(", :value").append((j+1)*5-3).append(", :value").append((j+1)*5-2).append(", :value").append((j+1)*5-1).append(", :value").append((j+1)*5).append("), ");
					parameters9.put("value" + ((j+1)*5-4), workUnit.getFlowController().getPredicate().getId());
					parameters9.put("value" + ((j+1)*5-3), workUnit.getId());
					parameters9.put("value" + ((j+1)*5-2), null);
					parameters9.put("value" + ((j+1)*5-1), workUnit.getFlowController().getPredicate().getTypeId());

					parameters9.put("value" + ((j+1)*5), (workUnit.getFlowController().getPredicate().hasChildren() ? predicateTypeService.getTypeId(workUnit.getFlowController().getPredicate().getClass()) : null));

					j++;

					for (Predicate predicate: workUnit.getFlowController().getPredicate().getDescendants())
					{
						for (Predicate childPredicate : predicate.getChildren())
						{
							sql9.append("(:value").append((j+1)*5-4).append(", :value").append((j+1)*5-3).append(", :value").append((j+1)*5-2).append(", :value").append((j+1)*5-1).append(", :value").append((j+1)*5).append("), ");
							parameters9.put("value" + ((j+1)*5-4), childPredicate.getId());
							parameters9.put("value" + ((j+1)*5-3), workUnit.getId());
							parameters9.put("value" + ((j+1)*5-2), predicate.getId());
							parameters9.put("value" + ((j+1)*5-1), childPredicate.getTypeId());

							parameters9.put("value" + ((j+1)*5), (childPredicate.hasChildren() ? predicateTypeService.getTypeId(childPredicate.getClass()) : null));

							j++;
						}
					}
				}
			}

			sql9.setLength(sql9.length()-2);

			if (j>0)
			{
				namedParameterJdbcTemplate.update(sql9.toString(), new MapSqlParameterSource(parameters9), keyHolder9);
			}
		}

		List<Map<String,Object>> insertedPredicates = keyHolder9.getKeyList();

		return insertedPredicates;
	}

	public void addDeclaredVariables(Workflow workflow)
	{
		List<WorkUnit> workUnits = new ArrayList<WorkUnit>(workflow.getRootWorkUnit().getTargetContainer().getDescendants());

		StringBuilder sql = new StringBuilder("insert into " + declaredVariableTable + " (workunit_id, variable_ordinal, name, value) values ");
		Map<String, Object> parameters = new HashMap<String, Object>();

		int i=0;
		for (WorkUnit workUnit : workUnits)
		{
			int j=0;
			for (Map.Entry<String, Object> variable : workUnit.getVariables().entrySet())
			{
				sql.append("(:workunit_id").append(i).append(", :variable_ordinal").append(i).append(", :name").append(i).append(", :value").append(i).append("), ");

				parameters.put("workunit_id" + i, workUnit.getId());
				parameters.put("variable_ordinal" + i, j++);
				parameters.put("name" + i, variable.getKey());

				parameters.put("value" + i, (variable.getValue() != null? variable.getValue().toString(): null));

				i++;
			}
		}
		sql.setLength(sql.length()-2);

		if (i>0)
		{
			namedParameterJdbcTemplate.update(sql.toString(), new MapSqlParameterSource(parameters));
		}
	}

	public void addParameters(Workflow workflow)
	{
		List<WorkUnit> workUnits = new ArrayList<WorkUnit>(workflow.getRootWorkUnit().getTargetContainer().getDescendants());

		StringBuilder sql = new StringBuilder("insert into " + parameterTable + " (workunit_id, predicate_id, parameter_ordinal, variable_name) values ");
		Map<String, Object> parameters = new HashMap<String, Object>();

		int i=0;
		for (WorkUnit workUnit : workUnits)
		{
			if (workUnit.isCommand() && !CollectionUtils.isEmpty(workUnit.getCommand().getParameters()))
			{
				int j=0;
				for (String parameter : workUnit.getCommand().getParameters())
				{
					sql.append("(:value").append(i).append(", :value").append(i+1).append(", :value").append(i+2).append(", :value").append(i+3).append("), ");

					parameters.put("value" + i, workUnit.getId());
					parameters.put("value" + (i+1), null);
					parameters.put("value" + (i+2), j++);
					parameters.put("value" + (i+3), parameter);

					i=i+4;
				}
			}

			else if (workUnit.isFlowController())
			{
				for (Predicate predicate : workUnit.getFlowController().getPredicate().getDescendants())
				{
					if (!CollectionUtils.isEmpty(predicate.getParameters()))
					{
						int j=0;
						for (String parameter : predicate.getParameters())
						{
							sql.append("(:value").append(i).append(", :value").append(i+1).append(", :value").append(i+2).append(", :value").append(i+3).append("), ");

							parameters.put("value" + i, null);
							parameters.put("value" + (i+1), predicate.getId());
							parameters.put("value" + (i+2), j++);
							parameters.put("value" + (i+3), parameter);

							i=i+4;
						}
					}
				}
			}
		}		
		sql.setLength(sql.length()-2);

		if (i>0)
		{
			namedParameterJdbcTemplate.update(sql.toString(), new MapSqlParameterSource(parameters));
		}
	}

	public void addReturnedVariables(Workflow workflow)
	{
		List<WorkUnit> workUnits = new ArrayList<WorkUnit>(workflow.getRootWorkUnit().getTargetContainer().getDescendants());

		StringBuilder sql = new StringBuilder("insert into " + returnedVariableTable + " (workunit_id, variable_ordinal, variable_name) values ");
		Map<String, Object> parameters = new HashMap<String, Object>();

		int i=0;
		for (WorkUnit workUnit : workUnits)
		{
			if (!workUnit.isCommand() && !CollectionUtils.isEmpty(workUnit.getReturnedVariables()))
			{
				int j=0;
				for (String variable : workUnit.getReturnedVariables())
				{
					sql.append("(:workunit_id").append(i).append(", :variable_ordinal").append(i).append(", :variable_name").append(i).append("), ");

					parameters.put("workunit_id" + i, workUnit.getId());
					parameters.put("variable_ordinal" + i, j++);
					parameters.put("variable_name" + i, variable);

					i++;
				}
			}
		}		
		sql.setLength(sql.length()-2);

		if (i>0)
		{
			namedParameterJdbcTemplate.update(sql.toString(), new MapSqlParameterSource(parameters));
		}
	}

	@Transactional
	public long saveWorkflow(Workflow workflow) {
		addWorkflow(workflow);

		List<Map<String,Object>> insertedWorkUnits = addWorkUnits(workflow);

		addTreePaths(workflow);

		List<Map<String,Object>> insertedPredicates = addPredicates(workflow, insertedWorkUnits);

		addDeclaredVariables(workflow);

		addParameters(workflow);

		addReturnedVariables(workflow);

		return workflow.getId();
	}
}