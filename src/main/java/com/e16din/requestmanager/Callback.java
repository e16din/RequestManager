package com.e16din.requestmanager;

public interface Callback<T> {

    /**
     * Called on success
     *  @param result         request result
     * @param statusCode     http status code
     */
    void onSuccess(T result, int statusCode);

    /**
     * Called on request success, with error in response object
     *
     * @param result         result with error
     */
    void onErrorFromServer(Result result);

    /**
     * Called on any exception
     *
     * @param e              the exception
     * @param responseString may contain null or error message
     */
    void onExceptionError(Throwable e, String responseString);

    /**
     * Called on http error
     *
     * @param code           http status code
     * @param message        error message
     * @param body           response body
     */
    void onHttpError(int code, String message, String body);

    /**
     * Called after result (success or error)
     *
     * @param withError             if result is error
     */
    void afterResult(boolean withError);

    /**
     * Set true, when you need cancel request
     */
    boolean needCancel();

    /**
     * Called if needCancel true, instead on success and on error methods
     */
    void onCancel();
}
