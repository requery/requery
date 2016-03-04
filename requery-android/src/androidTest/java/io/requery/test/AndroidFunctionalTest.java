package io.requery.test;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import io.requery.android.sqlite.DatabaseSource;
import io.requery.meta.EntityModel;
import io.requery.sql.EntityDataStore;
import io.requery.test.model.Models;
import org.junit.runner.RunWith;

import java.sql.SQLException;

/**
 * Reuses the core functional tests from the test project
 */
@RunWith(AndroidJUnit4.class)
public class AndroidFunctionalTest extends FunctionalTest {

    private DatabaseSource dataSource;

    @Override
    public void setup() throws SQLException {
        Context context = InstrumentationRegistry.getContext();
        final String dbName = "test.db";
        context.deleteDatabase(dbName);
        EntityModel model = Models.DEFAULT;
        dataSource = new DatabaseSource(context, model, dbName, 1);
        dataSource.setLoggingEnabled(true);
        data = new EntityDataStore<>(dataSource.getConfiguration());
    }

    @Override
    public void teardown() {
        super.teardown();
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
