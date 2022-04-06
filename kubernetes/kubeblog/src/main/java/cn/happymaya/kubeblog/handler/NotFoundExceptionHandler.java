package cn.happymaya.kubeblog.handler;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotFoundExceptionHandler extends RuntimeException{

    public NotFoundExceptionHandler() {
    }

    public NotFoundExceptionHandler(String message) {
        super(message);
    }

    public NotFoundExceptionHandler(String message, Throwable cause) {
        super(message, cause);
    }
}
