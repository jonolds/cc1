import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.apache.commons.io.FileUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
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
//		private final static Text one = new Text("1");
		private final static IntWritable one = new IntWritable(1);
		private Text word = new Text();
		
		public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
			StringTokenizer itr = new StringTokenizer(value.toString());
			String filename = ((FileSplit)context.getInputSplit()).getPath().getName();
			
			context.write(new Text(DELIM+filename), new IntWritable(key.hashCode()));
			while (itr.hasMoreTokens()) {
				word.set(itr.nextToken());
				
				context.write(word, one);
			}
		}
	}
	
	public static class IntSumReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
		private IntWritable result = new IntWritable();
		private MultipleOutputs<Text, IntWritable> mos;
		
		public void setup(Context context) {
			mos = new MultipleOutputs<Text, IntWritable>(context);
		}
		
		public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
			String keystring = key.toString();
			if(keystring.startsWith(DELIM)) {
				HashMap<IntWritable, Integer> hashmap = new HashMap<>();
				for (IntWritable val : values)
					hashmap.put(val, 1);
				System.out.println(hashmap.size());
				
//				mos.write(keystring.substring(5), "test1", new IntWritable(hashmap.size()));

				mos.write("mapCallCount", keystring.substring(5), new IntWritable(hashmap.size()));
			}
			
			else {
				int sum = 0;
				for (IntWritable val : values)
					sum += Integer.parseInt(val.toString());
				result.set(sum);
//				mos.write("mapcnt", key, result);

				context.write(key, result);
			}
		}
		
		public void cleanup(Context context) throws IOException, InterruptedException { mos.close(); }
	}
	
	public static void main(String[] args) throws Exception {
		Configuration conf = init(args);
		
		Job job = Job.getInstance(conf, "word count");
		job.setJarByClass(WordCount.class);
		job.setMapperClass(TokenizerMapper.class);
		job.setCombinerClass(IntSumReducer.class);
		job.setReducerClass(IntSumReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(IntWritable.class);
		
		
		
		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));
		
		MultipleOutputs.addNamedOutput(job, "mapCallCount", TextOutputFormat.class, Text.class, IntWritable.class);
//		MultipleOutputs.addNamedOutput(job, args[0], TextOutputFormat.class, Text.class, IntWritable.class);
		
		System.exit(job.waitForCompletion(true) ? 0 : 1);
	}
	
	static Configuration init(String[] args) throws IOException {
		Configuration conf = new Configuration();
		String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
		if (otherArgs.length != 2) {
			System.err.println("Usage: wordcount <in> <out>");
			System.exit(2);
		}
		FileUtils.deleteDirectory(new File(otherArgs[1]));
		return conf;
	}
}