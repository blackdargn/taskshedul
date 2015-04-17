package syling.task;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

/**
 * Created by Administrator on 2015/3/21 0021.
 * 回调监听者
 */
public class TaskHandler {

    private static final int MSG_NOTIFY_ON_START = 1;
    private static final int MSG_NOTIFY_ON_PROCESS = 2;
    private static final int MSG_NOTIFY_ON_ERROR = 3;
    private static final int MSG_NOTIFY_ON_SUCCEED = 4;

    public static class NotifyListener<T extends Object> {

        // 是否在UI线程回调
        private boolean isInUI;

        public NotifyListener() {
            this.isInUI = false;
        }

        public NotifyListener(boolean uiCall) {
            this.isInUI = uiCall;
        }

        public boolean isInUI() {
            return isInUI;
        }

        /**
         * 开始执行
         */
        protected void onStart(String tag) {
        }

        /**
         * 执行进度 done:total 字符串组合
         */
        protected void onProcess(String process) {
        }

        /**
         * 执行成功
         */
        protected void onSucceed(T object) {
        }

        /**
         * 执行出错
         */
        protected void onError(Object object) {
        }

        private void notify(int what, Object object) {
            switch (what) {
                case MSG_NOTIFY_ON_ERROR: {
                    onError(object);
                    break;
                }
                case MSG_NOTIFY_ON_SUCCEED: {
                    onSucceed((T) object);
                    break;
                }
                case MSG_NOTIFY_ON_START: {
                    onStart((String) object);
                    break;
                }
                case MSG_NOTIFY_ON_PROCESS: {
                    onProcess((String) object);
                    break;
                }
            }
        }
    }

    private TaskHandler(){}

    protected static MainHandler mainHandler;
    public static void initHandle() {
        if (mainHandler == null) {
            mainHandler = new MainHandler();
        }
    }

    public static void submit(Runnable task) {
        mainHandler.post(task);
    }

    public static boolean isMainThread() {
        return Looper.myLooper() != null && Looper.myLooper() == Looper.getMainLooper();
    }

    private static class MainHandler extends Handler{
        public MainHandler() {
            super(Looper.getMainLooper());
        }

        public void handleMessage(Message msg) {
            NotifyHolder holder = (NotifyHolder) msg.obj;
            try {
                holder.listener.notify(msg.what, holder.data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static class NotifyHolder {
        public NotifyListener<?> listener;
        public Object data;
    }

    public static void notifyStart(NotifyListener<?> listener, Object data) {
        notify(MSG_NOTIFY_ON_START, listener, data);
    }

    public static void notifyProcess(NotifyListener<?> listener, Object data) {
        notify(MSG_NOTIFY_ON_PROCESS, listener, data);
    }

    public static void notifySucceed(NotifyListener<?> listener, Object data) {
        notify(MSG_NOTIFY_ON_SUCCEED, listener, data);
    }

    public static void notifyError(NotifyListener<?> listener, Object data) {
        notify(MSG_NOTIFY_ON_ERROR, listener, data);
    }

    private static void notify(int what, NotifyListener<?> listener, Object data) {
        if (listener == null) return;
        initHandle();
        if (listener.isInUI() && !isMainThread()) {
            NotifyHolder holder = new NotifyHolder();
            holder.listener = listener;
            holder.data = data;
            Message msg = new Message();
            msg.what = what;
            msg.obj = holder;

            mainHandler.sendMessage(msg);
        } else {
            try {
                listener.notify(what, data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
