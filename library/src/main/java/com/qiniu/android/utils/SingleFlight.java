package com.qiniu.android.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * single flight
 *
 * @param <T> T
 * @hidden
 */
public class SingleFlight<T> {

    private Map<String, SingleFlightCall<T>> callInfo = new HashMap<>();

    /**
     * 构造函数
     */
    public SingleFlight() {
    }

    /**
     * 异步 SingleFlight 执行函数
     *
     * @param key             actionHandler 对应的 key，同一时刻同一个 key 最多只有一个对应的 actionHandler 在执行
     * @param actionHandler   执行函数，注意：actionHandler 中，【完成回调】和【抛出异常】二者有且有一个，且只能出现一次
     * @param completeHandler single flight 执行 actionHandler 后的完成回调
     * @throws Exception 异常
     */
    public void perform(String key, ActionHandler<T> actionHandler, CompleteHandler<T> completeHandler) throws Exception {
        if (actionHandler == null) {
            return;
        }

        boolean isFirstTask = false;
        boolean shouldComplete = false;
        SingleFlightCall<T> call = null;
        synchronized (this) {

            if (key != null) {
                call = callInfo.get(key);
            }

            if (call == null) {
                call = new SingleFlightCall<>();
                if (key != null) {
                    callInfo.put(key, call);
                }
                isFirstTask = true;
            }

            synchronized (call) {
                shouldComplete = call.isComplete;
                if (!shouldComplete) {
                    SingleFlightTask<T> task = new SingleFlightTask<>();
                    task.completeHandler = completeHandler;
                    call.tasks.add(task);
                }
            }
        }

        if (shouldComplete) {
            if (call.exception != null) {
                throw call.exception;
            } else {
                if (completeHandler != null) {
                    completeHandler.complete(call.value);
                }
            }
            return;
        }

        if (!isFirstTask) {
            return;
        }

        final String finalKey = key;
        final SingleFlightCall<T> finalCall = call;
        try {
            actionHandler.action(new CompleteHandler<T>() {
                @Override
                public void complete(T value) {
                    List<SingleFlightTask<T>> currentTasks = null;
                    synchronized (finalCall) {
                        if (finalCall.isComplete) {
                            return;
                        }
                        finalCall.isComplete = true;
                        finalCall.value = value;
                        currentTasks = new ArrayList<>(finalCall.tasks);
                    }
                    if (finalKey != null) {
                        synchronized (this) {
                            callInfo.remove(finalKey);
                        }
                    }
                    for (SingleFlightTask<T> task : currentTasks) {
                        if (task != null && task.completeHandler != null) {
                            task.completeHandler.complete(finalCall.value);
                        }
                    }
                }
            });
        } catch (Exception e) {
            List<SingleFlightTask<T>> currentTasks = null;
            synchronized (finalCall) {
                if (finalCall.isComplete) {
                    return;
                }
                finalCall.isComplete = true;
                finalCall.exception = e;
                currentTasks = new ArrayList<>(call.tasks);
            }
            if (key != null) {
                synchronized (this) {
                    callInfo.remove(key);
                }
            }
            for (SingleFlightTask<T> task : currentTasks) {
                if (task != null && task.completeHandler != null) {
                    throw call.exception;
                }
            }
        }
    }

    private static class SingleFlightTask<T> {
        private CompleteHandler<T> completeHandler;
    }

    private static class SingleFlightCall<T> {
        private boolean isComplete = false;
        private List<SingleFlightTask<T>> tasks = new ArrayList<>();
        private T value;
        private Exception exception;
    }

    /**
     * 结束回调
     *
     * @param <T> T
     * @hidden
     */
    public interface CompleteHandler<T> {

        /**
         * 结束回调
         *
         * @param value value
         */
        void complete(T value);
    }

    /**
     * action 回调
     *
     * @param <T> T
     * @hidden
     */
    public interface ActionHandler<T> {

        /**
         * action 回调
         *
         * @param completeHandler 结束回调
         * @throws Exception 异常
         */
        void action(CompleteHandler<T> completeHandler) throws Exception;
    }
}
