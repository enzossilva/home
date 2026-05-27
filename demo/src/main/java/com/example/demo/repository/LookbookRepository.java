package com.example.demo.repository;

import com.example.demo.model.LookbookItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LookbookRepository extends JpaRepository<LookbookItem, Long> {
    List<LookbookItem> findAllByOrderByOrdemAscIdAsc();
}
