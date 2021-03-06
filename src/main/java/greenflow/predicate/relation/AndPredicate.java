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

package greenflow.predicate.relation;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import greenflow.predicate.Predicate;

import java.util.Arrays;
import java.util.stream.Collectors;

@Component
@Scope("prototype")
public class AndPredicate extends RelationPredicate
{
	public AndPredicate(PredicateArray predicates)
	{
		super(predicates);
	}

	@Override
	public boolean isSatisfied()
	{
		return Arrays.stream(getPredicates()).allMatch(Predicate::isSatisfied);
	}

	@Override
	public String getSymbolicName()
	{
		return "(" + Arrays.stream(getPredicates()).map(Predicate::getSymbolicName).collect(Collectors.joining(" && ")) + ")";
	}
}