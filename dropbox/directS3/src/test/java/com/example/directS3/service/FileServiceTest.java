package com.example.directS3.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.example.directS3.model.FileMetadata;
import com.example.directS3.model.FileStatus;
import com.example.directS3.repository.FileMetadataRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    FileMetadataRepository repository;

    @Mock
    S3Presigner presigner;

    @InjectMocks
    FileService fileService;

    @Test
    void markAsAvailable_createsMetadataWhenMissing() {
        String key = "missing-key";
        when(repository.findFirstByS3Key(key)).thenReturn(Optional.empty());

        fileService.markAsAvailable(key);

        ArgumentCaptor<FileMetadata> cap = ArgumentCaptor.forClass(FileMetadata.class);
        verify(repository).save(cap.capture());
        FileMetadata saved = cap.getValue();
        assert saved.getS3Key().equals(key);
        assert saved.getStatus() == FileStatus.AVAILABLE;
    }

    @Test
    void markAsAvailable_updatesExisting() {
        String key = "existing-key";
        FileMetadata meta = new FileMetadata("name", 10L, key, FileStatus.PENDING);
        when(repository.findFirstByS3Key(key)).thenReturn(Optional.of(meta));

        fileService.markAsAvailable(key);

        ArgumentCaptor<FileMetadata> cap = ArgumentCaptor.forClass(FileMetadata.class);
        verify(repository).save(cap.capture());
        FileMetadata saved = cap.getValue();
        assert saved.getS3Key().equals(key);
        assert saved.getStatus() == FileStatus.AVAILABLE;
    }

    @Test
    void getDownloadUrl_throwsWhenNotAvailable() {
        var id = java.util.UUID.randomUUID();
        FileMetadata meta = new FileMetadata("n", 0L, "k", FileStatus.PENDING);
        when(repository.findById(id)).thenReturn(Optional.of(meta));

        assertThatThrownBy(() -> fileService.getDownloadUrl(id)).isInstanceOf(RuntimeException.class);
    }
}
