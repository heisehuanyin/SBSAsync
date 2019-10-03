package framework.testExample;

import framework.StreamQueue;

public class StreamRun {

    public static void main(String[] args){
        StreamQueue q = new StreamQueue();

        q.newStream(0, controller -> {
            System.out.println("First");
            controller.checkPass();
            System.out.println("Fend");
        }).Then(controller -> {
            System.out.println("Second");
            controller.checkPass();
            System.out.println("Send");
        }).Then(controller -> {
            System.out.println("Third");
            controller.checkPass();
            System.out.println("Tend");
        }).Then(controller -> {
            System.out.println("4");
            controller.checkPass();
            System.out.println("4end");
        }).complete();

        q.getController(0).run();

        q.newStream(1, controller -> {
            System.out.println("---1");
        }).Then(controller -> {
            System.out.println("----2");
        }).Then(controller -> {
            System.out.println("-----4");
        });
        q.getController(1).checkPass();
        q.getController(1).checkPass();
        q.getController(1).checkPass();
        q.getController(1).checkPass();
    }
}
