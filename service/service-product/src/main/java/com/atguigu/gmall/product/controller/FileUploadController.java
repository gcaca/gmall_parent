package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import org.apache.commons.io.FilenameUtils;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient1;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author ca ca
 * @version 1.0
 * @see
 */
@RestController
@RequestMapping("admin/product")
public class FileUploadController {

    @Value("${fileServer.url}")
    private String fileUrl;

    @PostMapping("fileUpload")
    public Result fileUpload(MultipartFile file) throws Exception{
        String configFile = this.getClass().getResource("/tracker.conf").getFile();
        String path = null;
        if (configFile != null) {
            //初始化
            ClientGlobal.init(configFile);
            //创建 trackerClient
            TrackerClient trackerClient = new TrackerClient();
            //获取 服务端
            TrackerServer trackerServer = trackerClient.getConnection();
            //创建 storageClient1
            StorageClient1 storageClient1 = new StorageClient1(trackerServer, null);
            //上传文件
            path = storageClient1.upload_appender_file1(file.getBytes(), FilenameUtils.getExtension(file.getOriginalFilename()), null);
        }
        //返回完整图片路径
        return Result.ok(fileUrl + path);
    }
}
