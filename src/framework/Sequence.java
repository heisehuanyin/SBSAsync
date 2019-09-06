/**
 * 用于单线程异步回调
 * 典型场景Android {@link android.app.Activity}主任务调度
 * Android：{@link android.app.Activity#startActivityForResult(android.content.Intent, int)}
 * 或者
 * Android：{@link android.app.Activity#requestPermissions(java.lang.String[], int)}。
 * 典型特点
 * 单个执行环境只存在一个线程，不存在多线程同时操作变量，不存在线程冲突，如果用多线程操作本{@link ws.toolbox.Sequence}
 * 实例及相关操作会发生不可知结果，请注意。
 */

package framework;

import java.util.*;


/**
 * 异步执行注册中心，使用本框架执行步进可控的异步任务
 * <ol>
 *     <li>外部控制，应用于类似Android:Activity
 *     #startActivityForResult(Intent, int)}的无法向异步API传委托的场景</li>
 *     <li>内部串联，应用于异步控制流（可以传参委托，在委托中利用{@link Controller}
 *     相关API返回判定结果）</li>
 * </ol>
 */
public class Sequence {
    private Map<Long, Controller> controllorMap = new HashMap<>();

    /**
     * 在Sequence中注册一个{@link Switch}实例
     *
     * @param switchMark {@link Switch}唯一标识
     * @param judgement  {@link Switch}指定判定条件
     * @return 自身
     */
    public Sequence registerSwitch(long switchMark, ImmediateTask judgement) {
        if (controllorMap.containsKey(switchMark)) {
            DuplicateException e = new DuplicateException("重复注册switchMark<" + switchMark + ">");
            this.getSwitch(switchMark).processExceptionCatch(null, e);
            return this;
        }

        Controller ctrl = new Controller() {
            private Switch target = new Switch(Sequence.this);

            @Override
            public Sequence start(Object unique) {
                target.startImmediate(unique, this);
                return Sequence.this;
            }

            @Override
            public Switch getSwitch() {
                return target;
            }

            @Override
            public void returnCheckPass(Object unique) {
                target.processCheckPass(unique);
            }

            @Override
            public void returnRefuse(Object unique) {
                target.processRefuse(unique, target);
            }
        };

        ctrl.getSwitch().setImmediateTask(judgement);
        this.controllorMap.put(switchMark, ctrl);

        return this;
    }

    /**
     * 通过switchMark获取指定{@link Switch}的关联{@link Controller}
     *
     * @param switchMark {@link Switch}标识
     * @return {@link Switch}实例关联{@link Controller}
     */
    public Controller getController(long switchMark) {
        return this.controllorMap.get(switchMark);
    }

    /**
     * 通过switchMark获取指定{@link Switch}实例
     *
     * @param switchMark {@link Switch}标识
     * @return {@link Switch}实例
     */
    public Switch getSwitch(long switchMark) {
        if (controllorMap.get(switchMark)==null)
            return null;
        else
            return controllorMap.get(switchMark).getSwitch();
    }


    /**
     * 配置实体
     */
    public class Switch {
        private final Sequence chain;

        private Switch(Sequence chain) {
            this.chain = chain;
        }


        private ImmediateTask judgement = null;

        private void setImmediateTask(ImmediateTask task) {
            judgement = task;
        }

        private final List<Object> unique_keys = new ArrayList<>();

        private void startImmediate(Object unique, Controller c) {
            unique_keys.add(unique);

            try {
                judgement.execute(c);
            } catch (Exception e) {
                if (catchIns.isEmpty())
                    e.printStackTrace();
                else
                    processExceptionCatch(unique, e);

                processFinally(unique);
            }
        }


        private Map<Object, List<GenericTask>> checkPassed = new HashMap<>();

        /**
         * 向本{@link Switch}增添CheckPass配置
         *
         * @param unique unique-key
         * @param task   {@link GenericTask}实例
         * @return 自身引用
         */
        public Switch appendCheckPass(Object unique, GenericTask task) {
            if (checkPassed.get(unique) == null)
                checkPassed.put(unique, new ArrayList<>());
            checkPassed.get(unique).add(task);
            return this;
        }

        private void processCheckPass(Object unique) {
            try {
                if (!unique_keys.contains(unique))
                    throw new MismatchException("指定了未注册unique-key");

                List<GenericTask> tasks = checkPassed.get(unique);
                if (tasks != null){
                    for (GenericTask task : tasks) {
                        task.execute(this);
                    }
                }
            } catch (Exception e) {
                if (catchIns.isEmpty()) {
                    e.printStackTrace();
                } else {
                    processExceptionCatch(unique, e);
                }
            }

            processFinally(unique);
        }


        private Map<Object, List<GenericTask>> refused = new HashMap<>();

        /**
         * 向本实例添加Refust配置
         *
         * @param unique unique-key
         * @param task   {@link GenericTask}实例
         * @return 自身引用
         */
        public Switch appendRefuse(Object unique, GenericTask task) {
            if (!refused.containsKey(unique))
                refused.put(unique, new ArrayList<>());
            refused.get(unique).add(task);
            return this;
        }

        private void processRefuse(Object unique, Switch s) {
            try {
                if (!unique_keys.contains(unique))
                    throw new MismatchException("指定了未注册unique-key");

                List<GenericTask> tasks = checkPassed.get(unique);
                if (tasks != null){
                    for (GenericTask task : tasks) {
                        task.execute(this);
                    }
                }
            } catch (Exception e) {
                if (catchIns.isEmpty()) {
                    e.printStackTrace();
                } else {
                    processExceptionCatch(unique, e);
                }
            }

            processFinally(unique);
        }


        private Map<Object, List<CatchHandle>> catchIns = new HashMap<>();

        public Switch appendExceptionCatch(Object unique, CatchHandle catchHandle) {
            if (!catchIns.containsKey(unique))
                catchIns.put(unique, new ArrayList<>());
            catchIns.get(unique).add(catchHandle);
            return this;
        }

        /**
         * 利用内部存储{@link CatchHandle}处理传入异常
         *
         * @param unique 指定unique-key，指定处理器类型，若为null，则调用全部处理器
         * @param e      异常
         */
        private void processExceptionCatch(Object unique, Exception e) {
            List<CatchHandle> catchs = new ArrayList<>();
            if (unique == null) {
                Collection<List<CatchHandle>> autos = catchIns.values();
                if (autos != null){
                    for (List<CatchHandle> items : autos) {
                        catchs.addAll(items);
                    }
                }
            } else {
                if (catchIns.containsKey(unique)){
                    catchs.addAll(catchIns.get(unique));
                }
            }

            for (CatchHandle catc : catchs) {
                catc.catchException(e, this);
            }
        }


        private Map<Object, List<GenericTask>> finallyTasks = new HashMap<>();

        /**
         * 向本{@link Switch}添加Finally配置
         *
         * @param unique unique-key
         * @param task   {@link GenericTask}实例
         * @return 自身引用
         */
        public Switch appendFinally(Object unique, GenericTask task) {
            if (!finallyTasks.containsKey(unique))
                finallyTasks.put(unique, new ArrayList<>());
            finallyTasks.get(unique).add(task);
            return this;
        }

        private void processFinally(Object unique) {
            if (finallyTasks.containsKey(unique)){
                List<GenericTask> tasks = finallyTasks.get(unique);
                for (GenericTask task : tasks) {
                    task.execute(this);
                }
            }

            // clear switch content
            unique_keys.remove(unique);
            checkPassed.remove(unique);
            refused.remove(unique);
            catchIns.remove(unique);
            finallyTasks.remove(unique);
        }

        /**
         * 完成{@link Switch}配置
         *
         * @return 所属{@link Sequence}实例
         */
        public Sequence switchComplete() {
            return chain;
        }


        private Map<String, List<String>> listBuf = new HashMap<>();

        public void putStringList(String key, List<String> list) {
            listBuf.put(key, list);
        }

        public List<String> getStringList(String key) {
            return listBuf.get(key);
        }


        private Map<String, String> strBuf = new HashMap<>();

        public void putString(String key, String value) {
            strBuf.put(key, value);
        }

        public String getString(String key) {
            return strBuf.get(key);
        }


        private Map<String, Object> objBuf = new HashMap<>();

        public void putObject(String key, Object value) {
            objBuf.put(key, value);
        }

        public Object getObject(String key) {
            return objBuf.get(key);
        }
    }


    public interface ImmediateTask {
        void execute(Controller c);
    }

    public interface GenericTask {
        /**
         * 任务入口点
         *
         * @param s 关联{@link Switch}
         */
        void execute(Switch s);
    }

    public interface Controller {

        /**
         * 通过指定unique-key启动{@link Switch}
         *
         * @param unique unique-key，用于批次验证，如果出现
         * @return 返回从属Sequence
         */
        Sequence start(Object unique);

        /**
         * 返回{@link Controller}关联的{@link Switch}实例
         *
         * @return {@link Switch}实例
         */
        Switch getSwitch();

        /**
         * 返回判定状态接受（是、真）
         *
         * @param unique 任务区分标识
         */
        void returnCheckPass(Object unique);

        /**
         * 返回判定状态拒绝（否、假）
         *
         * @param unique 任务区分标识
         */
        void returnRefuse(Object unique);
    }

    /**
     * 异常捕获实体
     */
    public interface CatchHandle {
        void catchException(Exception e, Switch aSwitch);
    }

    /**
     * 通用异常类型
     */
    public class SequenceException extends Exception {
        private final String shortMessage;
        private final Exception origin;

        public SequenceException(String shortMessage, Exception origin) {
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
     * <li>可能由于{@link Controller#returnRefuse(Object)}等函数返回了不存在记录的unique-key。</li>
     */
    public class MismatchException extends SequenceException {
        public MismatchException(String shortMsg) {
            super(shortMsg, null);
        }
    }

    /**
     * 特定异常：多个{@link Switch}的重复绑定同一个switchMark
     */
    public class DuplicateException extends SequenceException {

        public DuplicateException(String shotMsg) {
            super(shotMsg, null);
        }
    }


    private void example_text_method() {

        this    // 注册新判定
                .registerSwitch(0, c -> {
                })
                .registerSwitch(1, c -> {
                })
                .registerSwitch(2, c -> {
                })
                .registerSwitch(3, c -> {
                })


                .getSwitch(0)
                .appendCheckPass(new Object(), new GenericTask() {
                    @Override
                    public void execute(Switch s) {

                    }
                })
                .appendRefuse(new Object(), new GenericTask() {
                    @Override
                    public void execute(Switch s) {

                    }
                })
                .appendExceptionCatch(new Object(), new CatchHandle() {
                    @Override
                    public void catchException(Exception e, Switch aSwitch) {

                    }
                })
                .appendFinally(new Object(), new GenericTask() {
                    @Override
                    public void execute(Switch s) {

                    }
                })
                .switchComplete()


                .getSwitch(1)
                .appendCheckPass(new Object(), s -> {

                })
                .switchComplete()
                .getController(1).start(new Object())
                .getController(0).start(new Object());


        // 执行任务
        this.getController(0).start(new Object());


    }

    public static void main(String[] args) {
        Sequence chain = new Sequence();

        chain.example_text_method();
    }
}
