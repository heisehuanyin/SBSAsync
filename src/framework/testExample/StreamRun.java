package framework.testExample;

import framework.StreamQueue;

public class StreamRun {

    public static void main(String[] args){
        StreamQueue q = new StreamQueue();

        q.newStream(0, controller -> {
            System.out.println("First");
            controller.checkPass();
        }).Then(controller -> {
            System.out.println("Second");
            controller.checkPass();
        }).Then(controller -> {
            System.out.println("Third");
            controller.checkPass();
        }).Then(controller -> {
            System.out.println("4");
            controller.checkPass();
        }).complete();

        q.getController(0).run();
    }
}
