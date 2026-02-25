package com.example.demo;

import io.micrometer.observation.annotation.Observed;
import org.springframework.data.jpa.repository.JpaRepository;

@Observed(name = "issue.47969.repository")
public interface Issue47969NoteRepository extends JpaRepository<Issue47969Note, Long> {
}
