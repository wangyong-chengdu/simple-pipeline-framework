package cd.wangyong.simple_pipeline.test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cd.wangyong.simple_pipeline.PipeValueObj;
import cd.wangyong.simple_pipeline.Pipeline;

/**
 * @author andy
 * @since 2020/10/24
 */
@RunWith(JUnit4.class)
public class PipelineTest {
    private static final Logger logger = LoggerFactory.getLogger(PipelineTest.class);

    @Test(expected = IllegalArgumentException.class)
    public void testNotExistPipeline() throws ExecutionException, InterruptedException {
        // 异步执行流向线，阻塞获取
        Properties props = getProperties();
        PipeValueObj result = Pipeline.build("test_pipeline", 1, props)
                .executeAsync()
                .get();
        logger.info("Pipeline execute result:{}", result.getValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConfigPipelineError() throws ExecutionException, InterruptedException {
        // 异步执行流向线，阻塞获取
        Properties props = new Properties();
        props.put(1, Collections.singletonList(99));
        props.put(99, Collections.emptyList());

        PipeValueObj result = Pipeline.build("string_append_pipeline", 1, props)
                .executeAsync()
                .get();
        logger.info("Pipeline execute result:{}", result.getValue());
    }

    @Test()
    public void testOneNodePipeline() throws ExecutionException, InterruptedException {
        // 异步执行流向线，阻塞获取
        Properties props = new Properties();
        props.put(1, Collections.emptyList());

        PipeValueObj result = Pipeline.build("string_append_pipeline", 1, props)
                .executeAsync()
                .get();
        logger.info("Pipeline execute result:\n{}", result.getValue());
    }

    @Test()
    public void testTwoNodePipeline() throws ExecutionException, InterruptedException {
        // 异步执行流向线，阻塞获取
        Properties props = new Properties();
        props.put(1, Collections.singletonList(2));
        props.put(2, Collections.emptyList());

        PipeValueObj result = Pipeline.build("string_append_pipeline", 1, props)
                .executeAsync()
                .get();
        logger.info("Pipeline execute result:\n{}", result.getValue());
    }

    @Test()
    public void testTenNodePipeline() throws ExecutionException, InterruptedException {
        // 异步执行流向线，阻塞获取
        Properties props = getProperties();
        PipeValueObj result = Pipeline.build("string_append_pipeline", 1, props)
                .executeAsync()
                .get();
        logger.info("Pipeline execute result:\n{}", result.getValue());
    }

    private Properties getProperties() {
        // read pipeline config
        Properties props = new Properties();
        props.put(1, Arrays.asList(2, 4));
        props.put(2, Collections.singletonList(3));
        props.put(3, Collections.singletonList(6));
        props.put(4, Collections.singletonList(5));
        props.put(5, Collections.singletonList(6));

        props.put(6, Arrays.asList(7, 9));
        props.put(7, Collections.singletonList(8));
        props.put(8, Collections.singletonList(10));
        props.put(9, Collections.singletonList(10));
        props.put(10, Collections.emptyList());
        return props;
    }
}
