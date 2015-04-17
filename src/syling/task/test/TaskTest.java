package syling.task.test;

import android.util.Log;
import syling.task.ExTask;
import syling.task.TaskContext;
import syling.task.TaskExecutor;
import syling.task.TaskHandler;

/**
 * Created by Administrator on 2015/3/24 0024.
 * 测试模块
 */
public class TaskTest {
    private static final String Tag = "TaskTest";

    public static TaskTest instance = new TaskTest();
    private TaskExecutor mExecutor;
    private TaskTest(){
        mExecutor = new TaskExecutor(true, 5, 128, 2000, 10000);
    }

    // 测试多个任务
    public void testPools(TaskContext context, int num, TaskHandler.NotifyListener<Integer> listener){
        for (int i=0; i < num; i++){
            mExecutor.submit(new TxTask(context , i, listener));
        }
    }

    // 测试任务同步
    public void testWait(TaskHandler.NotifyListener<Integer> listener){
        mExecutor.submit(new ExTask<Integer>(listener) {
            @Override
            protected Integer work(Object... params) throws Exception {
                Integer v = (Integer) params[0];
                ExTask<Integer> asynTask  = mExecutor.submit(new ExTask<Integer>() {
                    @Override
                    protected Integer work(Object... params) throws Exception {
                        Integer v1 = (Integer) params[0];
                        Thread.sleep(v1 * 100);
                        return v1 * 3;
                    }
                }.setParams(v));
                asynTask.waitFinish();
                return asynTask.getResult();
            }
        } .setParams(5)
          .setTimeout(30000)
          .setPriority(ExTask.TaskPriority.LOCAL, true)
          .setTag(1 + ""));
    }

    // 测试任务顺序
    public void testSeq(TaskContext context, int num, TaskHandler.NotifyListener<Integer> listener){
        ExTask task = new TxTask(context, 0, listener);
        for (int i=1; i < num; i++){
            try {
                task.next(new TxTask(context, i, listener)
               .next(new TxTask(context, 2*i, listener)));
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        mExecutor.submit(task);
    }

    // 取消所有
    public void testCanAll(){
        mExecutor.cancelAll();
    }

    public void testCanPrix(String prix){
        mExecutor.cancelByPrix(prix);
    }

    public void testCanTag(String tag){
        mExecutor.cancel(tag);
    }

    public class TxTask extends ExTask<Integer> {
        public  TxTask(TaskContext context, int i, TaskHandler.NotifyListener<Integer> listener){
            super(listener);
            setContext(context)
                    .setParams(i)
                    .setTimeout(30000)
                    .setPriority(ExTask.TaskPriority.LOCAL, true)
                    .setTag(i + "");
        }

        @Override
        protected Integer work(Object... params) throws Exception {
            Integer v = (Integer) params[0];
            try {
                if (!TaskHandler.isMainThread()) {
                    Thread.sleep(v * 100);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (v % 7 == 0) {
                throw new Exception("oppo");
            }
            Log.d(Tag, "v=" + v);
            return v * 2;
        }
    }
}
