# RequestManager
Удобная обертка для Retrofit(1.9.0). 

Гибкий Callback расширяющий retrofit.Callback. 
Автоматическая работа с куками.
Автогенерация объекта из полученного json.

[![Release](https://jitpack.io/v/e16din/RequestManager.svg)](https://jitpack.io/#e16din/RequestManager)


## Пример использования:
```java
//retrofit service method with callback
@GET("/users/{id}")
void getUser(@Path("id") long id,
                RetrofitCallback<User> callback);

//simple callback (RetrofitCallback implement Callback)
new RetrofitCallback<Result>(){
            @Override
            public void onSuccess(Result result, int statusCode) {
                //do something
            }
        };


//call retrofit service
TheApplication.getServices().getUser(id,
                        new RetrofitCallback<User>() {
                            @Override
                            public void onSuccess(User result, int statusCode) {
                                //do something
                            }
                        });

//example User model
public class User implements Result {
    private int error;//serialized value from server

    @Override
    public boolean isSuccess() {
        return error == 0;//success condition
    }
}
```

## Callback
```java
public interface Callback<T> {

    void onSuccess(T result, int statusCode); // start on success result

    void onErrorFromServer(Result result); // start when Result.isSuccess() == false

    void onExceptionError(Throwable e, String responseString); // start on any exception 

    void onHttpError(int code, String message, String body);// start on http error

    void afterResult(boolean withError); // start after all (if needCancel() == false)

    boolean needCancel();//set true when you need to cancel other methods

    void onCancel();//start when needCancel() == true
}
```

## Download (Gradle)

```groovy
repositories {
    maven { url "https://jitpack.io" }
}

buildscript {
    repositories {
        maven { url "https://jitpack.io" }
    }
}

dependencies {
	compile 'com.github.e16din:RequestManager:1.+'
}
```