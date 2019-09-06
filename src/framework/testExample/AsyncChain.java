package framework.testExample;

import framework.Sequence;

public class AsyncChain {
    public static void main(String[] args) {
        Sequence one = new Sequence();

        // one task judgement
        one.registerSwitch(0, new Sequence.ImmediateTask() {
            @Override
            public void execute(Sequence.Controller c) {
                c.returnCheckPass(c);
            }
        })
                .getSwitch(0)
                .appendCheckPass(one.getController(0), new Sequence.GenericTask() {
                    @Override
                    public void execute(Sequence.Switch s) {
                        System.out.println("switch 0 was check passed");
                    }
                })
                .appendRefuse(one.getController(0), new Sequence.GenericTask() {
                    @Override
                    public void execute(Sequence.Switch s) {
                        System.out.println("switch 0 was refused.");
                    }
                })
                .switchComplete()

                // Async chain
                .registerSwitch(1, new Sequence.ImmediateTask() {
                    @Override
                    public void execute(Sequence.Controller c) {
                        c.returnCheckPass(one);
                    }
                })
                .getSwitch(1)
                .appendCheckPass(one, s -> {
                    one.getController(2).start(one);
                    System.out.println("switch 1 was checkpassd.");
                })
                .appendRefuse(one, s -> System.out.println("Switch 1 was refused."))
                .switchComplete()


                .registerSwitch(2, c -> {
                    c.returnRefuse(one);
                })
                .getSwitch(2)
                .appendRefuse(one, s -> System.out.println("Switch 2 was refused."))
                .appendCheckPass(one, s -> System.out.println("Switch 2 was check passed."))
                .switchComplete()

                .getController(0).start(one.getController(0))
                .getController(1).start(one)
                .getController(2).start(one);

        // unique-key 不匹配测试
        one.getController(1).start(new Object());
    }
}
