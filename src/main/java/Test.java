import com.my.qwe.controller.ImageMatchService;
import com.my.qwe.http.DeviceHttpClient;
import org.bytedeco.opencv.opencv_core.*;


import java.io.IOException;
import java.util.List;

public class Test {
    public static void main(String[] args) {

        int[]jianchaduihua = null;
        try {
            jianchaduihua = DeviceHttpClient.findMultiColor("54:99:63:DB:19:8A",0,0,2000,2000,"082318","2|4|082019,24|13|abeaf4,6|22|96dbe1,5|25|54808b,12|6|b7ecf7,9|2|284743,27|4|021a1b,27|12|00151f,2|11|86929c,14|13|37616c,18|2|8dacb3,12|25|0d211e,7|15|a5e8ee,11|6|b7ecf7,6|3|9cbdbd,2|19|101f1c,9|11|8bb7c4,7|4|c5edf6,1|20|17231c,13|17|9eeaee,15|12|315663,18|21|6b9da9",0.8,0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println(jianchaduihua[0]);
    }
}