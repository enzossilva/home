package com.example.demo.controller;

import com.example.demo.config.AuthHelper;
import com.example.demo.dto.ApiResponse;
import com.example.demo.model.Video;
import com.example.demo.repository.VideoRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/videos")
public class VideoController {
    private static final Logger logger = LoggerFactory.getLogger(VideoController.class);

    private final VideoRepository repo;
    private final AuthHelper authHelper;

    public VideoController(VideoRepository repo, AuthHelper authHelper) {
        this.repo = repo;
        this.authHelper = authHelper;
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        logger.info("Listing videos");
        List<Video> videos = repo.findAllByOrderByOrdemAscIdAsc();
        return ResponseEntity.ok(ApiResponse.success(videos));
    }

    @PostMapping
    public ResponseEntity<?> add(@RequestBody Video video, HttpServletRequest request) {
        authHelper.requireAdmin(request);
        logger.info("Adding video: {}", video.getYoutubeUrl());
        Video saved = repo.save(video);
        return ResponseEntity.status(201).body(ApiResponse.success(saved, "Vídeo adicionado com sucesso"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpServletRequest request) {
        authHelper.requireAdmin(request);
        logger.info("Deleting video: {}", id);

        repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vídeo não encontrado"));

        repo.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Vídeo removido com sucesso"));
    }
}
