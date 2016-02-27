[![Build Status](https://travis-ci.org/cyeagy/bss.svg?branch=master)](https://travis-ci.org/cyeagy/bss)

# BSS - Better SQL Support
### Lightweight JDBC enhancement library. Joinless ORM.
###### Dependencies: Java 8.
Vanilla JDBC is a utilitarian and lackluster experience, an early java API that feels dated.<br>
Lots of common usages are easy to screw up, like null handling primitives, or properly executing a transaction and rollback.
The index based parameter API is error prone, and support lacks for the common IN clause!<br>
This library can enhance the JDBC experience to one that's bearable. Near decent even!

[**BetterPreparedStatement**](https://github.com/cyeagy/bss/wiki/BetterPreparedStatement)
 * :named parameters!!
 * null-safe primitive set methods!
 * java 8 time set methods
 * automagic create/set array methods!
 * IN clause support with array simulation!!! (for DBs that don't support arrays)

[**BetterResultSet**](https://github.com/cyeagy/bss/wiki/BetterResultSet)
 * null-safe primitive get methods!
 * java 8 time get methods
 * SQL arrays casted to their java type

[**BetterSqlSupport**](https://github.com/cyeagy/bss/wiki/BetterSqlSupport)
 * provides a compact lambda based API encapsulating common CRUD usage of JDBC
 * additional fluent statement builder API for added flexibility

[**BetterSqlMapper**](https://github.com/cyeagy/bss/wiki/BetterSqlMapper)
 * joinless ORM
 * simple convention with annotation overrides
 * additional fluent select builder API allows any query to automagically map to a POJO

[**BetterSqlTransaction**](https://github.com/cyeagy/bss/wiki/BetterSqlTransaction)
 * encapsulates the JDBC transaction process into a simple lambda based API

[**BetterSqlGenerator**](https://github.com/cyeagy/bss/wiki/BetterSqlGenerator)
 * generate basic CRUD SQL from a POJO

#### Tested Databases: PostgreSQL. MySQL/MariaDB. H2.
## Usage
Check out the [wiki](https://github.com/cyeagy/bss/wiki) for basic usage examples. The test classes are another good reference.
