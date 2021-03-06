package io.github.yeagy.bss;

import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.Server;
import org.hamcrest.Matchers;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class BetterSqlMapperTest {
    private static BetterSqlMapper BSM = BetterSqlMapper.fromDefaults();
    private static Server server;
    private static Connection connection;

    @BeforeClass
    public static void setUpClass() throws Exception {
        server = Server.createTcpServer().start();
        Class.forName("org.h2.Driver");
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setUrl("jdbc:h2:mem:");
        connection = dataSource.getConnection();
        String create = new Scanner(BetterSqlMapperTest.class.getResourceAsStream("/sql/test_create.sql"), "UTF-8").useDelimiter("\\A").next();
        Statement statement = connection.createStatement();
        statement.execute(create);
        statement.close();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        connection.close();
        server.shutdown();
    }

    @Before
    public void setUp() throws Exception {
        truncateAndInsert();
    }

    private void truncateAndInsert() throws SQLException {
        Statement statement = connection.createStatement();
        String insert = new Scanner(BetterSqlMapperTest.class.getResourceAsStream("/sql/test_insert.sql"), "UTF-8").useDelimiter("\\A").next();
        statement.execute(insert);
        statement.close();
    }

    @Test
    public void testFull() {
        TestBean bean = new TestBean(null, Long.MAX_VALUE, Integer.MAX_VALUE, "test string", Timestamp.from(Instant.now()), 0.0, TestBean.Status.OFF);
        TestBean result = BSM.insert(connection, bean);
        assertNotNull(result.getTestKey());

        result = BSM.find(connection, result.getTestKey(), TestBean.class);
        assertThat(result.getSomeString(), equalTo(bean.getSomeString()));

        BSM.update(connection, new TestBean(result.getTestKey(), bean.getSomeLong(), bean.getSomeInt(), "changed string", bean.getSomeDtm(), 0.0, TestBean.Status.ON));

        result = BSM.find(connection, result.getTestKey(), TestBean.class);
        assertThat(result.getSomeString(), not(equalTo(bean.getSomeString())));

        BSM.delete(connection, result);

        result = BSM.find(connection, result.getTestKey(), TestBean.class);
        assertNull(result);
    }

    @Test
    public void testInsert() {
        Timestamp now = Timestamp.from(Instant.now());
        {
            TestBean testBean = new TestBean(null, Long.MAX_VALUE, Integer.MAX_VALUE, "test string", now, 0.0, TestBean.Status.OFF);
            TestBean result = BSM.insert(connection, testBean);
            assertNotNull(result);
            assertThat(result, is(not(testBean)));
            assertNotNull(result.getTestKey());
            assertThat(result.getTestKey(), is(not(0l)));
            assertThat(result.getSomeLong(), equalTo(testBean.getSomeLong()));
            assertThat(result.getSomeInt(), equalTo(testBean.getSomeInt()));
            assertThat(result.getSomeString(), equalTo(testBean.getSomeString()));
            assertThat(result.getSomeDtm(), equalTo(testBean.getSomeDtm()));
            assertThat(result.getSomeEnum(), equalTo(testBean.getSomeEnum()));
        }
        {
            final AnnotatedTestBean testbean = new AnnotatedTestBean(null, Long.MAX_VALUE, Integer.MAX_VALUE, "test string", now);
            final AnnotatedTestBean result = BSM.insert(connection, testbean);
            assertNotNull(result);
            assertThat(result, is(not(testbean)));
            assertNotNull(result.getLegacyKey());
            assertThat(result.getLegacyKey(), is(not(0l)));
            assertThat(result.getLegacyLong(), equalTo(testbean.getLegacyLong()));
            assertThat(result.getLegacyInt(), equalTo(testbean.getLegacyInt()));
            assertThat(result.getLegacyString(), equalTo(testbean.getLegacyString()));
            assertThat(result.getLegacyTimestamp(), equalTo(testbean.getLegacyTimestamp()));
        }
    }

    @Test
    public void testUpdate() {
        Timestamp now = Timestamp.from(Instant.now().plus(1, ChronoUnit.DAYS));
        {
            final long key = 4;
            TestBean testBean = new TestBean(4l, Long.MAX_VALUE, Integer.MAX_VALUE, "FOURTH", now, 0.0, TestBean.Status.ON);

            final TestBean preSelect = BSM.find(connection, key, TestBean.class);
            assertNotNull(preSelect);
            assertThat(preSelect.getTestKey(), equalTo(testBean.getTestKey()));
            assertThat(preSelect.getSomeLong(), not(equalTo(testBean.getSomeLong())));
            assertThat(preSelect.getSomeInt(), not(equalTo(testBean.getSomeInt())));
            assertThat(preSelect.getSomeString(), not(equalTo(testBean.getSomeString())));
            assertThat(preSelect.getSomeDtm(), not(equalTo(testBean.getSomeDtm())));
            assertThat(preSelect.getSomeEnum(), not(equalTo(testBean.getSomeEnum())));

            BSM.update(connection, testBean);

            final TestBean select = BSM.find(connection, key, TestBean.class);
            assertNotNull(select);
            assertThat(select.getTestKey(), equalTo(testBean.getTestKey()));
            assertThat(select.getSomeLong(), equalTo(testBean.getSomeLong()));
            assertThat(select.getSomeInt(), equalTo(testBean.getSomeInt()));
            assertThat(select.getSomeString(), equalTo(testBean.getSomeString()));
            assertThat(select.getSomeDtm(), equalTo(testBean.getSomeDtm()));
            assertThat(select.getSomeEnum(), equalTo(testBean.getSomeEnum()));
        }
        {
            final long key = 5;
            AnnotatedTestBean testBean = new AnnotatedTestBean(key, Long.MAX_VALUE, Integer.MAX_VALUE, "FOURTH", now);

            final AnnotatedTestBean preSelect = BSM.find(connection, key, AnnotatedTestBean.class);
            assertNotNull(preSelect);
            assertThat(preSelect.getLegacyKey(), equalTo(testBean.getLegacyKey()));
            assertThat(preSelect.getLegacyLong(), not(equalTo(testBean.getLegacyLong())));
            assertThat(preSelect.getLegacyInt(), not(equalTo(testBean.getLegacyInt())));
            assertThat(preSelect.getLegacyString(), not(equalTo(testBean.getLegacyString())));
            assertThat(preSelect.getLegacyTimestamp(), not(equalTo(testBean.getLegacyTimestamp())));

            BSM.update(connection, testBean);

            final AnnotatedTestBean select = BSM.find(connection, key, AnnotatedTestBean.class);
            assertNotNull(select);
            assertThat(select.getLegacyKey(), equalTo(testBean.getLegacyKey()));
            assertThat(select.getLegacyLong(), equalTo(testBean.getLegacyLong()));
            assertThat(select.getLegacyInt(), equalTo(testBean.getLegacyInt()));
            assertThat(select.getLegacyString(), equalTo(testBean.getLegacyString()));
            assertThat(select.getLegacyTimestamp(), equalTo(testBean.getLegacyTimestamp()));
        }
    }

    @Test
    public void testSelect() {
        final long key = 3;
        {
            final TestBean result = BSM.find(connection, key, TestBean.class);
            assertNotNull(result);
            assertThat(result.getTestKey(), equalTo(key));
        }
        {
            final AnnotatedTestBean result = BSM.find(connection, key, AnnotatedTestBean.class);
            assertNotNull(result);
            assertThat(result.getLegacyKey(), equalTo(key));
        }
    }

    @Test
    public void testDelete() {
        {
            final long key = 1;
            final TestBean preSelect = BSM.find(connection, key, TestBean.class);
            assertNotNull(preSelect);
            assertThat(preSelect.getTestKey(), equalTo(key));

            BSM.delete(connection, key, TestBean.class);

            final TestBean select = BSM.find(connection, key, TestBean.class);
            assertNull(select);
        }
        {
            final long key = 2;
            final AnnotatedTestBean preSelect = BSM.find(connection, key, AnnotatedTestBean.class);
            assertNotNull(preSelect);
            assertThat(preSelect.getLegacyKey(), equalTo(key));

            BSM.delete(connection, key, AnnotatedTestBean.class);

            final AnnotatedTestBean select = BSM.find(connection, key, AnnotatedTestBean.class);
            assertNull(select);
        }
    }

    @Test
    public void testBulkSelectAndDelete() {
        Timestamp now = Timestamp.from(Instant.now());
        final List<TestBean> beans = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            beans.add(BSM.insert(connection, new TestBean(null, Long.MAX_VALUE, Integer.MAX_VALUE, "test string", now, 0.0, TestBean.Status.ON)));
        }
        final List<Long> keys = beans.stream().map(TestBean::getTestKey).collect(Collectors.toList());
        List<TestBean> selected = BSM.find(connection, keys, TestBean.class);
        assertThat(selected.size(), equalTo(beans.size()));

        final int deleted = BSM.delete(connection, keys, TestBean.class);
        assertThat(deleted, equalTo(5));

        selected = BSM.find(connection, keys, TestBean.class);
        assertThat(selected.size(), equalTo(0));
    }

    @Test
    public void testSelectBuilder() {
        String selectOne = "SELECT * FROM test_bean WHERE test_key = :test_key";
        TestBean bean = BSM.select(selectOne, TestBean.class)
                .bind(ps -> ps.setLong("test_key", 1))
                .one(connection);
        assertNotNull(bean);
        assertThat(bean.getTestKey(), equalTo(1L));

        String selectMany = "SELECT * FROM test_bean WHERE test_key > :test_key";
        List<TestBean> beanList = BSM.select(selectMany, TestBean.class)
                .bind(ps -> ps.setLong("test_key", 1))
                .list(connection);
        assertNotNull(beanList);
        assertThat(beanList, not(empty()));
        beanList.forEach(elem -> assertThat(elem.getTestKey(), greaterThan(1L)));

        Map<Long, TestBean> beanMap = BSM.select(selectMany, TestBean.class)
                .bind(ps -> ps.setLong("test_key", 2))
                .map(connection, rs -> rs.getLong("test_key"));
        assertNotNull(beanMap);
        assertThat(beanMap.entrySet(), not(empty()));
        beanMap.keySet().forEach(key -> assertThat(key, greaterThan(2L)));

        Map<Long, List<TestBean>> beanMultiMap = BSM.select(selectMany, TestBean.class)
                .bind(ps -> ps.setLong("test_key", 1))
                .multiMap(connection, rs -> rs.getLong("some_int"));
        assertNotNull(beanMap);
        assertThat(beanMap.entrySet(), not(empty()));
        assertThat(1, lessThan(beanMultiMap.get(124L).size()));

        String selectAll = "SELECT * FROM test_bean";
        List<TestBean> all = BSM.select(selectAll, TestBean.class).list(connection);
        assertNotNull(all);
        assertThat(all, not(empty()));
    }
}
