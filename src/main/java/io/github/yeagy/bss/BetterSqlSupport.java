package io.github.yeagy.bss;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * CRUD automagic
 */
public final class BetterSqlSupport {
    private final BetterOptions options;

    private BetterSqlSupport(BetterOptions options) {
        this.options = options;
    }

    public static BetterSqlSupport fromDefaults() {
        return from(BetterOptions.fromDefaults());
    }

    public static BetterSqlSupport from(BetterOptions options) {
        return new BetterSqlSupport(options);
    }

    /**
     * primarily for single SELECT. also useful for INSERT/UPDATE with RETURNING
     *
     * @param connection db connection. close it yourself
     * @param sql        sql template
     * @param binding    bind parameter values to the PreparedStatement (optional)
     * @param mapping    map ResultSet to return entity
     * @param <T>        entity type
     * @return entity or null
     */
    public <T> T query(Connection connection, String sql, StatementBinding binding, ResultMapping<T> mapping) {
        Objects.requireNonNull(connection);
        Objects.requireNonNull(sql);
        Objects.requireNonNull(mapping);
        T entity = null;
        try (final BetterPreparedStatement ps = BetterPreparedStatement.create(connection, sql, false, !options.arraySupport())) {
            if (binding != null) {
                binding.bind(ps);
            }
            try (final BetterResultSet rs = BetterResultSet.from(ps.executeQuery())) {
                if (rs.next()) {
                    entity = mapping.map(rs);
                }
            }
        } catch (Exception e) {
            throw new BetterSqlException(e);
        }
        return entity;
    }

    /**
     * primarily for bulk SELECT. also useful for INSERT/UPDATE with RETURNING
     *
     * @param connection db connection. close it yourself
     * @param sql        sql template
     * @param binding    bind parameter values to the PreparedStatement (optional)
     * @param mapping    map ResultSet to return entity
     * @param <T>        entity type
     * @return list of entity or empty list
     */
    public <T> List<T> queryList(Connection connection, String sql, StatementBinding binding, ResultMapping<T> mapping) {
        Objects.requireNonNull(connection);
        Objects.requireNonNull(sql);
        Objects.requireNonNull(mapping);
        final List<T> entities = new ArrayList<>();
        try (final BetterPreparedStatement ps = BetterPreparedStatement.create(connection, sql, false, !options.arraySupport())) {
            if (binding != null) {
                binding.bind(ps);
            }
            try (final BetterResultSet rs = BetterResultSet.from(ps.executeQuery())) {
                while (rs.next()) {
                    entities.add(mapping.map(rs));
                }
            }
        } catch (Exception e) {
            throw new BetterSqlException(e);
        }
        return Collections.unmodifiableList(entities);
    }

    /**
     * primarily for bulk SELECT. also useful for INSERT/UPDATE with RETURNING
     *
     * @param connection    db connection. close it yourself
     * @param sql           sql template
     * @param binding       bind parameter values to the PreparedStatement (optional)
     * @param resultMapping map ResultSet to return entity
     * @param keyMapping    map ResultSet to a key
     * @param <K>           key type
     * @param <T>           entity type
     * @return map of entities by key or empty map
     */
    public <K, T> Map<K, T> queryMap(Connection connection, String sql, StatementBinding binding, ResultMapping<T> resultMapping, ResultMapping<K> keyMapping) {
        Objects.requireNonNull(connection);
        Objects.requireNonNull(sql);
        Objects.requireNonNull(resultMapping);
        Objects.requireNonNull(keyMapping);
        final Map<K, T> map = new HashMap<>();
        try (final BetterPreparedStatement ps = BetterPreparedStatement.create(connection, sql, false, !options.arraySupport())) {
            if (binding != null) {
                binding.bind(ps);
            }
            try (final BetterResultSet rs = BetterResultSet.from(ps.executeQuery())) {
                while (rs.next()) {
                    map.put(keyMapping.map(rs), resultMapping.map(rs));
                }
            }
        } catch (Exception e) {
            throw new BetterSqlException(e);
        }
        return Collections.unmodifiableMap(map);
    }

    /**
     * primarily for bulk SELECT. also useful for INSERT/UPDATE with RETURNING
     *
     * @param connection    db connection. close it yourself
     * @param sql           sql template
     * @param binding       bind parameter values to the PreparedStatement (optional)
     * @param resultMapping map ResultSet to return entity
     * @param keyMapping    map ResultSet to a key
     * @param <K>           key type
     * @param <T>           entity type
     * @return multi-map of entities by key or empty map
     */
    public <K, T> Map<K, List<T>> queryMultiMap(Connection connection, String sql, StatementBinding binding, ResultMapping<T> resultMapping, ResultMapping<K> keyMapping){
        Objects.requireNonNull(connection);
        Objects.requireNonNull(sql);
        Objects.requireNonNull(resultMapping);
        Objects.requireNonNull(keyMapping);
        final Map<K, List<T>> mmap = new HashMap<>();
        try (final BetterPreparedStatement ps = BetterPreparedStatement.create(connection, sql, false, !options.arraySupport())) {
            if (binding != null) {
                binding.bind(ps);
            }
            try (final BetterResultSet rs = BetterResultSet.from(ps.executeQuery())) {
                while (rs.next()) {
                    mmap.computeIfAbsent(keyMapping.map(rs), k -> new ArrayList<>()).add(resultMapping.map(rs));
                }
            }
        } catch (Exception e) {
            throw new BetterSqlException(e);
        }
        return Collections.unmodifiableMap(mmap);

    }

    /**
     * primarily for INSERT/UPDATE/DELETE
     *
     * @param connection db connection. close it yourself
     * @param sql        sql template
     * @param binding    bind parameter values to the PreparedStatement (optional)
     * @return number of rows updated
     */
    public int update(Connection connection, String sql, StatementBinding binding) {
        Objects.requireNonNull(connection);
        Objects.requireNonNull(sql);
        try (final BetterPreparedStatement ps = BetterPreparedStatement.create(connection, sql, false, !options.arraySupport())) {
            if (binding != null) {
                binding.bind(ps);
            }
            return ps.executeUpdate();
        } catch (Exception e) {
            throw new BetterSqlException(e);
        }
    }

    /**
     * primarily for single INSERT. takes a ResultMapping to handle compound keys.
     * if you need a bulk insert (w or w/o returned keys), try using BetterPreparedStatement directly
     * <p>
     * generated keys will transparently have their column names extracted from metadata for convenience.
     *
     * @param connection          db connection. close it yourself
     * @param sql                 sql template
     * @param binding             bind parameter values to the PreparedStatement (optional)
     * @param generatedKeyMapping map ResultSet for generated key
     * @param <K>                 key type
     * @return key mapping result or null
     */
    public <K> K insert(Connection connection, String sql, StatementBinding binding, ResultMapping<K> generatedKeyMapping) {
        Objects.requireNonNull(connection);
        Objects.requireNonNull(sql);
        try (final BetterPreparedStatement ps = BetterPreparedStatement.create(connection, sql, true, !options.arraySupport())) {
            if (binding != null) {
                binding.bind(ps);
            }
            ps.executeUpdate();
            try (final BetterResultSet rs = MetadataTranslatingResultSet.fromGeneratedKeys(ps)) {
                if (rs.next()) {
                    return generatedKeyMapping.map(rs);
                }
            }
        } catch (Exception e) {
            throw new BetterSqlException(e);
        }
        return null;
    }

    /**
     * primarily for single INSERT. this returns a simple (single field) generated key without supplying a result mapping.
     * if you need a bulk insert (w or w/o returned keys), try using BetterPreparedStatement directly
     *
     * @param connection db connection. close it yourself
     * @param sql        sql template
     * @param binding    bind parameter values to the PreparedStatement (optional)
     * @param <K>        key type
     * @return key or null
     */
    public <K> K insert(Connection connection, String sql, StatementBinding binding) {
        Objects.requireNonNull(connection);
        Objects.requireNonNull(sql);
        K key = null;
        try (final BetterPreparedStatement ps = BetterPreparedStatement.create(connection, sql, true, !options.arraySupport())) {
            if (binding != null) {
                binding.bind(ps);
            }
            ps.executeUpdate();
            try (final BetterResultSet rs = BetterResultSet.from(ps.getGeneratedKeys())) {
                if (rs.next()) {
                    //noinspection unchecked
                    key = (K) rs.getObject(1);
                }
            }
        } catch (Exception e) {
            throw new BetterSqlException(e);
        }
        return key;
    }

    //all cascading builders below

    public Builder builder(String sql) {
        return new Builder(sql);
    }

    public final class Builder {
        private final String sql;

        private Builder(String sql) {
            this.sql = sql;
        }

        public int update(Connection connection) {
            return BetterSqlSupport.this.update(connection, sql, null);
        }

        public <K> K insert(Connection connection) {
            return BetterSqlSupport.this.insert(connection, sql, null);
        }

        public BoundBuilder bind(StatementBinding statementBinding) {
            return new BoundBuilder(sql, statementBinding);
        }

        public <T> ResultBuilder<T> mapResult(ResultMapping<T> resultMapping) {
            return new ResultBuilder<>(sql, resultMapping);
        }

        public <K> KeyedBuilder<K> mapKey(ResultMapping<K> keyMapping) {
            return new KeyedBuilder<>(sql, keyMapping);
        }
    }

    public final class BoundBuilder {
        private final String sql;
        private final StatementBinding statementBinding;

        private BoundBuilder(String sql, StatementBinding statementBinding) {
            this.sql = sql;
            this.statementBinding = statementBinding;
        }

        public int update(Connection connection) {
            return BetterSqlSupport.this.update(connection, sql, statementBinding);
        }

        public <K> K insert(Connection connection) {
            return BetterSqlSupport.this.insert(connection, sql, statementBinding);
        }

        public <T> BoundResultBuilder<T> mapResult(ResultMapping<T> resultMapping) {
            return new BoundResultBuilder<>(sql, statementBinding, resultMapping);
        }

        public <K> BoundKeyedBuilder<K> mapKey(ResultMapping<K> keyMapping) {
            return new BoundKeyedBuilder<>(sql, statementBinding, keyMapping);
        }
    }

    public final class KeyedBuilder<K> {
        private final String sql;
        private final ResultMapping<K> keyMapping;

        public KeyedBuilder(String sql, ResultMapping<K> keyMapping) {
            this.sql = sql;
            this.keyMapping = keyMapping;
        }

        public K insert(Connection connection) {
            return BetterSqlSupport.this.insert(connection, sql, null, keyMapping);
        }

        public BoundKeyedBuilder<K> bind(StatementBinding statementBinding) {
            return new BoundKeyedBuilder<>(sql, statementBinding, keyMapping);
        }

        public <T> KeyedResultBuilder<K, T> mapResult(ResultMapping<T> resultMapping) {
            return new KeyedResultBuilder<>(sql, resultMapping, keyMapping);
        }
    }

    public final class BoundKeyedBuilder<K> {
        private final String sql;
        private final StatementBinding statementBinding;
        private final ResultMapping<K> keyMapping;

        public BoundKeyedBuilder(String sql, StatementBinding statementBinding, ResultMapping<K> keyMapping) {
            this.sql = sql;
            this.statementBinding = statementBinding;
            this.keyMapping = keyMapping;
        }

        public K insert(Connection connection) {
            return BetterSqlSupport.this.insert(connection, sql, statementBinding, keyMapping);
        }

        public <T> BoundKeyedResultBuilder<K, T> mapResult(ResultMapping<T> resultMapping) {
            return new BoundKeyedResultBuilder<>(sql, statementBinding, resultMapping, keyMapping);
        }
    }

    public final class ResultBuilder<T> {
        private final String sql;
        private final ResultMapping<T> resultMapping;

        private ResultBuilder(String sql, ResultMapping<T> resultMapping) {
            this.sql = sql;
            this.resultMapping = resultMapping;
        }

        public T query(Connection connection) {
            return BetterSqlSupport.this.query(connection, sql, null, resultMapping);
        }

        public List<T> queryList(Connection connection) {
            return BetterSqlSupport.this.queryList(connection, sql, null, resultMapping);
        }

        public BoundResultBuilder<T> bind(StatementBinding statementBinding) {
            return new BoundResultBuilder<>(sql, statementBinding, resultMapping);
        }

        public <K> KeyedResultBuilder<K, T> mapKey(ResultMapping<K> keyMapping) {
            return new KeyedResultBuilder<>(sql, resultMapping, keyMapping);
        }
    }

    public final class BoundResultBuilder<T> {
        private final String sql;
        private final StatementBinding statementBinding;
        private final ResultMapping<T> resultMapping;

        private BoundResultBuilder(String sql, StatementBinding statementBinding, ResultMapping<T> resultMapping) {
            this.sql = sql;
            this.statementBinding = statementBinding;
            this.resultMapping = resultMapping;
        }

        public T query(Connection connection) {
            return BetterSqlSupport.this.query(connection, sql, statementBinding, resultMapping);
        }

        public List<T> queryList(Connection connection) {
            return BetterSqlSupport.this.queryList(connection, sql, statementBinding, resultMapping);
        }

        public <K> BoundKeyedResultBuilder<K, T> mapKey(ResultMapping<K> keyMapping) {
            return new BoundKeyedResultBuilder<>(sql, statementBinding, resultMapping, keyMapping);
        }
    }

    public final class KeyedResultBuilder<K, T> {
        private final String sql;
        private final ResultMapping<T> resultMapping;
        private final ResultMapping<K> keyMapping;

        private KeyedResultBuilder(String sql, ResultMapping<T> resultMapping, ResultMapping<K> keyMapping) {
            this.sql = sql;
            this.resultMapping = resultMapping;
            this.keyMapping = keyMapping;
        }

        public Map<K, T> queryMap(Connection connection) {
            return BetterSqlSupport.this.queryMap(connection, sql, null, resultMapping, keyMapping);
        }

        public Map<K, List<T>> queryMultiMap(Connection connection) {
            return BetterSqlSupport.this.queryMultiMap(connection, sql, null, resultMapping, keyMapping);
        }

        public BoundKeyedResultBuilder<K, T> bind(StatementBinding statementBinding) {
            return new BoundKeyedResultBuilder<>(sql, statementBinding, resultMapping, keyMapping);
        }
    }

    public final class BoundKeyedResultBuilder<K, T> {
        private final String sql;
        private final StatementBinding statementBinding;
        private final ResultMapping<T> resultMapping;
        private final ResultMapping<K> keyMapping;

        private BoundKeyedResultBuilder(String sql, StatementBinding statementBinding, ResultMapping<T> resultMapping, ResultMapping<K> keyMapping) {
            this.sql = sql;
            this.statementBinding = statementBinding;
            this.resultMapping = resultMapping;
            this.keyMapping = keyMapping;
        }

        public Map<K, T> queryMap(Connection connection) {
            return BetterSqlSupport.this.queryMap(connection, sql, statementBinding, resultMapping, keyMapping);
        }

        public Map<K, List<T>> queryMultiMap(Connection connection) {
            return BetterSqlSupport.this.queryMultiMap(connection, sql, statementBinding, resultMapping, keyMapping);
        }
    }
}
