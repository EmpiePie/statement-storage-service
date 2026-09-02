package za.co.statements.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.List;

/**
 * S3-backed storage that talks to MinIO via the AWS S3 API.
 * Active in every profile except {@code test}.
 */
@Service
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class S3StorageService implements StorageService {

    private final S3Client s3Client;

    @Value("${storage.s3.bucket}")
    private String bucket;

    /** Create the bucket on startup if it does not already exist. */
    @PostConstruct
    void ensureBucketExists() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            log.info("S3 bucket '{}' already exists", bucket);
        } catch (NoSuchBucketException e) {
            log.info("S3 bucket '{}' not found, creating it", bucket);
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                log.info("S3 bucket '{}' not found (404), creating it", bucket);
                s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            } else {
                throw e;
            }
        }
    }

    @Override
    public void upload(final String path, final byte[] content) {
        s3Client.putObject(
                PutObjectRequest.builder().bucket(bucket).key(path).build(),
                RequestBody.fromBytes(content)
        );
        log.info("Uploaded object to s3://{}/{} ({} bytes)", bucket, path, content.length);
    }

    @Override
    public byte[] read(final String path) {
        ResponseBytes<GetObjectResponse> object = s3Client.getObjectAsBytes(
                GetObjectRequest.builder().bucket(bucket).key(path).build()
        );
        return object.asByteArray();
    }

    @Override
    public boolean exists(final String path) {
        try {
            s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(path).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw e;
        }
    }

    @Override
    public List<String> list(final String prefix) {
        return s3Client.listObjectsV2(
                        ListObjectsV2Request.builder().bucket(bucket).prefix(prefix).build())
                .contents()
                .stream()
                .map(software.amazon.awssdk.services.s3.model.S3Object::key)
                .toList();
    }
}
