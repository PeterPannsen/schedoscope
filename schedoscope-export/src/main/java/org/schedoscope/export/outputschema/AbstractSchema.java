/**
 * Copyright 2016 Otto (GmbH & Co KG)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.schedoscope.export.outputschema;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;

/**
 * This class provides a couple of functions
 * common to all concrete Schema implementations.
 *
 * @author Otto
 *
 */
abstract public class AbstractSchema implements Schema {

	protected Configuration conf;

	public AbstractSchema(Configuration conf) {
		this.conf = conf;
	}

	@Override
	public String getTable() {
		return conf.get(Schema.JDBC_OUTPUT_TABLE);
	}

	@Override
	public String[] getColumnNames() {
		return conf.getStrings(Schema.JDBC_OUTPUT_COLUMN_NAMES);
	}

	@Override
	public String[] getColumnTypes() {
		return conf.getStrings(Schema.JDBC_OUTPUT_COLUMN_TYPES);
	}

	@Override
	public String getCreateTableQuery() {
		return conf.get(Schema.JDBC_CREATE_TABLE_QUERY);
	}

	@Override
	public int getNumberOfPartitions() {
		return conf.getInt(Schema.JDBC_NUMBER_OF_PARTITIONS, 1);
	}

	@Override
	public int getCommitSize() {
		return conf.getInt(Schema.JDBC_COMMIT_SIZE, 1);
	}

	@Override
	public String getFilter() {
		return conf.get(Schema.JDBC_INPUT_FILTER);
	}

	@Override
	public Configuration getConf() {
		return conf;
	}

	@Override
	public Map<String, String> getColumnNameMapping() {
		Map<String, String> columnNames = new HashMap<String, String>();
		columnNames.put("year", "data_year");
		columnNames.put("month", "data_month");
		columnNames.put("day", "data_day");
		return columnNames;
	}

	protected String buildCreateTableStatement(String table,
			String[] columnNames, String[] columnTypes) {

		StringBuilder createTableStatement = new StringBuilder();

		createTableStatement.append("CREATE TABLE ");
		createTableStatement.append(table);
		createTableStatement.append(" \n");
		createTableStatement.append("(");
		createTableStatement.append("\n");

		for (int i = 0; i < columnNames.length; i++) {
			createTableStatement.append(columnNames[i]);
			createTableStatement.append(" ");
			createTableStatement.append(columnTypes[i]);
			if (i != columnNames.length - 1) {
				createTableStatement.append(",");
			}
			createTableStatement.append("\n");
		}

		createTableStatement = createTableStatement.append(")");

		return createTableStatement.toString();
	}

	@Override
	public void setOutput(String connectionString, String username, String password, String outputTable,
			String inputFilter, int outputNumberOfPartitions, int outputCommitSize, String[] columnNames,
			String[] columnTypes) {

		conf.set(Schema.JDBC_CONNECTION_STRING, connectionString);
		if (username != null) {
			conf.set(Schema.JDBC_USERNAME, username);
		}

		if (password != null) {
			conf.set(Schema.JDBC_PASSWORD, password);
		}

		if (inputFilter != null && !inputFilter.isEmpty()) {
			conf.set(Schema.JDBC_INPUT_FILTER, inputFilter);
		}

		conf.set(Schema.JDBC_OUTPUT_TABLE, outputTable);
		conf.setStrings(Schema.JDBC_OUTPUT_COLUMN_NAMES, columnNames);
		conf.setStrings(Schema.JDBC_OUTPUT_COLUMN_TYPES, columnTypes);
		conf.setInt(Schema.JDBC_NUMBER_OF_PARTITIONS, outputNumberOfPartitions);
		conf.setInt(Schema.JDBC_COMMIT_SIZE, outputCommitSize);

		conf.set(Schema.JDBC_CREATE_TABLE_QUERY, buildCreateTableStatement(outputTable, columnNames, columnTypes));
	}

	@Override
	public Connection getConnection() throws ClassNotFoundException, SQLException {
		Class.forName(conf.get(Schema.JDBC_DRIVER_CLASS));

		return DriverManager.getConnection(
				conf.get(Schema.JDBC_CONNECTION_STRING),
				conf.get(Schema.JDBC_USERNAME),
				conf.get(Schema.JDBC_PASSWORD));
	}

}
