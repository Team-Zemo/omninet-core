package org.zemo.omninet.notes.service.serviceImpl;


import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.zemo.omninet.notes.dto.EmailRequest;
import org.zemo.omninet.notes.entity.Todo;
import org.zemo.omninet.notes.enums.TodoStatus;
import org.zemo.omninet.notes.repository.TodoRepo;
import org.zemo.omninet.security.repository.UserRepository;
import org.zemo.omninet.security.model.User;

import java.io.UnsupportedEncodingException;
import java.util.List;

@Service
public class TodoNotificationService {

    // help by copilot for efficient

    @Autowired
    private TodoRepo todoRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private NotesEmailService notesEmailService;


    //Runs every day at 8 AM. ->0 0 8 * * ?

    // 1 min ->  testing ->0 * * * * ?


    @Scheduled(cron = "0 0 8 * * ?")
    public void notifyIncompleteTodos() {
        // Find all todos that are NOT_COMPLETED
        List<Todo> incompleteTodos = todoRepo.findByStatusIdIn(List.of(
                TodoStatus.NOT_STARTED.getId(),
                TodoStatus.IN_PROGRESS.getId()
        ));

        // Group by user and send reminders
        incompleteTodos.stream()
                .collect(java.util.stream.Collectors.groupingBy(Todo::getCreatedBy))
                .forEach((userId, todos) -> {
                    User user = userRepo.findByEmail(userId).orElse(null);
                    if (user != null && user.getEmail() != null && !todos.isEmpty()) {
                        StringBuilder todoListHtml = new StringBuilder();
                        for (Todo todo : todos) {
                            todoListHtml.append("<li><b>").append(todo.getTitle()).append("</b></li>");
                        }

                        String message = String.format("""
                                <!DOCTYPE html>
                                <html>
                                <head>
                                  <meta charset='UTF-8'>
                                  <meta name='viewport' content='width=device-width, initial-scale=1.0'>
                                  <style>
                                    body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin:0; padding:0; }
                                    .container { max-width: 600px; margin: 24px auto; background:#ffffff; padding: 20px; border-radius: 10px; box-shadow: 0 4px 15px rgba(0,0,0,0.08); }
                                    .header { text-align: center; padding-bottom: 16px; border-bottom: 1px solid #eee; }
                                    .header h1 { color: #4CAF50; margin:0; font-size: 22px; }
                                    .sub { margin:6px 0 0; color:#888; font-size:12px; }
                                    .content { padding: 18px 0; font-size: 16px; color: #333; line-height: 1.55; }
                                    .list { margin: 12px 0; padding-left: 20px; }
                                    .footer { margin-top: 18px; font-size: 12px; color: #777; text-align:center; border-top:1px solid #eee; padding-top: 12px; }
                                  </style>
                                </head>
                                <body>
                                  <div class='container'>
                                    <div class='header'>
                                      <h1>Incomplete Todo Reminder</h1>
                                      <p class='sub'>Team Zemo | OmniNet</p>
                                    </div>
                                    <div class='content'>
                                      <p>Hi <b>%s</b>,</p>
                                      <p>You have the following incomplete tasks in your <b>OmniNet</b> account:</p>
                                      <ul class='list'>%s</ul>
                                      <p>Please complete them as soon as possible.</p>
                                    </div>
                                    <div class='footer'>
                                      <p>Thanks,<br><b>Team Zemo — OmniNet</b></p>
                                    </div>
                                  </div>
                                </body>
                                </html>
                                """, user.getName(), todoListHtml);


                        EmailRequest emailRequest = EmailRequest.builder()
                                .to(user.getEmail())
                                .subject("Todo Reminder: Incomplete Tasks")
                                .title("NoteNestor Team")
                                .message(message)
                                .build();

                        try {
                            notesEmailService.sendEmail(emailRequest);
                        } catch (MessagingException | UnsupportedEncodingException e) {
                            // Log error
                            e.printStackTrace();
                        }
                    }
                });
    }
}
