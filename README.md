requery
=======

Micro object relationship mapper for Java/Android.

Entities:

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
or from an interface:

```java
@Entity
public interface Person {

    @Key @Generated
    int getId();

    String getName();
    String getEmail();
    Date getBirthday();
    @OneToMany
    Result<Phone> getPhoneNumbers();

}
```

Queries:

```java
List<Person> query = data
    .select(Person.class)
    .where(Person.NAME.lower().like("b%"))
    .orderBy(Person.AGE.desc())
    .limit(5)
    .execute().list();
```

Relationships: rather than sets, lists which have to be materialized with all the results,
you can alternatively use query results directly: (of course sets and lists are supported to)

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

Java 8 streams:

```java
List<Person> query = data
    .select(Person.class)
    .orderBy(Person.AGE.desc())
    .execute()
    .stream().forEach(System.out::println);
```

RxJava Observables:

```java
Observable<Person> query = data
    .select(Person.class)
    .orderBy(Person.AGE.desc())
    .execute()
    .toObservable();
```

RxJava observe query on table changes:

```java
Observable<Person> query = data
    .select(Person.class)
    .orderBy(Person.AGE.desc())
    .execute()
    .toSelfObservable().subscribe(::updateFromResult);
```

Optional Read/write separation. If you prefer separating read from writes mark the entity as
@ReadOnly and use update statements to modify data instead.

```java
data.update(Person.class)
    .set(Person.ABOUT, "nothing")
    .set(Person.AGE, 50)
    .where(Person.AGE.equal(100)).execute();
```

Features
--------

- No Reflection
- Fast startup
- Typed query language
- Table generation
- Supports JDBC and most popular databases
- Supports Android (RecyclerView, Databinding, Parcelable)
- RxJava support
- Blocking and non-blocking API
- Partial objects/refresh
- Caching
- Lifecycle callbacks
- Custom type converters
- JPA annotations (requery is not a JPA provider)

Motivation
----------

There are definitely some similar in concept compile time libraries (for Android) however
most of these omit the 'relational' part of object relational mapping. And generally only work for
SQLite and have a very small number of features.

The idea is to bring over those some of those ideas but to everywhere Java is used. At the same
time support real relationships, both blocking and non-blocking APIs, multiple databases and
much more.

Reflection free
---------------

requery uses compile time annotation processing to generate your entity model classes. On Android
this means you get about the same performance reading objects from a query as if you populated it
directly using the standard Cursor and ContentValues API.

Type safe query
---------------

The compiled classes work with the query API to take advantage of compile time generated attributes.
Create type safe queries and avoid hard to maintain, error prone string concatenated queries.

Relationships
-------------

You can define One-to-One, One-to-Many, Many-to-One, and Many-to-Many relations in your models using
annotations. Relationships can be navigated in both directions. Of many type relations can be loaded
into standard java collection objects or into a more efficient iterable only object. Many-to-Many
junction tables can be generated automatically. Additionally the relation model is validated at
compile time eliminating many runtime issues.

Android
-------

Designed specifically with Android support in mind. Easily use query results into recycler views.
Easily make your objects Data bindable.

Comparison to similar Android libraries:

Feature               |  requery |  ORMLite |  Squidb  |  DBFlow   | GreenDao
----------------------|----------|----------|----------|-----------|-----------
Relational mapping    |  Y       |  Y(1)    |  N       |  Y(1)     | Y(1)
Inverse relationships |  Y       |  N       |  N       |  N        | N
Compile time          |  Y       |  N       |  Y       |  Y        | Y(2)
JDBC Support          |  Y       |  Y       |  N       |  N        | N
query language        |  Y       |  N       |  Y(3)    |  Y(3)     | Y(3)
Table Generation      |  Y       |  Y       |  Y       |  Y        | Y
JPA annotations       |  Y       |  Y       |  N       |  N        | N

1) Excludes Many-to-Many
2) Not annotation based
3) Builder only

See requery-android/example for an example Android project using databinding and interface based
entities

Code generation
---------------

Generated entities from Abstract or Interface classes. Use JPA annotations or requery annotations.
Requery will generate getter/setters, equals() and hashcode().

Supported Databases
-------------------
Tested on some of the most popular databases:

- PostgresSQL (9.1+)
- MySQL 5.x
- Oracle 12c+
- Microsoft SQL Server 2012 or later
- SQLite (Android or with xerial JDBC driver)
- Apache Derby 10.11+
- H2 1.4+
- HSQLDB 2.3+

JPA Annotations
---------------

Only a subset of the JPA annotations are supported. These annotations are supported:

JPA Annotation      |
--------------------|
Basic               |
Cacheable           |
Column              |
Entity              |
Enumerated          |
GeneratedValue      |
Id                  |
JoinColumn          |
JoinTable           |
ManyToMany          |
ManyToOne           |
OneToMany           |
OneToOne            |
PostLoad            |
PostPersist         |
PostRemove          |
PostUpdate          |
PrePersist          |
PreRemove           |
PreUpdate           |
Table               |
Transient           |
Version             |

There is no support for embedded types or mapped superclasses. Unique/index constraints must
be placed on the field/method level. Some advanced JPA features are not yet supported such as
mapping to JoinTable or secondary tables to define relationships outside of ManyToMany.

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

