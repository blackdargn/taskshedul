package syling.task;

/**
 * Created by Administrator on 2015/3/21 0021.
 * 任务上下文
 */
public interface TaskContext {

    /** 是否在运行中...,是则执行该任务，否则不执行放弃 */
    public boolean isRunning();

}
