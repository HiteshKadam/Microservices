package com.casestudy.user.service;

import com.casestudy.user.repositories.CourseRepository;
import com.casestudy.user.repositories.StudentRepository;
import com.casestudy.user.entity.Course;
import com.casestudy.user.entity.Student;
import com.casestudy.user.model.CourseEnroll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;

@Service
public class StudentService {

    Logger logger = LoggerFactory.getLogger(StudentService.class);

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private static final String topic = "student_topic";
    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    public Student registerStudent(Student student) {
        Optional<Student> studentDetailsRepo = studentRepository.findByUsernameAndPassword(student.getUsername(),student.getPassword());
        if(studentDetailsRepo.isPresent()){
            return null;
        }
        else {
            return studentRepository.save(student);
        }
    }

    public Optional<Student> login(String username, String password) {
        return studentRepository.findByUsernameAndPassword(username,password);
    }

    public Student enrollment(CourseEnroll courseEnroll) {
        Student studentRepo = studentRepository.findByStudentUuid(courseEnroll.getStudentUuid());
        Course courseRepo = courseRepository.findByCourseUuid(courseEnroll.getCourseUuid());
        if (Objects.nonNull(studentRepo.getCourses()) && Objects.nonNull(courseRepo)) {
            ArrayList<Course> courses = new ArrayList<>();
            for(Course course: studentRepo.getCourses()) {
                Course repoCourse = courseRepository.findByCourseUuid(course.getCourseUuid());
                courses.add(repoCourse);
            }
            courses.add(courseRepo);
            Message<Course> message = MessageBuilder
                    .withPayload(courseRepo)
                    .setHeader(KafkaHeaders.TOPIC,topic).build();
            kafkaTemplate.send(message);
            studentRepo.setCourses(new HashSet<>(courses.stream().toList()));
        }
        return studentRepository.save(studentRepo);
    }

}