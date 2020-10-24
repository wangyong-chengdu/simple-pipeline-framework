package cd.wangyong.simple_pipeline.test.tasks;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import cd.wangyong.simple_pipeline.PipeTaskNode;
import cd.wangyong.simple_pipeline.PipeValueObj;

/**
 * @author andy
 * @since 2020/10/23
 */
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
