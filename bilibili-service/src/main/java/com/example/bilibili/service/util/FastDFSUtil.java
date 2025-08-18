package com.example.bilibili.service.util;

import com.github.tobato.fastdfs.domain.fdfs.FileInfo;
import com.github.tobato.fastdfs.domain.fdfs.MetaData;
import com.github.tobato.fastdfs.domain.fdfs.StorePath;
import com.github.tobato.fastdfs.domain.proto.storage.DownloadCallback;
import com.github.tobato.fastdfs.service.AppendFileStorageClient;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import com.example.bilibili.domain.exception.ConditionException;
import io.netty.util.internal.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;

@Component
public class FastDFSUtil {

    @Autowired
    private FastFileStorageClient fastFileStorageClient; //很快速地存中小型文件，但大文件很棘手，如果断掉了没传完前面的进度消失

    @Autowired
    private AppendFileStorageClient appendFileStorageClient; //实现断点传输，如果中间断掉了，没关系可以从断点接着跑，我们可以把原来很大的文件切成很小的部分

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String PATH_KEY = "path-key:";

    private static final String UPLOADED_SIZE_KEY = "uploaded-size-key:";

    private static final String UPLOADED_NO_KEY = "uploaded-no-key:";

    private static final String DEFAULT_GROUP = "group1";

    private static final int SLICE_SIZE = 1024 * 1024 * 2; //2MB的切片

    private static final Logger log = LoggerFactory.getLogger(FastDFSUtil.class);


    @Value("${fdfs.http.storage-addr}")
    private String httpFdfsStorageAddr;

    //检测传输的文件的类型。MultipartFile是SPRINGBOOT提供的，是一种上传文件的类型，SPRINGBOOT的惯例，真正的往数据库里存的则是普通FILE
    //所以我们上传的时候会传MultipartFile作为临时的，然后再把他转化成普通的FILE存进数据库
    public String getFileType(MultipartFile file){
        if (file == null) {
            throw new ConditionException("Illegal file！"); //Exception不能写太长太详细预防黑客
        }
        String fileName = file.getOriginalFilename(); //getOriginalFilename()读取原本的文件名，里面一定会带个点，点后面的就是文件的TYPE
        int index = fileName.lastIndexOf(".");
        return fileName.substring(index+1);
    }

    //断点续传，这个方法上传大文件
    // Upload files with support for breakpoint resume
    public String uploadAppenderFile(MultipartFile file) throws Exception {
        String fileType = this.getFileType(file);
        try (InputStream inputStream = file.getInputStream()) {
            StorePath storePath = appendFileStorageClient.uploadAppenderFile(DEFAULT_GROUP, inputStream, file.getSize(), fileType);
            return storePath.getPath();
        }
    }

    //如果出了故障或者东西没传完停下来了，我们可以调用这个方法续上
    // Modify appender file
    public void modifyAppenderFile(MultipartFile file, String filePath, long offset) throws Exception {
        try (InputStream inputStream = file.getInputStream()) {
            appendFileStorageClient.modifyFile(DEFAULT_GROUP, filePath, inputStream, file.getSize(), offset);
        }
    }

    //POSTMAN我们给他传的东西有：文件本身，MD5字符串文件标识符通关他能知道这个文件长怎么样，SliceNo是指的是上传的第几个文件，totalSliceNO一共要传几个文件
    // Upload file by slices
    public String uploadFileBySlices(MultipartFile file, String fileMd5, Integer sliceNo, Integer totalSliceNo) throws Exception {
        if (file == null || sliceNo == null || totalSliceNo == null) {
            throw new ConditionException("Parameter exception!");
        }

        //
        String pathKey = PATH_KEY + fileMd5;
        String uploadedSizeKey = UPLOADED_SIZE_KEY + fileMd5;
        String uploadedNoKey = UPLOADED_NO_KEY + fileMd5;
        String lockKey = "LOCK:" + fileMd5;

        //调用REDIS缓存去一点点存
        // Attempt to get the lock
        boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "LOCK", Duration.ofSeconds(10));
        if (!lockAcquired) {
            throw new ConditionException("Another upload is in progress. Please try again later.");
        }

        try {
            Long uploadedSize = 0L;
            String uploadedSizeStr = redisTemplate.opsForValue().get(uploadedSizeKey);
            if (!StringUtil.isNullOrEmpty(uploadedSizeStr)) {
                uploadedSize = Long.valueOf(uploadedSizeStr);
            }

            log.info("Uploading slice {} of {}, uploadedSize: {}", sliceNo, totalSliceNo, uploadedSize);

            //如果上传的是第一片，我们需要设置他的路径
            if (sliceNo == 1) {
                String path = this.uploadAppenderFile(file);
                if (StringUtil.isNullOrEmpty(path)) {
                    throw new ConditionException("Upload failed!");
                }
                redisTemplate.opsForValue().set(pathKey, path);
                redisTemplate.opsForValue().set(uploadedNoKey, "1");
            } else {
                String filePath = redisTemplate.opsForValue().get(pathKey);
                if (StringUtil.isNullOrEmpty(filePath)) {
                    throw new ConditionException("Upload failed! Missing file path.");
                }
                this.modifyAppenderFile(file, filePath, uploadedSize);
                redisTemplate.opsForValue().increment(uploadedNoKey);
            }

            // Update uploaded file size
            uploadedSize += file.getSize();
            redisTemplate.opsForValue().set(uploadedSizeKey, String.valueOf(uploadedSize));

            // Check video upload success
            String uploadedNoStr = redisTemplate.opsForValue().get(uploadedNoKey);
            Integer uploadedNo = Integer.valueOf(uploadedNoStr);

            //uploadedNo.equals(totalSliceNo)说明传完了，该清理REDIS里的内存
            if (uploadedNo.equals(totalSliceNo)) {
                log.info("All slices uploaded successfully. Cleaning up Redis keys.");
                String resultPath = redisTemplate.opsForValue().get(pathKey);
                List<String> keyList = Arrays.asList(uploadedNoKey, pathKey, uploadedSizeKey, lockKey);
                redisTemplate.delete(keyList);
                return resultPath;
            }

            return "";

        } catch (Exception e) {
            log.error("Upload slice failed: {}", e.getMessage(), e);
            // If upload failed, clean redis
            List<String> cleanupKeys = Arrays.asList(pathKey, uploadedSizeKey, uploadedNoKey, lockKey);
            redisTemplate.delete(cleanupKeys);
            throw e;
        } finally {
            // Release lock
            redisTemplate.delete(lockKey);
        }
    }


    //把大的文件拆成小的方便APPEND的SLICE
    public void convertFileToSlices(MultipartFile multipartFile) throws Exception{
        String fileType = this.getFileType(multipartFile);
        //Generate a temporary file and convert MultipartFile to File
        File file = this.multipartFileToFile(multipartFile);
        long fileLength = file.length();
        int count = 1;
        for (int i = 0; i < fileLength; i += SLICE_SIZE) {
            //RandomAccessFile()是SPRINGBOOT提供的方法，就是说这种CLASS的文件是可以从任何一个部分进行访问的，正常文件只能从头访问，等于我们方便从二进制流去切分
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
            randomAccessFile.seek(i);
            byte[] bytes = new byte[SLICE_SIZE];
            int len = randomAccessFile.read(bytes);

            String userHome = System.getProperty("user.home");
            Path dir  = Paths.get(userHome, "tmpfile");
            String fileName = count + "." + fileType;
            Path filePath  = dir.resolve(fileName);
            File slice = filePath.toFile();

            FileOutputStream fos = new FileOutputStream(slice);
            fos.write(bytes, 0, len);
            fos.close();
            randomAccessFile.close();
            count++;
        }
        //Delete temp files
        file.delete();
    }

    //把MultipartFile传唤成正常的FILE
    public File multipartFileToFile(MultipartFile multipartFile) throws Exception{
        String originalFileName = multipartFile.getOriginalFilename();
        String[] fileName = originalFileName.split("\\.");
        File file = File.createTempFile(fileName[0], "." + fileName[1]);
        multipartFile.transferTo(file);
        return file;
    }

    //Delete file
    public void deleteFile(String filePath){
        fastFileStorageClient.deleteFile(filePath);
    }



    public void viewVideoOnlineBySlices(HttpServletRequest request,
                                        HttpServletResponse response,
                                        String path) throws Exception{
        FileInfo fileInfo = fastFileStorageClient.queryFileInfo(DEFAULT_GROUP, path);
        long totalFileSize = fileInfo.getFileSize();
        String url = httpFdfsStorageAddr + path;
        Enumeration<String> headerNames = request.getHeaderNames();
        Map<String, Object> headers = new HashMap<>();
        while (headerNames.hasMoreElements()) {
            String header = headerNames.nextElement();
            headers.put(header, request.getHeader(header));
        }
        String rangeStr = request.getHeader("Range");
        String[] range;
        if (StringUtil.isNullOrEmpty(rangeStr)) {
            rangeStr = "bytes=0-" + (totalFileSize-1);
        }
        range = rangeStr.split("bytes=|-");
        long begin = 0;
        if (range.length >= 2) {
            begin = Long.parseLong(range[1]);
        }
        long end = totalFileSize - 1;
        if (range.length >= 3) {
            end = Long.parseLong(range[2]);
        }
        long len = (end - begin) + 1;
        String contentRange = "bytes " + begin + "-" + end + "/" + totalFileSize;
        response.setHeader("Content-Range", contentRange);
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("Content-Type", "video/mp4");
        response.setContentLength((int)len);
        response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        HttpUtil.get(url, headers, response);
    }

    // Download file
    public void downLoadFile(String url, String localPath) {
        fastFileStorageClient.downloadFile(DEFAULT_GROUP, url,
                new DownloadCallback<String>() {
                    @Override
                    public String recv(InputStream ins) throws IOException {
                        File file = new File(localPath);
                        OutputStream os = new FileOutputStream(file);
                        int len = 0;
                        byte[] buffer = new byte[1024];
                        while ((len = ins.read(buffer)) != -1) {
                            os.write(buffer, 0, len);
                        }
                        os.close();
                        ins.close();
                        return "success";
                    }
                });
    }
}