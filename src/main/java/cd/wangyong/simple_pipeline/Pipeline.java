package cd.wangyong.simple_pipeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * 流水线框架
 * @author andy
 * @since 2020/10/23
 */
public class Pipeline {
    private static final Map<String, List<PipeTaskNode>> ALL_PIPELINE_CONTEXT = StreamSupport
            .stream(ServiceLoader.load(PipeTaskNode.class).spliterator(), false)
            .collect(Collectors.groupingBy(PipeTaskNode::businessName));

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

    private Pipeline(PipeTaskNode header, Map<PipeTaskNode, List<PipeTaskNode>> nextNodeMap, Map<PipeTaskNode, List<PipeTaskNode>> preNodeMap) {
        this.header = header;
        this.nextNodeMap = nextNodeMap;
        this.preNodeMap = preNodeMap;
    }

    /**
     * 构建Pipeline
     * @param businessName 流水线业务名称
     * @param startId 起始结点编号
     * @param config 流水线结点连接配置
     */
    @SuppressWarnings("unchecked")
    public static Pipeline build(String businessName, int startId, Properties config) {
        Assert.notNull(businessName, "Business name is null.");
        Assert.notNull(config, "Pipeline config is null.");

        List<PipeTaskNode> pipeTaskNodes = ALL_PIPELINE_CONTEXT.get(businessName);
        Assert.notEmpty(pipeTaskNodes, String.format("Pipeline not exist, businessName:%s.", businessName));

        Map<Integer, PipeTaskNode> nodeMap = pipeTaskNodes.stream().collect(Collectors.toMap(PipeTaskNode::id, Function.identity()));

        Map<PipeTaskNode, List<PipeTaskNode>> nextNodeMap = new HashMap<>();
        Map<PipeTaskNode, List<PipeTaskNode>> preNodeMap = new HashMap<>();

        Queue<Integer> queue = new LinkedList<>();
        queue.add(startId);

        // build DiGraph
        while (!queue.isEmpty()) {
            Integer id = queue.poll();
            List<Integer> nextIds = (List<Integer>) config.get(id);

            Assert.isTrue(nodeMap.containsKey(id), String.format("Pipeline config error, not exist node id:%d.", id));
            List<Integer> notExistNodeIds = nextIds.stream().filter(nextId -> !nodeMap.containsKey(nextId)).collect(Collectors.toList());
            Assert.isTrue(CollectionUtils.isEmpty(notExistNodeIds), String.format("Pipeline config error, not exist node ids:%s.", notExistNodeIds));

            if (!CollectionUtils.isEmpty(nextIds)) {
                PipeTaskNode node = nodeMap.get(id);
                nextNodeMap.putIfAbsent(node, nextIds.stream().map(nodeMap::get).collect(Collectors.toList()));
                nextIds.forEach(nextId -> {
                    preNodeMap.computeIfAbsent(nodeMap.get(nextId), k -> new ArrayList<>()).add(node);
                    queue.offer(nextId);
                });
            }
        }
        return new Pipeline(nodeMap.get(startId), nextNodeMap, preNodeMap);
    }

    /**
     * 任务执行相当于无环有向图的遍历（BFS）
     */
    public CompletableFuture<PipeValueObj> executeAsync() {
        // 从头结点触发执行
        return execute(header);
    }

    private CompletableFuture<PipeValueObj> execute(PipeTaskNode node) {
        // 1.发现已经有任务执行失败，则不再执行，Pipeline执行失败，返回
        if (failValueObj != null) {
            return CompletableFuture.supplyAsync(() -> failValueObj);
        }

        // 2.尝试从备忘录中获取结点Future，Future不存在才去构建Future
        CompletableFuture<PipeValueObj> future = futureMemoMap.get(node);
        if (future == null) {
            future = buildAndSetMemoFuture(node);
        }

        // 3.再次判断一下，如果已经有结点任务执行失败，则不用触发后继结点，返回
        if (failValueObj != null) {
            return CompletableFuture.supplyAsync(() -> failValueObj);
        }

        // 4.触发结点后继结点任务执行
        List<PipeTaskNode> nextNodes = nextNodeMap.get(node);
        if (CollectionUtils.isEmpty(nextNodes)) {
            return future;
        }
        for (PipeTaskNode nextNode : nextNodes) {
            future = this.execute(nextNode);
        }
        return future;
    }

    private CompletableFuture<PipeValueObj> buildAndSetMemoFuture(PipeTaskNode node) {
        CompletableFuture<PipeValueObj> future = futureMemoMap.get(node);
        // 防止并发创建任务结点Future导致执行，若不存在此类并发，则为偏向锁，不影响性能
        synchronized (node) {
            if (futureMemoMap.get(node) == null) {
                // 获取前驱结点Future
                List<PipeTaskNode> preNodes = preNodeMap.get(node);
                if (CollectionUtils.isEmpty(preNodes)) {
                    future = buildExecuteFuture(node, null);
                }
                else {
                    int size = preNodes.size();
                    if (size == 1) {
                        future = futureMemoMap.get(preNodes.get(0)).thenComposeAsync(pipeValueObj -> buildExecuteFuture(node, Collections.singletonList(pipeValueObj))); // 节点的前驱Future是一定存在的
                    }
                    else {
                        List<PipeValueObj> inputParams = new ArrayList<>(size);
                        for (PipeTaskNode preNode : preNodes) {
                            try {
                                inputParams.add(futureMemoMap.get(preNode).get());
                            } catch (Throwable t) {
                                return CompletableFuture.supplyAsync(() -> PipeValueObj.fail(t));
                            }
                        }
                        future = buildExecuteFuture(node, inputParams);
                    }
                }
                futureMemoMap.putIfAbsent(node, future);
            }
        }
        return future;
    }

    private CompletableFuture<PipeValueObj> buildExecuteFuture(PipeTaskNode pipeTaskNode, List<PipeValueObj> inputParams) {
        // 流水线前驱结点任务任何一环处理失败，则流水线执行失败
        if (!CollectionUtils.isEmpty(inputParams)) {
            List<PipeValueObj> failedObjList = inputParams.stream().filter(pipeValueObj -> pipeValueObj.getThrowable() != null).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(failedObjList)) {
                PipeValueObj failObj = PipeValueObj.fail(failedObjList.get(0).getThrowable());
                this.failValueObj = failObj;
                return CompletableFuture.supplyAsync(() -> failObj); // 返回第一个执行失败的节点。
            }
        }
        return CompletableFuture.supplyAsync(() -> pipeTaskNode.execute(inputParams));
    }
}
