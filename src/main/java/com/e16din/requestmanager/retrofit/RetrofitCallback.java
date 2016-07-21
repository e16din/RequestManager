package com.e16din.requestmanager.retrofit;

import android.text.TextUtils;
import android.util.Log;

import com.e16din.requestmanager.Callback;
import com.e16din.requestmanager.EmptyListResult;
import com.e16din.requestmanager.Result;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import retrofit.RetrofitError;
import retrofit.client.Header;
import retrofit.client.Response;
import retrofit.mime.TypedByteArray;

public abstract class RetrofitCallback<T extends Result>
        implements Callback<T>, retrofit.Callback<T> {

    public static final String LOG_TAG = "RequestManager";

    public static final int HTTP_ERROR_BAD_REQUEST = 400;

    public static final int MAX_RESPONSES = 10;


    private boolean mWithError = false;

    private Response mResponse;

    private RetrofitError mError;


    @Override
    public void success(T result, Response response) {

        if (needIgnoreExceptions()) {
            try {
                runOnSuccess(result, response);

            } catch (Exception e) {
                mWithError = true;
                onExceptionError(e, null);
            }

        } else {
            runOnSuccess(result, response);
        }

        StaticErrorHandler.setLastRetrofitError(null);
    }

    @Override
    public void failure(RetrofitError error) {
        if (needIgnoreExceptions()) {
            try {
                runOnError(error);

            } catch (Exception e) {
                mWithError = true;
                onExceptionError(e, null);
            }

        } else {
            runOnError(error);
        }

        StaticErrorHandler.setLastRetrofitError(null);
    }

    private void runOnSuccess(T result, Response response) {
        mResponse = response;
        mError = null;

        final String cookies = getCookieString(response);
        if (!TextUtils.isEmpty(cookies)) {
            BaseRetrofitAdapter.setCookies(cookies);
        }

        if (needCancel()) {
            Log.w(LOG_TAG, "success: operation canceled! try override needCancel method");
            onCancel();
            return;
        }

        mWithError = false;

        if (StaticErrorHandler.getLastRetrofitError() != null) {
            mWithError = true;

            final Response handledResponse =
                    StaticErrorHandler.getLastRetrofitError().getResponse();

            if (handledResponse != null && handledResponse.getStatus() != 0) {
                onHttpError(handledResponse.getStatus(),
                        StaticErrorHandler.getLastRetrofitError().getMessage(),
                        new String(((TypedByteArray) StaticErrorHandler.getLastRetrofitError()
                                .getResponse().getBody()).getBytes()));

            } else {
                onExceptionError(StaticErrorHandler.getLastRetrofitError().getCause(),
                        StaticErrorHandler.getLastRetrofitError().getMessage());
            }

        } else {
            if (result == null || result.isSuccess()) {
                onSuccess(result, response.getStatus());

            } else if ((result + "").startsWith("[") && (result + "").length() <= 3) {
                final Gson gson = new Gson();

                EmptyListResult emptyList = new EmptyListResult();
                T emptyResult = gson.fromJson(gson.toJson(emptyList),
                        new TypeToken<EmptyListResult>() {
                        }.getType());

                onSuccess(emptyResult, response.getStatus());

            } else {
                mWithError = true;
                onErrorFromServer(result);
            }
        }

        afterResult(mWithError);
    }

    private void runOnError(RetrofitError error) {
        mError = error;
        mResponse = null;

        if (needCancel()) {
            onCancel();
            return;
        }

        mWithError = true;

        if (StaticErrorHandler.getLastRetrofitError() != null) {
            if (StaticErrorHandler.getLastRetrofitError().getResponse() != null
                    && StaticErrorHandler.getLastRetrofitError().getResponse().getStatus() != 0) {

                onHttpError(StaticErrorHandler.getLastRetrofitError().getResponse().getStatus(),
                        StaticErrorHandler.getLastRetrofitError().getMessage(),
                        new String(((TypedByteArray) StaticErrorHandler.getLastRetrofitError()
                                .getResponse().getBody()).getBytes()));

            } else {
                onExceptionError(StaticErrorHandler.getLastRetrofitError().getCause(),
                        StaticErrorHandler.getLastRetrofitError().getMessage());
            }

            StaticErrorHandler.setLastRetrofitError(null);
            afterResult(mWithError);
            return;
        }

        Log.e(LOG_TAG, "error: " + error.getMessage());

        if (error.getMessage() != null && (error.getMessage().contains("java.io.EOFException")
                || error.getMessage().contains("400 Bad Request"))) {

            onHttpError(HTTP_ERROR_BAD_REQUEST, error.getMessage(), null);
            afterResult(mWithError);
            return;
        }

        onExceptionError(error.getCause(), error.getMessage());
        StaticErrorHandler.setLastRetrofitError(null);
        afterResult(mWithError);
    }

    private String getCookieString(Response response) {
        for (Header header : response.getHeaders()) {
            if (header.getName() != null && header.getName().equals("Set-Cookie")) {
                Log.d(LOG_TAG, "Set-Cookie: " + header.getValue());
                return header.getValue();
            }
        }
        return null;
    }

    /**
     * Response on success
     *
     * @return retrofit Response object or null if error
     */
    public Response getResponse() {
        return mResponse;
    }

    /**
     * Error on failure
     *
     * @return retrofit RetrofitError object or null if success
     */
    public RetrofitError getError() {
        return mError;
    }

    @Override
    public void onErrorFromServer(Result result) {
    }

    @Override
    public void onExceptionError(Throwable e, String responseString) {
        e.printStackTrace();
    }

    @Override
    public void onHttpError(int code, String message, String body) {
    }

    @Override
    public void afterResult(boolean withError) {
    }

    @Override
    public boolean needCancel() {
        return false;
    }

    @Override
    public void onCancel() {
    }

    public boolean needIgnoreExceptions() {
        return true;
    }
}
