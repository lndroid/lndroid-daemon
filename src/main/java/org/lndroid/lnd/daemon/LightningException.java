package org.lndroid.lnd.daemon;

public class LightningException extends Exception {

    private int code_;
    private String message_;

    public LightningException(int code, String message) {
        code_ = code;
        message_ = message;
    }

    public int errorCode () {
        return code_;
    }

    public String errorMessage () {
        return message_;
    }

    @Override
    public String getMessage (){
        return "code "+code_+" message "+message_;
    }
}
