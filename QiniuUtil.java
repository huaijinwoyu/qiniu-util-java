package com.monicax.util;

import com.monicax.config.QiniuConfig;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.storage.model.FileInfo;
import com.qiniu.storage.model.FileListing;
import com.qiniu.util.Auth;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by zhouchunjie on 2017/1/15.
 */
@Component
public class QiniuUtil {

    @Autowired
    private QiniuConfig qiniuConfig;

    private Auth auth;

    @Autowired
    public void setAuth() {
        this.auth = Auth.create(qiniuConfig.getAccessKey(), qiniuConfig.getSecretKey());
    }

    private Configuration configuration;

    @Autowired
    public void setConfiguration() {
        this.configuration = new Configuration(Zone.autoZone());
    }

    private BucketManager bucketManager;

    @Autowired
    public void setBucketManager() {
        this.bucketManager = new BucketManager(auth, configuration);
    }

    private UploadManager uploadManager;

    @Autowired
    public void setUploadManager() {
        this.uploadManager = new UploadManager(configuration);
    }

    private static int LIMIT_SIZE = 1000;

    public String getBucketHostName() {
        return qiniuConfig.getBucketHostName();
    }

    /**
     * 返回七牛账号的所有空间
     *
     * @return
     * @throws QiniuException
     */
    public String[] listBucket() throws QiniuException {
        return bucketManager.buckets();
    }

    /**
     * 获取指定空间下的文件列表
     *
     * @param bucketName
     * @param prefix
     * @param limit
     * @return
     */
    public List<FileInfo> listFileOfBucket(String bucketName, String prefix, int limit) {
        if (StringUtils.isBlank(bucketName)) {
            bucketName = qiniuConfig.getBucketName();
        }
        BucketManager.FileListIterator it = bucketManager.createFileListIterator(bucketName, prefix, limit, null);
        List<FileInfo> list = new ArrayList<>();
        while (it.hasNext()) {
            FileInfo[] items = it.next();
            if (null != items && items.length > 0) {
                list.addAll(Arrays.asList(items));
            }
        }
        return list;
    }

    /**
     * 七牛图片上传
     *
     * @param inputStream
     * @param bucketName
     * @param key
     * @param mimeType
     * @return
     * @throws Exception
     */
    public String uploadFile(InputStream inputStream, String bucketName, String key, String mimeType) throws IOException {
        String token = auth.uploadToken(bucketName);
        byte[] byteData = IOUtils.toByteArray(inputStream);
        Response response = uploadManager.put(byteData, key, token, null, mimeType, false);
        inputStream.close();
        return response.bodyString();
    }

    /**
     * 七牛图片上传
     *
     * @param inputStream
     * @param bucketName
     * @param key
     * @return
     * @throws IOException
     */
    public String uploadFile(InputStream inputStream, String bucketName, String key) throws IOException {
        return uploadFile(inputStream, bucketName, key, null);
    }

    /**
     * 七牛图片上传
     *
     * @param filePath
     * @param fileName
     * @param bucketName
     * @param key
     * @return
     * @throws IOException
     */
    public String uploadFile(String filePath, String fileName, String bucketName, String key) throws IOException {
        String token = auth.uploadToken(bucketName);
        InputStream is = new FileInputStream(new File(filePath + fileName));
        byte[] byteData = IOUtils.toByteArray(is);
        Response response = uploadManager.put(byteData, (StringUtils.isBlank(key)) ? fileName : key, token);
        is.close();
        return response.bodyString();
    }

    public String uploadFile(String filePath, String fileName, String bucketName) throws IOException {
        return uploadFile(filePath, fileName, bucketName, null);
    }

    /**
     * 提取网络资源并上传到七牛空间里
     *
     * @param url
     * @param bucketName
     * @param key
     * @return
     * @throws QiniuException
     */
    public String fetchToBucket(String url, String bucketName, String key) throws QiniuException {
        if (StringUtils.isBlank(bucketName)) {
            bucketName = qiniuConfig.getBucketName();
        }
        BucketManager bucketManager = new BucketManager(auth, configuration);
        DefaultPutRet putret = bucketManager.fetch(url, bucketName, key);
        return putret.key;
    }

    public String fetchToBucket(String url, String bucketName) throws QiniuException {
        if (StringUtils.isBlank(bucketName)) {
            bucketName = qiniuConfig.getBucketName();
        }
        BucketManager bucketManager = new BucketManager(auth, configuration);
        DefaultPutRet putret = bucketManager.fetch(url, bucketName);
        return putret.key;
    }

    /**
     * 七牛空间内文件复制
     *
     * @param bucket
     * @param key
     * @param targetBucket
     * @param targetKey
     * @throws QiniuException
     */
    public void copyFile(String bucket, String key, String targetBucket, String targetKey) throws QiniuException {
        bucketManager.copy(bucket, key, targetBucket, targetKey);
    }

    /**
     * 七牛空间内文件剪切
     *
     * @param bucket
     * @param key
     * @param targetBucket
     * @param targetKey
     * @throws QiniuException
     */
    public void moveFile(String bucket, String key, String targetBucket, String targetKey) throws QiniuException {
        bucketManager.move(bucket, key, targetBucket, targetKey);
    }

    /**
     * 七牛空间文件重命名
     *
     * @param bucket
     * @param key
     * @param targetKey
     * @throws QiniuException
     */
    public void renameFile(String bucket, String key, String targetKey) throws QiniuException {
        bucketManager.rename(bucket, key, targetKey);
    }

    /**
     * 七牛空间内文件删除
     *
     * @param bucket
     * @param key
     * @throws QiniuException
     */
    public void deleteFile(String bucket, String key) throws QiniuException {
        bucketManager.delete(bucket, key);
    }

    /**
     * 返回制定空间下的所有文件信息
     *
     * @param bucketName
     * @param prefix
     * @param limit
     * @return
     * @throws QiniuException
     */
    public FileInfo[] findFiles(String bucketName, String prefix, int limit) throws QiniuException {
        FileListing listing = bucketManager.listFiles(bucketName, prefix, null, limit, null);
        if (listing == null || listing.items == null || listing.items.length <= 0) {
            return null;
        }
        return listing.items;
    }

    public FileInfo[] findFiles(String bucketName) throws QiniuException {
        FileListing listing = bucketManager.listFiles(bucketName, null, null, LIMIT_SIZE, null);
        if (listing == null || listing.items == null || listing.items.length <= 0) {
            return null;
        }
        return listing.items;
    }

    /**
     * 返回制定空间下的某个文件
     *
     * @param bucketName
     * @param key
     * @param limit
     * @return
     * @throws QiniuException
     */
    public FileInfo findOneFile(String bucketName, String key, int limit) throws QiniuException {
        FileListing listing = bucketManager.listFiles(bucketName, key, null, limit, null);
        if (listing == null || listing.items == null || listing.items.length <= 0) {
            return null;
        }
        return (listing.items)[0];
    }

    public FileInfo findOneFile(String bucketName, String key) throws QiniuException {
        FileListing listing = bucketManager.listFiles(bucketName, key, null, LIMIT_SIZE, null);
        if (listing == null || listing.items == null || listing.items.length <= 0) {
            return null;
        }
        return (listing.items)[0];
    }

    /**
     * 返回七牛空间内指定文件的访问URL
     * @param key
     * @return
     * @throws QiniuException
     */
    public String getFileAccessUrl(String key) throws QiniuException {
        return qiniuConfig.getBucketHostName() + "/" + key;
    }
}
