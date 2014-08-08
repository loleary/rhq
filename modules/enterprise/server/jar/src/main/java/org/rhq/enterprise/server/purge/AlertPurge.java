/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.enterprise.server.purge;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;
import javax.transaction.UserTransaction;

/**
 * @author Thomas Segismont
 */
class AlertPurge extends PurgeTemplate<Integer> {
    private static final String ENTITY_NAME = "Alert";

    private static final String QUERY_SELECT_KEYS_FOR_PURGE_POSTGRES = "" //
        + "SELECT ID FROM RHQ_ALERT WHERE CTIME BETWEEN ? AND ? LIMIT ?";

    private static final String QUERY_SELECT_KEYS_FOR_PURGE_ORACLE = "" //
        + "SELECT ID FROM RHQ_ALERT WHERE CTIME BETWEEN ? AND ? AND ROWNUM <= ?";

    private static final String QUERY_PURGE_BY_KEY = "DELETE FROM RHQ_ALERT WHERE ID = ?";

    private final long beginTime;
    private final long endTime;

    AlertPurge(DataSource dataSource, UserTransaction userTransaction, long beginTime, long endTime) {
        super(dataSource, userTransaction);
        this.beginTime = beginTime;
        this.endTime = endTime;
    }

    @Override
    protected String getEntityName() {
        return ENTITY_NAME;
    }

    @Override
    protected String getFindRowKeysQueryPostgres() {
        return QUERY_SELECT_KEYS_FOR_PURGE_POSTGRES;
    }

    @Override
    protected String getFindRowKeysQueryOracle() {
        return QUERY_SELECT_KEYS_FOR_PURGE_ORACLE;
    }

    @Override
    protected void setFindRowKeysQueryParams(PreparedStatement preparedStatement) throws SQLException {
        preparedStatement.setLong(1, beginTime);
        preparedStatement.setLong(2, endTime);
        preparedStatement.setInt(3, getBatchSize());
    }

    @Override
    protected Integer getKeyFromResultSet(ResultSet resultSet) throws SQLException {
        return resultSet.getInt(1);
    }

    @Override
    protected String getDeleteRowByKeyQueryPostgres() {
        return QUERY_PURGE_BY_KEY;
    }

    @Override
    protected String getDeleteRowByKeyQueryOracle() {
        return QUERY_PURGE_BY_KEY;
    }

    @Override
    protected void setDeleteRowByKeyQueryParams(PreparedStatement preparedStatement, Integer key) throws SQLException {
        preparedStatement.setInt(1, key);
    }
}