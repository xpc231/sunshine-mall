package com.xpcjsu.sunshinemall.pay.callback;

import java.util.List;

/**
 * 责任链执行器（最小实现）
 * 按顺序执行处理器，任一返回false或抛出异常则短路
 */
public class CallbackHandlerChain {

    /**
     * 处理器函数式接口
     * <p>
     * 定义了一个处理回调上下文的函数式接口，用于链式处理逻辑。
     * 实现类需要提供具体的处理逻辑，并通过返回值控制处理链的执行。
     * </p>
     */
    @FunctionalInterface
    public interface Handler {
        /**
         * 处理回调上下文
         *
         * @param ctx 回调上下文对象，包含处理所需的数据和状态信息
         * @return true表示处理成功并继续执行后续处理器，false表示处理中断，不再执行后续处理器
         * @throws Exception 处理过程中可能抛出的异常
         */
        boolean handle(CallbackContext ctx) throws Exception;
    }


    /**
     * 执行处理器链
     *
     * @param ctx 回调上下文对象，用于在处理器间传递状态和结果
     * @param handlers 处理器列表，按顺序执行每个处理器
     * @return 执行结果，true表示所有处理器都成功执行，false表示某个处理器执行失败
     * @throws Exception 处理过程中可能抛出的异常
     */
    public static boolean execute(CallbackContext ctx, List<Handler> handlers) throws Exception {
        // 遍历并执行所有处理器
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