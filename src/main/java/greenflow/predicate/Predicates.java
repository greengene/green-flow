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

import java.util.Arrays;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import greenflow.predicate.relation.*;
import greenflow.utilities.DefaultServiceLocator;

@Service
public class Predicates
{
	@Autowired
	private DefaultServiceLocator serviceLocator;

	public Predicate and(Predicate... predicates)
	{
		return serviceLocator.getInstance(AndPredicate.class, new PredicateArray(
				Arrays.stream(predicates)
				.map(this::copyPredicate).toArray(Predicate[]::new)));
	}

	public Predicate or(Predicate... predicates)
	{
		return serviceLocator.getInstance(OrPredicate.class, new PredicateArray(
				Arrays.stream(predicates)
				.map(this::copyPredicate).toArray(Predicate[]::new)));
	}

	public Predicate not(Predicate predicate)
	{
		return serviceLocator.getInstance(NotPredicate.class, new PredicateArray(
				Stream.of(predicate)
				.map(this::copyPredicate).toArray(Predicate[]::new)));
	}

	public Predicate copyPredicate(Predicate predicate)
	{
		Predicate clonedPredicate = null;

		if (predicate != null)
		{
			if (!predicate.hasChildren())
			{
				clonedPredicate = serviceLocator.getInstance(predicate.getClass());
			}
			else
			{
				clonedPredicate = serviceLocator.getInstance(predicate.getClass(), new PredicateArray(Arrays.stream(predicate.getPredicates()).map(this::copyPredicate).toArray(Predicate[]::new)));
			}

			for (String parameter : predicate.getParameters())
			{
				clonedPredicate.getParameters().add(parameter);
			}

			if (predicate.hasChildren()) {
				RelationPredicate relationPredicate = (RelationPredicate) predicate;
				RelationPredicate clonedRelationPredicate = (RelationPredicate) clonedPredicate;
			}
		}

		return clonedPredicate;
	}
}