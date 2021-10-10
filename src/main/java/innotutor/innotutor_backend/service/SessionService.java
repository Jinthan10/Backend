package innotutor.innotutor_backend.service;

import innotutor.innotutor_backend.dto.UserDTO;
import innotutor.innotutor_backend.dto.card.CardDTO;
import innotutor.innotutor_backend.dto.card.SubjectDTO;
import innotutor.innotutor_backend.dto.enrollment.EnrollmentDTO;
import innotutor.innotutor_backend.dto.session.SessionDTO;
import innotutor.innotutor_backend.dto.session.sessionsettings.SessionFormatDTO;
import innotutor.innotutor_backend.dto.session.sessionsettings.SessionTypeDTO;
import innotutor.innotutor_backend.entity.card.Card;
import innotutor.innotutor_backend.entity.session.Session;
import innotutor.innotutor_backend.entity.session.SessionFormat;
import innotutor.innotutor_backend.entity.session.SessionType;
import innotutor.innotutor_backend.entity.session.Subject;
import innotutor.innotutor_backend.entity.user.SessionStudent;
import innotutor.innotutor_backend.entity.user.User;
import innotutor.innotutor_backend.repository.card.CardRepository;
import innotutor.innotutor_backend.repository.session.SessionFormatRepository;
import innotutor.innotutor_backend.repository.session.SessionRepository;
import innotutor.innotutor_backend.repository.session.SessionTypeRepository;
import innotutor.innotutor_backend.repository.session.SubjectRepository;
import innotutor.innotutor_backend.repository.user.SessionStudentRepository;
import innotutor.innotutor_backend.repository.user.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class SessionService {
    private final static String PRIVATE_TYPE = "private";
    private final CardsListService cardsListService;
    private final UserService userService;
    private final SessionRepository sessionRepository;
    private final SessionFormatRepository sessionFormatRepository;
    private final SessionTypeRepository sessionTypeRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final SessionStudentRepository sessionStudentRepository;
    private final CardRepository cardRepository;
    private final StudentsService studentsService;

    public List<SessionFormatDTO> getSessionFormats() {
        final List<SessionFormatDTO> sessionFormats = new ArrayList<>();
        for (final SessionFormat format : sessionFormatRepository.findAll()) {
            sessionFormats.add(new SessionFormatDTO(format.getSessionFormatId(), format.getName()));
        }
        return sessionFormats;
    }

    public List<SessionTypeDTO> getSessionTypes() {
        final List<SessionTypeDTO> sessionTypes = new ArrayList<>();
        for (final SessionType type : sessionTypeRepository.findAll()) {
            sessionTypes.add(new SessionTypeDTO(type.getSessionTypeId(), type.getName()));
        }
        return sessionTypes;
    }

    public List<SubjectDTO> getSubjects() {
        final List<SubjectDTO> subjects = new ArrayList<>();
        for (final Subject subject : subjectRepository.findAll()) {
            subjects.add(new SubjectDTO(subject.getSubjectId(), subject.getName()));
        }
        return subjects;
    }

    public List<SubjectDTO> getAvailableServiceSubjects(final Long userId) { //NOPMD - suppressed ReturnEmptyCollectionRatherThanNull - null indicates that such collection doesn't exist
        if (userRepository.findById(userId).isPresent()) {
            return this.getAvailableSubjects(cardsListService.getServices(userId));
        }
        return null;
    }

    public List<SubjectDTO> getAvailableRequestSubjects(final Long userId) { //NOPMD - suppressed ReturnEmptyCollectionRatherThanNull - null indicates that such collection doesn't exist
        if (userRepository.findById(userId).isPresent()) {
            return this.getAvailableSubjects(cardsListService.getRequests(userId));
        }
        return null;
    }

    public SessionDTO postSession(final SessionDTO sessionDTO) {
        final SessionFormat sessionFormat = sessionFormatRepository.findSessionFormatByName(sessionDTO.getSessionFormat());
        final SessionType sessionType = sessionTypeRepository.findSessionTypeByName(sessionDTO.getSessionType());
        final Optional<User> userOptional = userRepository.findById(sessionDTO.getTutorId());
        if (userOptional.isPresent() && sessionFormat != null && sessionType != null) {
            final User tutor = userOptional.get();
            final Optional<Card> cardOptional = tutor.getServicesByUserId().stream()
                    .map(innotutor.innotutor_backend.entity.user.Service::getCardByCardId)
                    .filter(card -> card.getSubjectBySubjectId().getName().equals(sessionDTO.getSubject()))
                    .findAny();
            if (cardOptional.isPresent()) {
                return this.createSession(tutor, cardOptional.get(), sessionDTO, sessionFormat, sessionType);
            }
        }
        return null;
    }

    public List<UserDTO> filterStudentsForSession(final Long tutorId,
                                                  final String specifiedSubject,
                                                  final String specifiedFormat,
                                                  final String specifiedType) {
        List<EnrollmentDTO> students = studentsService.getUserStudentsList(tutorId).getAcceptedStudents();
        if (specifiedSubject != null) {
            students = students.stream()
                    .filter(enrollmentDTO -> cardRepository.findById(enrollmentDTO.getCardId()).orElse(new Card())
                            .getSubjectBySubjectId().getName().equals(specifiedSubject))
                    .collect(Collectors.toList());
        }
        if (specifiedFormat != null) {
            students = students.stream()
                    .filter(enrollmentDTO -> enrollmentDTO.getSessionFormat().contains(specifiedFormat))
                    .collect(Collectors.toList());
        }
        if (specifiedType != null) {
            students = students.stream()
                    .filter(enrollmentDTO -> enrollmentDTO.getSessionType().contains(specifiedType))
                    .collect(Collectors.toList());
        }
        final List<UserDTO> result = new ArrayList<>();
        students.forEach(student -> result.add(userService.getUserById(student.getEnrollerId())));
        return result;
    }

    private SessionDTO createSession(final User tutor, final Card card, final SessionDTO sessionDTO,
                                     final SessionFormat sessionFormat, final SessionType sessionType) {
        final List<User> students = this.getValidStudents(tutor.getUserId(), sessionDTO.getStudentIDsList(),
                card.getSubjectBySubjectId().getName(), sessionFormat.getName(), sessionType.getName());
        if (!students.isEmpty()) {
            if (PRIVATE_TYPE.equals(sessionType.getName()) && students.size() > 1) {
                return null;
            }
            final Session session = sessionRepository.save(
                    new Session(tutor.getUserId(),
                            card.getSubjectId(),
                            sessionFormat.getSessionFormatId(),
                            sessionType.getSessionTypeId(),
                            sessionDTO.getDate(),
                            sessionDTO.getStartTime(),
                            sessionDTO.getEndTime(),
                            sessionDTO.getDescription(),
                            tutor,
                            card.getSubjectBySubjectId(),
                            sessionFormat,
                            sessionType));
            this.saveSessionStudentList(session, students);
            final List<Long> studentIDsList = new ArrayList<>();
            students.forEach(student -> studentIDsList.add(student.getUserId()));
            return new SessionDTO(session.getSessionId(),
                    tutor.getUserId(),
                    studentIDsList,
                    card.getSubjectBySubjectId().getName(),
                    session.getDate(),
                    session.getStartTime(),
                    session.getEndTime(),
                    sessionFormat.getName(),
                    sessionType.getName(),
                    session.getDescription());
        }
        return null;
    }

    private List<User> getValidStudents(final Long tutorId, final List<Long> studentIDsList, final String subject, final String sessionFormat,
                                        final String sessionType) {
        final List<User> validStudents = new ArrayList<>();
        final List<User> students = new ArrayList<>();
        studentIDsList.forEach(studentId -> userRepository.findById(studentId).ifPresent(students::add));
        for (final User student : students) {
            if (this.filterStudentsForSession(tutorId, subject, sessionFormat, sessionType)
                    .stream().anyMatch(studentDTO -> studentDTO.getUserId().equals(student.getUserId()))) {
                validStudents.add(student);
            }
        }
        return validStudents;
    }

    private void saveSessionStudentList(final Session session, final List<User> students) {
        final List<SessionStudent> sessionStudentList = new ArrayList<>();
        students.forEach(student -> sessionStudentList.add(new SessionStudent(session.getSessionId(), student.getUserId(),
                session, student)));
        sessionStudentRepository.saveAll(sessionStudentList);
    }

    private List<SubjectDTO> getAvailableSubjects(final List<CardDTO> userCards) {
        final List<SubjectDTO> result = new ArrayList<>();
        for (final SubjectDTO subject : this.getSubjects()) {
            boolean available = true; //NOPMD - suppressed DataflowAnomalyAnalysis
            for (final CardDTO card : userCards) {
                if (card.getSubject().equals(subject.getName())) {
                    available = false;
                    break;
                }
            }
            if (available) {
                result.add(subject);
            }
        }
        return result;
    }
}
