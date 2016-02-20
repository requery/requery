package io.requery.test.model;


import io.requery.CascadeAction;
import io.requery.Column;
import io.requery.Entity;
import io.requery.ForeignKey;
import io.requery.Generated;
import io.requery.Index;
import io.requery.Key;
import io.requery.Lazy;
import io.requery.ManyToMany;
import io.requery.Naming;
import io.requery.Nullable;
import io.requery.OneToMany;
import io.requery.OneToOne;
import io.requery.PostDelete;
import io.requery.PostInsert;
import io.requery.PostLoad;
import io.requery.PostUpdate;
import io.requery.PreDelete;
import io.requery.PreInsert;
import io.requery.PreUpdate;
import io.requery.query.MutableResult;
import io.requery.test.EntityState;

import java.io.Serializable;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
public class AbstractPerson implements Serializable {

    @Key @Generated
    protected int id;

    protected String name;
    @Index(name = "email_index")
    protected String email;
    protected Date birthday;
    @Nullable
    protected int age;

    @ForeignKey
    @OneToOne
    protected Address address;

    @OneToMany(mappedBy = "owner", cascade =
            {CascadeAction.DELETE, CascadeAction.SAVE})
    protected MutableResult<Phone> phoneNumbers;

    @OneToMany(mappedBy = "owner")
    protected Set<Phone> phoneNumbersSet;

    @OneToMany
    protected List<Phone> phoneNumbersList;

    @ManyToMany(mappedBy = "members")
    protected MutableResult<Group> groups;

    @Lazy
    protected String about;

    @Column(unique = true)
    @Naming(getter = "getUUID", setter = "setUUID")
    protected UUID uuid;
    protected URL homepage;

    protected String picture;

    private EntityState previous;
    private EntityState current;

    public EntityState getPreviousState() {
        return previous;
    }

    public EntityState getCurrentState() {
        return current;
    }

    private void setState(EntityState state) {
        this.previous = current;
        this.current = state;
    }

    @PreInsert
    public void onPreInsert() {
        setState(EntityState.PRE_SAVE);
    }

    @PostInsert
    public void onPostInsert() {
        setState(EntityState.POST_SAVE);
    }

    @PostLoad
    public void onPostLoad() {
        setState(EntityState.POST_LOAD);
    }

    @PreUpdate
    public void onPreUpdate() {
        setState(EntityState.PRE_UPDATE);
    }

    @PostUpdate
    public void onPostUpdate() {
        setState(EntityState.POST_UPDATE);
    }

    @PreDelete
    public void onPreDelete() {
        setState(EntityState.PRE_DELETE);
    }

    @PostDelete
    public void onPostDelete() {
        setState(EntityState.POST_DELETE);
    }

}
