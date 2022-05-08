package uk.ac.napier.soc.ssd.coursework.web.rest;

import org.owasp.appsensor.core.*;

import com.codahale.metrics.annotation.Timed;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.owasp.appsensor.core.DetectionPoint;
import org.owasp.appsensor.core.Event;
import org.owasp.appsensor.core.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.Errors;
import uk.ac.napier.soc.ssd.coursework.abac.security.spring.ContextAwarePolicyEnforcement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
//import uk.ac.napier.soc.ssd.coursework.abac.security.spring.ContextAwarePolicyEnforcement;
import uk.ac.napier.soc.ssd.coursework.domain.Enrollment;
import uk.ac.napier.soc.ssd.coursework.domain.validators.RestEventManager;
import uk.ac.napier.soc.ssd.coursework.repository.EnrollmentRepository;
import uk.ac.napier.soc.ssd.coursework.repository.HibernateUtil;
import uk.ac.napier.soc.ssd.coursework.repository.search.EnrollmentSearchRepository;
import uk.ac.napier.soc.ssd.coursework.web.rest.errors.BadRequestAlertException;
import uk.ac.napier.soc.ssd.coursework.web.rest.util.HeaderUtil;
import io.github.jhipster.web.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.elasticsearch.index.query.QueryBuilders.*;

/**
 * REST controller for managing Enrollment.
 */
@RestController
@RequestMapping("/api")
public class EnrollmentResource
{

    private final Logger log = LoggerFactory.getLogger(EnrollmentResource.class);

    private static final String ENTITY_NAME = "enrollment";

    private final EnrollmentRepository enrollmentRepository;

    private final EnrollmentSearchRepository enrollmentSearchRepository;

    @Autowired
    private ContextAwarePolicyEnforcement policy;

    public EnrollmentResource(EnrollmentRepository enrollmentRepository, EnrollmentSearchRepository enrollmentSearchRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.enrollmentSearchRepository = enrollmentSearchRepository;
    }

    /**
     * POST  /enrollments : Create a new enrollment.
     *
     * @param enrollment the enrollment to create
     * @return the ResponseEntity with status 201 (Created) and with body the new enrollment, or with status 400 (Bad Request) if the enrollment has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/enrollments")
    @Timed
    public ResponseEntity<Enrollment> createEnrollment(@RequestBody Enrollment enrollment) throws URISyntaxException {
        log.debug("REST request to save Enrollment : {}", enrollment);

        //access check
        policy.checkPermission(enrollment, "CREATE_ENROLMENT");

        if (enrollment.getId() != null) {
            throw new BadRequestAlertException("A new enrollment cannot already have an ID", ENTITY_NAME, "idexists");
        }

        Enrollment result = enrollmentRepository.save(enrollment);

        return ResponseEntity.created(new URI("/api/enrollments/" + result.getId()))
                             .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
                             .body(result);
    }

    /**
     * PUT  /enrollments : Updates an existing enrollment.
     *
     * @param enrollment the enrollment to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated enrollment,
     * or with status 400 (Bad Request) if the enrollment is not valid,
     * or with status 500 (Internal Server Error) if the enrollment couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/enrollments")
    @Timed
    public ResponseEntity<Enrollment> updateEnrollment(@RequestBody Enrollment enrollment) throws URISyntaxException {
        log.debug("REST request to update Enrollment : {}", enrollment);
        policy.checkPermission(enrollment, "UPDATE_ENROLMENT");

        if (enrollment.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        Enrollment result = enrollmentRepository.save(enrollment);

        return ResponseEntity.ok()
                             .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, enrollment.getId().toString()))
                             .body(result);
    }

    /**
     * GET  /enrollments : get all the enrollments.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many)
     * @return the ResponseEntity with status 200 (OK) and the list of enrollments in body
     */
    @GetMapping("/enrollments")
    @Timed
    public List<Enrollment> getAllEnrollments(@RequestParam(required = false, defaultValue = "false") boolean eagerload) {
        log.debug("REST request to get all Enrollments");
        //dirty but it wants a resource and there is none to give
        policy.checkPermission(false, "GET_ENROLMENTS");

        List<Enrollment> enrollments = enrollmentRepository.findAllWithEagerRelationships();

        return enrollments;
    }

    /**
     * GET  /enrollments/:id : get the "id" enrollment.
     *
     * @param id the id of the enrollment to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the enrollment, or with status 404 (Not Found)
     */
    @GetMapping("/enrollments/{id}")
    @Timed
    public ResponseEntity<Enrollment> getEnrollment(@PathVariable Long id) {
        log.debug("REST request to get Enrollment : {}", id);

        Optional<Enrollment> enrollment = enrollmentRepository.findOneWithEagerRelationships(id);

        //access check
        policy.checkPermission(enrollment.get(), "GET_ENROLMENT");

        return ResponseUtil.wrapOrNotFound(enrollment);
    }

    /**
     * DELETE  /enrollments/:id : delete the "id" enrollment.
     *
     * @param id the id of the enrollment to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/enrollments/{id}")
    @Timed
    public ResponseEntity<Void> deleteEnrollment(@PathVariable Long id) {
        log.debug("REST request to delete Enrollment : {}", id);
        policy.checkPermission(id, "DELETE_ENROLMENT");

        enrollmentRepository.deleteById(id);
        enrollmentSearchRepository.deleteById(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }

    /**
     * SEARCH  /_search/enrollments?query=:query : search for the enrollment corresponding
     * to the query.
     *
     * @param query the query of the enrollment search
     * @return the result of the search
     */
    @GetMapping("/_search/enrollments")
    @Timed
    public List<Enrollment> searchEnrollments(@RequestParam String query) {
        log.debug("REST request to search Enrollments for query {}", query);
        policy.checkPermission(query, "SEARCH_ENROLMENTS");

        Session session = HibernateUtil.getSession();
        Query q = session.createQuery("select enrollment from Enrollment enrollment where enrollment.comments like :comment");
        q.setParameter("comment", query);

        List<Enrollment> enrollments = q.getResultList();

        return q.getResultList();
    }

    public void validate(Object target, Errors errors) {

        List<Enrollment> enrollments = (List<Enrollment>) target;

        //proof of concept, 500 would be an awful lot of enrolments but then again these are meant to be edge cases
//        if (enrollments != null && enrollments.size() > 500) {
        //for testing
        if (true) {
            signalSQLInjectionOverread();
            errors.rejectValue("enrollments.size()", "sqlInjection.attempt", "You tried fetching too much data! You stop that.!");
        }

    }

    private RestEventManager eventManager;

    private void signalSQLInjectionOverread() {
        User user = new User(getUserName());

        DetectionPoint detectionPoint = new DetectionPoint(DetectionPoint.Category.INPUT_VALIDATION, "CIE3");
        eventManager = new RestEventManager();
        eventManager.addEvent(new Event(user, detectionPoint, getDetectionSystem()));
    }

    private String getUserName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userName = authentication.getName();

        // overwrite if we can be more specific
        if (authentication instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication;

            userName = userDetails.getUsername();
        }

        return userName;
    }

    private DetectionSystem getDetectionSystem() {
        return new DetectionSystem("myclientapp");
    }
}
