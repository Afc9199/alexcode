package com.example.employeemanagement.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.employeemanagement.dto.AnnouncementRequest;
import com.example.employeemanagement.dto.AnnouncementResponse;
import com.example.employeemanagement.model.Announcement;
import com.example.employeemanagement.model.AnnouncementAudience;
import com.example.employeemanagement.repository.AnnouncementRepository;

@Service
public class AnnouncementService {

	private final AnnouncementRepository announcementRepository;

	public AnnouncementService(AnnouncementRepository announcementRepository) {
		this.announcementRepository = announcementRepository;
	}

	public AnnouncementResponse upsertAnnouncement(AnnouncementRequest request) {
		Announcement announcement = fetchAnnouncement(request.announcementId());
		announcement.setTitle(request.title());
		announcement.setMessage(request.message());
		announcement.setAudience(request.audience());
		announcement.setCreatedBy(request.createdBy());
		announcement.setExpiresOn(request.expiresOn());
		announcement.setPinned(request.pinned());
		return toResponse(announcementRepository.save(announcement));
	}

	public List<AnnouncementResponse> listForAudience(AnnouncementAudience audience) {
		LocalDate today = LocalDate.now();
		List<AnnouncementAudience> audiences = audience == AnnouncementAudience.ALL
				? List.of(AnnouncementAudience.ALL, AnnouncementAudience.EMPLOYEE, AnnouncementAudience.ADMIN)
				: List.of(AnnouncementAudience.ALL, audience);
		return announcementRepository.findByAudienceInOrderByPinnedDescCreatedAtDesc(audiences).stream()
				.filter(item -> item.getExpiresOn() == null || !item.getExpiresOn().isBefore(today))
				.map(this::toResponse).toList();
	}

	public List<AnnouncementResponse> listAll() {
		return announcementRepository.findAll().stream().map(this::toResponse).toList();
	}

	private Announcement fetchAnnouncement(String announcementId) {
		if (announcementId == null || announcementId.isBlank()) {
			return new Announcement();
		}
		return announcementRepository.findById(announcementId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Announcement not found"));
	}

	public void deleteAnnouncement(String id) {
		if (id == null || id.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Announcement ID is required");
		}
		if (!announcementRepository.existsById(id)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Announcement not found");
		}
		announcementRepository.deleteById(id);
	}

	private AnnouncementResponse toResponse(Announcement announcement) {
		return new AnnouncementResponse(announcement.getId(), announcement.getTitle(), announcement.getMessage(),
				announcement.getAudience(), announcement.getCreatedBy(), announcement.getCreatedAt(),
				announcement.getExpiresOn(), announcement.isPinned());
	}
}

