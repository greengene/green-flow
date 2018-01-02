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

package greenflow.container;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import greenflow.container.interaction.ConcreteContainerElementAdapter;
import greenflow.container.interaction.TargetContainerElement;

public class GenericContainerNode<T>  {
	private long id;

	private Map<Long, GenericContainerNode<T>> locateId;

	private Map<T, GenericContainerNode<T>> locate;

	private Tree<T> coreTree;

	public GenericContainerNode() {}

	public TargetContainerElement<T> insertRoot(T head) {
		setLocate(new HashMap<T, GenericContainerNode<T>>());
		getLocate().put(head, this);

		setCoreTree(new Tree<T>(head));

		ConcreteContainerElementAdapter<T> concreteContainerAdapter = new ConcreteContainerElementAdapter<T>();

		concreteContainerAdapter.setAdaptee(this);

		setLocateId(new HashMap<Long, GenericContainerNode<T>>());

		return concreteContainerAdapter;
	}

	public TargetContainerElement<T> addLeaf(T leaf) {
		Tree<T> leafTree = getCoreTree().addLeaf(leaf);

		GenericContainerNode<T> interfacedContainer = new GenericContainerNode<T>();
		interfacedContainer.setCoreTree(leafTree);

		ConcreteContainerElementAdapter<T> concreteContainerAdapter = new ConcreteContainerElementAdapter<T>();

		concreteContainerAdapter.setAdaptee(interfacedContainer);

		interfacedContainer.setLocateId(getLocateId());

		interfacedContainer.setLocate(getLocate());
		interfacedContainer.getLocate().put(leaf, interfacedContainer);

		return concreteContainerAdapter;
	}

	public GenericContainerNode<T> getTree(T element) {
		return getLocate().get(element);
	}

	public GenericContainerNode<T> getTreeByContainerId(long containerId) {
		return getLocateId().get(containerId);
	}

	public String getBreadcrumbId() {
		if (getCoreTree().getParent() != null) {
			List<Tree<T>> list = new ArrayList<Tree<T>>(getCoreTree().getParent().getSubTrees());

			int index = list.indexOf(getCoreTree());

			if (index != -1) {
				return getLocate().get(getCoreTree().getParent().getHead()).getBreadcrumbId() +  "." + (index + 1);
			}

			return "";
		}
		else return "1";
	}

	@Override
	public String toString() {
		return getCoreTree().toString();
	}

	public Tree<T> getCoreTree() {
		return coreTree;
	}

	public void setCoreTree(Tree<T> coreTree) {
		this.coreTree = coreTree;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Map<Long, GenericContainerNode<T>> getLocateId() {
		return locateId;
	}

	public void setLocateId(Map<Long, GenericContainerNode<T>> locateContainerId) {
		this.locateId = locateContainerId;
	}

	public Map<T, GenericContainerNode<T>> getLocate() {
		return locate;
	}

	public void setLocate(Map<T, GenericContainerNode<T>> locate) {
		this.locate = locate;
	}
}