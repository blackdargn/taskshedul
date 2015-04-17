package syling.task.test;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import syling.task.R;
import syling.task.TaskActivity;
import syling.task.TaskHandler;


public class My2Activity extends TaskActivity {

    TextView my_hello_text_view, tvShow;
    Button mClickMeBtn, mClickMeBtn2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        my_hello_text_view = (TextView) findViewById(R.id.my_hello_text_view);
        mClickMeBtn = (Button) findViewById(R.id.clickMeBtn);
        mClickMeBtn2 = (Button) findViewById(R.id.clickMeBtn2);
        tvShow = (TextView) findViewById(R.id.tvShow);
        TaskHandler.initHandle();
    }

    public void clickMeBtnPressed5(View view) {
        TaskTest.instance.testCanAll();
    }

    public void clickMeBtnPressed6(View view) {
        TaskTest.instance.testCanPrix("1");
    }

    public void clickMeBtnPressed7(View view) {
        TaskTest.instance.testCanTag(15 + "");
    }

    public void clickMeBtnPressed4(View view) {
        TaskTest.instance.testSeq(this, 8, mListener);
    }

    public void clickMeBtnPressed3(View view) {
        TaskTest.instance.testWait(mListener);
    }

    public void clickMeBtnPressed2(View view) {
        finish();
        startActivity(new Intent(this, MyActivity.class));
    }

    public void clickMeBtnPressed(View view) {
        my_hello_text_view.setText("thanks");
        TaskTest.instance.testPools(this, 40, new TaskHandler.NotifyListener<Integer>(true) {
            @Override
            protected void onStart(String tag) {
                tvShow.append("starting " + tag + "\n");
            }

            @Override
            protected void onSucceed(Integer object) {
                tvShow.append("ok:" + object + "\n");
            }

            @Override
            protected void onError(Object object) {
                tvShow.append("error:" + object + "\n");
            }
        });
    }

    private TaskHandler.NotifyListener mListener = new TaskHandler.NotifyListener<Integer>(true) {
        @Override
        protected void onStart(String tag) {
            tvShow.append("starting " + tag + "\n");
        }

        @Override
        protected void onSucceed(Integer object) {
            tvShow.append("ok:" + object + "\n");
        }

        @Override
        protected void onError(Object object) {
            tvShow.append("error:" + object + "\n");
        }
    };
}
