Change Log
==========

## 1.5.0

- Deprecates RxJava 1.0 support in order to bring improvements to RxJava 2.0 support in a future version
- Default indexes now include table name in the index name
- Support setting generated member visibility (for removing synthetic accessors)
- Support package private code generation for reduced method count on Android
- Fix NoSuchElementException during table generation of an entity with no keys
- Fix Android proguard rules
- Fix EntityDataStore synchronization that could dead lock
- Fix inserting empty collection returning null key set
- Fix Kotlin ClassCastException when using long/int key types in the insert returning query
- Fix CloseableIterator accumulation when using Result functions
- Fix Connection not closed when exception thrown from a raw query 

## 1.4.1

- Support accessing the Configuration instance from EntityDataStore
- Support setting generated member visibility (for removing synthetic accessors)
- Support accessing Transaction instance from KotlinReactiveEntityStore
- PostgresSQL store blobs as bytea
- Fix foreign key support in Android sqlcipher/sqlite-support
- Fix stack overflow on bi-directional attributes in EnityProxy.toString
- Fix references cascaded even if Cascade.NONE specified
- Fix NoSuchMethodError when converting a entity with @Embedded types to json
- Fix Observables not triggered for insert operations 
- Fix @Transient annotation not applied to methods
- Fix @Transient members were checked for reserved works
- Fix Connection instance not closed on transaction rollback

## 1.4.0

- Support item view types in Android RecyclerView adapter
- Support withTransaction overloads for Kotlin entity stores
- Support guava/streamsupport Optional types
- Rx support transaction methods modified to update in a more expected way
- Fix default value usage in inserts
- Fix aliased expression not usable in orderby clause
- Fix collection getters not used in Jackson serialization
- Fix collection modification behavior
- Fix cases where duplicate entity attributes would be generated
- Fix MutableResult not usable in an entity definition 
- Fix Android unique column not being able to be added in an upgrade
- Fix Kotlin Update.set() method property support

## 1.3.2

- Fix Rx observable changes not triggered for nested transactions
- Fix query table auto aliases removed in union statement
- Fix CascadeAction.NONE having no effect during a insert/update/delete

## 1.3.1

- Support Logical NOT operator
- Support Converter for java.util.Currency
- Fix non-null reference returned for null foreign key reference on entity retrieval 
- Fix column transformers not used when aliasing columns
- Fix ConnectionPoolDataSource not used in ConnectionProvider
- Fix Read only values not filtered from insert/update operations
- Fix Android schema upgrade using not yet created indexes  
- Fix Android incorrect concurrent access of DateFormat instance 
- Fix Android QueryLoader closed previous result instance
- Fix processing error generated for @Transient entity not used in a relationship
- Fix Kotlin EntityDataStore inner instance not exposed

## 1.3.0

- Support @Superclass hierarchies  
- Support @Embedded types in Jackson serialization
- Support mixed Kotlin queries (note this is an API change to Kotlin partial select statements)
- Fix incorrect join alias generated in query
- Fix Android gradle plugin update 2.3.0 not working with entities using databinding
- Fix Kotlin insert into select query
- Fix non-null constraint not generated on Postgres
- Fix @View entities created as tables during schema generation

## 1.2.1

- Support row value expressions
- Support generating entities from Kotlin abstract classes
- Support Kotlin 1.1
- Fix handling of self referencing Many-to-Many relationship
- Fix Android table/column name transformers not used during upgrade
- Fix use AutoValue builder setters if available
- Fix @Value.Default handling for immutable.org types
- Fix deleting entities from a One-To-Many when using delete(Iterable)
- Kotlin fix varargs in raw query not expanded
- Kotlin fix ClassCastException when using a join clause

## 1.2.0

- Support Jackson serialization with a new Jackson serialization module (requery-jackson)
- Support insert into select query
- Support additional query functions
- Support improved for Immutable.org types
- Fix unique constraints not applied during table generation
- Fix closing connection/statement if an exception is thrown during prepare
- Fix ClassCastException when using a function in a where expression
- Fix NPE when a junction table contains a non-associative attribute
- Fix unsupported on update clause generated when using an Oracle database 
- Fix Kotlin EntityStore delete should return a nullable type

## 1.1.2

- Support @View annotation for mapping entities to table views
- Fix runInTransaction rollback behavior
- Fix add column not added to foreign key during table upgrade
- Fix function expression in order by column caused incorrect sql generation
- Fix rx observableResult not triggered for multiple entity delete
- Fix OptimisticLockException when updating an entity with a @Version column
- Fix Kotlin select distinct query
- Fix Android incompatible date types, now stored as ISO 8601
- Fix update listener not invoked for some upsert calls

## 1.1.1

- Support new KotlinReactiveEntityStore/KotlinRxEntityStore for RxJava 2.0 & RxJava 1.0
- Support improved compile errors when an invalid relationship is defined
- Support improved handling of entity associations marked with @Key
- Support kotlin-kapt plugin for annotation processing
- Support kotlin raw queries
- Fix incorrect type in raw queries for integer values on Oracle
- Fix Java 8 date time conversion for zoned times
- Fix empty index name during table generation
- Fix ClassCastException when using a custom converter that converted to a collection in a query
- Fix NPE when using non relational attributes in a junction table
- Fix Kotlin withTransaction rollback
- Fix Kotlin join on condition incorrect sql generated

## 1.1.0

- Support extendable query types. RxJava and RxJava 2.0 result conversion methods are now separated
into different classes. This is a minor API change that should not effect most code.
- Support [Reactor Core extensions](http://projectreactor.io)
- Support custom column definitions for table generation
- Support for @Entity(copyable) shallow copying via generated copy() method
- Kotlin findByKey returns nullable value
- Fix ClassNotFoundException referencing a RxJava 1.0 class when using RxJava 2.0
- Fix OneToMany cascaded elements incorrectly updated instead of inserted
- Fix stackoverflow in ManagedTransaction.begin()
- Fix foreign key support using Android SQLCipher database
- Fix multiple blob arguments in Android query expression
- Fix some compilation issues using Android Jack compiler

## 1.0.2

- Support repeated @Embedded fields
- Support RxJava 2.0 maybe operator
- Fix null TypeElement when using Android Jack compiler
- Fix parentheses in nested conditional expressions
- Fix parameter name ordering for kotlin data classes
- Fix reserved name field names generated from properties
- Fix incorrect behavior of Rx runInTransaction
- Fix stack overflow in Kotlin refreshAll
- Fix OrderBy attribute name prefix not removed

## 1.0.1

- Support multi-column unique indexes
- Support delete on entities with no key
- Fix EntityStore.insert method returning generated keys transaction not committed
- Fix Android proguard rules for RxJava 2.0
- Fix Android sqlite blob query arguments not working
- Fix Android onCreateMapping invocation for SqlitexDatabaseSource
- Fix Kotlin type variance parameter on join queries
- Fix m prefixed member name not removed in generated setMappedAttribute

## 1.0.0

- Support for RxJava 2.0
- Support update on specific attributes
- Support insert with default values
- Support Kotlin 1.0.4
- Fix Android mapping support for BigDecimal
- Fix cascading Many-to-Many inserts

## 1.0.0-rc4

- Fix compilation of certain queries under Java 8
- Fix Android Uri converter not applied
- Fix multiple model definitions inside the same java package
- Fix member prefixes in Attribute static field names not removed
- Fix Kotlin attribute missing declared type definitions

## 1.0.0-rc3

- Support embeddable AutoValue types
- Fix Kotlin processing of interfaces containing companion objects
- Fix Kotlin query matching attributes to property names
- Fix Kotlin unsupported method names on Android 
- Fix multiple column unique constraint

## 1.0.0-rc2

- Support custom function calls on expressions in generated sql
- Support @Superclass on interface entity types
- Fix @Transient property not generated when using an interface entity type
- Fix @JunctionTable.type() error during processing
- Fix @JunctionTable custom column referencedColumn not used

## 1.0.0-rc1

- Kotlin module
- Support global custom table / column naming
- Fix self referencing table when used in a relationship
- Fix relational column ordering when using a set

## 1.0.0-beta23

- Support global entity state change listeners
- Support Tuple result in Android query adapter
- Fix NPE in query generation for MS SQL
- Fix read timeout on Android when using toSelfObservable
- Fix reserved name checks for table & column names

## 1.0.0-beta22

- Support Rx type changes for update queries
- Fix RowCountException when deleting cascading entity
- Fix ClassCastException when using CompositeKey

## 1.0.0-beta21

- Support for @Embedded types
- Support 'is' prefix for boolean getters
- Support compile time validation of many-to-one & one-to-many mapping
- Fix possible Constraint violation in One-to-Many insert
- Fix invalid UUID conversion
- Fix query/update of a relational field that is also used as a key

## 1.0.0-beta20

- Fix possible StackOverflow in One-to-Many insert
- Fix possible closed Result in toObservable()
- Fix ClassCastException for key attributes that are also entity references
- Fix Android default proguard rules for SQLCipher/SQLite support

## 1.0.0-beta19

- Support self referential entity types
- Fix cascading upsert for relational entities
- Fix foreign key column not included in default selection in some cases
- Fix findByKey defaults to returning null if entity not present
- Fix Android default proguard rules

## 1.0.0-beta18

- Support relational type changes in rx toSelfObservable
- Fix @Entity.name not taking effect for the generated class name
- Fix cascading upsert for relational entities
- Fix entity state listeners not add from @Superclass types
- Fix annotations only looked up when added directly from the annotation processor
- Fix Type.singleKeyAttribute not available until keyAttributes was called

## 1.0.0-beta17

- Support @OrderBy annotation for ordering relations in entities
- Support separate query expression for foreign keys using the key raw type
- Support @JunctionTable#type value that allows the junction table to be manually specified
- Fix @Superclass non-annotated fields/methods ignored
- Fix issues when generating from an Kotlin interface/data class

## 1.0.0-beta16

- Support generating entity types from Kotlin abstract and data classes
- Support non integer key types in generated join tables
- Support @CheckReturnValue to add IDE warnings for methods that require additional calls
- Support iterable overloads for update/upsert
- Support additional validations of @OneToOne relationships
- Fix stackoverflow exception in cascade reference
- Fix null mapping instance in reader/writer classes in certain cases

## 1.0.0-beta15

- Fix generated SQL incorrect in some non-US locales
- Fix several issues when using immutable types
- Fix RxJava type changes not serialized
- Fix Android library publish
- Fix Android table not existing error on database first created w/ WAL mode

## 1.0.0-beta14

- Support for generating mappings for final/non-extendable classes
- Support raw query parameter expansion
- Fix cascade saving of entities in update
- Fix upsert for CompletableEntityStore
- Fix entity parceling on Android

## 1.0.0-beta13

- Support non observable collections in relations
- Annotation processor dependencies shadowed to prevent conflicts with other libraries

## 1.0.0-beta12

- Support for Android SQLite support library
- Support table creation mode in Android for development
- Fix @Converter implementations using generic types
- Fix cascade saving of foreign key references
- Fix missing sources for requery-android artifact

## 1.0.0-beta11

- Support for Upserts
- Support for raw queries
- Fix cascading insert/updates based on primary key presence

## 1.0.0-beta10

- Support for multi-column indexes
- Support on update referential action for foreign key annotation
- Fix for checking type hierarchy for superclass elements
- Fix missing space in sub select query alias

## 1.0.0-beta9

- Support substr() function
- Support sub selects in join queries
- Support generation of metadata only classes

## 1.0.0-beta8

- Support foreign key references in queries
- Fix create result iterator only when Observable is subscribed

## 1.0.0-beta7

- Support @Superclass and @MappedSuperclass entity inheritance
- Fix Observable not triggered when deleting a entity

## 1.0.0-beta6

- Change behavior of toSelfObservable to use PublishSubject

## 1.0.0-beta5

- Support primitive byte type specialization

## 1.0.0-beta4

- Support query expressions on right side of conditions
- Fix missing imports in generated code

## 1.0.0-beta3

- Support for @AutoValue types

## 1.0.0-beta2

- Support SQLCipher on Android

## 1.0.0-beta1

- Initial beta release