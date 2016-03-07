![requery](http://requery.github.io/logo.png)

A light but powerful ORM and SQL query generator for Java/Android with RxJava and Java 8 support.

Examples
--------

**Entities:**

```java
@Entity
abstract class AbstractPerson {

    @Key @Generated
    int id;

    @Index(name = "name_index")              // table specification
    String name;

    @OneToMany                               // relationships 1:1, 1:many, many to many
    Set<Phone> phoneNumbers;

    @Converter(EmailToStringConverter.class) // custom type conversion
    Email email;

    @PostLoad                                // lifecycle callbacks
    void afterLoad() {
        updatePeopleList();
    }

    // getter, setters, equals & hashCode automatically generated into Person.java
}

```
or from an **interface:**

```java
@Entity
public interface Person {

    @Key @Generated
    int getId();

    String getName();

    @OneToMany
    Set<Phone> getPhoneNumbers();

    String getEmail();
}
```

**Queries:**

```java
Result<Person> query = data
    .select(Person.class)
    .where(Person.NAME.lower().like("b%")).and(Person.AGE.gt(20))
    .orderBy(Person.AGE.desc())
    .limit(5)
    .get();
```

**Relationships:** represent relations more efficiently with Java 8 Streams, RxJava Observables or
plain iterables. (sets and lists are supported to)

```java
@Entity
abstract class AbstractPerson {

    @Key @Generated
    int id;

    @ManyToMany
    Result<Group> groups;
    // equivalent to:
    // data.select(Group.class)
    // .join(Group_Person.class).on(Group_ID.equal(Group_Person.GROUP_ID))
    // .join(Person.class).on(Group_Person.PERSON_ID.equal(Person.ID))
    // .where(Person.ID.equal(id))
}
```

**Java 8 [streams](https://docs.oracle.com/javase/8/docs/api/java/util/stream/Stream.html):**

```java
data.select(Person.class)
    .orderBy(Person.AGE.desc())
    .get()
    .stream().forEach(System.out::println);
```

**Java 8 optional and time support:**

```java
public interface Person {

    @Key @Generated
    int getId();

    String getName();
    Optional<String> getEmail();
    ZonedDateTime getBirthday();
}
```

**[RxJava](https://github.com/ReactiveX/RxJava) [Observables](http://reactivex.io/documentation/observable.html):**

```java
Observable<Person> observable = data
    .select(Person.class)
    .orderBy(Person.AGE.desc())
    .get()
    .toObservable();
```

**[RxJava](https://github.com/ReactiveX/RxJava) observe query on table changes:**

```java
Observable<Person> observable = data
    .select(Person.class)
    .orderBy(Person.AGE.desc())
    .get()
    .toSelfObservable().subscribe(::updateFromResult);
```

**Immutable types** You can combine requery/JPA attributes on [@AutoValue](https://github.com/google/auto/tree/master/value)
and other generated immutable types and use requery object mapping in queries.

```java
@AutoValue
@Entity
public abstract class Person implements Serializable {

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder setId(int id);
        public abstract Builder setName(String name);
        public abstract Builder setBirthday(Date date);
        public abstract Builder setAge(int age);
        public abstract Person build();
    }

    public static Builder builder() {
        return new AutoValue_Person.Builder();
    }

    @Id @GeneratedValue
    public abstract int getId();

    public abstract String getName();
    public abstract Date getBirthday();
    public abstract int getAge();
}
```
(Note some features will not be available when using immutable types, see the
[wiki](https://github.com/requery/requery/wiki/Immutable-types) for more info)

**Read/write separation** Along with immutable types you can choose to separate querying (reading)
and modifications (writing):

```java
int rows = data.update(Person.class)
    .set(Person.ABOUT, "nothing")
    .set(Person.AGE, 50)
    .where(Person.AGE.equal(100)).get();
```

Features
--------

- No Reflection
- Fast startup & performance
- No dependencies (RxJava is optional)
- Typed query language
- Table generation
- Supports JDBC and many popular databases
- Supports Android (SQLite, RecyclerView, Databinding)
- RxJava support
- Blocking and non-blocking API
- Partial objects/refresh
- Caching
- Lifecycle callbacks
- Custom type converters
- Compile time entity validation
- JPA annotations (however requery is not a JPA provider)

Reflection free
---------------

requery uses compile time annotation processing to generate your entity model classes. On Android
this means you get about the same performance reading objects from a query as if it was populated
using the standard Cursor and ContentValues API.

Type safe query
---------------

The compiled classes work with the query API to take advantage of compile time generated attributes.
Create type safe queries and avoid hard to maintain, error prone string concatenated queries.

Relationships
-------------

You can define One-to-One, One-to-Many, Many-to-One, and Many-to-Many relations in your models using
annotations. Relationships can be navigated in both directions. Of many type relations can be loaded
into standard java collection objects or into a more efficient
[Result](http://requery.github.io/javadoc/io/requery/query/Result.html) type.
From a [Result](http://requery.github.io/javadoc/io/requery/query/Result.html)
easily create a Stream, RxJava Observable, Iterator, List or Map.

Many-to-Many junction tables can be generated automatically. Additionally the relation model is
validated at compile time eliminating runtime errors.

Android
-------

Designed specifically with Android support in mind.

Comparison to similar Android libraries:

Feature               |  requery |  ORMLite |  Squidb  |  DBFlow   | GreenDao
----------------------|----------|----------|----------|-----------|-----------
Relational mapping    |  Y       |  Y(1)    |  N       |  Y        | Y(1)
Inverse relationships |  Y       |  N       |  N       |  N        | N
Compile time          |  Y       |  N       |  Y       |  Y        | Y(2)
Query DSL             |  Y       |  N       |  N(3)    |  N(3)     | N(3)
JDBC Support          |  Y       |  Y       |  N       |  N        | N
Table Generation      |  Y       |  Y       |  Y       |  Y        | Y
JPA annotations       |  Y       |  Y       |  N       |  N        | N
RxJava support        |  Y       |  N       |  Y(4)    |  N        | N

1) Excludes Many-to-Many
2) Not annotation based
3) Builder only not DSL
4) Table changes only

See [requery-android/example](https://github.com/requery/requery/tree/master/requery-android/example)
for an example Android project using databinding and interface based entities. For more information
see the [Android](https://github.com/requery/requery/wiki/Android) page.

Code generation
---------------

Generate entities from Abstract or Interface classes. Use JPA annotations or requery annotations.
requery will generate getter/setters, equals() and hashcode() when needed.

Supported Databases
-------------------
Tested on some of the most popular databases:

- PostgresSQL (9.1+)
- MySQL 5.x
- Oracle 12c+
- Microsoft SQL Server 2012 or later
- SQLite (Android or with the [xerial](https://github.com/xerial/sqlite-jdbc) JDBC driver)
- Apache Derby 10.11+
- H2 1.4+
- HSQLDB 2.3+

JPA Annotations
---------------

A subset of the JPA annotations that map onto the requery annotations are supported.
See [here](https://github.com/requery/requery/wiki/JPA-Annotations) for more information.

Using it
--------

Currently SNAPSHOT versions are available on http://oss.jfrog.org.

```gradle
repositories {
    jcenter()
    maven { url 'http://oss.jfrog.org/artifactory/oss-snapshot-local' }
}

dependencies {
    compile 'io.requery:requery:1.0-SNAPSHOT'
    compile 'io.requery:requery-android:1.0-SNAPSHOT' // for android
    apt 'io.requery:requery-processor:1.0-SNAPSHOT'   // prefer an APT plugin
}
```

For more information on gradle and annotation processing & gradle see the [wiki](https://github.com/requery/requery/wiki/Gradle-&-Annotation-processing#annotation-processing).

Beta release on bintray will arrive soon!

License
-------

    Copyright (C) 2016 requery.io

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

