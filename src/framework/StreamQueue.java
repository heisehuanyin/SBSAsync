package framework;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StreamQueue {
    private final Map<Integer, Controller> controllerMap = new HashMap<>();

    public Stream newStream(int mark, GTask task) {
        var entry = new Stream(this, task);
        var c = new Controller(entry);
        controllerMap.put(mark, c);

        return entry;
    }

    public Controller getController(int mark) {
        return controllerMap.get(mark);
    }

    public static class Controller {
        private final Stream target;
        private PromiseState state = PromiseState.CONTINUE;

        public Controller(Stream target) {
            this.target = target;
        }

        public Stream getPromise() {
            return target;
        }

        public int bindMark(){
            var set = target.con.controllerMap.keySet();
            for (var key : set){
                if (target.con.controllerMap.get(key)==this){
                    return key;
                }
            }
            throw new RuntimeException("运行时错误：丢失必要信息。");
        }

        public void checkPass() {
            state = PromiseState.CONTINUE;
            target.processOneTasks(this);
        }

        public void refuse() {
            state = PromiseState.BREAK;
            target.processOneTasks(this);
        }

        public void run() {
            target.processOneTasks(this);
        }
    }


    enum PromiseState {
        CONTINUE,
        BREAK
    }

    private static class Group<T1, T2, T3> {
        private T1 first;
        private T2 second;
        private T3 third;

        public Group(T1 a, T2 b, T3 c) {
            first = a;
            second = b;
            third = c;
        }

        public void resetFirst(T1 one) {
            this.first = one;
        }

        public void resetSecond(T2 one) {
            this.second = one;
        }

        public void resetThird(T3 one) {
            this.third = one;
        }
    }

    public static class Stream {
        private List<Group<GTask, Handle, FRun>> tasks = new ArrayList<>();
        private final StreamQueue con;

        public Stream(StreamQueue con, GTask one) {
            tasks.add(new Group<>(one, null, null));
            this.con = con;
        }

        public Stream Then(GTask one) {
            tasks.add(new Group<>(one, null, null));
            return this;
        }

        public Stream Catch(Handle catchHandle) {
            tasks.get(tasks.size() - 1).resetSecond(catchHandle);
            return this;
        }

        public Stream Final(FRun run) {
            tasks.get(tasks.size() - 1).resetThird(run);
            return this;
        }

        public StreamQueue complete() {
            return con;
        }

        private Map<String, Object> objectMap = new HashMap<>();

        public Stream putObject(String key, Object obj) {
            objectMap.put(key, obj);
            return this;
        }

        public Object getObject(String key) {
            return objectMap.get(key);
        }

        private void processOneTasks(Controller one) {
            if (tasks.size()==0)
                return;

            var item = tasks.remove(0);
            var unit = item.first;
            try {
                if (one.state == PromiseState.CONTINUE)
                    unit.execute(one);
            } catch (Exception e) {
                if (item.second != null) {
                    item.second.ExceptionCatched(e);
                } else {
                    throw e;
                }
            } finally {
                if (item.third != null) {
                    item.third.execute(this);
                }
            }
        }
    }

    public interface GTask {
        void execute(Controller controller);
    }

    public interface Handle {
        void ExceptionCatched(Exception e);
    }

    public interface FRun {
        void execute(Stream promise);
    }
}
