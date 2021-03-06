package com.cloudera.framework.example.model.input;

import java.io.IOException;

import org.apache.hadoop.hive.serde2.avro.AvroGenericRecordWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileRecordReader;

import com.cloudera.framework.example.model.Record;
import com.cloudera.framework.example.model.RecordFactory;
import com.cloudera.framework.example.model.RecordKey;
import com.cloudera.framework.example.model.serde.RecordStringSerDe;

/**
 * An {@link InputFormat} to act on sequence files of {@link RecordKey
 * RecordKeys} and XML {@link Text Texts}, presented upstream as
 * {@link AvroGenericRecordWritable} wrapped {@link Record Records}
 */
public class RecordSequenceInputFormatXml extends SequenceFileInputFormat<RecordKey, AvroGenericRecordWritable> {

  @Override
  public RecordReader<RecordKey, AvroGenericRecordWritable> createRecordReader(InputSplit split, TaskAttemptContext context)
      throws IOException {
    return new RecordReaderSequenceXml();
  }

  public static class RecordReaderSequenceXml extends RecordTextReader {

    public RecordReaderSequenceXml() throws IOException {
      super();
    }

    @Override
    public RecordReader<RecordKey, Text> getRecordReader(InputSplit split, TaskAttemptContext context, Integer index) {
      return new SequenceFileRecordReader<>();
    }

    @Override
    public RecordStringSerDe getRecordStringSerDe() throws IOException {
      return RecordFactory.getRecordStringSerDe(RecordFactory.RECORD_STRING_SERDE_XML);
    }

  }

}
