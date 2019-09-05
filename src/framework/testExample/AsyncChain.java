package framework.testExample;

import framework.Sequence;

public class AsyncChain {
    public static void main(String[] args) {
        Sequence one = new Sequence();

        // one task judgement
        one.newSwitch(0)
                .startAsyncJudgement(new Sequence.ImmediateTask() {
                    @Override
                    public void execute(Sequence.Controller c) {
                        c.returnCheckPass(one);
                    }
                })
                .setCheckPassed(new Sequence.GenericTask() {
                    @Override
                    public void execute(Sequence.Switch s) {
                        System.out.println("It was check passed.");
                    }
                })
                .setRefused(new Sequence.GenericTask() {
                    @Override
                    public void execute(Sequence.Switch s) {
                        System.out.println("It was refused.");
                    }
                })
                .setCatch(new Sequence.CatchHandle() {
                    @Override
                    public void catchException(Sequence.SequenceExp e, Sequence.Switch aSwitch) {
                        System.out.println("Catach one exception");
                    }
                })
                .setFinally(new Sequence.GenericTask() {
                    @Override
                    public void execute(Sequence.Switch s) {

                    }
                })
                .switchDone()

                // Async chain
                .newSwitch(1)
                .startAsyncJudgement(c -> {
                    // Async Task
                    c.returnCheckPass(one);
                })
                .setCheckPassed((s) -> {
                    System.out.println("Switch 1 was check passed.");
                    s.switchDone().start(2, one);
                })
                .setRefused(s -> {
                    System.out.println("Switch 1 was refused.");
                })
                .switchDone()
                .newSwitch(2)
                .startAsyncJudgement(c -> {
                    c.returnCheckPass(one);
                })
                .setCheckPassed(s -> {
                    System.out.println("Switch 2 was check passed.");
                })
                .switchDone()

                // 启动指定“独立switch-0”
                .start(0, one)
                // 启动异步链“switch-1-2”
                .start(1, one)
                // 再启动异步链“switch-1-2”
                .start(1,one);

        // unique-key 不匹配测试
        one.start(1, new Object());
    }
}
