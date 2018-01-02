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

package greenflow.test;

import java.util.List;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import greenflow.command.Command;
import greenflow.command.concrete.ValueAssignerCommand;
import greenflow.conversation.*;
import greenflow.flowcontroller.FlowController;
import greenflow.flowcontroller.WhileFlowController;
import greenflow.persistence.WorkflowConfigurationDao;
import greenflow.predicate.Predicate;
import greenflow.predicate.Predicates;
import greenflow.test.command.ConsolePrinterCommand;
import greenflow.test.command.IntegerIncrementerCommand;
import greenflow.test.predicate.IntegerLessThanPredicate;
import greenflow.utilities.DefaultServiceLocator;
import greenflow.utilities.WorkflowXmlUtilities;
import greenflow.workflow.Workflow;
import greenflow.workunit.WorkUnit;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:spring-module.xml"})
public class FlowControllerTest
{
	private static final Logger logger = LoggerFactory.getLogger(FlowControllerTest.class);

	private Workflow workflow;

	private Workflow workflow1;
	private Workflow workflow2;

	@Autowired
	private WorkflowXmlUtilities workflowXmlUtilities;

	@Autowired
	private WorkflowConfigurationDao workUnitConfigurationDao;

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private DefaultServiceLocator serviceLocator;

	@Autowired
	private Predicates predicates;

	@Autowired
	private ConversationFactory conversationFactory;

	@BeforeClass
	public static void testSetup()
	{
	}

	@AfterClass
	public static void testCleanup()
	{
	}

    @Before
    public void setUp()
    {
    	{
			workflow = new Workflow(null);

			WorkUnit rootWorkUnit = workflow.getRootWorkUnit();

			rootWorkUnit.getVariables().put("counter", 0);

			rootWorkUnit.setReturnAtCompletion(true);
			rootWorkUnit.getReturnedVariables().add("counter");

			Predicate predicate_01 = serviceLocator.getInstance(IntegerLessThanPredicate.class);
			predicate_01.getParameters().add("counter");
			predicate_01.getParameters().add("'10'");
			FlowController flowController_01 = serviceLocator.getInstance(WhileFlowController.class);
			flowController_01.setPredicate(predicate_01);
			WorkUnit workUnit_01 = new WorkUnit(flowController_01);

			workUnit_01.setReturnAtCompletion(true);

			workUnit_01.setPersistAfterReturn(true);
			workUnit_01.getReturnedVariables().add("counter");

			rootWorkUnit.addChildWorkUnit(workUnit_01);

			Command<Integer> command_02 = serviceLocator.getInstance(IntegerIncrementerCommand.class);
			command_02.getParameters().add("counter");
			command_02.setAssignTo("counter");
			WorkUnit workUnit_02 = new WorkUnit(command_02);
			workUnit_01.addChildWorkUnit(workUnit_02);

			Command<Object> command_03 = serviceLocator.getInstance(ConsolePrinterCommand.class);
			command_03.getParameters().add("counter");
			WorkUnit workUnit_03 = new WorkUnit(command_03);
			workUnit_01.addChildWorkUnit(workUnit_03);

			workflow1 = workflow;
    	}

    	{
			workflow = new Workflow(null);

			WorkUnit rootWorkUnit = workflow.getRootWorkUnit();

			rootWorkUnit.getVariables().put("counter_i", 0);

			rootWorkUnit.setReturnAtCompletion(true);
			rootWorkUnit.getReturnedVariables().add("counter_i");

			Predicate predicate_01 = serviceLocator.getInstance(IntegerLessThanPredicate.class);
			predicate_01.getParameters().add("counter_i");
			predicate_01.getParameters().add("'10'");
			FlowController flowController_01 = serviceLocator.getInstance(WhileFlowController.class);
			flowController_01.setPredicate(predicate_01);
			WorkUnit workUnit_01 = new WorkUnit(flowController_01);

			workUnit_01.getVariables().put("counter_j", 0);

			rootWorkUnit.addChildWorkUnit(workUnit_01);

			Command<Integer> command_02 = serviceLocator.getInstance(IntegerIncrementerCommand.class);
			command_02.getParameters().add("counter_i");
			command_02.setAssignTo("counter_i");
			WorkUnit workUnit_02 = new WorkUnit(command_02);
			workUnit_01.addChildWorkUnit(workUnit_02);

			Command<Object> command_03 = serviceLocator.getInstance(ConsolePrinterCommand.class);
			command_03.getParameters().add("counter_i");
			WorkUnit workUnit_03 = new WorkUnit(command_03);
			workUnit_01.addChildWorkUnit(workUnit_03);

			Command<Object> command_04 = serviceLocator.getInstance(ValueAssignerCommand.class);
			command_04.getParameters().add("counter_j");
			command_04.getParameters().add("'0'");

			WorkUnit workUnit_04 = new WorkUnit(command_04);
			workUnit_01.addChildWorkUnit(workUnit_04);

			Predicate predicate_05 = serviceLocator.getInstance(IntegerLessThanPredicate.class);
			predicate_05.getParameters().add("counter_j");
			predicate_05.getParameters().add("'10'");
			FlowController flowController_05 = serviceLocator.getInstance(WhileFlowController.class);
			flowController_05.setPredicate(predicate_05);
			WorkUnit workUnit_05 = new WorkUnit(flowController_05);

			workUnit_05.setReturnAtCompletion(true);

			workUnit_05.setPersistAfterReturn(true);

			workUnit_05.getReturnedVariables().add("counter_i");
			workUnit_05.getReturnedVariables().add("counter_j");

			workUnit_01.addChildWorkUnit(workUnit_05);

			Command<Integer> command_06 = serviceLocator.getInstance(IntegerIncrementerCommand.class);
			command_06.getParameters().add("counter_j");
			command_06.setAssignTo("counter_j");
			WorkUnit workUnit_06 = new WorkUnit(command_06);
			workUnit_05.addChildWorkUnit(workUnit_06);

			Command<Object> command_07 = serviceLocator.getInstance(ConsolePrinterCommand.class);
			command_07.getParameters().add("counter_j");
			WorkUnit workUnit_07 = new WorkUnit(command_07);
			workUnit_05.addChildWorkUnit(workUnit_07);

			workflow2 = workflow;
    	}
    }

	@Test
	public void WhileFlowController_IteratingWithReturnWithinWhile_IteratingOnWhile10Times()
	{
		workflow = workflow1;

		System.out.println("Workflow before persisting configuration:\n");

		System.out.println(workflow);

		System.out.println();

		String xmlString = workflowXmlUtilities.workflowToXml(workflow);

		System.out.println(xmlString);

		long workflowId = workUnitConfigurationDao.saveWorkflow(workflow);

		System.out.println("\n******************************************************\n");

		System.out.println("Workflow after persisting configuration:\n");

		System.out.println(workflow);

		System.out.println();

		xmlString = workflowXmlUtilities.workflowToXml(workUnitConfigurationDao.retrieveConfigurationWorkflow(workflowId));

		System.out.println(xmlString);

		TransactionData transactionData = new TransactionData();

		Transaction transaction = null;

		List<TransactionResult<?>> transactionResults;

		Conversation conversation = conversationFactory.startConversation(workflowId);

		System.out.println("\n******************************************************\n");

		while ((transaction = conversation.startTransaction(transactionData)) != null)
		{
			transactionResults = transaction.execute();

			if (transactionResults.size() > 0)
			{
				System.out.println("\nTransaction result(s): counter: " + transactionResults.get(0).getData().toString());
			}

			System.out.println("\nExecuted workflow:\n");
			System.out.println(transaction.workflowToString());

			System.out.println("\n******************************************************\n");
		}
	}

	@Test
	public void WhileFlowController_IteratingWithReturnWithinNestedWhileOverMultidimensionalArray_IteratingOnWhile100Times()
	{
		workflow = workflow2;

		System.out.println("Workflow before persisting configuration:\n");

		System.out.println(workflow);

		System.out.println();

		String xmlString = workflowXmlUtilities.workflowToXml(workflow);

		System.out.println(xmlString);

		long workflowId = workUnitConfigurationDao.saveWorkflow(workflow);

		System.out.println("\n******************************************************\n");

		System.out.println("Workflow after persisting configuration:\n");

		System.out.println(workflow);

		System.out.println();

		xmlString = workflowXmlUtilities.workflowToXml(workUnitConfigurationDao.retrieveConfigurationWorkflow(workflowId));

		System.out.println(xmlString);

		TransactionData transactionData = new TransactionData();

		Transaction transaction = null;

		List<TransactionResult<?>> transactionResults;

		Conversation conversation = conversationFactory.startConversation(workflowId);

		System.out.println("\n******************************************************\n");

		while ((transaction = conversation.startTransaction(transactionData)) != null)
		{
			transactionResults = transaction.execute();

			if (transactionResults.size() == 2)
			{
				System.out.println("\nTransaction result(s):");
				System.out.println("counter_i: " + transactionResults.get(0).getData().toString());
				System.out.println("counter_j: " + transactionResults.get(1).getData().toString());
			}

			System.out.println("\nExecuted workflow:\n");
			System.out.println(transaction.workflowToString());

			System.out.println("\n******************************************************\n");
		}
	}
}