package com.example.bilibili.service.util;

import com.github.tobato.fastdfs.domain.conn.TrackerConnectionManager;
import com.github.tobato.fastdfs.domain.fdfs.StorageNode;
import com.github.tobato.fastdfs.domain.fdfs.StorageNodeInfo;
import com.github.tobato.fastdfs.service.DefaultTrackerClient;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

//Fixed the problem that new version of fastFDFS is retrieving Storage port as 0
@Primary
@Component("customizeTrackerClient")
public class CustomizeTrackerClient extends DefaultTrackerClient {

    private static final int DEFAULT_PORT = 23000;

    public CustomizeTrackerClient(TrackerConnectionManager trackerConnectionManager) {
        super();
    }

    public StorageNode getStoreStorage() {
        StorageNode res = super.getStoreStorage();
        res.setPort(getPort(res.getPort()));
        return res;
    }

    public StorageNode getStoreStorage(String groupName) {
        StorageNode res = super.getStoreStorage(groupName);
        res.setPort(getPort(res.getPort()));
        return res;
    }

    public StorageNodeInfo getFetchStorage(String groupName, String filename) {
        StorageNodeInfo res = super.getFetchStorage(groupName, filename);
        res.setPort(getPort(res.getPort()));
        return res;
    }

    public StorageNodeInfo getUpdateStorage(String groupName, String filename) {
        StorageNodeInfo res = super.getUpdateStorage(groupName, filename);
        res.setPort(getPort(res.getPort()));
        return res;
    }

    private int getPort(int port){
        if(port == 0){
            return DEFAULT_PORT;
        }
        return port;
    }
}
