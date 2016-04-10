Change Log
==========

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