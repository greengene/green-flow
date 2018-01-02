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

import greenflow.exception.WorkflowException;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class EnhancedResultSet
{
	private ResultSet resultSet;

	public EnhancedResultSet(ResultSet resultSet)
	{
		this.resultSet = resultSet;
	}

	 <V> V getValue(String tableName, String columnName) throws SQLException
	{
		ResultSetMetaData metaData = resultSet.getMetaData();

		for (int i=1; i<=metaData.getColumnCount(); i++)
		{
			if (metaData.getColumnLabel(i).equalsIgnoreCase(columnName) && metaData.getTableName(i).equalsIgnoreCase(tableName))
			{
				return (V) resultSet.getObject(i);
			}
		}

		throw new WorkflowException(String.format("Strange. I cannot find this combination of table/column: tableName = %s, columnName = %s", tableName, columnName));
	}

	public <U> U get(String tableName, String columnName) throws SQLException
	{
		return getValue(tableName, columnName);
	}

	public <U> U  get(String aliasName) throws SQLException
	{
		return (U) resultSet.getObject(aliasName);
	}

	public boolean isNull(String tableName, String columnName) throws SQLException
	{
		return get(tableName, columnName) == null;
	}

	public boolean isNull(String aliasName) throws SQLException
	{
		return get(aliasName) == null;
	}
}