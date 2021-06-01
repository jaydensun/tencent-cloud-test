import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;

import com.tencentcloudapi.tsf.v20180326.TsfClient;
import com.tencentcloudapi.tsf.v20180326.models.*;
import sun.plugin2.gluegen.runtime.CPU;;import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DescribeGroup {

    public static final DecimalFormat CPU_FORMAT = new DecimalFormat("0.00");
    public static final DecimalFormat MEM_FORMAT = new DecimalFormat("0");

    public static void main(String[] args) {
        try {
            // 参数中传入密钥
            String secretId = args[0];
            String secretKey = args[1];
            Credential cred = new Credential(secretId, secretKey);

            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("tsf.tencentcloudapi.com");

            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);

            TsfClient client = new TsfClient(cred, "ap-guangzhou", clientProfile);

            DescribeSimpleApplicationsRequest req = new DescribeSimpleApplicationsRequest();
            req.setLimit(1000L);

            // 查询应用
            DescribeSimpleApplicationsResponse resp = client.DescribeSimpleApplications(req);

            System.out.println(DescribeSimpleApplicationsResponse.toJsonString(resp));

            // 遍历应用，查询部署组
            Map<String, List<ContainGroup>> clusterContainerGroups = Stream.of(resp.getResult().getContent()).flatMap(x -> {
                DescribeContainerGroupsRequest request = new DescribeContainerGroupsRequest();
                request.setApplicationId(x.getApplicationId());
                boolean retry = false;
                do {
                    retry = false;
                    try {
                        return Stream.of(client.DescribeContainerGroups(request).getResult().getContent());
                    } catch (TencentCloudSDKException e) {
                        if (e.getErrorCode().equals("RequestLimitExceeded")) {
                            retry = true;
                        } else {
                            e.printStackTrace();
                        }
                    }
                } while (retry);
                return Stream.empty();
            }).collect(Collectors.groupingBy(ContainGroup::getClusterName));

            // 打印部署组信息
            String print = clusterContainerGroups.entrySet().stream().map(x -> {
                String groups = x.getValue().stream().sorted(
                        Comparator.comparing(ContainGroup::getCpuRequest).thenComparing(ContainGroup::getGroupName)
                ).map(y -> String.format("%s %-40s %10s %10s %10s %10s",
                        y.getGroupId(), y.getGroupName(), cpuFormat(y.getCpuRequest()), cpuFormat(y.getCpuLimit()),
                        memFormat(y.getMemRequest()), memFormat(y.getMemLimit())
                )).collect(Collectors.joining("\n"));
                return String.format("%s -> \n%s", x.getKey(), groups);
            }).collect(Collectors.joining("\n\n"));
            System.out.println(print);

            System.out.println();
            System.out.println(clusterContainerGroups.keySet());


        } catch (TencentCloudSDKException e) {
            System.out.println(e.toString());
        }

    }

    private static String cpuFormat(String str) {
        return format(str, CPU_FORMAT);
    }

    private static String memFormat(String str) {
        return format(str, MEM_FORMAT);
    }

    private static String format(String str, DecimalFormat cpuFormat) {
        if (str == null || str.trim().isEmpty()) return str;
        Double aDouble = null;
        try {
            aDouble = new Double(str);
        } catch (NumberFormatException e) {
            System.out.println(str + " is not a number !!!");
            return str;
        }
        return cpuFormat.format(aDouble);
    }


}