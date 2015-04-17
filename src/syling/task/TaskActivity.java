package syling.task;

import android.app.Activity;

/**
 * Created by Administrator on 2015/3/24 0024.
 */
public class TaskActivity extends Activity implements TaskContext{

    private volatile boolean isRunning = false;

    @Override
    protected void onResume() {
        super.onResume();
        isRunning = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isRunning = false;
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }
}
