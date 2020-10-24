# simple-pipeline-framework(任务流水线框架)
Async execute pipeline task which API is simple.


## 环境要求

运行示例之前需要先安装：

* JDK 1.8
* Maven 3.3.9

```bash
$java -version
java version "1.8.0_202"
Java(TM) SE Runtime Environment (build 1.8.0_202-b08)
Java HotSpot(TM) 64-Bit Server VM (build 25.202-b08, mixed mode)

$mvn -version
Apache Maven 3.3.9 (bb52d8502b132ec0a5a3f4c09453c07478323dc5; 2015-11-11T00:41:47+08:00)
```

## 下载编译源代码

```bash
$git clone https://github.com/wangyong-chengdu/simple-pipeline-framework.git
$cd simple-pipeline-framework
$mvn package
```


## Pipeline框架功能定义
源文件见 cd.wangyong.simple_pipeline.Pipeline
```java
public class Pipeline {
    /**
     * 构建Pipeline
     * @param businessName 流水线业务名称
     * @param startId 起始结点编号
     * @param config 流水线结点连接配置
     */
    @SuppressWarnings("unchecked")
    public static Pipeline build(String businessName, int startId, Properties config) {
        // ...    
    }

    /**
     * 任务执行相当于无环有向图的遍历（BFS）
     */
    public CompletableFuture<PipeValueObj> executeAsync() {
        // 从头结点触发执行
        return execute(header);
    }
```

Pipeline重要属性说明：

```java
    /**
     * 头结点（有向无环图头结点）
     */
    private final PipeTaskNode header;

    /**
     * 结点后继结点
     */
    private final Map<PipeTaskNode, List<PipeTaskNode>> nextNodeMap;

    /**
     * 结点前驱结点
     */
    private final Map<PipeTaskNode, List<PipeTaskNode>> preNodeMap;

    /**
     * 结点任务执行Future备忘录
     */
    private final Map<PipeTaskNode, CompletableFuture<PipeValueObj>> futureMemoMap = new ConcurrentHashMap<>();

    /**
     * 性能优化，如果已经确定前面结点已经执行失败，则后续结点任务无需执行
     */
    private volatile PipeValueObj failValueObj;

```

## 例子

可通过单元测试类了解API使用：cd.wangyong.simple_pipeline.test.PipelineTest

调用举例
- 实现任务节点接口 cd.wangyong.simple_pipeline.PipeTaskNode
```java
public abstract class AbstractStringTask implements PipeTaskNode {
    private static final Logger logger = LoggerFactory.getLogger(AbstractStringTask.class);

    @Override
    public String businessName() {
        return "string_append_pipeline";
    }

    @Override
    public PipeValueObj execute(List<PipeValueObj> inputs) {
        logger.info("Node-{}:task execute.", id());
        try {
            if (CollectionUtils.isEmpty(inputs)) {
                return PipeValueObj.success("Node-" + id() + " execute success.\n");
            }

            StringBuilder sb = new StringBuilder();
            inputs.forEach(pipeValueObj -> sb.append(pipeValueObj.getValue()));
            return PipeValueObj.success(sb.append("Node-").append(id()).append(" execute success.\n").toString());
        } catch (Throwable t) {
            return PipeValueObj.fail(String.format("Node-%d execute fail.", id()), t);
        }
    }
}

public class TaskOne extends AbstractStringTask {

    @Override
    public int id() {
        return 1;
    }
}
```

- 通过SPI机制配置任务实现：
创建配置文件，将接口和对应实现放在Classpath:resources/META-INF/services路径下

```java
cd.wangyong.simple_pipeline.test.tasks.TaskOne
cd.wangyong.simple_pipeline.test.tasks.TaskTwo
cd.wangyong.simple_pipeline.test.tasks.TaskThree
cd.wangyong.simple_pipeline.test.tasks.TaskFour
cd.wangyong.simple_pipeline.test.tasks.TaskFive
cd.wangyong.simple_pipeline.test.tasks.TaskSix
cd.wangyong.simple_pipeline.test.tasks.TaskSeven
cd.wangyong.simple_pipeline.test.tasks.TaskEight
cd.wangyong.simple_pipeline.test.tasks.TaskNine
cd.wangyong.simple_pipeline.test.tasks.TaskTen
```

- 客户端调用Pipeline接口构建Pipeline并执行任务

```java
// 异步执行流向线，阻塞获取
Properties props = getProperties();
PipeValueObj result = Pipeline.build("string_append_pipeline", 1, props)
        .executeAsync()
        .get();
logger.info("Pipeline execute result:\n{}", result.getValue());
```
