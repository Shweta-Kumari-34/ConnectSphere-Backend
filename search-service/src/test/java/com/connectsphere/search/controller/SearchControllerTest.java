package com.connectsphere.search.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import com.connectsphere.search.entity.Hashtag;
import com.connectsphere.search.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class SearchControllerTest {

    @Mock
    private SearchService searchService;

    @InjectMocks
    private SearchController searchController;

    private MockMvc mockMvc;
    private Hashtag testHashtag;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(searchController).build();
        testHashtag = new Hashtag();
        testHashtag.setId(1L);
        testHashtag.setTag("connectsphere");
        testHashtag.setPostId(10L);
    }

    @Test
    @DisplayName("POST /search/index — should index post with content")
    void indexPost_WithContent() throws Exception {
        doNothing().when(searchService).indexPost(eq(10L), eq("Hello #connectsphere"));

        mockMvc.perform(post("/search/index")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"postId\":10,\"content\":\"Hello #connectsphere\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Indexed successfully"));
    }

    @Test
    @DisplayName("POST /search/index — should index specific tag when no content")
    void indexPost_WithTag() throws Exception {
        when(searchService.indexHashtag("java", 10L)).thenReturn(testHashtag);

        mockMvc.perform(post("/search/index")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"postId\":10,\"tag\":\"java\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Indexed successfully"));
    }

    @Test
    @DisplayName("DELETE /search/index/{postId} — should remove post index")
    void removePostIndex_Success() throws Exception {
        doNothing().when(searchService).removePostIndex(10L);

        mockMvc.perform(delete("/search/index/10"))
                .andExpect(status().isOk())
                .andExpect(content().string("Post index removed"));
    }

    @Test
    @DisplayName("GET /search/posts — should return matching post IDs")
    void searchPosts_Success() throws Exception {
        when(searchService.searchPosts("connectsphere")).thenReturn(List.of(10L, 20L));

        mockMvc.perform(get("/search/posts").param("q", "connectsphere"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /search/users — should return matching user emails")
    void searchUsers_Success() throws Exception {
        when(searchService.searchUsers("alice")).thenReturn(List.of("alice@gmail.com"));

        mockMvc.perform(get("/search/users").param("q", "alice"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /search/hashtags/post/{postId} — should return hashtags for post")
    void getHashtagsForPost_Success() throws Exception {
        when(searchService.getHashtagsForPost(10L)).thenReturn(List.of(testHashtag));

        mockMvc.perform(get("/search/hashtags/post/10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /search/trending — should return trending hashtags")
    void getTrending_Success() throws Exception {
        when(searchService.getTrendingHashtags(10)).thenReturn(List.of());

        mockMvc.perform(get("/search/trending").param("limit", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /search/posts-by-hashtag — should return post IDs for hashtag")
    void getPostsByHashtag_Success() throws Exception {
        when(searchService.getPostsByHashtag("connectsphere")).thenReturn(List.of(10L));

        mockMvc.perform(get("/search/posts-by-hashtag").param("tag", "connectsphere"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /search/hashtags — should search hashtags by keyword")
    void searchHashtags_Success() throws Exception {
        when(searchService.searchHashtags("connect")).thenReturn(List.of(testHashtag));

        mockMvc.perform(get("/search/hashtags").param("q", "connect"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /search/count — should return hashtag usage count")
    void getCount_Success() throws Exception {
        when(searchService.getHashtagCount("connectsphere")).thenReturn(42L);

        mockMvc.perform(get("/search/count").param("tag", "connectsphere"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /search/test — should return health check")
    void testEndpoint() throws Exception {
        mockMvc.perform(get("/search/test"))
                .andExpect(status().isOk())
                .andExpect(content().string("Search Service is running!"));
    }
}
