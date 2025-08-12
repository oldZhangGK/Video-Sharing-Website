package com.example.bilibili.domain;

public class JsonResponse<T> {
    private String code; //returned status code e.g: 200/300/404
    private String msg; // returned a message tell us whether not it success
    private T data; // generic type, we may return different types of data

    public JsonResponse(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public JsonResponse(T data) {
        this.data = data;
        msg = "success";
        code = "0";
    }

    // default setting, dont need to return anything
    public static JsonResponse<String> success() {
        return new JsonResponse<String>(null);
    }

    // need to return data
    public static JsonResponse<String> success(String data) {
        return new JsonResponse<String>(data);
    }

    public static JsonResponse<String> fail() {
        return new JsonResponse<String>("1", "fail");
    }

    public static JsonResponse<String> fail(String code, String msg) {
        return new JsonResponse<String>(code, msg);
    }


    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
