package com.xpcjsu.sunshinemall.pay.callback;

import java.util.List;

/**
 * 责任链执行器（最小实现）
 * 按顺序执行处理器，任一返回false或抛出异常则短路
 */
public class CallbackHandlerChain {

    @FunctionalInterface
    public interface Handler {
        /** 返回true继续，返回false短路 */
        boolean handle(CallbackContext ctx) throws Exception;
    }

    public static boolean execute(CallbackContext ctx, List<Handler> handlers) throws Exception {
        for (Handler handler : handlers) {
            boolean next = handler.handle(ctx);
            if (!next) {
                ctx.setSuccess(false);
                return false;
            }
        }
        ctx.setSuccess(true);
        return true;
    }
}