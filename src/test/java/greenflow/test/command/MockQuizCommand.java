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

import java.util.Random;

import greenflow.xmlbeans.Address;
import greenflow.xmlbeans.Name;
import greenflow.xmlbeans.PersonSubject;

import org.apache.xmlbeans.XmlObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import greenflow.command.Command;
import greenflow.command.CommandResult;

@Component
@Scope("prototype")
public class MockQuizCommand extends Command<XmlObject> {
	private static final Logger logger = LoggerFactory.getLogger(MockQuizCommand.class);

	@Override
	public CommandResult<XmlObject> execute() {
		final PersonSubject personSubject = PersonSubject.Factory.newInstance();

		Name name =	personSubject.addNewName();
		if (new Random().nextDouble() > 0.5)
		{
			name.setFirst("John");
			name.setLast("Smith");
			Address address = Address.Factory.newInstance();
			address.setState("GA");
			personSubject.getAddressList().add(address);
		}
		else
		{
			name.setFirst("John");
			name.setLast("Smith");
			Address address = Address.Factory.newInstance();
			address.setState("FL");
			personSubject.getAddressList().add(address);
		}

		return new CommandResult<XmlObject>() {
			public XmlObject getData()
			{
				return personSubject;
			}
		};
	}

	@Override
	public String getSymbolicName() {
		return "mock-quiz-command";
	}
}