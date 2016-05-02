package io.requery.test.model;


import io.requery.Entity;
import io.requery.Generated;
import io.requery.JunctionTable;
import io.requery.Key;
import io.requery.ManyToMany;
import io.requery.OrderBy;
import io.requery.PostInsert;
import io.requery.PostLoad;
import io.requery.PreUpdate;
import io.requery.PropertyNameStyle;
import io.requery.Table;
import io.requery.Transient;
import io.requery.Version;
import io.requery.query.MutableResult;

@Entity(propertyNameStyle = PropertyNameStyle.FLUENT_BEAN)
@Table(name = "Groups")
public class AbstractGroup {

    @Key @Generated
    protected int id;

    protected String name;
    protected String description;
    protected GroupType type;
    protected byte[] picture;

    @Version
    protected int version;

    @JunctionTable
    @ManyToMany
    protected MutableResult<Person> members;

    @JunctionTable(name = "Group_Owners")
    @OrderBy("name")
    @ManyToMany
    protected MutableResult<Person> owners;

    protected java.sql.Date createdDate;

    @Transient
    protected String temporaryName;

    @PostInsert
    @PostLoad
    @PreUpdate
    public void combinedListener() {

    }
}
