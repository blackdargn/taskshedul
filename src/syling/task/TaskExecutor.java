package syling.task;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Administrator on 2015/3/21 0021.
 * 任务执行调度者, 管理任务的调度工作
 */
@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class TaskExecutor {
    final String Tag = "TaskExecutor";
    /** 任务队列,自己按照顺序与优先级添加到线程中*/
    private final ConcurrentHashMap< ExTask.TaskPriority, ArrayDeque<ExTask>> mTaskChannels
            = new ConcurrentHashMap<ExTask.TaskPriority, ArrayDeque<ExTask>>();
    /** 待执行任务集合  */
    private final ConcurrentHashMap<Integer, ExTask> mPendingTasks = new ConcurrentHashMap<Integer, ExTask>();
    /**正在执行任务集合 */
    private final ConcurrentHashMap<Integer, ExTask> mRunningTasks = new ConcurrentHashMap<Integer, ExTask>();
    /** 工作池*/
    private TaskWorkPool mWorkPools;

    /**
     * 任务调度者
     * @param isFixCore 是否固定核心线程
     * @param coreSize 核心线程的数量
     * @param maxNum  最大线程数，当isFixCore为false时有效
     * @param idleTime 线程空闲时间ms，当isFixCore为false时有效
     * @param timeOut 线程执行超时时间ms，默认超时时间，可单独定制task的超时
     */
    public TaskExecutor(boolean isFixCore, int coreSize, int maxNum, int idleTime, int timeOut){
        mWorkPools = new TaskWorkPool(this, isFixCore, coreSize, maxNum, idleTime, timeOut);
    }

    /** 获取对应的任务队列*/
    ArrayDeque<ExTask> getTaskQueue(ExTask.TaskPriority priority){
        if(mTaskChannels.containsKey(priority)){
            return mTaskChannels.get(priority);
        }
        ArrayDeque<ExTask> queue = new ArrayDeque<ExTask>();
        mTaskChannels.put(priority, queue);
        return queue;
    }

    /** 取消所有的任务*/
    public void cancelAll(){
        Collection<ExTask> values = mPendingTasks.values();
        if(values != null){
            for(ExTask task : values){
                task.cancel();
            }
        }
        values = mRunningTasks.values();
        if(values != null){
            for(ExTask task : values){
                task.cancel();
            }
        }
        Collection<ArrayDeque<ExTask>> values2 = mTaskChannels.values();
        if(values2 != null){
            for(ArrayDeque<ExTask> tasks : values2){
                synchronized (tasks) {
                    tasks.clear();
                }
            }
        }
        mPendingTasks.clear();
        mRunningTasks.clear();
        mTaskChannels.clear();
    }

    /** 取消指定任务，与tag对应*/
    public void cancel(String tag){
        if(tag == null || !mPendingTasks.containsKey(tag.hashCode())){
            return;
        }
        ExTask task = mPendingTasks.get(tag.hashCode());
        task.cancel();

        if(tag == null || !mRunningTasks.containsKey(tag.hashCode())){
            return;
        }
        task = mRunningTasks.get(tag.hashCode());
        task.cancel();
        return;
    }

    /** 取消带前缀的指定任务，prixXXX*/
    public void cancelByPrix(String prix){
        if(prix == null) return;
        Collection<ExTask> values = mPendingTasks.values();
        if(values != null) {
            for (ExTask task : values) {
                if(task.mTag != null && task.mTag.startsWith(prix)){
                    task.cancel();
                }
            }
        }
        values = mRunningTasks.values();
        if(values != null) {
            for (ExTask task : values) {
                if(task.mTag != null && task.mTag.startsWith(prix)){
                    task.cancel();
                }
            }
        }
    }

    /** 提交任务，相同key且未执行 和 正在执行 的任务提交失败*/
    public ExTask submit(ExTask task){
        if(task == null || mPendingTasks.containsKey(task.getKey())
           || mRunningTasks.containsKey(task.getKey())) {
            return task;
        }
        mPendingTasks.put(task.getKey(), task);
        ArrayDeque<ExTask> queue = getTaskQueue(task.mPriority);
        synchronized (queue){
            if(task.isFirst){
                queue.addFirst(task);
            }else{
                queue.addLast(task);
            }
            Log.d(Tag, "submit size = " + queue.size());
        }
        mWorkPools.notifyWork();
        return task;
    }

    /** 按优先级提取任务*/
    ExTask popTask(){
        ExTask task = popTask(ExTask.TaskPriority.UI);
        if(task != null) return task;
        task = popTask(ExTask.TaskPriority.LOCAL);
        if(task != null) return task;
        task = popTask(ExTask.TaskPriority.S_NETWORK);
        if(task != null) return task;
        task = popTask(ExTask.TaskPriority.UNKNOWN);
        if(task != null) return task;
        task = popTask(ExTask.TaskPriority.L_NETWORK);
        if(task != null) return task;
        return task;
    }

    /** 是否有待执行的任务*/
    boolean havePendingTask(){
        return mPendingTasks.size() > 0;
    }

    /** 提取类型任务*/
    private ExTask popTask(ExTask.TaskPriority priority){
        ArrayDeque<ExTask> queue = null;
        if(mTaskChannels.containsKey(priority)){
            queue = mTaskChannels.get(priority);
        }
        if(queue != null){
            synchronized (queue) {
                ExTask task;
                while(!queue.isEmpty()) {
                    if(queue.peek().isCanceled()){
                        // 已取消
                        mPendingTasks.remove(queue.poll().getKey());
                    }else {
                        // 未取消
                        task = queue.poll();
                        mPendingTasks.remove(task.getKey());
                        // 执行完成后，再去除
                        mRunningTasks.put(task.getKey(), task);
                        return task;
                    }
                }
            }
        }
        return null;
    }

    /** 执行完成后移除 */
    void mvRunningTask(int key){
        if(mRunningTasks.containsKey(key)) {
            mRunningTasks.remove(key);
        }
    }
}
