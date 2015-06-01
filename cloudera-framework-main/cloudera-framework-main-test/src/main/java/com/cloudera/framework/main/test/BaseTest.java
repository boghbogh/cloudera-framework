package com.cloudera.framework.main.test;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

public abstract class BaseTest {

  public static String DIR_WORKING = "target";
  public static String DIR_FS_LOCAL = "test-fs-local";
  public static String DIR_HDFS_LOCAL = "test-hdfs-local";
  public static String DIR_HDFS_MINICLUSTER = "test-hdfs-minicluster";

  public static String PATH_FS_LOCAL = DIR_WORKING + "/" + DIR_FS_LOCAL;
  public static String PATH_HDFS_LOCAL = DIR_WORKING + "/" + DIR_HDFS_LOCAL;
  public static String PATH_HDFS_MINICLUSTER = DIR_WORKING + "/"
      + DIR_HDFS_MINICLUSTER;

  public static String PATH_LOCAL_WORKING_DIR = new File(".").getAbsolutePath();
  public static String PATH_LOCAL_WORKING_DIR_TARGET = PATH_LOCAL_WORKING_DIR
      + "/" + DIR_WORKING;
  public static String PATH_LOCAL_WORKING_DIR_TARGET_HDFS_LOCAL = PATH_LOCAL_WORKING_DIR_TARGET
      + "/" + DIR_HDFS_LOCAL;
  public static String PATH_LOCAL_WORKING_DIR_TARGET_HDFS_MINICLUSTER = PATH_LOCAL_WORKING_DIR_TARGET
      + "/" + DIR_HDFS_MINICLUSTER;

  public abstract FileSystem getFileSystem() throws Exception;

  @BeforeClass
  public static void setUpClass() {
    System.setProperty("java.security.krb5.realm", "CDHCLUSTER.com");
    System.setProperty("java.security.krb5.kdc", "kdc.cdhcluster.com");
    System.setProperty("java.security.krb5.conf", "/dev/null");
    System.setProperty("dir.working", PATH_LOCAL_WORKING_DIR);
    System.setProperty("dir.working.target", PATH_LOCAL_WORKING_DIR_TARGET);
    System.setProperty("dir.working.target.hdfs",
        PATH_LOCAL_WORKING_DIR_TARGET_HDFS_LOCAL);
    System.setProperty("test.build.data",
        PATH_LOCAL_WORKING_DIR_TARGET_HDFS_MINICLUSTER);
    System.setProperty("dir.working.target.derby", PATH_LOCAL_WORKING_DIR
        + "/target/derby");
    System.setProperty("dir.working.target.derby.db",
        System.getProperty("dir.working.target.derby") + "/db");
    System.setProperty("derby.stream.error.file",
        System.getProperty("dir.working.target.derby") + "/derby.log");
    File derbyDir = new File(System.getProperty("dir.working.target.derby.db"));
    try {
      FileUtils.deleteDirectory(derbyDir);
      derbyDir.mkdirs();
    } catch (IOException e) {
    }
  }

  @Before
  public void setUp() throws Exception {
    FileSystem fileSystem = getFileSystem();
    if (fileSystem != null) {
      Path rootPath = new Path(BaseTest.getPathHDFS("/"));
      Path tmpPath = new Path(BaseTest.getPathHDFS("/tmp"));
      Path userPath = new Path(BaseTest.getPathHDFS("/user"));
      fileSystem.delete(rootPath, true);
      fileSystem.mkdirs(rootPath);
      fileSystem.mkdirs(tmpPath);
      fileSystem.setPermission(tmpPath, new FsPermission(FsAction.ALL,
          FsAction.ALL, FsAction.ALL));
      fileSystem.mkdirs(userPath);
      fileSystem.setPermission(userPath, new FsPermission(FsAction.ALL,
          FsAction.ALL, FsAction.ALL));
    }
  }

  @After
  public void tearDown() throws Exception {
    getFileSystem().close();
  }

  public static String getPathLocal(String pathRelativeToModuleRoot) {
    String pathRelativeToModuleRootSansLeadingSlashes = stripLeadingSlashes(pathRelativeToModuleRoot);
    return pathRelativeToModuleRootSansLeadingSlashes.equals("") ? PATH_LOCAL_WORKING_DIR
        .length() < 2 ? "/" : PATH_LOCAL_WORKING_DIR.substring(0,
        PATH_LOCAL_WORKING_DIR.length() - 2) : new Path(PATH_LOCAL_WORKING_DIR,
        pathRelativeToModuleRootSansLeadingSlashes).toUri().toString();
  }

  public static String getPathHDFS(String pathRelativeToHDFSRoot) {
    String pathRelativeToHDFSRootSansLeadingSlashes = stripLeadingSlashes(pathRelativeToHDFSRoot);
    return pathRelativeToHDFSRootSansLeadingSlashes.equals("") ? PATH_HDFS_LOCAL
        : new Path(PATH_HDFS_LOCAL, pathRelativeToHDFSRootSansLeadingSlashes)
            .toUri().toString();
  }

  private static String stripLeadingSlashes(String string) {
    int indexAfterLeadingSlash = 0;
    while (indexAfterLeadingSlash < string.length()
        && string.charAt(indexAfterLeadingSlash) == '/')
      ++indexAfterLeadingSlash;
    return indexAfterLeadingSlash == 0 ? string : string.substring(
        indexAfterLeadingSlash, string.length());
  }

}
