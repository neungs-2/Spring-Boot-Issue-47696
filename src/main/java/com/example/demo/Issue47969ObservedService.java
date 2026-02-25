package com.example.demo;

import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Service
public class Issue47969ObservedService {

	@Observed(name = "issue.47969.service")
	public void invoke() {
	}

}
