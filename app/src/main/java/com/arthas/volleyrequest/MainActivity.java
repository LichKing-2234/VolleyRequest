package com.arthas.volleyrequest;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.android.volley.VolleyError;
import com.arthas.volleyrequest.utils.LogUtils;
import com.google.gson.reflect.TypeToken;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        VolleyRequest.init(getApplication(), new VolleyRequest.RequestKey("code", 1, "desc", "data"));
        VolleyRequest.httpPost("http://www.baidu.com", null, null,
                new VolleyRequest.ResponseListener<User>() {
                    @Override
                    public void onSuccess(User data) {
                        LogUtils.i(data.toString());
                    }

                    @Override
                    public void onFail(int code, String msg) {
                        LogUtils.e(msg);
                    }

                    @Override
                    public void onError(VolleyError error) {
                        LogUtils.e(error.toString());
                    }

                    @Override
                    public TypeToken<User> getTypeToken() {
                        return new TypeToken<User>() {
                        };
                    }
                });
    }

}
