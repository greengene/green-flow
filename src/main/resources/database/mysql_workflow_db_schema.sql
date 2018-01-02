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

USE my_schema;

DROP TABLE IF EXISTS `execution_assigned_variable`;

DROP TABLE IF EXISTS `execution_transaction`;
DROP TABLE IF EXISTS `execution_conversation`;

DROP TABLE IF EXISTS `configuration_returned_variable`;
DROP TABLE IF EXISTS `configuration_parameter`;
DROP TABLE IF EXISTS `configuration_declared_variable`;
DROP TABLE IF EXISTS `configuration_predicate`;
DROP TABLE IF EXISTS `configuration_treepath`;
DROP TABLE IF EXISTS `configuration_workunit`;
DROP TABLE IF EXISTS `configuration_workflow`;

DROP TABLE IF EXISTS `sequence`;

CREATE TABLE IF NOT EXISTS Configuration_Workflow(
id SERIAL PRIMARY KEY,
name VARCHAR(64) NOT NULL,
description VARCHAR(64),
creation_date DATETIME, 
creator VARCHAR(64),
UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS Configuration_WorkUnit(
id SERIAL PRIMARY KEY,
workflow_id BIGINT UNSIGNED NOT NULL,
discriminator TINYINT UNSIGNED NOT NULL, 
type SMALLINT NOT NULL, 
`return` boolean, 
persist_after_return boolean, 
assign_to VARCHAR(64), 
FOREIGN KEY (workflow_id) REFERENCES Configuration_Workflow(id)
);

CREATE TABLE IF NOT EXISTS Configuration_TreePath (
ancestor BIGINT UNSIGNED NOT NULL,
descendant BIGINT UNSIGNED NOT NULL,
length BIGINT UNSIGNED NOT NULL,
PRIMARY KEY(ancestor, descendant),
FOREIGN KEY (ancestor) REFERENCES Configuration_WorkUnit(id),
FOREIGN KEY (descendant) REFERENCES Configuration_WorkUnit(id)
);

CREATE TABLE IF NOT EXISTS Configuration_Predicate(
id 

	DECLARE cur_val BIGINT;

	IF val_num = null THEN SET val_num = 1; END IF;

	SELECT
		cur_value INTO cur_val
	FROM
		SEQUENCE
	WHERE
		name = seq_name;

	IF cur_val IS NOT NULL THEN
		UPDATE
			SEQUENCE
		SET
			cur_value = IF (
				(cur_value + INCREMENT * val_num) > max_value OR (cur_value + INCREMENT * val_num) < min_value,
				IF (
					cycle = TRUE,
					IF (
						(cur_value + INCREMENT * val_num) > max_value,
						min_value,
						max_value
					),
					NULL
				),
				cur_value + INCREMENT * val_num
			)
		WHERE
			name = seq_name;
	END IF;
	RETURN cur_val;
END;
$$

DELIMITER $$
DROP FUNCTION IF EXISTS `nextval` $$
CREATE FUNCTION `nextval` (`seq_name` VARCHAR(100))
RETURNS BIGINT NOT DETERMINISTIC
BEGIN
	RETURN nextvals(seq_name, 1);
END;
$$