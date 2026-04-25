package com.sky.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import java.io.ByteArrayInputStream;
@Data
@AllArgsConstructor
@Slf4j
public class AliOssUtil {

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;

    public String upload(byte[] bytes, String objectName) {
        // 提取 region（如 ams3）
        String region = endpoint.replace("https://", "").split("\\.")[0];

        BasicAWSCredentials credentials =
                new BasicAWSCredentials(accessKeyId, accessKeySecret);

        AmazonS3 s3 = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(endpoint, region))
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withPathStyleAccessEnabled(true)
                .build();

        // 上传文件
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(bytes.length);
        s3.putObject(bucketName, objectName,
                new ByteArrayInputStream(bytes), metadata);

        // 设置文件公开访问
        s3.setObjectAcl(bucketName, objectName, CannedAccessControlList.PublicRead);

        // 返回文件访问地址
        String fileUrl = endpoint.replace("://", "://" + bucketName + ".")
                + "/" + objectName;
        log.info("文件上传成功，访问地址：{}", fileUrl);
        return fileUrl;
    }
}