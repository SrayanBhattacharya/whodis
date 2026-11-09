package com.whodis.backend.photoface.service;

import com.whodis.backend.photoface.entity.PhotoFace;
import com.whodis.backend.photoface.repository.PhotoFaceRepository;
import com.whodis.backend.processingjob.service.ProcessingJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PhotoFacePersistenceService {

    private final PhotoFaceRepository photoFaceRepository;
    private final ProcessingJobService processingJobService;

    @Transactional
    public void saveFacesAndCompleteJob(
            UUID jobId,
            UUID photoId,
            List<PhotoFace> faces
    ) {
        photoFaceRepository.deleteAllByPhotoId(photoId);
        photoFaceRepository.saveAll(faces);
        processingJobService.completeJob(jobId);
    }
}