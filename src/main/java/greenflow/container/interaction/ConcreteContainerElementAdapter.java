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

package greenflow.container.interaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;

import greenflow.container.GenericContainerNode;
import greenflow.container.Tree;

public class ConcreteContainerElementAdapter<T> implements TargetContainerElement<T> {
	GenericContainerNode<T> adaptee;

	private GenericContainerNode<T> getAdaptee() {
		return adaptee;
	}

	public void setAdaptee(GenericContainerNode<T> adaptee) {
		this.adaptee = adaptee;
	}

	@Override
	public Collection<T> getChildren()
	{
		return getAdaptee().getCoreTree().getSuccessors(getAdaptee().getCoreTree().getHead());
	}

	@Override

	public List<T> getDescendants()
	{
		List<T> descendants = new ArrayList<T>();

		for (Tree<T> subTree : getDescendantTrees(getAdaptee().getCoreTree())) {
			descendants.add(subTree.getHead());
		}

		return descendants;
	}

	private List<Tree<T>> getDescendantTrees(Tree<T> tree)
	{
		List<Tree<T>> descendantTrees = new ArrayList<Tree<T>>(Arrays.asList(tree));

		for (Tree<T> subTree : tree.getSubTrees()) {
			descendantTrees.addAll(getDescendantTrees(subTree));
		}

		return descendantTrees;
	}

	@Override
	public void attachContainerIds(List<Long> idList) {
		attachContainerIds(getAdaptee(), idList);
	}

	private void attachContainerIds(GenericContainerNode<T> container, List<Long> idList) {
		container.setId(idList.get(0));
		getAdaptee().getLocateId().put(idList.get(0), container);

		for (Tree<T> subTree : container.getCoreTree().getSubTrees()) {
			idList.remove(0);

			attachContainerIds(getAdaptee().getLocate().get(subTree.getHead()), idList);
		}
	}

	@Override
	public Collection<Triple<T, T, Long>> getPaths() {
		return getPaths(getAdaptee().getCoreTree(), new HashSet<Triple<T, T, Long>>());
	}

	private Collection<Triple<T, T, Long>> getPaths(Tree<T> tree, Collection<Triple<T, T, Long>> paths) {
		Set<Triple<T, T, Long>> tempPaths = new HashSet<Triple<T, T, Long>>();

		paths.add(new ImmutableTriple<T, T, Long>(tree.getHead(), tree.getHead(), 0l));

		if (tree.getParent() != null) {
			for (Triple<T, T, Long> path : paths) {
				if (path.getMiddle() == tree.getParent().getHead()) {
					tempPaths.add(new ImmutableTriple<T, T, Long>(path.getLeft(), tree.getHead(), path.getRight()+1));
				}
			}
		}

		paths.addAll(tempPaths);

		for (Tree<T> subTree : tree.getSubTrees()) {
			paths.addAll(getPaths(subTree, paths));
		}

		return paths;
	}

	@Override
	public T getParent() {
		return getAdaptee().getCoreTree().getParent().getHead();
	}

	@Override
	public TargetContainerElement<T> addChildWorkUnit(T t) {
		return getAdaptee().addLeaf(t);
	}

	@Override
	public String getBreadcrumbId() {
		return getAdaptee().getBreadcrumbId();
	}

	@Override
	public void setId(long id) {
		getAdaptee().setId(id);
	}

	@Override
	public long getId() {
		return getAdaptee().getId();
	}

	@Override
	public String toString() {
		return getAdaptee().toString();
	}

	@Override
	public void updateLocateId(long id) {
		getAdaptee().getLocateId().put(id, getAdaptee());
	}
}