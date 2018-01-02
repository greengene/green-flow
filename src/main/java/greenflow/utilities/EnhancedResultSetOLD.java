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

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class EnhancedResultSetOLD
{
	private ResultSet resultSet;

	private Object value = null;

	public EnhancedResultSetOLD(ResultSet resultSet)
	{
		this.resultSet = resultSet;
	}

	public boolean isNull(String tableName, String columnName)
	{
		class IsNullEnhancedResultSet extends EnhancedResultSetOLD {
			private IsNullEnhancedResultSet(ResultSet resultSet) {
				super(resultSet);
			}
		}

		return true;
	}

	synchronized Object getValue(String tableName, String columnName, DoSomething doSomething) throws SQLException
	{
		ResultSetMetaData metaData = resultSet.getMetaData();

		for (int i=1; i<=metaData.getColumnCount(); i++)
		{
			if (metaData.getColumnLabel(i).equalsIgnoreCase(columnName) && metaData.getTableName(i).equalsIgnoreCase(tableName))
			{
				doSomething.getConcreteValueByColumnCounter(i);
			}
		}

		return value;
	}

	private abstract class DoSomething {
		abstract protected void getConcreteValueByColumnCounter(int i) throws SQLException;
	}

	public boolean wasNull() throws SQLException
	{
		return resultSet.wasNull();
	}

	public int getInt(final String tableName, final String columnName) throws SQLException
	{
		return (int)getValue(tableName, columnName,  
				new DoSomething()
				{
					@Override
					protected void getConcreteValueByColumnCounter(int i) throws SQLException {
						value = resultSet.getInt(i);
					}
				});
	}

	public int getInt(String aliasName) throws SQLException
	{
		return resultSet.getInt(aliasName);
	}

	public long getLong(String tableName, String columnName) throws SQLException
	{
		return (long)getValue(tableName, columnName,  
				new DoSomething()
				{
					@Override
					protected void getConcreteValueByColumnCounter(int i) throws SQLException {
						value = resultSet.getLong(i);
					}
				});
	}

	public long getLong(String aliasName) throws SQLException
	{
		return resultSet.getLong(aliasName);
	}

	public String getString(String tableName, String columnName) throws SQLException
	{
		return (String)getValue(tableName, columnName,  
				new DoSomething()
				{
					@Override
					protected void getConcreteValueByColumnCounter(int i) throws SQLException {
						value = resultSet.getString(i);
					}
				});
	}

	public String getString(String aliasName) throws SQLException
	{
		return resultSet.getString(aliasName);
	}

	public short getShort(String tableName, String columnName) throws SQLException
	{
		return (short)getValue(tableName, columnName,  
				new DoSomething()
				{
					@Override
					protected void getConcreteValueByColumnCounter(int i) throws SQLException {
						value = resultSet.getShort(i);
					}
				});
	}

	public short getShort(String aliasName) throws SQLException
	{
		return resultSet.getShort(aliasName);
	}

	public boolean getBoolean(final String tableName, final String columnName) throws SQLException
	{
		return (boolean)getValue(tableName, columnName,  
				new DoSomething()
				{
					@Override
					protected void getConcreteValueByColumnCounter(int i) throws SQLException {
						value = resultSet.getBoolean(i);
					}
				});
	}

	public boolean getBoolean(String aliasName) throws SQLException
	{
		return resultSet.getBoolean(aliasName);
	}
}