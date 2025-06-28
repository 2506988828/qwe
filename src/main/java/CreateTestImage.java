import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Point;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;
import static org.bytedeco.opencv.global.opencv_imgproc.rectangle;

public class CreateTestImage {
    public static void main(String[] args) {
        // 创建一个空白彩色图像，大小为200x200，3通道(BGR)，初始为黑色
        Mat img = new Mat(200, 200, org.bytedeco.opencv.global.opencv_core.CV_8UC3, new Scalar(0, 0, 0, 0));


        // 在图像中画一个绿色矩形
        rectangle(img, new Point(50, 50), new Point(150, 150), new Scalar(0, 255, 0, 0), 5, 8, 0);

        // 保存到文件
        boolean result = imwrite("D:/myapp/images/test_generated.png", img);
        System.out.println("图片保存结果: " + result);
    }
}
