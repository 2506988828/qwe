import com.my.qwe.http.DeviceHttpClient;
import com.my.qwe.task.*;

import java.util.List;

public class Test {
    public static void main(String[] args) {

        DutuTask dutuTask = new DutuTask();
        dutuTask.cangbaotupaixu(new TaskContext("54:99:63:DB:19:8A","1"),new TaskThread("54:99:63:DB:19:8A",dutuTask,new TaskContext("54:99:63:DB:19:8A","1")));

    }
}