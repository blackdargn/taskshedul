package syling.task;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Administrator on 2015/3/21 0021.
 * 可执行任务
 */
public abstract class ExTask<T> implements Runnable{
    // 任务的优先级
    public static enum TaskPriority{
        UI,// 非耗时UI操作，用于定时UI任务
        LOCAL,// 应用本地耗时操作，如DB、File等
        S_NETWORK,// 小量耗时网络请求，如API流量小的请求
        UNKNOWN,// 不确定的耗时操作
        L_NETWORK,// 大量耗时网络请求，文件上传或者下载
    }
    // 任务回调监听器
    WeakReference<TaskHandler.NotifyListener<T>> mUiListener;
    TaskHandler.NotifyListener<T> mBackListener;

    // 执行的参数
    Object[] mParams;
    // 执行的结果
    private T mResult;
    // 执行的上下文
    TaskContext mContext;
    // 该任务的标签，一对一对应，前缀表示一个组，如 image_xx
    String mTag;
    // 超时时间
    long timeout = 0;
    // 任务权限，默认为LOCAL
    TaskPriority mPriority = TaskPriority.LOCAL;
    // 在相同TaskPriority下，是否先执行，是放在头，否放在尾
    boolean isFirst = false;
    // 是否取消
    private final Lock mLock = new ReentrantLock();
    private boolean isCanceled = false;
    private boolean isDoing    = false;
    // 同步锁
    private final Object mSynLock = new Object();
    private ArrayList<ExTask> mSubTask;

    public ExTask(){}

    public ExTask(TaskHandler.NotifyListener<T> listener){
        if(listener != null) {
            if(listener.isInUI()) {
                // UI对象进行弱引用，防止Activity内存泄漏
                mUiListener = new WeakReference<TaskHandler.NotifyListener<T>>(listener);
            }else{
                mBackListener = listener;
            }
        }
    }

    public ExTask setParams(Object...params){
        mParams = params;
        return this;
    }

    public ExTask setContext(TaskContext context){
        mContext = context;
        return this;
    }

    public ExTask setTag(String tag){
        mTag = tag;
        return this;
    }

    public ExTask setPriority(TaskPriority Priority){
        return setPriority(Priority , false);
    }
    public ExTask setPriority(TaskPriority Priority, boolean isFirst){
        mPriority = Priority;
        this.isFirst = isFirst;
        return this;
    }

    public ExTask setTimeout(long timeout){
        this.timeout = timeout;
        return this;
    }

    // 下一个顺序任务,按照层级顺序
    public ExTask next(ExTask task) throws Exception{
        mLock.lock();
        try{
            if(isDoing){
                // 开始后无法再执行操作
                throw new Exception("task is starting, please before op");
            }
            if (mSubTask == null) {
                mSubTask = new ArrayList<ExTask>();
            }
            mSubTask.add(task);
            return this;
        }finally {
            mLock.unlock();
        }
    }

    // 下一个顺序任务,按照层级顺序
    public ExTask first(ExTask task) throws Exception{
        mLock.lock();
        try{
            if(isDoing){
                // 开始后无法再执行操作
                throw new Exception("task is starting, please before op");
            }
            if (mSubTask == null) {
                mSubTask = new ArrayList<ExTask>();
            }
            mSubTask.add(0, task);
            return this;
        }finally {
            mLock.unlock();
        }
    }

    // 下一个顺序任务,按照层级顺序
    public boolean remove(String tag) throws Exception{
        mLock.lock();
        try{
            if(isDoing){
                // 开始后无法再执行操作
                throw new Exception("task is starting, please before op");
            }
            if (mSubTask == null || mSubTask.size() == 0) {
                return false;
            }
            for (ExTask task : mSubTask) {
                if (task.mTag != null && task.mTag.equals(tag)) {
                    return mSubTask.remove(task);
                }
            }
            return false;
        }finally {
            mLock.unlock();
        }
    }

    public T getResult(){
        return mResult;
    }

    public void cancel(){
        mLock.lock();
        try {
            isCanceled = true;
        }finally {
            mLock.unlock();
        }
    }

    ArrayList<ExTask> getSubTask(){
        mLock.lock();
        try {
            isDoing = true;
            return mSubTask;
        }finally {
            mLock.unlock();
        }
    }

    boolean isCanceled(){
        mLock.lock();
        try {
            return isCanceled;
        }finally {
            mLock.unlock();
        }
    }

    // 同步等待
    public void waitFinish(){
        if(TaskHandler.isMainThread()){
            // 主线程无法等待
            return;
        }
        synchronized (mSynLock){
            try {
                mSynLock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // 同步唤醒
    protected void notifyFinish(){
        synchronized (mSynLock){
            mSynLock.notifyAll();
        }
    }

    public int getKey(){
        return mTag != null ? mTag.hashCode() : this.hashCode();
    }

    protected abstract T work(Object...params) throws Exception;

    @Override
    public void run() {
        doWork();
    }

    private TaskHandler.NotifyListener<T> getListener(){
        if(mBackListener != null){
            return mBackListener;
        }
        if(mUiListener != null ) {
            return mUiListener.get();
        }
        return null;
    }

    public void doStart(){
        TaskHandler.notifyStart(getListener(), mTag);
    }

    public void doProcess(String process){
        TaskHandler.notifyProcess(getListener(), process);
    }

    public void doWork(){
        mResult = null;
        if(mUiListener != null && mUiListener.get() == null) {
            // UI弱引用null，提前结束
            return;
        }
        if(mContext != null && !mContext.isRunning()){
            // Context周期结束，提前结束
            return;
        }
        try{
            mResult  = work(mParams);
            notifyFinish();
            if(!isCanceled()) {
                TaskHandler.notifySucceed(getListener(), mResult);
            }
        }catch (Exception e){
            e.printStackTrace();
            if(!isCanceled()) {
                TaskHandler.notifyError(getListener(), e.getMessage());
            }
        }
    }
}
