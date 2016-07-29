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

public abstract class RetrofitCallback<T extends Result> implements Callback<T>, retrofit.Callback<T> {

    public static final String LOG_TAG = "RequestManager";

    public static final int HTTP_ERROR_BAD_REQUEST = 400;

    private static final Gson GSON = new Gson();


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
                onExceptionErrorWithAnotherCallbacks(e, null);
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
                onExceptionErrorWithAnotherCallbacks(e, null);
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
            onCancelWithAnotherCallbacks();
            return;
        }

        mWithError = false;

        if (StaticErrorHandler.getLastRetrofitError() != null) {
            mWithError = true;

            onLastRetrofitError();
        } else {
            final int status = response.getStatus();

            if (result == null || result.isSuccess()) {
                onSuccessWithAnotherCallbacks(result, status);

            } else if ((result + "").startsWith("[") && (result + "").length() <= 3) {

                EmptyListResult emptyList = new EmptyListResult();
                T emptyResult = GSON.fromJson(GSON.toJson(emptyList),
                        new TypeToken<EmptyListResult>() {
                        }.getType());

                onSuccessWithAnotherCallbacks(emptyResult, status);

            } else {
                mWithError = true;

                onErrorFromServerWithAnotherCallbacks(result);
            }
        }

        afterResultWithAnotherCallbacks();
    }

    private void runOnError(RetrofitError error) {
        mError = error;
        mResponse = null;

        if (needCancel()) {
            Log.w(LOG_TAG, "error: operation canceled! try override needCancel method");
            onCancelWithAnotherCallbacks();
            return;
        }

        mWithError = true;

        if (StaticErrorHandler.getLastRetrofitError() != null) {
            onLastRetrofitError();

            afterResultWithAnotherCallbacks();
            return;
        }

        final String errorMessage = error.getMessage();

        Log.e(LOG_TAG, "error: " + errorMessage);

        if (errorMessage != null && (errorMessage.contains("java.io.EOFException")
                || errorMessage.contains("400 Bad Request"))) {

            onHttpErrorWithAnotherCallbacks(HTTP_ERROR_BAD_REQUEST, errorMessage, null);
            afterResultWithAnotherCallbacks();
            return;
        }

        onExceptionErrorWithAnotherCallbacks(error.getCause(), errorMessage);
        StaticErrorHandler.setLastRetrofitError(null);
        afterResultWithAnotherCallbacks();
    }

    private void onLastRetrofitError() {
        final Response errorResponse = StaticErrorHandler.getLastRetrofitError().getResponse();
        final int status = errorResponse.getStatus();
        final String message = StaticErrorHandler.getLastRetrofitError().getMessage();

        if (status != 0) {
            final String body = new String(((TypedByteArray) errorResponse.getBody()).getBytes());
            onHttpErrorWithAnotherCallbacks(status, message, body);

        } else {
            final Throwable cause = StaticErrorHandler.getLastRetrofitError().getCause();
            onExceptionErrorWithAnotherCallbacks(cause, message);
        }

        StaticErrorHandler.setLastRetrofitError(null);
    }

    private void onSuccessWithAnotherCallbacks(T result, int status) {
        if (previousCallback() != null && !ignorePreviousCallback()) {
            previousCallback().onSuccess(result, status);
        }

        onSuccess(result, status);

        if (nextCallback() != null && !ignoreNextCallback()) {
            nextCallback().onSuccess(result, status);
        }
    }


    private void onErrorFromServerWithAnotherCallbacks(T result) {
        if (previousCallback() != null && !ignorePreviousCallback()) {
            previousCallback().onErrorFromServer(result);
        }

        onErrorFromServer(result);

        if (nextCallback() != null && !ignoreNextCallback()) {
            nextCallback().onErrorFromServer(result);
        }
    }

    private void onCancelWithAnotherCallbacks() {
        if (previousCallback() != null && !ignorePreviousCallback()) {
            previousCallback().onCancel();
        }

        onCancel();

        if (nextCallback() != null && !ignoreNextCallback()) {
            nextCallback().onCancel();
        }
    }

    private void onExceptionErrorWithAnotherCallbacks(Throwable cause, String message) {
        if (previousCallback() != null && !ignorePreviousCallback()) {
            previousCallback().onExceptionError(cause, message);
        }

        onExceptionError(cause, message);

        if (nextCallback() != null && !ignoreNextCallback()) {
            nextCallback().onExceptionError(cause, message);
        }
    }

    private void onHttpErrorWithAnotherCallbacks(int status, String message, String body) {
        if (previousCallback() != null && !ignorePreviousCallback()) {
            previousCallback().onHttpError(status, message, body);
        }

        onHttpError(status, message, body);

        if (nextCallback() != null && !ignoreNextCallback()) {
            nextCallback().onHttpError(status, message, body);
        }
    }

    private void afterResultWithAnotherCallbacks() {
        if (previousCallback() != null && !ignorePreviousCallback()) {
            previousCallback().afterResult(mWithError);
        }

        afterResult(mWithError);

        if (nextCallback() != null && !ignoreNextCallback()) {
            nextCallback().afterResult(mWithError);
        }
    }


    private String getCookieString(Response response) {
        for (Header header : response.getHeaders()) {
            if (header.getName() != null && header.getName().equals(getCookieHeaderName())) {
                Log.d(LOG_TAG, getCookieHeaderName() + ": " + header.getValue());
                return header.getValue();
            }
        }
        return null;
    }

    private String getCookieHeaderName() {
        return "Set-Cookie";
    }


    /**
     * Override this method if you need to call any callback after current callback
     */
    public RetrofitCallback<T> nextCallback() {
        return null;
    }

    /**
     * Override this method if you need to call any callback before current callback
     */
    public RetrofitCallback<T> previousCallback() {
        return null;
    }

    /**
     * Set true to ignore previous callback
     */
    public boolean ignorePreviousCallback() {
        return false;
    }

    /**
     * Set true to ignore next callback
     */
    public boolean ignoreNextCallback() {
        return false;
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
