package cyeagy.dorm;

import java.lang.reflect.Field;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * This class analyzes a POJO via reflection to identify table data
 */
public class TableData {
    private final String tableName;
    private final Field primaryKey;
    private final List<Field> columns;//excluding PK

    private TableData(String tableName, Field primaryKey, List<Field> columns) {
        this.tableName = tableName;
        this.primaryKey = primaryKey;
        this.columns = columns;
    }

    public String getTableName() {
        return tableName;
    }

    public Field getPrimaryKey() {
        return primaryKey;
    }

    public List<Field> getColumns() {
        return columns;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TableData tableData = (TableData) o;
        return Objects.equals(tableName, tableData.tableName) &&
                Objects.equals(primaryKey, tableData.primaryKey) &&
                Objects.equals(columns, tableData.columns);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tableName, primaryKey, columns);
    }

    private static final Map<Class<?>, TableData> METADATA_CACHE = new HashMap<>();

    public static TableData from(Class<?> clazz) {
        return from(clazz, true);
    }

    public static TableData from(Class<?> clazz, boolean forceAccessible) {
        TableData tableData = METADATA_CACHE.get(clazz);
        if (tableData == null) {
            final String tableName = getTableName(clazz);
            final Field[] fields = clazz.getDeclaredFields();
            final List<Field> columns = new ArrayList<>(fields.length);
            Field primaryKey = null;
            for (Field field : fields) {
                if (field.isAnnotationPresent(Id.class)) {
                    primaryKey = field;
                } else {
                    columns.add(field);
                }
                if (forceAccessible) {
                    ReflectUtil.setAccessible(field);
                }
            }
            if (primaryKey == null) {
                primaryKey = fields[0];//use first column as pk
                columns.remove(0);//remove the pk from the column list
            }
            tableData = new TableData(tableName, primaryKey, Collections.unmodifiableList(columns));
            METADATA_CACHE.put(clazz, tableData);
        }
        return tableData;
    }

    public static String getColumnName(Field field) {
        final Column annotation = field.getDeclaredAnnotation(Column.class);
        return camelToSnake(annotation == null ? field.getName() : annotation.name());
    }

    public static String getTableName(Class<?> clazz) {
        final Table annotation = clazz.getDeclaredAnnotation(Table.class);
        if (annotation != null) {
            if (!annotation.schema().isEmpty()) {
                if (!annotation.name().isEmpty()) {
                    return annotation.schema() + "." + annotation.name();
                } else {
                    return annotation.schema() + "." + camelToSnake(clazz.getSimpleName());
                }
            } else {
                if (!annotation.name().isEmpty()) {
                    return annotation.name();
                }
            }
        }
        return camelToSnake(clazz.getSimpleName());
    }

    private static String camelToSnake(String camel) {
        final StringBuilder sb = new StringBuilder();
        final StringCharacterIterator iter = new StringCharacterIterator(camel);
        boolean prevLower = false;
        for (char c = iter.first(); c != CharacterIterator.DONE; c = iter.next()) {
            if (Character.isUpperCase(c)) {
                if (prevLower) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(c));
                prevLower = false;
            } else {
                sb.append(c);
                prevLower = true;
            }
        }
        return sb.toString();
    }
}
