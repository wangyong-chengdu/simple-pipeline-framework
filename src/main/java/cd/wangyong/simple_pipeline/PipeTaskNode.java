package cd.wangyong.simple_pipeline;

import java.util.List;

/**
 * 流水线任务结点
 * @author andy
 * @since 2020/10/23
 */
public interface PipeTaskNode {
    /**
     * 流水线结点归属的业务名称
     */
    String businessName();

    /**
     * 流水线结点编号
     */
    int id();

    /**
     * 流水线结点任务执行
     * @param inputs 输入
     * @return 输出
     */
    PipeValueObj execute(List<PipeValueObj> inputs);
}
