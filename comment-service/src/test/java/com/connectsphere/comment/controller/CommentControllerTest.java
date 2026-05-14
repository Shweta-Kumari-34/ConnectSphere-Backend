package com.connectsphere.comment.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import com.connectsphere.comment.dto.CommentRequestDto;
import com.connectsphere.comment.entity.Comment;
import com.connectsphere.comment.service.CommentService;
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
class CommentControllerTest {

    @Mock
    private CommentService commentService;

    @InjectMocks
    private CommentController commentController;

    private MockMvc mockMvc;
    private Comment testComment;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(commentController).build();
        testComment = new Comment();
        testComment.setId(1L);
        testComment.setPostId(10L);
        testComment.setUserEmail("user@gmail.com");
        testComment.setContent("Great post!");
        testComment.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("POST /comments — should add comment and return 200")
    void addComment_Success() throws Exception {
        when(commentService.addComment(eq("user@gmail.com"), any(CommentRequestDto.class))).thenReturn(testComment);

        mockMvc.perform(post("/comments")
                        .header("X-User-Email", "user@gmail.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"postId\":10,\"content\":\"Great post!\"}"))
                .andExpect(status().isOk());

        verify(commentService).addComment(eq("user@gmail.com"), any(CommentRequestDto.class));
    }

    @Test
    @DisplayName("GET /comments/post/{postId} — should return comments for post")
    void getByPost_Success() throws Exception {
        when(commentService.getCommentsByPost(10L)).thenReturn(List.of(testComment));

        mockMvc.perform(get("/comments/post/10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /comments/{id} — should return comment by id")
    void getById_Success() throws Exception {
        when(commentService.getCommentById(1L)).thenReturn(testComment);

        mockMvc.perform(get("/comments/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /comments/replies/{parentId} — should return replies")
    void getReplies_Success() throws Exception {
        when(commentService.getReplies(1L)).thenReturn(List.of(testComment));

        mockMvc.perform(get("/comments/replies/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /comments/{id} — should update comment")
    void updateComment_Success() throws Exception {
        when(commentService.updateComment(eq(1L), eq("user@gmail.com"), anyString())).thenReturn(testComment);

        mockMvc.perform(put("/comments/1")
                        .header("X-User-Email", "user@gmail.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"Updated content\""))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /comments/{id} — should delete comment")
    void deleteComment_Success() throws Exception {
        doNothing().when(commentService).deleteComment(1L, "user@gmail.com");

        mockMvc.perform(delete("/comments/1")
                        .header("X-User-Email", "user@gmail.com"))
                .andExpect(status().isOk())
                .andExpect(content().string("Comment deleted"));
    }

    @Test
    @DisplayName("GET /comments/user/{userEmail} — should return user's comments")
    void getByUser_Success() throws Exception {
        when(commentService.getCommentsByUser("user@gmail.com")).thenReturn(List.of(testComment));

        mockMvc.perform(get("/comments/user/user@gmail.com"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /comments/{id}/like — should like comment")
    void likeComment_Success() throws Exception {
        doNothing().when(commentService).likeComment(1L);

        mockMvc.perform(post("/comments/1/like"))
                .andExpect(status().isOk())
                .andExpect(content().string("Comment liked"));
    }

    @Test
    @DisplayName("POST /comments/{id}/unlike — should unlike comment")
    void unlikeComment_Success() throws Exception {
        doNothing().when(commentService).unlikeComment(1L);

        mockMvc.perform(post("/comments/1/unlike"))
                .andExpect(status().isOk())
                .andExpect(content().string("Comment unliked"));
    }

    @Test
    @DisplayName("GET /comments/count/{postId} — should return comment count")
    void getCount_Success() throws Exception {
        when(commentService.getCommentCount(10L)).thenReturn(5L);

        mockMvc.perform(get("/comments/count/10"))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }

    @Test
    @DisplayName("GET /comments/test — should return health check")
    void testEndpoint() throws Exception {
        mockMvc.perform(get("/comments/test"))
                .andExpect(status().isOk())
                .andExpect(content().string("Comment Service is running"));
    }
}
