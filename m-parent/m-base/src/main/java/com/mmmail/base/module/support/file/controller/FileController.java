package com.mmmail.base.module.support.file.controller;

import cn.hutool.extra.servlet.JakartaServletUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.mmmail.base.common.constant.RequestHeaderConst;
import com.mmmail.base.common.controller.SupportBaseController;
import com.mmmail.base.common.domain.RequestUser;
import com.mmmail.base.common.domain.ResponseDTO;
import com.mmmail.base.common.util.SmartRequestUtil;
import com.mmmail.base.common.util.SmartResponseUtil;
import com.mmmail.base.constant.SwaggerTagConst;
import com.mmmail.base.module.support.file.domain.vo.FileDownloadVO;
import com.mmmail.base.module.support.file.domain.vo.FileUploadVO;
import com.mmmail.base.module.support.file.service.FileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 文件服务
 *
 * @Author 1024创新实验室: 罗伊
 * @Date 2019年10月11日 15:34:47
 * @Wechat zhuoda1024
 * @Email lab1024@163.com
 * @Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@RestController
@Tag(name = SwaggerTagConst.Support.FILE)
public class FileController extends SupportBaseController {

    @Resource
    private FileService fileService;


    @Operation(summary = "文件上传 @author 胡克")
    @PostMapping("/file/upload")
    public ResponseDTO<FileUploadVO> upload(@RequestParam MultipartFile file, @RequestParam Integer folder) {
        RequestUser requestUser = SmartRequestUtil.getRequestUser();
        return fileService.fileUpload(file, folder, requestUser);
    }

    @Operation(summary = "获取文件URL：根据fileKey @author 胡克")
    @GetMapping("/file/getFileUrl")
    public ResponseDTO<String> getUrl(@RequestParam String fileKey) {
        return fileService.getFileUrl(fileKey);
    }

    @Operation(summary = "下载文件流（根据fileKey） @author 胡克")
    @GetMapping("/file/downLoad")
    public void downLoad(@RequestParam String fileKey, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String userAgent = JakartaServletUtil.getHeaderIgnoreCase(request, RequestHeaderConst.USER_AGENT);
        ResponseDTO<FileDownloadVO> downloadFileResult = fileService.getDownloadFile(fileKey, userAgent);
        if (!downloadFileResult.getOk()) {
            SmartResponseUtil.write(response, downloadFileResult);
            return;
        }
        // 下载文件信息
        FileDownloadVO fileDownloadVO = downloadFileResult.getData();
        // 设置下载消息头
        SmartResponseUtil.setDownloadFileHeader(response, fileDownloadVO.getMetadata().getFileName(), fileDownloadVO.getMetadata().getFileSize());
        // 下载
        response.getOutputStream().write(fileDownloadVO.getData());
    }
}
