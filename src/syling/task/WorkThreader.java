package syling.task;

import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by Administrator on 2015/3/22 0022.
 * 工作线程
 */
class WorkThreader extends Thread{
    public static volatile int COUNT = 0;
    // 开始执行时间，外部只读
    private volatile long mStartTime;
    // 是否是核心线程
    private boolean isCore = true;
    // 是否是守护线程
    private boolean _isDaemon = false;

    private boolean isStop = false;
    private final Object mLock = new Object();

    // 顺序子任务
    private Queue<ExTask> mSeqQueue;
    private TaskWorkPool mPools;
    private ExTask mTask;
    private int mParentKey;
    String mTag;

    WorkThreader(TaskWorkPool pool, int mode){
        mPools = pool;
        isCore = mode == 0;
        _isDaemon = mode < 0;
        mTag = (_isDaemon ? "TaskDaemon": "ZH-Thread-")+ COUNT++;
        setName(mTag);
        setDaemon(_isDaemon);
        start();
    }

    @Override
    public void run() {
        Log.d(mTag, "->start<-");
        int s_time = 500;
        while (!_isStop()){
            if(_isDaemon){
                try {
                    Thread.sleep(s_time);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // 是否有待执行任务
                if(mPools.haveTask()){
                    if(mPools.check() < 0){
                        // 满负荷
                        s_time = 3000;
                    }else{
                        // 有闲置
                        s_time = 200;
                    }
                    mPools.notifyTake();
                }else
                // 无待执行任务
                if(0 == mPools.check()){
                    // 都闲置，等待唤醒
                    _wait(0);
                    mPools.notifyTake();
                    s_time = 200;
                }else{
                    // 部分闲置
                    s_time = 3000;
                }
            }else {
                mTask = mPools.takeTask();
                if (mTask == null) {
                    mStartTime = 0l;
                    if (isCore) {
                        // 一直等待
                        _wait(0);
                    } else {
                        // 等待休息时间
                        _stop();
                        _wait(mPools.mIdleTime);
                    }
                } else {
                    mParentKey = mTask.getKey();
                    doTask();
                    mPools.mvTask(mParentKey);
                }
            }
        }
        mPools.mvWork(this);
        Log.d(mTag, "->end<-");
    }

    private void doTask(){
        mStartTime = System.currentTimeMillis();
        setPriority(Thread.MAX_PRIORITY - mTask.mPriority.ordinal());
        // 添加子序列
        ArrayList<ExTask> tasks = mTask.getSubTask();
        if (tasks != null && tasks.size() > 0) {
            if (mSeqQueue == null) {
                mSeqQueue = new LinkedList<ExTask>();
            }
            for (ExTask obj : tasks) {
                mSeqQueue.offer(obj);
            }
        }
        // 执行任务
        if (mTask.mPriority == ExTask.TaskPriority.UI) {
            TaskHandler.submit(mTask);
        } else {
            mTask.doStart();
            mTask.doWork();
        }
        // 执行子序列任务
        if(mSeqQueue != null && !mSeqQueue.isEmpty()){
            mTask = mSeqQueue.poll();
            // 递归执行子序列
            doTask();
        }
    }

    private void _wait(int timeout){
        synchronized (mLock){
            if(timeout > 0) {
                try {
                    mLock.wait(timeout);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }else{
                try {
                    mLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    void _notify(){
        synchronized (mLock){
            if(isStop) {
                isStop = false;
            }
            mLock.notify();
        }
    }

    boolean _isStop(){
        synchronized (mLock){
            return isStop;
        }
    }

    void _stop(){
        synchronized (mLock){
            isStop = true;
        }
    }

    boolean _isTimeout(){
        if(mTask != null && mTask.mPriority == ExTask.TaskPriority.L_NETWORK){
            // 大文件传输，不超时
            return false;
        }
        if(isWorking()){
            return System.currentTimeMillis() - mStartTime > (mTask.timeout > 0 ? mTask.timeout : mPools.mTimeOut);
        }else{
            return false;
        }
    }

    void cancel(){
        if(mTask != null){
            mTask.cancel();
        }
        _stop();
    }

    boolean isWorking(){
        synchronized (mLock) {
            return mStartTime > 0;
        }
    }
}
