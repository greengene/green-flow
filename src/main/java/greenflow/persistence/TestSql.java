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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.object.GenericStoredProcedure;
import org.springframework.jdbc.object.StoredProcedure;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;
import greenflow.predicate.Predicates;
import greenflow.utilities.*;

import javax.sql.DataSource;
import java.sql.Types;
import java.util.*;

@Repository
public class TestSql {
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

	public void test(DataSource dataSource)
	{
		StoredProcedure procedure = new GenericStoredProcedure();

		procedure.setDataSource(dataSource);
		procedure.setSql("MY_PROCEDURE");
		procedure.setFunction(false); 

		SqlParameter[] parameters = {
				new SqlOutParameter("my_output", Types.INTEGER),
				new SqlOutParameter("out_a_1", Types.INTEGER),
				new SqlOutParameter("out_a_2", Types.INTEGER),
				new SqlOutParameter("out_a_3", Types.INTEGER),

				new SqlParameter(Types.INTEGER)
		};

		procedure.setParameters(parameters);
		procedure.compile();

		int predicateNumber = 1;

		Map<String, Object> result = procedure.execute( predicateNumber);

		int my_output = (int) result.get("my_output");
		int out_a_1 = (int) result.get("out_a_1");
		int out_a_2 = (int) result.get("out_a_2");
		int out_a_3 = (int) result.get("out_a_3");

		System.out.println("DONE");
	}
}