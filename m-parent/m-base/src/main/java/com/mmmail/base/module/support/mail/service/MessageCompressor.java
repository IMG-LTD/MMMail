package com.mmmail.base.module.support.mail.service;

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.MessagingException;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Component
public class MessageCompressor {
    private static final int BUFFER_SIZE = 8192;

    /**
     * 压缩邮件消息
     * Compress the mail message
     */
    public byte[] compressMessage(Message message) throws MessagingException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (BZip2CompressorOutputStream bzOut = new BZip2CompressorOutputStream(baos)) {
            message.writeTo(bzOut);
        }
        logger.info("压缩邮件消息成功 | Mail message compressed successfully");
        return baos.toByteArray();
    }

    /**
     * 解压邮件消息
     * Decompress the mail message
     */
    public Message decompressMessage(byte[] compressedData, Session session) throws IOException, MessagingException {
        try (InputStream is = new ByteArrayInputStream(compressedData);
             BZip2CompressorInputStream bzIn = new BZip2CompressorInputStream(is)) {
            logger.info("解压邮件消息成功 | Mail message decompressed successfully");
            return new MimeMessage(session, bzIn);
        }
    }

    /**
     * 使用GZIP压缩字节数组
     * Compress byte array using GZIP
     */
    public byte[] compress(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gos = new GZIPOutputStream(baos)) {
            gos.write(data);
        }
        logger.info("GZIP压缩数据成功 | GZIP data compressed successfully");
        return baos.toByteArray();
    }

    /**
     * 使用GZIP解压字节数组
     * Decompress byte array using GZIP
     */
    public byte[] decompress(byte[] compressedData) throws IOException {
        try (InputStream is = new ByteArrayInputStream(compressedData);
             GZIPInputStream gis = new GZIPInputStream(is)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = gis.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }
            logger.info("GZIP解压数据成功 | GZIP data decompressed successfully");
            return baos.toByteArray();
        }
    }

    /**
     * 获取压缩比
     * Get compression ratio
     */
    public double getCompressionRatio(byte[] original, byte[] compressed) {
        if (original.length == 0) return 0.0;
        logger.info("计算压缩比 | Compression ratio calculated");
        return ((double) compressed.length / original.length) * 100;
    }

    private static final org.slf4j.Logger logger = 
        org.slf4j.LoggerFactory.getLogger(MessageCompressor.class);
}
