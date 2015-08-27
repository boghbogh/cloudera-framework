package com.cloudera.example.process;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.avro.mapred.AvroKey;
import org.apache.avro.mapreduce.AvroJob;
import org.apache.avro.mapreduce.AvroKeyInputFormat;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputCommitter;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.example.Constants;
import com.cloudera.example.model.Record;
import com.cloudera.example.model.RecordCounter;
import com.cloudera.example.model.RecordPartition;
import com.cloudera.framework.main.common.Driver;
import com.cloudera.framework.main.common.util.DfsUtil;

import parquet.avro.AvroParquetOutputFormat;
import parquet.hadoop.ParquetOutputFormat;
import parquet.hadoop.metadata.CompressionCodecName;

/**
 * Process driver, take a set of partitioned Avro files and rewrite them into
 * consolidated, schema partitioned, column order Parquet. The driver can be
 * configured as a pass-through, de-depulication and most-recent filter.
 * Malformed files are annexed off and written in the staging Avro file format.
 */
public class Process extends Driver {

  public static final RecordCounter[] COUNTERS = new RecordCounter[] { RecordCounter.RECORDS,
      RecordCounter.RECORDS_CANONICAL, RecordCounter.RECORDS_DUPLICATE, RecordCounter.RECORDS_MALFORMED };

  private static final Logger LOG = LoggerFactory.getLogger(Process.class);

  private Path inputPath;
  private Path outputPath;
  private Set<Path> inputPaths;

  private FileSystem hdfs;

  public Process() {
    super();
  }

  public Process(Configuration confguration) {
    super(confguration);
  }

  @Override
  public String description() {
    return "Process my dataset";
  }

  @Override
  public String[] options() {
    return new String[] {};
  }

  @Override
  public String[] parameters() {
    return new String[] { "input-path", "output-path" };
  }

  @Override
  public void reset() {
    super.reset();
    for (RecordCounter counter : COUNTERS) {
      incrementCounter(Process.class.getCanonicalName(), counter, 0);
    }
  }

  @Override
  public int prepare(String... arguments) throws Exception {
    if (arguments == null || arguments.length != 2) {
      throw new Exception("Invalid number of arguments");
    }
    hdfs = FileSystem.newInstance(getConf());
    inputPath = hdfs.makeQualified(new Path(arguments[0]));
    if (LOG.isInfoEnabled()) {
      LOG.info("Input path [" + inputPath + "] validated");
    }
    inputPaths = DfsUtil.listDirs(hdfs, inputPath, true, true);
    outputPath = hdfs.makeQualified(new Path(arguments[1]));
    hdfs.mkdirs(outputPath.getParent());
    if (LOG.isInfoEnabled()) {
      LOG.info("Output path [" + outputPath + "] validated");
    }
    return RETURN_SUCCESS;
  }

  @Override
  public int execute() throws Exception {
    boolean jobSuccess = true;
    List<Job> jobs = new ArrayList<Job>();
    for (Path inputPath : inputPaths) {
      String outputPathString = inputPath.getName();
      Path inputPathParent = inputPath;
      for (int i = 1; i < RecordPartition.RECORD_COL_YEAR_MONTH.length; i++) {
        outputPathString = (inputPathParent = inputPathParent.getParent()).getName() + Path.SEPARATOR_CHAR
            + outputPathString;
      }
      Job job = Job.getInstance(getConf());
      job.setJobName(getClass().getSimpleName());
      job.setJarByClass(Process.class);
      FileInputFormat.addInputPath(job, inputPath);
      job.setInputFormatClass(AvroKeyInputFormat.class);
      job.setOutputFormatClass(AvroParquetOutputFormat.class);
      FileOutputFormat.setOutputPath(job, new Path(this.outputPath,
          Constants.DIR_DS_MYDATASET_PROCESSED_CANONICAL_PARQUET_RELATIVE + Path.SEPARATOR_CHAR + outputPathString));
      AvroJob.setInputKeySchema(job, Record.getClassSchema());
      AvroParquetOutputFormat.setSchema(job, Record.getClassSchema());
      ParquetOutputFormat.setCompression(job, CompressionCodecName.SNAPPY);
      FileOutputFormat.setCompressOutput(job, true);
      ParquetOutputFormat.setBlockSize(job, 500 * 1024 * 1024);
      job.setMapperClass(Mapper.class);
      job.setNumReduceTasks(0);
      job.submit();
      jobs.add(job);
    }
    for (Job job : jobs) {
      jobSuccess = jobSuccess && job.waitForCompletion(LOG.isInfoEnabled());
      importCounters(job, COUNTERS);
    }
    if (jobSuccess) {
      for (Path path : inputPaths) {
        hdfs.createNewFile(new Path(path, FileOutputCommitter.SUCCEEDED_FILE_NAME));
      }
    }
    return jobSuccess ? RETURN_SUCCESS : RETURN_FAILURE_RUNTIME;
  }

  /**
   * Mapper.<br>
   * <br>
   * Note this class is not thread-safe but is jvm-reuse-safe, reusing objects
   * where possible.
   */
  private static class Mapper extends org.apache.hadoop.mapreduce.Mapper<AvroKey<Record>, NullWritable, Void, Record> {

    @Override
    protected void map(AvroKey<Record> key, NullWritable value,
        org.apache.hadoop.mapreduce.Mapper<AvroKey<Record>, NullWritable, Void, Record>.Context context)
            throws IOException, InterruptedException {
      context.getCounter(RecordCounter.RECORDS).increment(1);
      context.getCounter(RecordCounter.RECORDS_CANONICAL).increment(1);
      context.write(null, key.datum());
    }

  }

  public static void main(String... arguments) throws Exception {
    System.exit(new Process().runner(arguments));
  }

}
