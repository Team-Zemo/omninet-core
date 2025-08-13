package org.zemo.omninet.notes.service;


import org.springframework.web.multipart.MultipartFile;
import org.zemo.omninet.notes.dto.NotesDto;
import org.zemo.omninet.notes.dto.NotesResponse;
import org.zemo.omninet.notes.entity.FileDetails;
import org.zemo.omninet.notes.exception.ResourceNotFoundException;

import java.io.IOException;
import java.util.List;


public interface NotesService {


    Boolean saveNote(String Notes, MultipartFile file) throws ResourceNotFoundException, IOException;

    List<NotesDto> getAllNotes();


    byte[] downloadFile(FileDetails fileDetails) throws ResourceNotFoundException, IOException;

    FileDetails getFileDetails(Integer id) throws ResourceNotFoundException;

    NotesResponse getAllNotesByUser(String userID, Integer pageNo, Integer pageSize);


    NotesResponse getAllNotesBySearch(String userID, Integer pageNo, Integer pageSize, String keyword);

    void softDeleteNotes(Integer id) throws ResourceNotFoundException;

    void restoreNotes(Integer id) throws ResourceNotFoundException;

    List<NotesDto> getUserRecycleBInNotes(String userID);

    void hardDeleteNotes(Integer id) throws ResourceNotFoundException;

    void emptyRecycleBin(String userID);

    boolean copyNotes(Integer id) throws ResourceNotFoundException;


}
