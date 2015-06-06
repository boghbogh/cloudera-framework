package com.cloudera.framework.main.test;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.MRConfig;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for all local-cluster tests, intended to be extended in public
 */
public class LocalClusterDfsMrBaseTest extends BaseTest {

  private static Logger LOG = LoggerFactory
      .getLogger(LocalClusterDfsMrBaseTest.class);

  private static JobConf conf;
  private static FileSystem fileSystem;

  @Override
  public Configuration getConf() {
    return conf;
  }

  @Override
  public FileSystem getFileSystem() {
    return fileSystem;
  }

  @Override
  public String getPathDfs(String pathRelativeToDfsRoot) {
    String pathRelativeToDfsRootSansLeadingSlashes = stripLeadingSlashes(pathRelativeToDfsRoot);
    return pathRelativeToDfsRootSansLeadingSlashes.equals("") ? PATH_DFS_LOCAL
        : new Path(PATH_DFS_LOCAL, pathRelativeToDfsRootSansLeadingSlashes)
            .toUri().toString();
  }

  @BeforeClass
  public static void setUpRuntime() throws Exception {
    long time = debugMessageHeader(LOG, "setUpRuntime");
    fileSystem = FileSystem.getLocal(conf = new JobConf());
    conf.set(MRConfig.FRAMEWORK_NAME, MRConfig.LOCAL_FRAMEWORK_NAME);
    debugMessageFooter(LOG, "setUpRuntime", time);
  }

  @AfterClass
  public static void tearDownRuntime() throws Exception {
    long time = debugMessageHeader(LOG, "tearDownRuntime");
    if (fileSystem != null) {
      fileSystem.close();
    }
    debugMessageFooter(LOG, "tearDownRuntime", time);
  }

}