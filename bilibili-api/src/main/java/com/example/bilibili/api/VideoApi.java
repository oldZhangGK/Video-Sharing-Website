package com.example.bilibili.api;

import com.example.bilibili.api.support.UserSupport;
import com.example.bilibili.domain.*;
import com.example.bilibili.service.ElasticSearchService;
import com.example.bilibili.service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.bilibili.service.ElasticSearchService;
import org.apache.mahout.cf.taste.common.TasteException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class VideoApi {

    @Autowired
    private VideoService videoService;

    @Autowired
    private UserSupport userSupport;

    @Autowired
    private ElasticSearchService elasticSearchService;

    /**
     * Video upload
     */
    @PostMapping("/videos")
    public JsonResponse<String> addVideos(@RequestBody Video video){
        Long userId = userSupport.getCurrentUserId();
        video.setUserId(userId);
        videoService.addVideos(video);
        //Add a video record in elastic search
        elasticSearchService.addVideo(video);
        return JsonResponse.success();
    }

    /**
     * Paginate the list of videos
     */
    @GetMapping("/videos")
    public JsonResponse<PageResult<Video>> pageListVideos(@RequestParam Integer size,
                                                          @RequestParam Integer no,
                                                          String area){
        PageResult<Video> result = videoService.pageListVideos(size, no ,area);
        return new JsonResponse<>(result);
    }
    /**
     * Video live play
     */
    @GetMapping("/video-slices")
    public void viewVideoOnlineBySlices(HttpServletRequest request,
                                        HttpServletResponse response,
                                        String url) {
        videoService.viewVideoOnlineBySlices(request, response, url);
    }

    /**
     * Like the video
     */
    @PostMapping("/video-likes")
    public JsonResponse<String> addVideoLike(@RequestParam Long videoId){
        Long userId = userSupport.getCurrentUserId();
        videoService.addVideoLike(videoId, userId);
        return JsonResponse.success();
    }

    /**
     * Unlike the video
     */
    @DeleteMapping("/video-likes")
    public JsonResponse<String> deleteVideoLike(@RequestParam Long videoId){
        Long userId = userSupport.getCurrentUserId();
        videoService.deleteVideoLike(videoId, userId);
        return JsonResponse.success();
    }

    /**
     * Query the number of likes for a video
     * No need for login as visitors can also watch videos
     */
    @GetMapping("/video-likes")
    public JsonResponse<Map<String, Object>> getVideoLikes(@RequestParam Long videoId){
        Long userId = null;
        try {
            userId = userSupport.getCurrentUserId();
        } catch (Exception ignored){}
        Map<String, Object> result = videoService.getVideoLikes(videoId, userId);
        return new JsonResponse<>(result);
    }

    /**
     * Collect video
     */
    @PostMapping("/video-collections")
    public JsonResponse<String> addVideoCollection(@RequestBody VideoCollection videoCollection){
        Long userId = userSupport.getCurrentUserId();
        videoService.addVideoCollection(videoCollection, userId);
        return JsonResponse.success();
    }

    /**
     * Update video collection
     */
    @PutMapping("/video-collections")
    public JsonResponse<String> updateVideoCollection(@RequestBody VideoCollection videoCollection){
        Long userId = userSupport.getCurrentUserId();
        videoService.updateVideoCollection(videoCollection, userId);
        return JsonResponse.success();
    }

    /**
     * Cancel video collection
     */
    @DeleteMapping("/video-collections")
    public JsonResponse<String> deleteVideoCollection(@RequestParam Long videoId){
        Long userId = userSupport.getCurrentUserId();
        videoService.deleteVideoCollection(videoId, userId);
        return JsonResponse.success();
    }

    /**
     * Query the number of collections for a video
     */
    @GetMapping("/video-collections")
    public JsonResponse<Map<String, Object>> getVideoCollections(@RequestParam Long videoId){
        Long userId = null;
        try{
            userId = userSupport.getCurrentUserId();
        }catch (Exception ignored){}
        Map<String, Object> result = videoService.getVideoCollections(videoId, userId);
        return new JsonResponse<>(result);
    }

    /**
     * Video coin deposit
     */
    @PostMapping("/video-coins")
    public JsonResponse<String> addVideoCoins(@RequestBody VideoCoin videoCoin){
        Long userId = userSupport.getCurrentUserId();
        videoService.addVideoCoins(videoCoin, userId);
        return JsonResponse.success();
    }

    /**
     * Query the number of coins for a video
     */
    @GetMapping("/video-coins")
    public JsonResponse<Map<String, Object>> getVideoCoins(@RequestParam Long videoId){
        Long userId = null;
        try{
            userId = userSupport.getCurrentUserId();
        }catch (Exception ignored){}
        Map<String, Object> result = videoService.getVideoCoins(videoId, userId);
        return new JsonResponse<>(result);
    }

    /**
     * Add video comment
     */
    @PostMapping("/video-comments")
    public JsonResponse<String> addVideoComment(@RequestBody VideoComment videoComment){
        Long userId = userSupport.getCurrentUserId();
        videoService.addVideoComment(videoComment, userId);
        return JsonResponse.success();
    }

    /**
     * Paginate the list of video comments
     */
    @GetMapping("/video-comments")
    public JsonResponse<PageResult<VideoComment>> pageListVideoComments(@RequestParam Integer size,
                                                                        @RequestParam Integer no,
                                                                        @RequestParam Long videoId){
        PageResult<VideoComment> result = videoService.pageListVideoComments(size, no, videoId);
        return new JsonResponse<>(result);
    }

    /**
     * Get video details
     */
    @GetMapping("/video-details")
    public JsonResponse<Map<String, Object>> getVideoDetails(@RequestParam Long videoId){
        Map<String, Object> result = videoService.getVideoDetails(videoId);
        return new JsonResponse<>(result);
    }

    /**
     * Add video view record
     */
    @PostMapping("/video-views")
    public JsonResponse<String> addVideoView(@RequestBody VideoView videoView,
                                             HttpServletRequest request){
        Long userId;
        try{
            userId = userSupport.getCurrentUserId();
            videoView.setUserId(userId);
            videoService.addVideoView(videoView, request);
        }catch (Exception e){
            videoService.addVideoView(videoView, request);
        }
        //Sync video view count to Elasticsearch
        elasticSearchService.updateVideoViewCount(videoView.getVideoId());
        return JsonResponse.success();
    }

    /**
     * Get video view count
     */
    @GetMapping("/video-view-counts")
    public JsonResponse<Integer> getVideoViewCounts(@RequestParam Long videoId){
        Integer count = videoService.getVideoViewCounts(videoId); //一个视频的总观看量
        return new JsonResponse<>(count);
    }

    /**
     * Video recommendation
     */
    @GetMapping("/recommendations")
    public JsonResponse<List<Video>> recommend() throws TasteException {
        Long userId = userSupport.getCurrentUserId();
        List<Video> list = videoService.recommend(userId);
        return new JsonResponse<>(list);
    }

    /**
     * Video Frame Capture and Black-and-White Silhouette Generation
     */
//    @GetMapping("/video-frames")
//    public JsonResponse<List<VideoBinaryPicture>> captureVideoFrame(@RequestParam Long videoId,
//                                                                    @RequestParam String fileMd5) throws Exception {
//        List<VideoBinaryPicture> list = videoService.convertVideoToImage(videoId, fileMd5);
//        return new JsonResponse<>(list);
//    }
//
//    /**
//     * 查询视频黑白剪影
//     */
//    @GetMapping("/video-binary-images")
//    public JsonResponse<List<VideoBinaryPicture>> getVideoBinaryImages(@RequestParam Long videoId,
//                                                                       Long videoTimestamp,
//                                                                       String frameNo) {
//        Map<String, Object> params = new HashMap<>();
//        params.put("videoId", videoId);
//        params.put("videoTimestamp", videoTimestamp);
//        params.put("frameNo", frameNo);
//        List<VideoBinaryPicture> list = videoService.getVideoBinaryImages(params);
//        return new JsonResponse<>(list);
//    }
//
//    /**
//     * Search video tags
//     */
//    @GetMapping("/video-tags")
//    public JsonResponse<List<VideoTag>> getVideoTagsByVideoId(@RequestParam Long videoId) {
//        List<VideoTag> list = videoService.getVideoTagsByVideoId(videoId);
//        return new JsonResponse<>(list);
//    }
//
//    /**
//     * Delete video tags
//     */
//    @DeleteMapping("/video-tags")
//    public JsonResponse<String> deleteVideoTags(@RequestBody JSONObject params) {
//        String tagIdList = params.getString("tagIdList");
//        Long videoId = params.getLong("videoId");
//        videoService.deleteVideoTags(JSONArray.parseArray(tagIdList).toJavaList(Long.class), videoId);
//        return JsonResponse.success();
//    }

    /**
     * Video recommendation (visitor)
     */
    @GetMapping("/visitor-video-recommendations")
    public JsonResponse<List<Video>> getVisitorVideoRecommendations() {
        List<Video> list = videoService.getVisitorVideoRecommendations();
        return new JsonResponse<>(list);
    }

    /**
     * Video recommendation (combined)
     */
    @GetMapping("/video-recommendations")
    public JsonResponse<List<Video>> getVideoRecommendations(@RequestParam String recommendType) {
        Long userId = userSupport.getCurrentUserId();
        List<Video> list = videoService.getVideoRecommendations(recommendType, userId);
        return new JsonResponse<>(list);
    }


}
