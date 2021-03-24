import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;

import com.tencentcloudapi.tsf.v20180326.TsfClient;
import com.tencentcloudapi.tsf.v20180326.models.*;;import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DescribeGroup {
    public static void main(String[] args) {
        try {

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

            DescribeSimpleApplicationsResponse resp = client.DescribeSimpleApplications(req);

            System.out.println(DescribeSimpleApplicationsResponse.toJsonString(resp));

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

            String print = clusterContainerGroups.entrySet().stream().map(x -> {
                String groups = x.getValue().stream().sorted(
                        Comparator.comparing(ContainGroup::getCpuRequest).thenComparing(ContainGroup::getGroupName)
                ).map(y -> String.format("%s %-40s %10s %10s %10s %10s",
                        y.getGroupId(), y.getGroupName(),
                        y.getCpuRequest(), y.getCpuLimit(), y.getMemRequest(), y.getMemLimit())
                ).collect(Collectors.joining("\n"));
                return String.format("%s -> \n%s", x.getKey(), groups);
            }).collect(Collectors.joining("\n\n"));
            System.out.println(print);

            System.out.println(clusterContainerGroups.keySet());


        } catch (TencentCloudSDKException e) {
            System.out.println(e.toString());
        }

    }

}