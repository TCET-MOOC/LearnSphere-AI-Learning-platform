package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Dto.AffiliationRequestResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.CollegeResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.AffiliationRequest;
import com.MOOC.OnlineLearningPlatfrom.Entity.College;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.AffiliationRequestRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.CollegeRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserRoleRepository;
import com.MOOC.OnlineLearningPlatfrom.Service.AdminService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminServiceImpl implements AdminService {

    private final CollegeRepository collegeRepository;
    private final UserRoleRepository userRoleRepository;
    private final AffiliationRequestRepository affiliationRequestRepository;

    public AdminServiceImpl(CollegeRepository collegeRepository,
                             UserRoleRepository userRoleRepository,
                             AffiliationRequestRepository affiliationRequestRepository) {
        this.collegeRepository = collegeRepository;
        this.userRoleRepository = userRoleRepository;
        this.affiliationRequestRepository = affiliationRequestRepository;
    }

    @Override
    public List<CollegeResponseDto> getAllColleges() {
        return collegeRepository.findAll().stream().map(this::toDto).toList();
    }

    @Override
    public CollegeResponseDto createCollege(College college) {
        college.setVerificationStatus(College.VerificationStatus.PENDING);
        College saved = collegeRepository.save(college);
        return toDto(saved);
    }

    @Override
    @Transactional
    public void verifyCollege(Long collegeId) {
        College college = collegeRepository.findById(collegeId)
                .orElseThrow(() -> new ResourceNotFoundException("College not found with id: " + collegeId));
        college.setVerificationStatus(College.VerificationStatus.VERIFIED);
        collegeRepository.save(college);
    }

    @Override
    @Transactional
    public void rejectCollege(Long collegeId) {
        College college = collegeRepository.findById(collegeId)
                .orElseThrow(() -> new ResourceNotFoundException("College not found with id: " + collegeId));
        collegeRepository.delete(college);
    }

    @Override
    public List<AffiliationRequestResponseDto> getAffiliationRequests() {
        return affiliationRequestRepository.findAll().stream()
                .filter(r -> r.getStatus() == AffiliationRequest.Status.PENDING)
                .map(AffiliationRequestResponseDto::from)
                .toList();
    }

    @Override
    @Transactional
    public void approveAffiliation(Long requestId) {
        AffiliationRequest request = affiliationRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Affiliation request not found with id: " + requestId));
        request.setStatus(AffiliationRequest.Status.APPROVED);
        UserAccount teacher = request.getTeacher();
        teacher.setCollege(request.getClaimedCollege());
        affiliationRequestRepository.save(request);
    }

    @Override
    @Transactional
    public void rejectAffiliation(Long requestId) {
        AffiliationRequest request = affiliationRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Affiliation request not found with id: " + requestId));
        request.setStatus(AffiliationRequest.Status.REJECTED);
        affiliationRequestRepository.save(request);
    }

    private CollegeResponseDto toDto(College college) {
        long students = userRoleRepository.countByRole_NameAndUser_College_Id("STUDENT", college.getId());
        long teachers = userRoleRepository.countByRole_NameAndUser_College_Id("TEACHER", college.getId());
        // Course domain is wired separately; course counts default to 0 until CourseRepository is injected here.
        long courses = 0;
        return CollegeResponseDto.from(college, students, teachers, courses);
    }
}
