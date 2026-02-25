package com.example.demo;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "issue_47969_note")
public class Issue47969Note {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String title;

	protected Issue47969Note() {
	}

	public Issue47969Note(String title) {
		this.title = title;
	}

	public Long getId() {
		return this.id;
	}

	public String getTitle() {
		return this.title;
	}

}
