package com.qiniu.android.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SingleFlight {

    private Map<String, SingleFlightCall> callInfo = new HashMap<>();

    void perform(String key, ActionHandler actionHandler, CompleteHandler completeHandler) throws Exception {
        if (actionHandler == null) {
            return;
        }

        boolean isFirstTask = false;
        boolean shouldComplete = false;
        SingleFlightCall call = null;
        synchronized (this) {

            if (key != null) {
                call = callInfo.get(key);
            }

            if (call == null) {
                call = new SingleFlightCall();
                if (key != null) {
                    callInfo.put(key, call);
                }
                isFirstTask = true;
            }

            synchronized (call) {
                if (call.exception != null || call.value != null) {
                    shouldComplete = true;
                } else {
                    SingleFlightTask task = new SingleFlightTask();
                    task.completeHandler = completeHandler;
                    call.tasks.add(task);
                }
            }
        }

        if (shouldComplete) {
            if (call.exception != null) {
                throw call.exception;
            } else if (call.value != null) {
                if (completeHandler != null) {
                    completeHandler.complete(null);
                }
            }
            return;
        }

        if (!isFirstTask) {
            return;
        }

        final String finalKey = key;
        final SingleFlightCall finalCall = call;
        try {
            actionHandler.action(new CompleteHandler() {
                @Override
                public void complete(Object value) {
                    List<SingleFlightTask> currentTasks = null;
                    synchronized (finalCall) {
                        finalCall.value = value;
                        currentTasks = new ArrayList<>(finalCall.tasks);
                    }
                    if (finalKey != null) {
                        synchronized (this) {
                            callInfo.remove(finalKey);
                        }
                    }
                    for (SingleFlightTask task : currentTasks) {
                        if (task != null && task.completeHandler != null) {
                            task.completeHandler.complete(finalCall.value);
                        }
                    }
                }
            });
        } catch (Exception e) {
            List<SingleFlightTask> currentTasks = null;
            synchronized (finalCall) {
                finalCall.exception = e;
                currentTasks = new ArrayList<>(call.tasks);
            }
            if (key != null) {
                synchronized (this) {
                    callInfo.remove(key);
                }
            }
            for (SingleFlightTask task : currentTasks) {
                if (task != null && task.completeHandler != null) {
                    throw call.exception;
                }
            }
        }
    }

    private static class SingleFlightTask {
        private CompleteHandler completeHandler;
    }

    private static class SingleFlightCall {
        private List<SingleFlightTask> tasks = new ArrayList<>();
        private Object value;
        private Exception exception;
    }

    public interface CompleteHandler {
        void complete(Object value);
    }

    public interface ActionHandler {
        void action(CompleteHandler completeHandler) throws Exception;
    }
}
