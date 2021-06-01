import java.text.DecimalFormat;
import java.text.NumberFormat;

public class Test
{
    public static void main(String[] args) {
        String str = "0.50";
//        String str = "1280.00";
        System.out.println(new DecimalFormat("0.00").format(new Double(str)));
    }

}
