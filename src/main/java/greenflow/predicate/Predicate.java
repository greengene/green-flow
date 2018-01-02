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

package greenflow.predicate;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import greenflow.flowcontroller.FlowController;

import greenflow.predicate.relation.PredicateArray;
import greenflow.utilities.DefaultServiceLocator;
import greenflow.utilities.PredicateTypeService;

import javax.annotation.PostConstruct;

@Component
@Scope("prototype")
public abstract class Predicate {
	@Autowired
	private DefaultServiceLocator serviceLocator;

	@Autowired
	private PredicateTypeService predicateTypeService;

	private Predicate[] predicates = {};

	private long id;

	private FlowController wrapperFlowController;

	private List<String> parameters = new ArrayList<>();

	public Predicate() {}

	public Predicate(PredicateArray predicates)
	{
		this.predicates = predicates.getPredicates();
	}

	public boolean hasChildren()
	{
		return predicates.length > 0;
	}

	public Predicate[] getPredicates()
	{
		return predicates;
	}

	public List<Predicate> getDescendants()
	{
		return Stream.concat(Stream.of(this),
				Arrays.stream(getPredicates())
						.flatMap(e -> e.getDescendants().stream()))
				.collect(Collectors.toList());
	}

	public Collection<Predicate> getChildren()
	{
		return Arrays.asList(getPredicates());
	}

	@PostConstruct
	protected void init()
	{
		Class<? extends Predicate> clazz = getClass();

		predicateTypeService.registerType(clazz);
	}

	abstract public boolean isSatisfied();

	public abstract String getSymbolicName();

	public int getTypeId()
	{
		return predicateTypeService.getTypeId(getClass());
	}

	public FlowController getWrapperFlowController()
	{
		return wrapperFlowController;
	}

	public void setWrapperFlowController(FlowController wrapperFlowController)
	{
		this.wrapperFlowController = wrapperFlowController;
	}

	public long getId()
	{
		return id;
	}

	public void setId(long id)
	{
		this.id = id;
	}

	public List<String> getParameters()
	{
		if (parameters == null)
		{
			setParameters(new ArrayList<String>());
		}
		return parameters;
	}

	private void setParameters(List<String> parameters)
	{
		this.parameters = parameters;
	}
}