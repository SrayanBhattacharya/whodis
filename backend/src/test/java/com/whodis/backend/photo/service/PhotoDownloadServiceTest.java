package com.whodis.backend.photo.service;

import com.whodis.backend.person.entity.Person;
import com.whodis.backend.person.service.PersonNotFoundException;
import com.whodis.backend.person.service.PersonService;
import com.whodis.backend.photo.entity.Photo;
import com.whodis.backend.photo.repository.PhotoRepository;
import com.whodis.backend.session.entity.Session;
import com.whodis.backend.session.entity.SessionStatus;
import com.whodis.backend.session.service.SessionExpiredException;
import com.whodis.backend.session.service.SessionNotFoundException;
import com.whodis.backend.session.service.SessionService;
import com.whodis.backend.storage.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhotoDownloadServiceTest {

    @Mock
    private SessionService sessionService;

    @Mock
    private PersonService personService;

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private StorageService storageService;

    private ZipStreamingService zipStreamingService;
    private PhotoDownloadService photoDownloadService;

    private UUID sessionId;
    private Session session;

    @BeforeEach
    void setUp() {
        zipStreamingService = new ZipStreamingService(storageService);
        photoDownloadService = new PhotoDownloadService(
                sessionService,
                personService,
                photoRepository,
                storageService,
                zipStreamingService
        );

        sessionId = UUID.randomUUID();
        session = new Session(sessionId, Instant.now(), Instant.now().plusSeconds(3600), SessionStatus.ACTIVE);
    }

    private Photo createMockPhoto(UUID photoId, String filename, String storageKey) {
        return new Photo(
                photoId,
                session,
                storageKey,
                filename,
                "image/jpeg",
                1024L,
                Instant.now()
        );
    }

    // --- Individual Download Tests ---

    @Test
    void getSinglePhotoDownload_success() {
        UUID photoId = UUID.randomUUID();
        Photo photo = createMockPhoto(photoId, "family_portrait.jpg", "key_1");
        InputStream mockStream = new ByteArrayInputStream("image-bytes".getBytes(StandardCharsets.UTF_8));

        when(sessionService.getSession(sessionId)).thenReturn(session);
        when(photoRepository.findByIdAndSessionId(photoId, sessionId)).thenReturn(Optional.of(photo));
        when(storageService.load("key_1")).thenReturn(mockStream);

        PhotoDownloadService.PhotoDownload result = photoDownloadService.getSinglePhotoDownload(sessionId, photoId);

        assertThat(result).isNotNull();
        assertThat(result.photo()).isEqualTo(photo);
        assertThat(result.photo().getOriginalFilename()).isEqualTo("family_portrait.jpg");
        assertThat(result.stream()).isSameAs(mockStream);
    }

    @Test
    void getSinglePhotoDownload_photoNotFound_throwsException() {
        UUID photoId = UUID.randomUUID();

        when(sessionService.getSession(sessionId)).thenReturn(session);
        when(photoRepository.findByIdAndSessionId(photoId, sessionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> photoDownloadService.getSinglePhotoDownload(sessionId, photoId))
                .isInstanceOf(PhotoNotFoundException.class)
                .hasMessageContaining(photoId.toString());
    }

    @Test
    void getSinglePhotoDownload_sessionExpired_throwsException() {
        UUID photoId = UUID.randomUUID();

        when(sessionService.getSession(sessionId)).thenThrow(new SessionExpiredException(sessionId));

        assertThatThrownBy(() -> photoDownloadService.getSinglePhotoDownload(sessionId, photoId))
                .isInstanceOf(SessionExpiredException.class);
    }

    @Test
    void getSinglePhotoDownload_sessionNotFound_throwsException() {
        UUID photoId = UUID.randomUUID();

        when(sessionService.getSession(sessionId)).thenThrow(new SessionNotFoundException(sessionId));

        assertThatThrownBy(() -> photoDownloadService.getSinglePhotoDownload(sessionId, photoId))
                .isInstanceOf(SessionNotFoundException.class);
    }

    // --- Bulk Download Tests ---

    @Test
    void getBulkPhotosZip_success_andHandlesDuplicateFilenamesAndDeduplication() throws Exception {
        UUID photoId1 = UUID.randomUUID();
        UUID photoId2 = UUID.randomUUID();
        // Duplicate filenames in different photos
        Photo photo1 = createMockPhoto(photoId1, "vacation.jpg", "key_1");
        Photo photo2 = createMockPhoto(photoId2, "vacation.jpg", "key_2");

        when(sessionService.getSession(sessionId)).thenReturn(session);
        // Pass photoId1 twice in request to verify request deduplication
        when(photoRepository.findAllByIdInAndSessionId(any(), eq(sessionId)))
                .thenReturn(List.of(photo1, photo2));

        when(storageService.load("key_1")).thenAnswer(inv -> new ByteArrayInputStream("photo-1-data".getBytes(StandardCharsets.UTF_8)));
        when(storageService.load("key_2")).thenAnswer(inv -> new ByteArrayInputStream("photo-2-data".getBytes(StandardCharsets.UTF_8)));

        StreamingResponseBody body = photoDownloadService.getBulkPhotosZip(sessionId, List.of(photoId1, photoId2, photoId1));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        body.writeTo(baos);

        // Verify ZIP contents
        Map<String, String> zipEntries = readZipEntries(baos.toByteArray());
        assertThat(zipEntries).hasSize(2);
        assertThat(zipEntries.keySet()).containsExactlyInAnyOrder("vacation.jpg", "vacation_1.jpg");
        assertThat(zipEntries.get("vacation.jpg")).isEqualTo("photo-1-data");
        assertThat(zipEntries.get("vacation_1.jpg")).isEqualTo("photo-2-data");
    }

    @Test
    void getBulkPhotosZip_emptySelection_throwsIllegalArgumentException() {
        when(sessionService.getSession(sessionId)).thenReturn(session);

        assertThatThrownBy(() -> photoDownloadService.getBulkPhotosZip(sessionId, Collections.emptyList()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void getBulkPhotosZip_invalidOrForeignPhotoId_throwsPhotoNotFoundException() {
        UUID validId = UUID.randomUUID();
        UUID missingId = UUID.randomUUID();
        Photo photo1 = createMockPhoto(validId, "valid.jpg", "key_1");

        when(sessionService.getSession(sessionId)).thenReturn(session);
        when(photoRepository.findAllByIdInAndSessionId(any(), eq(sessionId)))
                .thenReturn(List.of(photo1)); // missingId not returned by query

        assertThatThrownBy(() -> photoDownloadService.getBulkPhotosZip(sessionId, List.of(validId, missingId)))
                .isInstanceOf(PhotoNotFoundException.class)
                .hasMessageContaining(missingId.toString());
    }

    // --- Person Photos Download Tests ---

    @Test
    void getPersonPhotosZip_success_distinctPhotosAndSanitizedFilename() throws Exception {
        UUID personId = UUID.randomUUID();
        Person person = new Person(personId, session, "Alice Cooper", Instant.now());

        UUID photoId1 = UUID.randomUUID();
        UUID photoId2 = UUID.randomUUID();
        Photo photo1 = createMockPhoto(photoId1, "concert_1.jpg", "key_1");
        Photo photo2 = createMockPhoto(photoId2, "concert_2.jpg", "key_2");

        when(sessionService.getSession(sessionId)).thenReturn(session);
        when(personService.getPerson(sessionId, personId)).thenReturn(person);
        when(photoRepository.findDistinctPhotosByPersonIdAndSessionId(personId, sessionId))
                .thenReturn(List.of(photo1, photo2));

        when(storageService.load("key_1")).thenAnswer(inv -> new ByteArrayInputStream("alice-1".getBytes(StandardCharsets.UTF_8)));
        when(storageService.load("key_2")).thenAnswer(inv -> new ByteArrayInputStream("alice-2".getBytes(StandardCharsets.UTF_8)));

        PhotoDownloadService.PersonPhotosZip result = photoDownloadService.getPersonPhotosZip(sessionId, personId);

        assertThat(result.filename()).isEqualTo("whodis-alice-cooper-photos.zip");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        result.stream().writeTo(baos);

        Map<String, String> zipEntries = readZipEntries(baos.toByteArray());
        assertThat(zipEntries).hasSize(2);
        assertThat(zipEntries).containsKeys("concert_1.jpg", "concert_2.jpg");
        assertThat(zipEntries.get("concert_1.jpg")).isEqualTo("alice-1");
        assertThat(zipEntries.get("concert_2.jpg")).isEqualTo("alice-2");
    }

    @Test
    void getPersonPhotosZip_personNotFound_throwsPersonNotFoundException() {
        UUID personId = UUID.randomUUID();

        when(sessionService.getSession(sessionId)).thenReturn(session);
        when(personService.getPerson(sessionId, personId)).thenThrow(new PersonNotFoundException(personId));

        assertThatThrownBy(() -> photoDownloadService.getPersonPhotosZip(sessionId, personId))
                .isInstanceOf(PersonNotFoundException.class);
    }

    @Test
    void getPersonPhotosZip_noAssociatedPhotos_throwsNoSuchElementException() {
        UUID personId = UUID.randomUUID();
        Person person = new Person(personId, session, "Bob", Instant.now());

        when(sessionService.getSession(sessionId)).thenReturn(session);
        when(personService.getPerson(sessionId, personId)).thenReturn(person);
        when(photoRepository.findDistinctPhotosByPersonIdAndSessionId(personId, sessionId))
                .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> photoDownloadService.getPersonPhotosZip(sessionId, personId))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("No photos found");
    }

    // --- Helper to parse generated ZIP ---

    private Map<String, String> readZipEntries(byte[] zipBytes) throws Exception {
        Map<String, String> entries = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                ByteArrayOutputStream entryBaos = new ByteArrayOutputStream();
                byte[] buf = new byte[1024];
                int read;
                while ((read = zis.read(buf)) != -1) {
                    entryBaos.write(buf, 0, read);
                }
                entries.put(entry.getName(), entryBaos.toString(StandardCharsets.UTF_8));
                zis.closeEntry();
            }
        }
        return entries;
    }
}
