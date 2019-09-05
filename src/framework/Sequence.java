package framework;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Sequence {
    private Map<Long, Controller> controllorMap = new HashMap<>();

    public Switch newSwitch(long switchMark) {
        Switch one = new Switch(this);
        Controller at = new Controller() {

            @Override
            public Switch getSwitch() {
                return one;
            }

            @Override
            public void returnCheckPass(Object unique) {
                one.processCheckPassed(unique);
            }

            @Override
            public void returnRefuse(Object unique) {
                one.processRefused(unique);
            }
        };

        controllorMap.put(switchMark, at);
        return one;
    }

    private Controller getSwitchController(long switchMark) {
        return this.controllorMap.get(switchMark);
    }

    public Sequence start(long switchMark, Object unique) {
        Controller c = getSwitchController(switchMark);
        c.getSwitch().startAllImmediateTask(unique,c);

        return this;
    }



    // ==============================================================================


    public class Switch {
        private final Sequence chain;
        private Map<Object, Boolean> stateMap = new HashMap<>();

        private Switch(Sequence chain) {
            this.chain = chain;
        }


        private List<ImmediateTask> immediateList = new ArrayList<>();

        public Switch startAsyncJudgement(ImmediateTask asyncExecutor) {
            immediateList.add(asyncExecutor);
            return this;
        }


        private List<GenericTask> checkpassed = new ArrayList<>();

        public Switch setCheckPassed(GenericTask executor) {
            checkpassed.add(executor);
            return this;
        }


        private List<GenericTask> refused = new ArrayList<>();

        public Switch setRefused(GenericTask executor) {
            refused.add(executor);
            return this;
        }


        private List<CatchHandle> catchIns = new ArrayList<>();

        public Switch setCatch(CatchHandle ins) {
            catchIns.add(ins);
            return this;
        }


        private List<GenericTask> finallyTasks = new ArrayList<>();

        public Switch setFinally(GenericTask executor) {
            finallyTasks.add(executor);
            return this;
        }

        private void startAllImmediateTask(Object unique, Controller c){
            try {
                if (stateMap.containsKey(unique))
                    throw new DuplicateException(null);
                stateMap.put(unique, true);

                for (ImmediateTask task:immediateList){
                    task.execute(c);
                }

            } catch (SequenceExp e) {
                if (catchIns.isEmpty())
                    e.printStackTrace();
                else {
                    for (CatchHandle ins : catchIns) {
                        ins.catchException(e, this);
                    }
                }
            }

            for (GenericTask fi : finallyTasks) {
                fi.execute(this);
            }
        }

        private void processCheckPassed(Object unique) {
            try {
                if (stateMap.get(unique) == null)
                    throw new MismatchException(null);
                stateMap.remove(unique);


                for (GenericTask task : checkpassed) {
                    task.execute(this);
                }
            } catch (SequenceExp e) {
                if (catchIns.isEmpty())
                    e.printStackTrace();
                else {
                    for (CatchHandle ins : catchIns) {
                        ins.catchException(e, this);
                    }
                }
            }

            for (GenericTask fi : finallyTasks) {
                fi.execute(this);
            }
        }

        private void processRefused(Object unique) {
            try {
                if (stateMap.get(unique) == null)
                    throw new MismatchException(null);
                stateMap.remove(unique);


                for (GenericTask task : refused) {
                    task.execute(this);
                }
            } catch (SequenceExp e) {
                if (catchIns.isEmpty())
                    e.printStackTrace();
                else {
                    for (CatchHandle ins : catchIns) {
                        ins.catchException(e, this);
                    }
                }
            }

            for (GenericTask fi : finallyTasks) {
                fi.execute(this);
            }
        }

        public Sequence switchDone() {
            return chain;
        }
    }


    /**
     * <h1>异步立即执行任务</h1>
     * 输入{@link Switch#startAsyncJudgement(ImmediateTask)}，任务立即执行，然后返回。<br/>
     * 异步任务需要写在{@link ImmediateTask#execute(Controller)} 中作为请调方
     * 当异步任务返回结果，在异步回调函数中操作{@link Controller#returnCheckPass(Object)}
     * 或者{@link Controller#returnRefuse(Object)}返回状态，以继续执行指定{@link Switch}余下的过程。
     */
    public interface ImmediateTask {
        /**
         * 任务入口点
         * @param c 关联{@link Controller}，内含用于接力的{@link Switch}实例
         */
        void execute(Controller c);
    }

    /**
     * <h1>通用任务</h1>
     * 多种函数中当做回调委托，调用方通过{@link Switch}提供接力数据传递
     */
    public interface GenericTask {
        /**
         * 任务入口点
         * @param s 关联{@link Switch}
         */
        void execute(Switch s);
    }

    /**
     * <h1>{@link Switch}判定控制器</h1>
     * 用于控制{@link Switch}的运行。
     * 在{@link Switch#startAsyncJudgement(ImmediateTask)}方法中返回{@link Controller#returnCheckPass(Object)}或者
     * {@link Controller#returnRefuse(Object)}状态判定，以接续指定{@link Switch}判定运行。
     * 中间结果考虑存储到{@link Switch}实例上
     */
    public interface Controller {
        /**
         * 返回{@link Controller}关联的{@link Switch}实例
         * @return {@link Switch}实例
         */
        Switch getSwitch();

        /**
         * 返回判定状态接受（是、真）
         * @param unique 任务区分标识
         */
        void returnCheckPass(Object unique);

        /**
         * 返回判定状态拒绝（否、假）
         * @param unique 任务区分标识
         */
        void returnRefuse(Object unique);
    }

    /**
     * 异常捕获实体
     */
    public interface CatchHandle {
        void catchException(SequenceExp e, Switch aSwitch);
    }

    /**
     * 通用异常类型
     */
    public class SequenceExp extends Exception {
        private final String shortMessage;
        private final Exception origin;

        public SequenceExp(String shortMessage, Exception origin) {
            this.shortMessage = shortMessage;
            this.origin = origin;
        }

        public String getShortMessage() {
            return this.shortMessage;
        }

        public Exception getOriginException() {
            return this.origin;
        }
    }

    /**
     * 特定异常：{@link Switch}绑定与判定unique-key不匹配异常
     * <li>可能由于{@link Controller#returnRefuse(Object)}等函数返回了不匹配unique-key造成。</li>
     */
    public class MismatchException extends SequenceExp {
        public MismatchException(Exception origin) {
            super("对指定Switch基于指定unique key进行了重复判定", origin);
        }
    }

    /**
     * 同一时间内，指定{@link Switch}多次绑定同一unique-key错误
     * <li>该{@link Switch}中指定unique-key存在未完成判定</li>
     */
    public class DuplicateException extends SequenceExp{
        public DuplicateException(Exception origin){
            super("多次添加同一unique-key",origin);
        }
    }







    private void example_text_method() {
        this
                .newSwitch(0)
                .startAsyncJudgement(new ImmediateTask() {
                    @Override
                    public void execute(Controller c) {
                        c.returnRefuse(Sequence.this);      // 拒绝继续串行执行
                        c.returnCheckPass(Sequence.this);   // 继续串行执行
                    }
                })
                .setCheckPassed(new GenericTask() {
                    @Override
                    public void execute(Switch s) {
                        System.out.println("CheckedPassed");
                    }
                })
                .setRefused(new GenericTask() {
                    @Override
                    public void execute(Switch s) {
                        System.out.println("Refused");
                    }
                })
                .setCatch(new CatchHandle() {
                    @Override
                    public void catchException(SequenceExp e, Switch aSwitch) {
                        System.out.println("Catched");
                    }
                })
                .setFinally(new GenericTask() {
                    @Override
                    public void execute(Switch s) {
                        System.out.println("finally");
                    }
                })
                .switchDone()
                .start(0, Sequence.this);


    }
    public static void main(String[] args) {
        Sequence chain = new Sequence();

        chain.example_text_method();
    }
}
