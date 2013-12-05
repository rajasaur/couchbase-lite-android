package com.couchbase.lite.testapp.tests;

import com.couchbase.lite.Database;
import com.couchbase.lite.Manager;

import junit.framework.Assert;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Server extends CBLiteTestCase {

    public void testServer() {

        //to ensure this test is easily repeatable we will explicitly remove
        //any stale foo.cblite
        Database old = manager.getExistingDatabase("foo");
        if(old != null) {
            old.delete();
        }

        Database db = manager.getDatabase("foo");
        Assert.assertNotNull(db);
        Assert.assertEquals("foo", db.getName());
        Assert.assertTrue(db.getPath().startsWith(getServerPath()));
        Assert.assertFalse(db.exists());

        Assert.assertEquals(db, manager.getDatabase("foo"));

        // because foo doesn't exist yet
        List<String> databaseNames = manager.getAllDatabaseNames();
        Assert.assertTrue(!databaseNames.contains("foo"));

        Assert.assertTrue(db.open());
        Assert.assertTrue(db.exists());

        databaseNames = manager.getAllDatabaseNames();
        Assert.assertTrue(databaseNames.contains("foo"));

        db.close();
        db.delete();

    }

    public void testUpgradeOldDatabaseFiles() throws Exception {
        String directoryName = "test-directory-" + System.currentTimeMillis();
        String normalFilesDir = getInstrumentation().getContext().getFilesDir().getAbsolutePath();
        String fakeFilesDir = String.format("%s/%s", normalFilesDir, directoryName);

        File directory = new File(fakeFilesDir);
        if(!directory.exists()) {
            boolean result = directory.mkdir();
            if(!result) {
                throw new IOException("Unable to create directory " + directory);
            }
        }
        File oldTouchDbFile = new File(directory, String.format("old%s", Manager.DATABASE_SUFFIX_OLD));
        oldTouchDbFile.createNewFile();
        File newCbLiteFile = new File(directory, String.format("new%s", Manager.DATABASE_SUFFIX));
        newCbLiteFile.createNewFile();

        File migratedOldFile = new File(directory, String.format("old%s", Manager.DATABASE_SUFFIX));
        migratedOldFile.createNewFile();
        super.stopCBLite();
        manager = new Manager(new File(getInstrumentation().getContext().getFilesDir(), directoryName), Manager.DEFAULT_OPTIONS);

        Assert.assertTrue(migratedOldFile.exists());
        //cannot rename old.touchdb in old.cblite, old.cblite already exists
        Assert.assertTrue(oldTouchDbFile.exists());
        Assert.assertTrue(newCbLiteFile.exists());

        File dir=new File(getInstrumentation().getContext().getFilesDir(), directoryName);
        Assert.assertEquals(3, dir.listFiles().length);

        super.stopCBLite();
        migratedOldFile.delete();
        manager = new Manager(new File(getInstrumentation().getContext().getFilesDir(), directoryName), Manager.DEFAULT_OPTIONS);

        //rename old.touchdb in old.cblite, previous old.cblite already doesn't exist
        Assert.assertTrue(migratedOldFile.exists());
        Assert.assertTrue(oldTouchDbFile.exists() == false);
        Assert.assertTrue(newCbLiteFile.exists());
        dir=new File(getInstrumentation().getContext().getFilesDir(), directoryName);
        Assert.assertEquals(2, dir.listFiles().length);

    }

}
