package org.zemo.omninet.notes.controller;


import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.zemo.omninet.notes.dto.NotesDto;
import org.zemo.omninet.notes.dto.NotesRequest;
import org.zemo.omninet.notes.dto.NotesResponse;
import org.zemo.omninet.notes.entity.FileDetails;
import org.zemo.omninet.notes.exception.ResourceNotFoundException;
import org.zemo.omninet.notes.service.NotesService;
import org.zemo.omninet.notes.util.CommonUtil;

import java.io.IOException;
import java.util.List;

import static org.zemo.omninet.notes.util.Constants.DEFAULT_PAGE_NO;
import static org.zemo.omninet.notes.util.Constants.DEFAULT_PAGE_SIZE;


@Tag(name = "Notes Related", description = "All the Notes  APIs")
@RestController
@RequestMapping("/api/v1/notes")
public class NotesController {


    @Autowired
    private NotesService notesService;

    // save or update
    @PostMapping(value = "/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ROLE_USER','ADMIN')")
    public ResponseEntity<?> saveNotes(@RequestParam
                                       @Parameter(description = "Json String Notes", required = true, content = @Content(schema = @Schema(implementation = NotesRequest.class)))
                                       String notes, @RequestParam(required = false) MultipartFile file) throws ResourceNotFoundException, IOException {

        Boolean saved = notesService.saveNote(notes, file);
        if (saved) {
            return CommonUtil.createBuildResponseMessage("notes saved Successfully", HttpStatus.CREATED);
        }
        return CommonUtil.createErrorResponseMessage("notes saving Failed", HttpStatus.INTERNAL_SERVER_ERROR);
    }


    // ye admin use ke lye hai
    @GetMapping("/")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllNotes() throws ResourceNotFoundException {

        List<NotesDto> notes = notesService.getAllNotes();

        if (CollectionUtils.isEmpty(notes)) {
            return CommonUtil.createErrorResponseMessage("No notes found", HttpStatus.NOT_FOUND);
        }
        return CommonUtil.createBuildResponse(notes, HttpStatus.OK);
    }


    @GetMapping("/download/{id}")
    @PreAuthorize("hasAnyRole('ROLE_USER','ADMIN')")
    public ResponseEntity<?> downloadFile(@PathVariable Integer id) throws ResourceNotFoundException, IOException {
        FileDetails fileDetails = notesService.getFileDetails(id);
        byte[] downloadFile = notesService.downloadFile(fileDetails);

        HttpHeaders headers = new HttpHeaders();
        String contentType = CommonUtil.getContentType(fileDetails.getOriginalFileName());
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDispositionFormData("attachment", fileDetails.getOriginalFileName());

        return ResponseEntity.ok().headers(headers).body(downloadFile);
    }


    @GetMapping("/user-notes")
    @PreAuthorize("hasAnyRole('ROLE_USER','ADMIN')")
    public ResponseEntity<?> getAllNotesByUser(
            @RequestParam(name = "pageNo", defaultValue = DEFAULT_PAGE_NO) Integer pageNo,
            @RequestParam(name = "pageSize", defaultValue = DEFAULT_PAGE_SIZE) Integer pageSize

    ) throws ResourceNotFoundException {
        // yha pageNo. follow index type rule
        String userID = CommonUtil.getLoggedInUser().getEmail();

        NotesResponse notes = notesService.getAllNotesByUser(userID, pageNo, pageSize);


        return CommonUtil.createBuildResponse(notes, HttpStatus.OK);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ROLE_USER','ADMIN')")
    public ResponseEntity<?> getAllNotesBySearch(
            @RequestParam(name = "keyword", defaultValue = "") String keyword,
            @RequestParam(name = "pageNo", defaultValue = DEFAULT_PAGE_NO) Integer pageNo,
            @RequestParam(name = "pageSize", defaultValue = DEFAULT_PAGE_SIZE) Integer pageSize

    ) throws ResourceNotFoundException {
        // yha pageNo. follow index type rule
        String userID = CommonUtil.getLoggedInUser().getEmail();


        NotesResponse notes = notesService.getAllNotesBySearch(userID, pageNo, pageSize, keyword);

        return CommonUtil.createBuildResponse(notes, HttpStatus.OK);
    }


    @GetMapping("/delete/{id}")
    public ResponseEntity<?> deleteNotes(@PathVariable Integer id) throws ResourceNotFoundException {
        notesService.softDeleteNotes(id);
        return CommonUtil.createBuildResponseMessage("Delete success :)", HttpStatus.OK);

    }


    @GetMapping("/restore/{id}")
    public ResponseEntity<?> restoreNotes(@PathVariable Integer id) throws ResourceNotFoundException {
        notesService.restoreNotes(id);
        return CommonUtil.createBuildResponseMessage("restore success :)", HttpStatus.OK);

    }

    @GetMapping("/recycle-bin")
    public ResponseEntity<?> getUserRecycleNotes() throws ResourceNotFoundException {
        String userID = CommonUtil.getLoggedInUser().getEmail();
        List<NotesDto> notes = notesService.getUserRecycleBInNotes(userID);

        if (CollectionUtils.isEmpty(notes)) {
            return CommonUtil.createErrorResponseMessage("No notes found", HttpStatus.NOT_FOUND);
        }
        // if no notes handle by UI
        return CommonUtil.createBuildResponse(notes, HttpStatus.OK);

    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> hardDeleteNotes(@PathVariable Integer id) throws ResourceNotFoundException {
        notesService.hardDeleteNotes(id);
        return CommonUtil.createBuildResponseMessage("Delete success :)", HttpStatus.OK);

    }

    @DeleteMapping("/delete-recycle")
    public ResponseEntity<?> emptyUserRecycleBin() throws ResourceNotFoundException {
        String userID = CommonUtil.getLoggedInUser().getEmail();
        notesService.emptyRecycleBin(userID);
        return CommonUtil.createBuildResponseMessage("Delete success :)", HttpStatus.OK);
    }


    //add favorite if need -> upcoming features


    @GetMapping("/copy/{id}")
    public ResponseEntity<?> copyNotes(@PathVariable Integer id) throws ResourceNotFoundException {
        boolean copied = notesService.copyNotes(id);
        if (copied) {
            return CommonUtil.createBuildResponseMessage("Copy success :)", HttpStatus.OK);
        }
        return CommonUtil.createErrorResponseMessage("Copy Failed", HttpStatus.INTERNAL_SERVER_ERROR);
    }


}
