package time.processing;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.configuration.ConfigConstants;
import org.apache.flink.configuration.ConfigOptions;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.windowing.AllWindowFunction;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author : viktor
 * Date  : 2019/11/12 10:45 AM
 * 功能  :
 */
public class ProcessingTimeWindow {

    public static void main(String[] args) throws Exception{


        /**
         * 设置env参数
         * local模式启用web UI
         */
        Configuration config = new Configuration();
        config.setInteger(ConfigOptions.key("rest.port").defaultValue(8081),8082);
        config.setBoolean(ConfigConstants.LOCAL_START_WEBSERVER, true);


        /**
         * 获取flink stream运行环境
         */
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(config);


        DataStream<String> source = env.socketTextStream("localhost",9999);


        DataStream<Tuple2<String,Long>> flat = source
                .flatMap(new FlatMapFunction<String, Tuple2<String, Long>>() {

                    @Override
                    public void flatMap(String value, Collector<Tuple2<String, Long>> out) throws Exception {
                        String [] values = value.split(",");
                        for (String s:values) {
                            out.collect(new Tuple2<>(s , 1L));
                        }
                    }
                });


        flat
                .timeWindowAll(Time.minutes(3L))
                .apply(new AllWindowFunction<Tuple2<String,Long>, Tuple3<String,String,Long>, TimeWindow>() {
                    @Override
                    public void apply(TimeWindow window, Iterable<Tuple2<String, Long>> values, Collector<Tuple3<String,String,Long>> out) throws Exception {
                        Long sum = 0L;
                        for (Tuple2<String,Long> value:values) {
                            sum +=value.f1;
                        }

                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        Date start = new Date(window.getStart());
                        Date end = new Date(window.getEnd());


                        out.collect(new Tuple3<>("窗口开始时间："+sdf.format(start),"窗口结束时间："+sdf.format(end),sum));
                    }
                })
                .print();



        env.execute("processing time test");
    }
}