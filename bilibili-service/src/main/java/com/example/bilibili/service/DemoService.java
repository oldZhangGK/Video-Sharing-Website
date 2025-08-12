package com.example.bilibili.service;
import com.example.bilibili.DemoDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class DemoService {
    @Autowired
    // help to add dependency relations
    private DemoDao demoDao;

    public Map<String, Object> query(Long id){
        return demoDao.query(id);
    }
}
