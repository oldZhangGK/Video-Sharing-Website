package com.example.bilibili.service.util;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 * MD5加密
 * 单向加密算法，不可还原
 * 特点：加密速度快，不需要秘钥，但是安全性不高，需要搭配随机盐值使用
 *
 */
public class MD5Util {

    public static String sign(String content, String salt, String charset) {
        content = content + salt;
        return DigestUtils.md5Hex(getContentBytes(content, charset));
    }

    public static boolean verify(String content, String sign, String salt, String charset) {
        content = content + salt;
        String mysign = DigestUtils.md5Hex(getContentBytes(content, charset));
        return mysign.equals(sign);
    }

    private static byte[] getContentBytes(String content, String charset) {
        if (!"".equals(charset)) {
            try {
                return content.getBytes(charset);
            } catch (UnsupportedEncodingException var3) {
                throw new RuntimeException("MD5签名过程中出现错误,指定的编码集错误");
            }
        } else {
            return content.getBytes();
        }
    }

    //获取文件md5加密后的字符串
    public static String getFileMD5(MultipartFile file) throws Exception {
        //输入流生成
        InputStream fis = file.getInputStream();
        //把输出流变成了一个个BYTE
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int byteRead;

        //只要FILE不为空，就把INPUT导入到OUTPUT
        while((byteRead = fis.read(buffer)) > 0){
            baos.write(buffer, 0, byteRead);
        }
        fis.close();

        //将OUTPUT转化为2进制流，返回一个MD5加密的流
        return DigestUtils.md5Hex(baos.toByteArray());
    }
}