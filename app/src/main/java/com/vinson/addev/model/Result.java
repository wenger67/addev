package com.vinson.addev.model;

public class Result<T> {
    private int code;
    private Data<T> data;
    private String message;

    public Result(int code, Data<T> data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public Data<T> getData() {
        return data;
    }

    public void setData(Data<T> data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public static class Data<T> {
        private T lift;

        public Data(T lift) {
            this.lift = lift;
        }

        public T getLift() {
            return lift;
        }

        public void setLift(T lift) {
            this.lift = lift;
        }
    }
}
