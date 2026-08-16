package com.MOOC.OnlineLearningPlatfrom.Service;

import com.MOOC.OnlineLearningPlatfrom.Dto.CertificateResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.Certificate;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;

import java.util.List;

public interface CertificateService {
    List<CertificateResponseDto> getCertificatesForUser(Long userId);
    CertificateResponseDto issueCertificate(UserAccount user, Long courseId, Certificate.Type type);
}
