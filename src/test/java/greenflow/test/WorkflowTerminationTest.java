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

import org.apache.xmlbeans.XmlObject;
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
import greenflow.command.concrete.ExecutionTerminationCommand;
import greenflow.flowcontroller.ElseFlowController;
import greenflow.flowcontroller.ElseIfFlowController;
import greenflow.flowcontroller.FlowController;
import greenflow.flowcontroller.IfFlowController;
import greenflow.predicate.Predicate;
import greenflow.test.command.MockQuizCommand;
import greenflow.test.predicate.MockAuthenticationPredicate;
import greenflow.utilities.DefaultServiceLocator;
import greenflow.utilities.WorkflowXmlUtilities;
import greenflow.workflow.Workflow;
import greenflow.workunit.WorkUnit;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:spring-module.xml"})
public class WorkflowTerminationTest {
	private static final Logger logger = LoggerFactory.getLogger(WorkflowTerminationTest.class);

	private Workflow workflow;

	@Autowired
	private WorkflowXmlUtilities workflowXmlUtilities;

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private DefaultServiceLocator serviceLocator;

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
		workflow = new Workflow(null);

		Predicate predicate_01 = serviceLocator.getInstance(MockAuthenticationPredicate.class);
		FlowController flowController_01 = serviceLocator.getInstance(IfFlowController.class);
		flowController_01.setPredicate(predicate_01);
		WorkUnit workUnit_01 = new WorkUnit(flowController_01);
		workflow.getRootWorkUnit().addChildWorkUnit(workUnit_01);

		Command<XmlObject> command_02 = serviceLocator.getInstance(MockQuizCommand.class);
		WorkUnit workUnit_02 = new WorkUnit(command_02);
		workUnit_01.addChildWorkUnit(workUnit_02);

		Predicate predicate_03 = serviceLocator.getInstance(MockAuthenticationPredicate.class);
		FlowController flowController_03 = serviceLocator.getInstance(IfFlowController.class);
		flowController_03.setPredicate(predicate_03);
		WorkUnit workUnit_03 = new WorkUnit(flowController_03);
		workflow.getRootWorkUnit().addChildWorkUnit(workUnit_03);

		Command<XmlObject> command_04 = serviceLocator.getInstance(MockQuizCommand.class);
		WorkUnit workUnit_04 = new WorkUnit(command_04);
		workUnit_03.addChildWorkUnit(workUnit_04);

		Predicate predicate_05 = serviceLocator.getInstance(MockAuthenticationPredicate.class);
		FlowController flowController_05 = serviceLocator.getInstance(IfFlowController.class);
		flowController_05.setPredicate(predicate_05);
		WorkUnit workUnit_05 = new WorkUnit(flowController_05);
		workflow.getRootWorkUnit().addChildWorkUnit(workUnit_05);

		Command<String> command_06 = serviceLocator.getInstance(ExecutionTerminationCommand.class);
		WorkUnit workUnit_06 = new WorkUnit(command_06);
		workUnit_05.addChildWorkUnit(workUnit_06);

		Predicate predicate_08 = serviceLocator.getInstance(MockAuthenticationPredicate.class);
		FlowController flowController_08 = serviceLocator.getInstance(ElseIfFlowController.class);
		flowController_08.setPredicate(predicate_08);
		WorkUnit workUnit_08 = new WorkUnit(flowController_08);
		workflow.getRootWorkUnit().addChildWorkUnit(workUnit_08);

		Predicate predicate_09 = serviceLocator.getInstance(MockAuthenticationPredicate.class);
		FlowController flowController_09 = serviceLocator.getInstance(IfFlowController.class);
		flowController_09.setPredicate(predicate_09);
		WorkUnit workUnit_09 = new WorkUnit(flowController_09);
		workUnit_08.addChildWorkUnit(workUnit_09);

		Predicate predicate_10 = serviceLocator.getInstance(MockAuthenticationPredicate.class);
		FlowController flowController_10 = serviceLocator.getInstance(ElseFlowController.class);
		flowController_10.setPredicate(predicate_10);
		WorkUnit workUnit_10 = new WorkUnit(flowController_10);
		workUnit_08.addChildWorkUnit(workUnit_10);

		Command<XmlObject> command_11 = serviceLocator.getInstance(MockQuizCommand.class);
		WorkUnit workUnit_11 = new WorkUnit(command_11);
		workUnit_09.addChildWorkUnit(workUnit_11);

		Command<XmlObject> command_12 = serviceLocator.getInstance(MockQuizCommand.class);
		WorkUnit workUnit_12 = new WorkUnit(command_12);
		workUnit_10.addChildWorkUnit(workUnit_12);
    }

	@Test
	public void WorkflowExecution_WorkflowWithTerminationCommand_TerminatesExecutionWhenCommandExecuted()
	{
		System.out.println(workflow);

		System.out.println();

		System.out.println(workflowXmlUtilities.workflowToXml(workflow));

		System.out.println();

		workflow.execute();
	}
}