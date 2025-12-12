package com.example.employeemanagement.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.employeemanagement.model.Announcement;
import com.example.employeemanagement.model.AnnouncementAudience;

public interface AnnouncementRepository extends MongoRepository<Announcement, String> {
	List<Announcement> findByAudienceInOrderByPinnedDescCreatedAtDesc(List<AnnouncementAudience> audiences);
}

