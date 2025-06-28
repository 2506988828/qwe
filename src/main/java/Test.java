import com.my.qwe.controller.ImageMatchService;
import org.bytedeco.opencv.opencv_core.*;


import java.util.List;

public class Test {
    public static void main(String[] args) {
        String deviceId = "18:81:0E:83:13:F4";
        String imagePath = "D:/myapp/images/shuzi6.bmp";

        List<int[]> points = ImageMatchService.findAllMatches(deviceId, imagePath, 0.8f, 10);
        System.out.println("<UNK>: " + points.size());

        for (int[] p : points) {
            System.out.printf("%d, %d\n", p[0], p[1]);
        }

    }
}