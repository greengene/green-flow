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

package greenflow.utilities;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Map;

import javanet.staxutils.IndentingXMLEventWriter;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import greenflow.flowcontroller.ElseFlowController;
import greenflow.predicate.Predicate;
import greenflow.workflow.Workflow;
import greenflow.workunit.WorkUnit;

import com.google.common.base.Joiner;

@Repository
public class WorkflowXmlUtilities {
	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private DefaultServiceLocator serviceLocator;

	@Autowired
	private PredicateTypeService predicateTypeService;

	public String workflowToXml(Workflow workflow)
	{
		OutputStream outputStream = null;

		try
		{
			XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();

			outputStream = new ByteArrayOutputStream();

			IndentingXMLEventWriter eventWriter = new IndentingXMLEventWriter(xmlOutputFactory.createXMLEventWriter(new BufferedOutputStream(outputStream), "UTF-8"));
			eventWriter.setIndent("    ");
			XMLEventFactory eventFactory = XMLEventFactory.newInstance();

			eventWriter.add(eventFactory.createStartDocument());

			eventWriter.add(eventFactory.createStartElement("", "", "workflow"));
			if (workflow.getName() != null)
			{
				eventWriter.add(eventFactory.createAttribute("name", workflow.getName()));
			}
			eventWriter.add(eventFactory.createAttribute("persistence-id", String.valueOf(workflow.getId())));

			workUnitToXml(workflow.getRootWorkUnit(), eventWriter, eventFactory);

			eventWriter.add(eventFactory.createEndElement("", "", "workflow"));

			eventWriter.add(eventFactory.createEndDocument());
		}
		catch (FactoryConfigurationError e)
		{
			e.printStackTrace();
		}
		catch (XMLStreamException e)
		{
			e.printStackTrace();
		}

		return outputStream.toString();
	}

	private void workUnitToXml(WorkUnit workUnit, XMLEventWriter eventWriter, XMLEventFactory eventFactory) throws XMLStreamException
	{
		if(workUnit.isFlowController())
		{
			eventWriter.add(eventFactory.createStartElement("", "", "flow-controller"));

			eventWriter.add(eventFactory.createAttribute("type", workUnit.getFlowController().getSymbolicName()));
			eventWriter.add(eventFactory.createAttribute("id", workUnit.getTargetContainer().getBreadcrumbId()));
			eventWriter.add(eventFactory.createAttribute("persistence-id", String.valueOf(workUnit.getTargetContainer().getId())));

			if (!(workUnit.getFlowController() instanceof ElseFlowController))
			{
				predicateToXml(workUnit.getFlowController().getPredicate(), eventWriter, eventFactory);
			}

			if (!MapUtils.isEmpty(workUnit.getVariables()))
			{
				eventWriter.add(eventFactory.createStartElement("", "", "variables"));

				for (Map.Entry<String, Object> variable : workUnit.getVariables().entrySet())
				{
					eventWriter.add(eventFactory.createStartElement("", "", "variable"));

					eventWriter.add(eventFactory.createAttribute("name", variable.getKey()));
					if (variable.getValue() != null)
					{
						eventWriter.add(eventFactory.createAttribute("value", String.valueOf(variable.getValue())));
					}

					eventWriter.add(eventFactory.createEndElement("", "", "variable"));
				}

				eventWriter.add(eventFactory.createEndElement("", "", "variables"));
			}

			for (WorkUnit childWorkUnit : workUnit.getChildWorkUnits()) workUnitToXml(childWorkUnit, eventWriter, eventFactory);

			if (workUnit.isReturnAtCompletion())
			{
				eventWriter.add(eventFactory.createStartElement("", "", "return"));

				if (!CollectionUtils.isEmpty(workUnit.getReturnedVariables()))
				{
					StringBuilder variableList = new StringBuilder("{");

					for (String variable : workUnit.getReturnedVariables())
					{
						variableList.append(variable).append(", ");
					}
					variableList.setLength(variableList.length()-2);
					variableList.append("}");

					eventWriter.add(eventFactory.createAttribute("variable-names", variableList.toString()));
				}

				if (!workUnit.isPersistAfterReturn())
				{
					eventWriter.add(eventFactory.createAttribute("persist-after-return", "false"));
				}

				eventWriter.add(eventFactory.createEndElement("", "", "return"));
			}

			eventWriter.add(eventFactory.createEndElement("", "", "flow-controller"));
		}

		else if (workUnit.isCommand())
		{
			eventWriter.add(eventFactory.createStartElement("", "", "command"));
			eventWriter.add(eventFactory.createAttribute("type", workUnit.getCommand().getSymbolicName()));

			if (!CollectionUtils.isEmpty(workUnit.getCommand().getParameters()))
			{
				eventWriter.add(eventFactory.createAttribute("parameters", "{" + Joiner.on(", ").join(workUnit.getCommand().getParameters()) + "}"));
			}

			if (workUnit.getCommand().getAssignTo() != null)
			{
				eventWriter.add(eventFactory.createAttribute("assign-to", workUnit.getCommand().getAssignTo()));
			}

			eventWriter.add(eventFactory.createAttribute("id", workUnit.getTargetContainer().getBreadcrumbId()));
			eventWriter.add(eventFactory.createAttribute("persistence-id", String.valueOf(workUnit.getTargetContainer().getId())));

			for (WorkUnit childWorkUnit : workUnit.getChildWorkUnits()) workUnitToXml(childWorkUnit, eventWriter, eventFactory);			

			eventWriter.add(eventFactory.createEndElement("", "", "command"));
		}
		else
		{
			eventWriter.add(eventFactory.createStartElement("", "", "container-block"));
			eventWriter.add(eventFactory.createAttribute("id", workUnit.getTargetContainer().getBreadcrumbId()));
			eventWriter.add(eventFactory.createAttribute("persistence-id", String.valueOf(workUnit.getTargetContainer().getId())));

			if (!MapUtils.isEmpty(workUnit.getVariables()))
			{
				eventWriter.add(eventFactory.createStartElement("", "", "variables"));

				for (Map.Entry<String, Object> variable : workUnit.getVariables().entrySet())
				{
					eventWriter.add(eventFactory.createStartElement("", "", "variable"));

					eventWriter.add(eventFactory.createAttribute("name", variable.getKey()));
					if (variable.getValue() != null)
					{
						eventWriter.add(eventFactory.createAttribute("value", String.valueOf(variable.getValue())));
					}

					eventWriter.add(eventFactory.createEndElement("", "", "variable"));
				}

				eventWriter.add(eventFactory.createEndElement("", "", "variables"));
			}

			for (WorkUnit childWorkUnit : workUnit.getChildWorkUnits()) workUnitToXml(childWorkUnit, eventWriter, eventFactory);

			if (workUnit.isReturnAtCompletion())
			{
				eventWriter.add(eventFactory.createStartElement("", "", "return"));

				if (!CollectionUtils.isEmpty(workUnit.getReturnedVariables()))
				{
					StringBuilder variableList = new StringBuilder("{");

					for (String variable : workUnit.getReturnedVariables())
					{
						variableList.append(variable).append(", ");
					}
					variableList.setLength(variableList.length()-2);
					variableList.append("}");

					eventWriter.add(eventFactory.createAttribute("variable-names", variableList.toString()));
				}

				if (!workUnit.isPersistAfterReturn())
				{
					eventWriter.add(eventFactory.createAttribute("persist-after-return", "false"));
				}

				eventWriter.add(eventFactory.createEndElement("", "", "return"));
			}

			eventWriter.add(eventFactory.createEndElement("", "", "container-block"));
		}
	}

	private void predicateToXml(Predicate predicate, XMLEventWriter eventWriter, XMLEventFactory eventFactory) throws XMLStreamException
	{
		eventWriter.add(eventFactory.createStartElement("", "", "predicate"));

		if (predicate.hasChildren())
		{
			eventWriter.add(eventFactory.createAttribute("relation", predicate.getSymbolicName()));
			eventWriter.add(eventFactory.createAttribute("persistence-id", String.valueOf(predicate.getId())));

			for (Predicate childPredicate: predicate.getChildren())
			{
				predicateToXml(childPredicate, eventWriter, eventFactory);
			}
		}
		else
		{
			eventWriter.add(eventFactory.createAttribute("type", predicate.getSymbolicName()));

			if (!CollectionUtils.isEmpty(predicate.getParameters()))
			{
				eventWriter.add(eventFactory.createAttribute("parameters", "{" + Joiner.on(", ").join(predicate.getParameters()) + "}"));
			}

			eventWriter.add(eventFactory.createAttribute("persistence-id", String.valueOf(predicate.getId())));
		}

		eventWriter.add(eventFactory.createEndElement("", "", "predicate"));
	}
}