package com.secure_share.services;

import java.io.StringWriter;
import java.io.Writer;
import java.time.LocalDate;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.secure_share.entities.UserEntity;

import freemarker.template.Configuration;
import freemarker.template.Template;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;
    private final Configuration configuration;

    @Value("${application.frontendUrl}")
    private String frontendUrl;

    public void sendEmail(UserEntity userEvent, String subject, String templateName, String route) {
        log.info("Sending activation mail for user with id : {}", userEvent.getId());

        HashMap<String, Object> map = new HashMap<>();
        Writer out = new StringWriter();

        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);

        try {
            helper.setFrom("vitap.library@gmail.com", "Admin");
            helper.setTo(userEvent.getEmail());
            helper.setSubject(subject);

            Template template = configuration.getTemplate(templateName);

            map.put("userName", userEvent.getFirstName() + " " + userEvent.getLastName());
            map.put("link", String.format("%s/%s/%s", frontendUrl, route, userEvent.getToken()));
            map.put("currentYear", String.format("%d", LocalDate.now().getYear()));

            template.process(map, out);

            helper.setText(out.toString(), true);

            javaMailSender.send(mimeMessage);
            log.info("Successfully sent activation mail for user with id : {}", userEvent.getId());
        } catch (Exception e) {
            log.error(e.getMessage());
        }

    }
}
