package com.example.bilibili.service;

import com.example.bilibili.dao.repository.UserInfoRepository;
import com.example.bilibili.dao.repository.VideoRepository;
import com.example.bilibili.domain.UserInfo;
import com.example.bilibili.domain.Video;
//import com.example.bilibili.domain.constant.SearchConstant;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.core.type.TypeReference;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;
import co.elastic.clients.elasticsearch.core.search.SourceFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ElasticSearchService {

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private UserInfoRepository userInfoRepository;

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    public void addUserInfo(UserInfo userInfo) {
        userInfoRepository.save(userInfo);
    }

    public void addVideo(Video video) {
        videoRepository.save(video);
    }

    public List<Map<String, Object>> getContents(String keyword, Integer pageNo, Integer pageSize) throws IOException {
        // 搜索的数据种类，种类在dao.domain里定义
        String[] indices = {"videos", "user-infos"};

        // Build the search request
        SearchRequest request = SearchRequest.of(s -> s
                .index(Arrays.asList(indices)) // Indices to search
                .from((pageNo - 1) * pageSize) // Pagination: calculate from
                .size(pageSize) // Pagination: size per page
                .query(q -> q
                        .multiMatch(m -> m
                                .query(keyword)
                                .fields("title", "nick", "description") // Fields to match on, title来自Video, nick, description来自UserInfo
                        )
                )
                .highlight(h -> h
                        .fields("title", f -> f) // Highlight these fields
                        .fields("nick", f -> f)
                        .fields("description", f -> f)
                        .preTags("<span style=\"color:red\">") // Pre-highlight tag 高亮显示
                        .postTags("</span>") // Post-highlight tag
                )
                .timeout("60s")  // Set timeout
        );

        // Execute the search request
        SearchResponse<Map> response = elasticsearchClient.search(request, Map.class); // Process the search hits and handle highlighting

        // Process the search hits and handle highlighting
        List<Map<String, Object>> results = new ArrayList<>();
        //为什么response.hits().hits()有两个hits()? 在ES里，第一次hits()执行完后只能找到所有的hits(),再执行一次hits()才能找到每一个hit
        for (Hit<Map> hit : response.hits().hits()) {
            Map source = hit.source(); // Generic Map type
            if (source != null) {
                // Convert source to Map<String, Object> for consistency
                Map<String, Object> sourceMap = new HashMap<>();
                source.forEach((key, value) -> sourceMap.put(String.valueOf(key), value));

                // Check and handle highlighting
                Map<String, List<String>> highlights = hit.highlight();
                if (highlights != null) {
                    highlights.forEach((field, values) -> {
                        if (values != null && !values.isEmpty()) {
                            sourceMap.put(field, String.join(", ", values));
                        }
                    });
                }

                results.add(sourceMap);
            }
        }

        return results;
    }


    public Video getVideos(String keyword) {
        return videoRepository.findByTitleLike(keyword);
    }

    public void deleteAllVideos() {
        videoRepository.deleteAll();
    }

    public long countVideoBySearchTxt(String searchTxt) {
        return this.videoRepository.countByTitleOrDescription(searchTxt, searchTxt);
    }

    public long countUserBySearchTxt(String searchTxt) {
        return this.userInfoRepository.countByNick(searchTxt);
    }

    public void updateVideoViewCount(Long videoId) {
        Optional<Video> videoOpt = videoRepository.findById(videoId);
        videoOpt.ifPresent(video -> {
            int viewCount = video.getViewCount() == null ? 0 : video.getViewCount();
            video.setViewCount(viewCount + 1);
            videoRepository.save(video);
        });
    }
}