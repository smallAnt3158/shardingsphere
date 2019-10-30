/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.core.rewrite.feature.encrypt.token.generator.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import lombok.Setter;
import org.apache.shardingsphere.core.parse.sql.segment.dml.column.ColumnSegment;
import org.apache.shardingsphere.core.parse.sql.segment.dml.column.InsertColumnsSegment;
import org.apache.shardingsphere.core.parse.sql.statement.dml.InsertStatement;
import org.apache.shardingsphere.core.preprocessor.statement.SQLStatementContext;
import org.apache.shardingsphere.core.preprocessor.statement.impl.InsertSQLStatementContext;
import org.apache.shardingsphere.core.rewrite.feature.encrypt.token.generator.EncryptRuleAware;
import org.apache.shardingsphere.core.rewrite.sql.token.pojo.generic.InsertColumnsToken;
import org.apache.shardingsphere.core.rewrite.sql.token.generator.CollectionSQLTokenGenerator;
import org.apache.shardingsphere.core.rule.EncryptRule;
import org.apache.shardingsphere.core.strategy.encrypt.EncryptTable;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Assist query and plain insert columns token generator.
 *
 * @author panjuan
 * @author zhangliang
 */
@Setter
public final class AssistQueryAndPlainInsertColumnsTokenGenerator implements CollectionSQLTokenGenerator, EncryptRuleAware {
    
    private EncryptRule encryptRule;
    
    @Override
    public boolean isGenerateSQLToken(final SQLStatementContext sqlStatementContext) {
        return sqlStatementContext instanceof InsertSQLStatementContext && sqlStatementContext.getSqlStatement().findSQLSegment(InsertColumnsSegment.class).isPresent()
                && !((InsertStatement) sqlStatementContext.getSqlStatement()).useDefaultColumns()
                && encryptRule.findEncryptTable(sqlStatementContext.getTablesContext().getSingleTableName()).isPresent();
    }
    
    @Override
    public Collection<InsertColumnsToken> generateSQLTokens(final SQLStatementContext sqlStatementContext) {
        Collection<InsertColumnsToken> result = new LinkedList<>();
        Optional<EncryptTable> encryptTable = encryptRule.findEncryptTable(sqlStatementContext.getTablesContext().getSingleTableName());
        Preconditions.checkState(encryptTable.isPresent());
        for (ColumnSegment each : ((InsertStatement) sqlStatementContext.getSqlStatement()).getColumns()) {
            List<String> columns = getColumns(encryptTable.get(), each);
            if (!columns.isEmpty()) {
                result.add(new InsertColumnsToken(each.getStopIndex() + 1, columns));
            }
        }
        return result;
    }
    
    private List<String> getColumns(final EncryptTable encryptTable, final ColumnSegment columnSegment) {
        List<String> result = new LinkedList<>();
        Optional<String> assistedQueryColumn = encryptTable.findAssistedQueryColumn(columnSegment.getName());
        if (assistedQueryColumn.isPresent()) {
            result.add(assistedQueryColumn.get());
        }
        Optional<String> plainColumn = encryptTable.findPlainColumn(columnSegment.getName());
        if (plainColumn.isPresent()) {
            result.add(plainColumn.get());
        }
        return result;
    }
}
