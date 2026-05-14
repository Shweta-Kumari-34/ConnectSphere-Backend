package com.connectsphere.search.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.connectsphere.search.entity.Hashtag;
import com.connectsphere.search.repository.HashtagRepository;
import com.connectsphere.search.service.impl.SearchServiceImpl;

@ExtendWith(MockitoExtension.class)
class SearchServiceImplTest {

    @Mock private HashtagRepository hashtagRepository;
    @InjectMocks private SearchServiceImpl searchService;

    private Hashtag testHashtag;

    @BeforeEach
    void setUp() {
        testHashtag = new Hashtag();
        testHashtag.setId(1L);
        testHashtag.setTag("java");
        testHashtag.setPostId(10L);
    }

    @Test
    @DisplayName("IndexHashtag - should save lowercase tag")
    void indexHashtag_Success() {
        when(hashtagRepository.save(any(Hashtag.class))).thenReturn(testHashtag);
        Hashtag result = searchService.indexHashtag("JAVA", 10L);
        assertNotNull(result);
        verify(hashtagRepository).save(any(Hashtag.class));
    }

    @Test
    @DisplayName("SearchByTag - should return matching hashtags")
    void searchByTag_Success() {
        when(hashtagRepository.findByTagContainingIgnoreCase("java")).thenReturn(List.of(testHashtag));
        List<Hashtag> result = searchService.searchByTag("java");
        assertEquals(1, result.size());
        assertEquals("java", result.get(0).getTag());
    }

    @Test
    @DisplayName("GetHashtagsForPost - should return hashtags for post")
    void getHashtagsForPost_Success() {
        when(hashtagRepository.findByPostId(10L)).thenReturn(List.of(testHashtag));
        assertEquals(1, searchService.getHashtagsForPost(10L).size());
    }

    @Test
    @DisplayName("RemovePostIndex - should delete all hashtags for post")
    void removePostIndex_Success() {
        when(hashtagRepository.findByPostId(10L)).thenReturn(List.of(testHashtag));
        searchService.removePostIndex(10L);
        verify(hashtagRepository).deleteAll(List.of(testHashtag));
    }

    @Test
    @DisplayName("GetTrendingHashtags - should return limited results")
    void getTrending_Success() {
        Object[] trend1 = new Object[]{"java", 50L};
        Object[] trend2 = new Object[]{"spring", 30L};
        when(hashtagRepository.findTrendingHashtags()).thenReturn(List.of(trend1, trend2));
        List<Object[]> result = searchService.getTrendingHashtags(1);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("GetHashtagCount - should return count")
    void getHashtagCount_Success() {
        when(hashtagRepository.countByTagIgnoreCase("java")).thenReturn(1L);
        assertEquals(1, searchService.getHashtagCount("java"));
    }
}
