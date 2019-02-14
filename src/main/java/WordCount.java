import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.stream.StreamSupport;

import org.apache.commons.io.FileUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

@SuppressWarnings("unused")
public class WordCount {
	static final String DELIM = "*-*-*";
	
	public static class TokenizerMapper extends Mapper<Object, Text, Text, IntWritable> {
		private final static IntWritable one = new IntWritable(1);
		
		public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
			String filename = ((FileSplit)context.getInputSplit()).getPath().getName();
			context.write(new Text(DELIM+filename), one);
			
			StringTokenizer itr = new StringTokenizer(value.toString());
			while (itr.hasMoreTokens())
				context.write(new Text(itr.nextToken()), one);
		}
	}
	
	public static class IntSumReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
		private IntWritable result = new IntWritable();
		private MultipleOutputs<Text, IntWritable> mos;
		
		public void setup(Context context) { mos = new MultipleOutputs<Text, IntWritable>(context); }
		
		public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
			if(key.toString().startsWith(DELIM)) {
				int mapcount = StreamSupport.stream(values.spliterator(), false).mapToInt(x->x.get()).sum();
				mos.write("mapCallCount", key.toString().substring(5), new IntWritable(mapcount));
			}
			
			else {
				int sum = 0;
				for (IntWritable val : values)
					sum += Integer.parseInt(val.toString());
				result.set(sum);

				context.write(key, result);
			}
		}
		
		public void cleanup(Context context) throws IOException, InterruptedException { mos.close(); }
	}
	
	public static void main(String[] args) throws Exception {
		Job job = init(args);
		
		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));
		MultipleOutputs.addNamedOutput(job, "mapCallCount", TextOutputFormat.class, Text.class, IntWritable.class);
		
		System.exit(job.waitForCompletion(true) ? 0 : 1);
	}
	
	static Job init(String[] args) throws IOException {
		Configuration conf = new Configuration();
		String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
		if (otherArgs.length != 2) {
			System.err.println("Usage: wordcount <in> <out>");
			System.exit(2);
		}
		FileUtils.deleteDirectory(new File(otherArgs[1]));
		Job job = Job.getInstance(conf, "word count");
		job.setJarByClass(WordCount.class);
		job.setMapperClass(TokenizerMapper.class);
		job.setCombinerClass(IntSumReducer.class);
		job.setReducerClass(IntSumReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(IntWritable.class);
		return job;
	}
}

class BigramWritable implements WritableComparable<BigramWritable> {
	Text a, b;
	BigramWritable() { this.a = null; this.b = null; }
	BigramWritable(String a, String b) { this.a = new Text(a); this.b = new Text(b); }
	
	public void write(DataOutput out) throws IOException {
	}

	public void readFields(DataInput in) throws IOException {		
	}
	public int compareTo(BigramWritable o) {
		return 0;
	}
}