package org.zemo.omninet.notes.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.zemo.omninet.notes.entity.FileDetails;

public interface FileRepo extends JpaRepository<FileDetails, Integer> {

}
