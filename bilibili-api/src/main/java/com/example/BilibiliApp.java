package com.example;
import com.github.tobato.fastdfs.FdfsClientConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(FdfsClientConfig.class)
public class BilibiliApp {
    public static void main(String[] args) {
        ApplicationContext app = SpringApplication.run(BilibiliApp.class, args);
    }
}