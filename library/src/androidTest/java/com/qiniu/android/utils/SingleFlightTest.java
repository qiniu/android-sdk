package com.qiniu.android.utils;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.qiniu.android.BaseTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SingleFlightTest extends BaseTest {

    private static final int RetryCount = 5;

    @Test
    public void testSync() {
        final TestStatus testStatus = new TestStatus();
        testStatus.maxCount = 1000;
        testStatus.completeCount = 0;

        SingleFlight singleFlight = new SingleFlight();
        for (int i = 0; i < testStatus.maxCount; i++) {
            singleFlightPerform(singleFlight, i, RetryCount, false, new CompleteHandler() {
                @Override
                public void complete() throws Exception {
                    testStatus.completeCount += 1;
                    LogUtil.d("== sync completeCount:" + testStatus.completeCount);
                }
            });
        }

        wait(new WaitConditional() {
            @Override
            public boolean shouldWait() {
                return testStatus.maxCount != testStatus.completeCount;
            }
        }, 60);
    }

    @Test
    public void testSyncRetry() {
        final TestStatus testStatus = new TestStatus();
        testStatus.maxCount = 1000;
        testStatus.completeCount = 0;

        SingleFlight singleFlight = new SingleFlight();
        for (int i = 0; i < testStatus.maxCount; i++) {
            singleFlightPerform(singleFlight, i, 0, false, new CompleteHandler() {
                @Override
                public void complete() throws Exception {
                    testStatus.completeCount += 1;
                    LogUtil.d("== sync completeCount:" + testStatus.completeCount);
                }
            });
        }

        wait(new WaitConditional() {
            @Override
            public boolean shouldWait() {
                return testStatus.maxCount != testStatus.completeCount;
            }
        }, 60);
    }

    @Test
    public void testAsync() {
        final TestStatus testStatus = new TestStatus();
        testStatus.maxCount = 1000;
        testStatus.completeCount = 0;

        SingleFlight singleFlight = new SingleFlight();
        for (int i = 0; i < testStatus.maxCount; i++) {
            singleFlightPerform(singleFlight, i, RetryCount, true, new CompleteHandler() {
                @Override
                public void complete() throws Exception {
                    synchronized (testStatus) {
                        testStatus.completeCount += 1;
                    }
                    LogUtil.d("== async complete Count:" + testStatus.completeCount);
                }
            });
        }

        wait(new WaitConditional() {
            @Override
            public boolean shouldWait() {
                return testStatus.maxCount != testStatus.completeCount;
            }
        }, 60);

        assertTrue("== async" + "max Count:" + testStatus.maxCount + " complete Count:" + testStatus.completeCount, testStatus.maxCount == testStatus.completeCount);
        LogUtil.d("== async" + "max Count:" + testStatus.maxCount + " complete Count:" + testStatus.completeCount);
    }

    @Test
    public void testAsyncRetry() {
        final TestStatus testStatus = new TestStatus();
        testStatus.maxCount = 1000;
        testStatus.completeCount = 0;

        SingleFlight singleFlight = new SingleFlight();
        for (int i = 0; i < testStatus.maxCount; i++) {
            singleFlightPerform(singleFlight, i, 0, true, new CompleteHandler() {
                @Override
                public void complete() throws Exception {
                    synchronized (testStatus) {
                        testStatus.completeCount += 1;
                    }
                    LogUtil.d("== async completeCount:" + testStatus.completeCount);
                }
            });
        }

        wait(new WaitConditional() {
            @Override
            public boolean shouldWait() {
                return testStatus.maxCount != testStatus.completeCount;
            }
        }, 60);

        LogUtil.d("== async completeCount:" + testStatus.completeCount + " end");
    }

    private void singleFlightPerform(final SingleFlight singleFlight,
                                     final int index,
                                     final int retryCount,
                                     final boolean isAsync,
                                     final CompleteHandler completeHandler) {

        try {
            singleFlight.perform("key", new SingleFlight.ActionHandler() {
                @Override
                public void action(final SingleFlight.CompleteHandler singleFlightCompleteHandler) throws Exception {

                    final CompleteHandler completeHandlerP = new CompleteHandler() {
                        @Override
                        public void complete() throws Exception {
                            if (retryCount < RetryCount) {
                                LogUtil.d("== " + (isAsync ? "async" : "sync") + " action retryCount:" + retryCount + " index:" + index + " error");
                                throw new Exception("== 123 ==");
                            } else {
                                LogUtil.d("== " + (isAsync ? "async" : "sync") + " action retryCount:" + retryCount + " index:" + index + " value");
                                singleFlightCompleteHandler.complete(index + "");
                            }
                        }
                    };

                    if (isAsync) {
                         new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    completeHandlerP.complete();
                                } catch (Exception e) {
                                    singleFlightCompleteHandler.complete(null);
                                }
                            }
                        }).start();
                    } else {
                        completeHandlerP.complete();
                    }
                }
            }, new SingleFlight.CompleteHandler() {
                @Override
                public void complete(Object value) {
                    if (retryCount < RetryCount) {
                        singleFlightPerform(singleFlight, index, retryCount + 1, isAsync, completeHandler);
                    } else {
                        LogUtil.d("== " + (isAsync ? "async" : "sync") + " action complete retryCount:" + retryCount + " value:" + value + " index:" + index);
                        if (!isAsync) {
                            assertTrue("index:" + index + "value error",(value + "").equals(index + ""));
                        }
                        try {
                            completeHandler.complete();
                        } catch (Exception e) {
                        }
                    }
                }
            });
        } catch (Exception e) {
            singleFlightPerform(singleFlight, index, retryCount + 1, isAsync, completeHandler);
        }
    }



    private interface CompleteHandler {
        void complete() throws Exception;
    }


    protected static class TestStatus {
        int maxCount;
        int completeCount;
    }

}
