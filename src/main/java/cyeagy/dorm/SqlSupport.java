package cyeagy.dorm;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SqlSupport {

    public static SqlSupport fromDefaults() {
        return new SqlSupport();
    }

    private SqlSupport() { }

    /**
     * primarily for single SELECT. also useful for INSERT/UPDATE with RETURNING
     *
     * @param connection db connection. close it yourself
     * @param sql        sql template
     * @param binding    bind values to the prepared statement (optional)
     * @param mapping    map values from the result set
     * @param <T>        entity type
     * @return entity or null
     * @throws SQLException
     */
    public <T> T query(Connection connection, String sql, QueryBinding binding, ResultMapping<T> mapping) throws SQLException {
        Objects.requireNonNull(connection, "connection is null");
        Objects.requireNonNull(sql, "sql is null");
        Objects.requireNonNull(mapping, "result mapping is null");
        T entity = null;
        try (final BetterPreparedStatement ps = BetterPreparedStatement.from(connection, sql)) {
            if (binding != null) {
                binding.bind(ps);
            }
            try (final BetterResultSet rs = BetterResultSet.from(ps.executeQuery())) {
                if (rs.next()) {
                    entity = mapping.map(rs, 0);
                }
            }
        }
        return entity;
    }

    /**
     * primarily for bulk SELECT. also useful for INSERT/UPDATE with RETURNING
     *
     * @param connection db connection. close it yourself
     * @param sql        sql template
     * @param binding    bind values to the prepared statement (optional)
     * @param mapping    map values from the result set
     * @param <T>        entity type
     * @return list of entity or empty list
     * @throws SQLException
     */
    public <T> List<T> queryList(Connection connection, String sql, QueryBinding binding, ResultMapping<T> mapping) throws SQLException {
        Objects.requireNonNull(connection, "connection is null");
        Objects.requireNonNull(sql, "sql is null");
        Objects.requireNonNull(mapping, "result mapping is null");
        final List<T> entities = new ArrayList<>();
        try (final BetterPreparedStatement ps = BetterPreparedStatement.from(connection, sql)) {
            if (binding != null) {
                binding.bind(ps);
            }
            try (final BetterResultSet rs = BetterResultSet.from(ps.executeQuery())) {
                int i = 0;
                while (rs.next()) {
                    entities.add(mapping.map(rs, i++));
                }
            }
        }
        return entities;
    }

    /**
     * primarily for bulk SELECT. also useful for INSERT/UPDATE with RETURNING
     *
     * @param connection    db connection. close it yourself
     * @param sql           sql template
     * @param binding       bind values to the prepared statement (optional)
     * @param resultMapping map values from the result set
     * @param keyMapping    map key from the result set
     * @param <K>           key type
     * @param <T>           entity type
     * @return map of entities by key or empty map
     * @throws SQLException
     */
    public <K, T> Map<K, T> queryMapped(Connection connection, String sql, QueryBinding binding, ResultMapping<T> resultMapping, ResultMapping<K> keyMapping) throws SQLException {
        Objects.requireNonNull(connection, "connection is null");
        Objects.requireNonNull(sql, "sql is null");
        Objects.requireNonNull(resultMapping, "result mapping is null");
        Objects.requireNonNull(keyMapping, "key mapping is null");
        final Map<K, T> map = new HashMap<>();
        try (final BetterPreparedStatement ps = BetterPreparedStatement.from(connection, sql)) {
            if (binding != null) {
                binding.bind(ps);
            }
            try (final BetterResultSet rs = BetterResultSet.from(ps.executeQuery())) {
                int i = 0;
                while (rs.next()) {
                    map.put(keyMapping.map(rs, i), resultMapping.map(rs, i++));
                }
            }
        }
        return map;
    }

    /**
     * primarily for INSERT/UPDATE/DELETE
     *
     * @param connection db connection. close it yourself
     * @param sql        sql template
     * @param binding    bind values to the prepared statement (optional)
     * @return number of rows updated
     * @throws SQLException
     */
    public int update(Connection connection, String sql, QueryBinding binding) throws SQLException {
        Objects.requireNonNull(connection, "connection is null");
        Objects.requireNonNull(sql, "sql is null");
        try (final BetterPreparedStatement ps = BetterPreparedStatement.from(connection, sql)) {
            if (binding != null) {
                binding.bind(ps);
            }
            return ps.executeUpdate();
        }
    }

    /**
     * primarily for single INSERT returning the auto-generated key
     *
     * @param connection db connection. close it yourself
     * @param sql        sql template
     * @param binding    bind values to the prepared statement
     * @param <K>        key type
     * @return key or null
     * @throws SQLException
     */
    public <K> K insert(Connection connection, String sql, QueryBinding binding) throws SQLException {
        Objects.requireNonNull(connection, "connection is null");
        Objects.requireNonNull(sql, "sql is null");
        Objects.requireNonNull(binding, "query binding is null");
        K key = null;
        try (final BetterPreparedStatement ps = BetterPreparedStatement.from(connection, sql, true)) {
            binding.bind(ps);
            ps.executeUpdate();
            try (final BetterResultSet rs = BetterResultSet.from(ps.getGeneratedKeys())) {
                if (rs.next()) {
                    //noinspection unchecked
                    key = (K) rs.getObject(1);
                }
            }
        }
        return key;
    }


    @FunctionalInterface
    public interface QueryBinding {
        void bind(BetterPreparedStatement ps) throws SQLException;
    }

    @FunctionalInterface
    public interface ResultMapping<T> {
        T map(BetterResultSet rs, int idx) throws SQLException;
    }

    //can be used inline on the builders, makes for simpler lambdas as people rarely need the result index
    @FunctionalInterface
    public interface SimpleResultMapping<T> extends ResultMapping<T> {
        T map(BetterResultSet rs) throws SQLException;

        default T map(BetterResultSet rs, int idx) throws SQLException {
            return map(rs);
        }
    }

    //all cascading builders below

    public Builder builder(String sql) {
        return new BuilderImpl(sql);
    }

    public interface Builder {
        int executeUpdate(Connection connection) throws SQLException;
        BoundBuilder queryBinding(QueryBinding queryBinding);
        <T> ResultBuilder<T> resultMapping(ResultMapping<T> resultMapping);
        <T> ResultBuilder<T> resultMapping(SimpleResultMapping<T> resultMapping);
    }

    public interface BoundBuilder {
        int executeUpdate(Connection connection) throws SQLException;
        <K> K executeInsert(Connection connection) throws SQLException;
        <T> BoundResultBuilder<T> resultMapping(ResultMapping<T> resultMapping);
        <T> BoundResultBuilder<T> resultMapping(SimpleResultMapping<T> resultMapping);
    }

    public interface ResultBuilder<T> {
        T executeQuery(Connection connection) throws SQLException;
        List<T> executeQueryList(Connection connection) throws SQLException;
        BoundResultBuilder<T> queryBinding(QueryBinding queryBinding);
        <K> KeyedResultBuilder<K, T> keyMapping(ResultMapping<K> keyMapping);
    }

    public interface BoundResultBuilder<T> {
        T executeQuery(Connection connection) throws SQLException;
        List<T> executeQueryList(Connection connection) throws SQLException;
        <K> BoundKeyedResultBuilder<K, T> keyMapping(ResultMapping<K> keyMapping);
    }

    public interface KeyedResultBuilder<K, T> {
        Map<K, T> executeQueryMapped(Connection connection, ResultMapping<K> keyMapping) throws SQLException;
        BoundKeyedResultBuilder<K, T> queryBinding(QueryBinding queryBinding);
    }

    public interface BoundKeyedResultBuilder<K, T> {
        Map<K, T> executeQueryMapped(Connection connection, ResultMapping<K> keyMapping) throws SQLException;
    }

    private static class BuilderImpl implements Builder {
        private static final SqlSupport SQL_SUPPORT = new SqlSupport();
        private final String sql;

        public BuilderImpl(String sql) {
            this.sql = sql;
        }

        @Override
        public int executeUpdate(Connection connection) throws SQLException {
            return SQL_SUPPORT.update(connection, sql, null);
        }

        @Override
        public BoundBuilder queryBinding(QueryBinding queryBinding) {
            return new BoundBuilderImpl(sql, queryBinding);
        }

        @Override
        public <T> ResultBuilder<T> resultMapping(ResultMapping<T> resultMapping) {
            return new ResultBuilderImpl<>(sql, resultMapping);
        }

        @Override
        public <T> ResultBuilder<T> resultMapping(SimpleResultMapping<T> resultMapping) {
            return new ResultBuilderImpl<>(sql, resultMapping);
        }
    }

    private static class BoundBuilderImpl implements BoundBuilder {
        private final String sql;
        private final QueryBinding queryBinding;

        public BoundBuilderImpl(String sql, QueryBinding queryBinding) {
            this.sql = sql;
            this.queryBinding = queryBinding;
        }

        @Override
        public int executeUpdate(Connection connection) throws SQLException {
            return BuilderImpl.SQL_SUPPORT.update(connection, sql, queryBinding);
        }

        @Override
        public <K> K executeInsert(Connection connection) throws SQLException {
            return BuilderImpl.SQL_SUPPORT.insert(connection, sql, queryBinding);
        }

        @Override
        public <T> BoundResultBuilder<T> resultMapping(ResultMapping<T> resultMapping) {
            return new BoundResultBuilderImpl<>(sql, queryBinding, resultMapping);
        }

        @Override
        public <T> BoundResultBuilder<T> resultMapping(SimpleResultMapping<T> resultMapping) {
            return new BoundResultBuilderImpl<>(sql, queryBinding, resultMapping);
        }
    }

    private static class ResultBuilderImpl<T> implements ResultBuilder<T> {
        private final String sql;
        private final ResultMapping<T> resultMapping;

        public ResultBuilderImpl(String sql, ResultMapping<T> resultMapping) {
            this.sql = sql;
            this.resultMapping = resultMapping;
        }

        @Override
        public T executeQuery(Connection connection) throws SQLException {
            return BuilderImpl.SQL_SUPPORT.query(connection, sql, null, resultMapping);
        }

        @Override
        public List<T> executeQueryList(Connection connection) throws SQLException {
            return BuilderImpl.SQL_SUPPORT.queryList(connection, sql, null, resultMapping);
        }

        @Override
        public BoundResultBuilder<T> queryBinding(QueryBinding queryBinding) {
            return new BoundResultBuilderImpl<>(sql, queryBinding, resultMapping);
        }

        @Override
        public <K> KeyedResultBuilder<K, T> keyMapping(ResultMapping<K> keyMapping) {
            return new KeyedResultBuilderImpl<>(sql, resultMapping, keyMapping);
        }
    }

    private static class BoundResultBuilderImpl<T> implements BoundResultBuilder<T> {
        private final String sql;
        private final QueryBinding queryBinding;
        private final ResultMapping<T> resultMapping;

        public BoundResultBuilderImpl(String sql, QueryBinding queryBinding, ResultMapping<T> resultMapping) {
            this.sql = sql;
            this.queryBinding = queryBinding;
            this.resultMapping = resultMapping;
        }

        @Override
        public T executeQuery(Connection connection) throws SQLException {
            return BuilderImpl.SQL_SUPPORT.query(connection, sql, queryBinding, resultMapping);
        }

        @Override
        public List<T> executeQueryList(Connection connection) throws SQLException {
            return BuilderImpl.SQL_SUPPORT.queryList(connection, sql, queryBinding, resultMapping);
        }

        @Override
        public <K> BoundKeyedResultBuilder<K, T> keyMapping(ResultMapping<K> keyMapping) {
            return new BoundKeyedResultBuilderImpl<>(sql, queryBinding, resultMapping, keyMapping);
        }
    }

    private static class KeyedResultBuilderImpl<K, T> implements KeyedResultBuilder<K, T> {
        private final String sql;
        private final ResultMapping<T> resultMapping;
        private final ResultMapping<K> keyMapping;

        public KeyedResultBuilderImpl(String sql, ResultMapping<T> resultMapping, ResultMapping<K> keyMapping) {
            this.sql = sql;
            this.resultMapping = resultMapping;
            this.keyMapping = keyMapping;
        }

        @Override
        public Map<K, T> executeQueryMapped(Connection connection, ResultMapping<K> keyMapping) throws SQLException {
            return BuilderImpl.SQL_SUPPORT.queryMapped(connection, sql, null, resultMapping, keyMapping);
        }

        @Override
        public BoundKeyedResultBuilder<K, T> queryBinding(QueryBinding queryBinding) {
            return new BoundKeyedResultBuilderImpl<>(sql, queryBinding, resultMapping, keyMapping);
        }
    }

    private static class BoundKeyedResultBuilderImpl<K, T> implements BoundKeyedResultBuilder<K, T> {
        private final String sql;
        private final QueryBinding queryBinding;
        private final ResultMapping<T> resultMapping;
        private final ResultMapping<K> keyMapping;

        public BoundKeyedResultBuilderImpl(String sql, QueryBinding queryBinding, ResultMapping<T> resultMapping, ResultMapping<K> keyMapping) {
            this.sql = sql;
            this.queryBinding = queryBinding;
            this.resultMapping = resultMapping;
            this.keyMapping = keyMapping;
        }

        @Override
        public Map<K, T> executeQueryMapped(Connection connection, ResultMapping<K> keyMapping) throws SQLException {
            return BuilderImpl.SQL_SUPPORT.queryMapped(connection, sql, queryBinding, resultMapping, keyMapping);
        }
    }
}
