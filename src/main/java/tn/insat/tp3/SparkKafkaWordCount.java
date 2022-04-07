package tn.insat.tp3;

import org.apache.spark.SparkConf;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.api.java.*;
import org.apache.spark.streaming.kafka.KafkaUtils;

import scala.Tuple2;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class SparkKafkaWordCount {
    private static final Pattern SPACE = Pattern.compile(" ");

    private SparkKafkaWordCount() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.err.println("Usage: SparkKafkaWordCount <zkQuorum> <group> <topics> <numThreads>");
            System.exit(1);
        }

        SparkConf sparkConf = new SparkConf().setAppName("SparkKafkaWordCount");
        // Creer le contexte avec une taille de batch de 2 secondes
        JavaStreamingContext jssc = new JavaStreamingContext(sparkConf,
            new Duration(2000));

        int numThreads = Integer.parseInt(args[3]);
        Map<String, Integer> topicMap = new HashMap<String, Integer>();
        String[] topics = args[2].split(",");
        for (String topic: topics) {
            topicMap.put(topic, numThreads);
        }

        JavaPairReceiverInputDStream<String, String> messages =
                KafkaUtils.createStream(jssc, args[0], args[1], topicMap);

        JavaDStream<String> lines = messages.map(Tuple2::_2);

        JavaDStream<String> words =
                lines.flatMap(x -> Arrays.asList(SPACE.split(x)).iterator());

        JavaPairDStream<String, Integer> wordCounts =
                words.mapToPair(s -> new Tuple2<>(s, 1))
                     .reduceByKey((i1, i2) -> i1 + i2);

        wordCounts.print();
        jssc.start();
        jssc.awaitTermination();
    }
}