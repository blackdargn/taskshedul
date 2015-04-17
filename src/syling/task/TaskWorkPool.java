package syling.task;

import android.util.Log;
import android.view.inputmethod.ExtractedText;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Administrator on 2015/3/21 0021.
 * 任务工作池，管理工作线程
 */
class TaskWorkPool {

    /** 空闲时间*/
    int mIdleTime = 2000;
    /** 最大工作数量*/
    private int mMaxNum  = 128;
    /** 保留核心数量*/
    private int mCoreNum = 5;
    /** 超时时间*/
    int mTimeOut = 30000;
    /** 是否是固定核心线程*/
    private boolean isFixCore = true;

    private final LinkedList<WorkThreader> mPools = new LinkedList<WorkThreader>();
    private final ReentrantLock mLock = new ReentrantLock();
    private TaskExecutor mTaskExecutor;
    private WorkThreader mDemanWorker;

    TaskWorkPool(TaskExecutor executor, boolean isFixCore, int coreSize, int maxNum, int idleTime, int timeOut){
        this.mCoreNum = coreSize;
        this.isFixCore = isFixCore;
        this.mMaxNum = maxNum;
        this.mIdleTime = idleTime;
        this.mTimeOut = timeOut;
        this.mTaskExecutor = executor;
        mDemanWorker = new WorkThreader(this, -1);
    }

    ExTask takeTask(){
       return mTaskExecutor.popTask();
    }

    boolean haveTask(){
        return mTaskExecutor.havePendingTask();
    }

    void mvTask(int key){
        mTaskExecutor.mvRunningTask(key);
    }

    void notifyWork(){
        mDemanWorker._notify();
    }

    // 通知开始工作
    void notifyTake() {
        mLock.lock();
        try{
            int size = mPools.size();
            Log.d("pool", "size = " + size);
            // 从已有线程中唤醒
            for(WorkThreader worker : mPools){
                if(!worker.isWorking()){
                    worker._notify();
                    // 通知一次，只唤醒一个工作线程
                    Log.d("pool", "notify work ");
                    return;
                }
            }
            // 先初始化核心线程
            if(size < mCoreNum){
                mPools.addFirst(new WorkThreader(this, 0));
                Log.d("pool", "new core work ");
                return;
            }
            // 新增其它工作线程
            if( !isFixCore && size < mMaxNum){
                mPools.addFirst(new WorkThreader(this, size < mCoreNum ? 0 : 1));
                Log.d("pool", "new option work ");
                return;
            }
            // 大于最大工作线程，则等待...
        }finally {
            mLock.unlock();
        }
    }

    /** 检查超时与是否有运行线程，
     *  <0, 满固定核心的人在工作
     *  =0, 都停止工作
     *  >0, 有人在工作*/
    public int check(){
        mLock.lock();
        try{
            List<WorkThreader> list = new ArrayList<WorkThreader>();
            boolean isWorking = false;
            for(WorkThreader worker : mPools){
                if(worker._isTimeout()){
                    Log.d("pool", "timeout = " + worker.mTag);
                    worker.cancel();
                    list.add(worker);
                    isWorking = true;
                }else if(worker.isWorking()){
                    isWorking = true;
                }
            }
            for(WorkThreader worker : list){
                mPools.remove(worker);
            }
            if(isFixCore && mPools.size() == mCoreNum){
                return isWorking ? -1 : 0;
            }else{
                return isWorking ? 1 : 0;
            }
        }finally {
            mLock.unlock();
        }
    }

    // 移除消亡的工作线程
    public void mvWork(WorkThreader worker){
        mLock.lock();
        try{
            mPools.remove(worker);
            Log.d("pool", "mvWork = " + worker.mTag);
        }finally {
            mLock.unlock();
        }
    }
}
