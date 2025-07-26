import com.my.qwe.http.DeviceHttpClient;
import com.my.qwe.task.CommonActions;
import com.my.qwe.task.DutuTask;
import com.my.qwe.task.TaskContext;
import com.my.qwe.task.TaskThread;

import java.util.List;

public class Test {
    public static void main(String[] args) {

        CommonActions commonActions = new CommonActions(new TaskContext("54:99:63:DB:19:8A","1"),new TaskThread("54:99:63:DB:19:8A",new DutuTask(),new TaskContext("54:99:63:DB:19:8A","1")));
        List<Integer> a= commonActions.findEmptyBagIndices("54:99:63:DB:19:8A");
        System.out.println(a.size());
    }
}