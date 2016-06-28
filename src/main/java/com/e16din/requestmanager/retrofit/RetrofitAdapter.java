package com.e16din.requestmanager.retrofit;

import android.text.TextUtils;

import java.util.Map;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;

/**
 * Created by e16din on 20.08.15.
 */
public class RetrofitAdapter extends BaseRetrofitAdapter {
    public static Object getService(Class requestManagerInterface,
                                    RestAdapter.Builder builder,
                                    Map<String, String> headers) {
        builder.setErrorHandler(new StaticErrorHandler())
                .setRequestInterceptor(getRequestInterceptor(headers));

        return builder.build().create(requestManagerInterface);
    }

    public static Object getService(Class requestManagerInterface, RestAdapter.Builder builder) {
        return builder.build().create(requestManagerInterface);
    }

    private static RequestInterceptor getRequestInterceptor(final Map<String, String> headers) {
        return new RequestInterceptor() {
            @Override
            public void intercept(RequestFacade request) {
                for (String key : headers.keySet()) {
                    request.addHeader(key, headers.get(key));
                }

                if (!TextUtils.isEmpty(getCookies()))
                    request.addHeader("Cookie", getCookies());
            }
        };
    }
}
