import com.my.qwe.http.DeviceHttpClient;

public class Test {
    public static void main(String[] args) {

       int[]pos= DeviceHttpClient.findImages("18:81:0E:83:13:F4","国境驿站1","国境驿站2","国境驿站3","国境驿站4",0.7);
        System.out.println(pos[0]);
    }
}