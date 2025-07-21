import com.my.qwe.controller.ImageMatchService;
import com.my.qwe.http.DeviceHttpClient;
import org.bytedeco.opencv.opencv_core.*;


import java.io.IOException;
import java.util.List;

public class Test {
    public static void main(String[] args) {

        int[] COORDINATE_RECT = {454,211, 537, 229};
        try {
            String ocrResult = DeviceHttpClient.ocrEx("18:81:0E:83:13:F4", COORDINATE_RECT);
            System.out.println("RRR");
            System.out.println(ocrResult);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}